package com.rpggame.server;

import com.rpggame.entities.Chest;
import com.rpggame.entities.Player;
import com.rpggame.factions.FactionSystem;
import com.rpggame.npcs.NPC;
import com.rpggame.shared.InputPacket;
import com.rpggame.shared.WorldSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Loop de simulação do servidor a tick fixo.
 *
 * Na Fase 1 (in-process, sem rede), o ServerLoop roda na mesma JVM que o
 * cliente mas em uma thread dedicada. Ele itera sobre todos os mapas
 * registrados
 * no {@link WorldState} e executa ticks de background nos mapas sem jogadores
 * conforme o {@link SimulationClock} de cada mapa (4 TPS).
 *
 * O mapa com jogador ativo é atualizado pelo game loop principal (GamePanel)
 * a 60 FPS — o ServerLoop não o toca para evitar double-tick.
 *
 * Nas fases seguintes a lógica de simulação migrará de EnemyManager para
 * {@link MapSimulation} e o ServerLoop passará a ser o único responsável por
 * todos os ticks.
 *
 * Uso:
 * 
 * <pre>
 * serverLoop = new ServerLoop(worldState);
 * serverLoop.start();
 * // ... durante o jogo ...
 * serverLoop.stop();
 * </pre>
 */
public class ServerLoop implements Runnable {

  private static final long ACTIVE_TICK_NANOS = 1_000_000_000L / SimulationClock.ACTIVE_TPS;

  private final WorldState worldState;
  private final WorldSnapshotAssembler snapshotAssembler;

  // Fase 4: transporte via fila (substitui campos volatile de snapshot/input)
  private final InProcessTransport transport;

  private final AtomicLong tickCounter = new AtomicLong(0L);

  private volatile String activeMapId;
  private volatile Player activePlayer;
  private volatile FactionSystem factionSystem;
  private volatile List<NPC> activeNpcs;
  private volatile List<Chest> activeChests;

  // Mantido para compatibilidade com GamePanel.getLatestSnapshot() (in-process)
  private volatile WorldSnapshot latestSnapshot;

  // Fase 5: múltiplos players — mapa de playerId → PlayerSimulation
  private final Map<String, PlayerSimulation> playerSimulations = new ConcurrentHashMap<>();
  // Transporte por cliente — mapa de playerId → InProcessTransport
  private final Map<String, InProcessTransport> clientTransports = new ConcurrentHashMap<>();

  // Compat Fase 3: player-1 direto (mantido para não quebrar GamePanel
  // single-player)
  private volatile PlayerSimulation playerSimulation;
  // pendingInput mantido como fallback; Fase 4 usa transport.drainInputs()
  private volatile InputPacket pendingInput;

  private volatile boolean running = false;
  private Thread thread;

  public ServerLoop(WorldState worldState) {
    this.worldState = worldState;
    this.snapshotAssembler = new WorldSnapshotAssembler();
    this.transport = new InProcessTransport();
  }

  /** Retorna o transporte in-process para uso pelo GameClient/GamePanel. */
  public InProcessTransport getTransport() {
    return transport;
  }

  public void start() {
    if (running)
      return;
    running = true;
    thread = new Thread(this, "ServerLoop");
    thread.setDaemon(true); // não impede o JVM de encerrar
    thread.start();
  }

  public void stop() {
    running = false;
    if (thread != null) {
      thread.interrupt();
    }
  }

  @Override
  public void run() {
    long lastNanos = System.nanoTime();

    while (running) {
      long now = System.nanoTime();
      long deltaNanos = now - lastNanos;
      lastNanos = now;

      try {
        tickAllMaps(deltaNanos);
        publishSnapshot();
      } catch (Exception e) {
        System.err.println("[ServerLoop] Excecao no tick (continuando): " + e);
      }

      // Dorme até o próximo tick do mapa mais rápido (20 TPS)
      long elapsed = System.nanoTime() - now;
      long sleepNanos = ACTIVE_TICK_NANOS - elapsed;
      if (sleepNanos > 0) {
        try {
          long sleepMs = sleepNanos / 1_000_000;
          int sleepNs = (int) (sleepNanos % 1_000_000);
          Thread.sleep(sleepMs, sleepNs);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  /**
   * Avança o clock de cada mapa e executa ticks de background nos mapas sem
   * jogadores. O mapa ativo é atualizado pelo game loop principal (GamePanel)
   * para manter sincronismo com o rendering.
   */
  private final java.util.List<InputPacket> inputDrainBuffer = new java.util.ArrayList<>(8);

  private void tickAllMaps(long deltaNanos) {
    // Fase 4: drenar inputs da fila de transporte
    inputDrainBuffer.clear();
    transport.drainInputs(inputDrainBuffer);
    if (!inputDrainBuffer.isEmpty() && playerSimulation != null) {
      // Aplica apenas o input mais recente para evitar duplo-movimento
      playerSimulation.applyInput(inputDrainBuffer.get(inputDrainBuffer.size() - 1));
    } else {
      // Fallback Fase 3: campo volatile
      InputPacket input = pendingInput;
      if (input != null && playerSimulation != null) {
        playerSimulation.applyInput(input);
      }
    }

    // Modo headless (GameServer dedicado/hosted):
    // processa inputs de todos os clientes TCP e avança update dos players.
    if (activePlayer == null && !clientTransports.isEmpty()) {
      for (Map.Entry<String, InProcessTransport> entry : clientTransports.entrySet()) {
        String playerId = entry.getKey();
        InProcessTransport playerTransport = entry.getValue();
        PlayerSimulation sim = playerSimulations.get(playerId);
        if (sim == null) {
          continue;
        }

        inputDrainBuffer.clear();
        playerTransport.drainInputs(inputDrainBuffer);
        if (!inputDrainBuffer.isEmpty()) {
          sim.applyInput(inputDrainBuffer.get(inputDrainBuffer.size() - 1));
        } else {
          // Sem input novo neste tick: zerar flags de movimento para que o player
          // pare quando o cliente soltar as teclas (keyReleased não chega ao servidor).
          sim.applyInput(InputPacket.builder(playerId, Long.MIN_VALUE).build());
        }

        sim.getPlayer().update();
        System.out.println("[SRV_UPDATE] player=" + playerId
            + " pos=(" + (int)sim.getPlayer().getX() + "," + (int)sim.getPlayer().getY() + ")"
            + " inputs=" + inputDrainBuffer.size());
      }
    }

    for (MapSimulation sim : worldState.getAllSimulations()) {
      // O mapa com jogador ativo é atualizado pelo GamePanel — pular aqui para
      // evitar double-tick e condições de corrida nesta fase (in-process).
      if (sim.hasActivePlayers()) {
        continue;
      }

      int ticks = sim.getClock().advance(deltaNanos);
      for (int i = 0; i < ticks; i++) {
        tickBackground(sim);
      }
    }
  }

  /**
   * Tick de background: atualiza apenas entidades vivas, sem rendering.
   * Nesta fase é um stub — a lógica completa de background virá na Fase 2
   * quando a simulação for extraída do EnemyManager para MapSimulation.
   */
  private void tickBackground(MapSimulation sim) {
    // Remove inimigos mortos do estado persistido para não acumular
    synchronized (sim) {
      sim.getEnemies().removeIf(e -> !e.isAlive());
    }

    // Decrementar timer de respawn de famílias no background
    int familyTimer = sim.getFamilyRespawnTimer();
    if (familyTimer > 0) {
      sim.setFamilyRespawnTimer(familyTimer - 1);
    }
  }

  public boolean isRunning() {
    return running;
  }

  public void updateSnapshotContext(
      String activeMapId,
      Player activePlayer,
      FactionSystem factionSystem,
      List<NPC> activeNpcs,
      List<Chest> activeChests) {
    this.activeMapId = activeMapId;
    this.activePlayer = activePlayer;
    this.factionSystem = factionSystem;
    this.activeNpcs = activeNpcs;
    this.activeChests = activeChests;
  }

  public WorldSnapshot getLatestSnapshot() {
    return latestSnapshot;
  }

  /**
   * Registra o PlayerSimulation de P1 (compat Fase 3).
   */
  public void setPlayerSimulation(PlayerSimulation playerSimulation) {
    this.playerSimulation = playerSimulation;
    playerSimulations.put(playerSimulation.getPlayer().getPlayerId(), playerSimulation);
  }

  /**
   * Fase 5: registra um PlayerSimulation para qualquer playerId.
   * Cria também o InProcessTransport dedicado a esse cliente.
   * Retorna o transporte para que o GameClient/ClientInput possam ser conectados.
   */
  public InProcessTransport registerPlayer(PlayerSimulation sim) {
    String pid = sim.getPlayer().getPlayerId();
    playerSimulations.put(pid, sim);
    InProcessTransport t = new InProcessTransport();
    clientTransports.put(pid, t);
    // Marcar mapa ativo para que tickAllMaps() não pule o processamento headless.
    // O mapa alvo é o activeMapId se já configurado, ou "village" como padrão.
    String mapId = activeMapId != null ? activeMapId : "village";
    if (worldState != null) {
      MapSimulation mapSim = worldState.get(mapId);
      if (mapSim != null) {
        mapSim.setHasActivePlayers(true);
      }
    }
    return t;
  }

  /**
   * Fase 6: retorna o PlayerSimulation existente para um playerId, ou cria um
   * stub
   * novo caso ainda nao exista (usado pelo ServerNetwork no handshake TCP).
   * O stub usa o activePlayer se o playerId for "player-1", caso contrario cria
   * um Player temporario que sera substituido quando o GamePanel registrar o
   * real.
   */
  public PlayerSimulation getOrCreateSimulationForPlayer(String playerId) {
    return getOrCreateSimulationForPlayer(playerId, "Warrior");
  }

  /**
   * Versao com playerClass: garante que o stub do servidor usa a classe correta
   * vinda do handshake TCP, evitando que o snapshot envie "Warrior" para todos
   * os clientes independente da classe escolhida.
   */
  public PlayerSimulation getOrCreateSimulationForPlayer(String playerId, String playerClass) {
    PlayerSimulation existing = playerSimulations.get(playerId);
    if (existing != null) {
      // Atualizar classe se o stub foi criado antes do handshake com playerClass
      if (playerClass != null && !playerClass.isEmpty()) {
        existing.getPlayer().setPlayerClass(playerClass);
        // Reinicializar habilidades para a nova classe e marcá-las como aprendidas
        if (existing.getPlayer().getSkillManager() != null) {
          existing.getPlayer().getSkillManager().reinitializeSkills();
          existing.getPlayer().getSkillManager().learnAllSkills();
        }
      }
      return existing;
    }
    com.rpggame.entities.Player stub = new com.rpggame.entities.Player(558, 217, null);
    stub.setPlayerId(playerId);
    stub.setPlayerClass(playerClass != null ? playerClass : "Warrior");
    // Correção Bug 4: marcar todas as habilidades como aprendidas no stub do servidor.
    // O servidor não rastreia progressão de level, então habilidades nunca seriam
    // desbloqueadas pelo caminho normal, impedindo skills de funcionar em modo rede.
    // Reinicializar skills para garantir que a classe correta (não a default "Warrior")
    // seja usada, já que setPlayerClass não reinicializa o SkillManager.
    if (stub.getSkillManager() != null) {
      stub.getSkillManager().reinitializeSkills();
      stub.getSkillManager().learnAllSkills();
    }
    PlayerSimulation sim = new PlayerSimulation(stub);
    playerSimulations.put(playerId, sim);
    return sim;
  }

  public boolean hasPlayerSimulation(String playerId) {
    return playerSimulations.containsKey(playerId);
  }

  /**
   * Remove um player do servidor (desconexão / game over).
   */
  public void unregisterPlayer(String playerId) {
    playerSimulations.remove(playerId);
    clientTransports.remove(playerId);
  }

  /**
   * Enfileira um InputPacket via transporte (Fase 4) e também atualiza
   * o campo volatile de fallback (Fase 3).
   */
  public void submitInput(InputPacket packet) {
    this.pendingInput = packet; // fallback Fase 3
    transport.publishInput(packet); // Fase 4: fila P1 legado
    // Fase 5: enfileira no transporte dedicado do player SOMENTE no modo headless
    // (activePlayer == null). Em modo in-process o input ja e processado pelo
    // path legado acima — publicar no clientTransports causaria double-apply.
    if (activePlayer == null) {
      InProcessTransport playerTransport = clientTransports.get(packet.getPlayerId());
      if (playerTransport != null) {
        playerTransport.publishInput(packet);
      }
    }
  }

  private void publishSnapshot() {
    String mapId = activeMapId;
    // Se updateSnapshotContext ainda nao foi chamado, tentar derivar o mapId
    // do WorldState (primeiro mapa ativo registrado) para nao suprimir snapshots.
    if (mapId == null) {
      for (MapSimulation sim : worldState.getAllSimulations()) {
        if (sim.hasActivePlayers()) {
          mapId = worldState.getMapIdForSimulation(sim);
          break;
        }
      }
      if (mapId == null) return;
    }

    long tick = tickCounter.incrementAndGet();

    // Coletar todos os players registrados para o snapshot sem duplicatas.
    // Usa um Set de playerId como guarda — mais seguro que comparacao por referencia,
    // que falha se activePlayer e sim.getPlayer() forem objetos distintos com mesmo ID.
    java.util.Set<String> addedIds = new java.util.HashSet<>();
    List<Player> allPlayers = new ArrayList<>(playerSimulations.size() + 1);
    if (activePlayer != null) {
      allPlayers.add(activePlayer);
      addedIds.add(activePlayer.getPlayerId());
    }
    for (PlayerSimulation sim : playerSimulations.values()) {
      Player p = sim.getPlayer();
      if (addedIds.add(p.getPlayerId())) { // add retorna false se ja existia
        allPlayers.add(p);
      }
    }

    WorldSnapshot snapshot = snapshotAssembler.assembleMulti(
        tick,
        mapId,
        worldState,
        allPlayers,
        factionSystem,
        activeNpcs,
        activeChests);

    latestSnapshot = snapshot;

    // Publicar snapshot no transporte padrão (P1 / GamePanel legado)
    transport.publishSnapshot(snapshot);

    // Fase 5: publicar snapshot em cada transporte de cliente registrado
    for (InProcessTransport t : clientTransports.values()) {
      t.publishSnapshot(snapshot);
    }
  }
}
