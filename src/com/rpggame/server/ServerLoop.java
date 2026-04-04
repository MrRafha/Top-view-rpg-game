package com.rpggame.server;

import com.rpggame.entities.Chest;
import com.rpggame.entities.Player;
import com.rpggame.factions.FactionSystem;
import com.rpggame.npcs.NPC;
import com.rpggame.shared.WorldSnapshot;

import java.util.List;
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

  private final AtomicLong tickCounter = new AtomicLong(0L);

  private volatile String activeMapId;
  private volatile Player activePlayer;
  private volatile FactionSystem factionSystem;
  private volatile List<NPC> activeNpcs;
  private volatile List<Chest> activeChests;
  private volatile WorldSnapshot latestSnapshot;

  private volatile boolean running = false;
  private Thread thread;

  public ServerLoop(WorldState worldState) {
    this.worldState = worldState;
    this.snapshotAssembler = new WorldSnapshotAssembler();
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

      tickAllMaps(deltaNanos);
      publishSnapshot();

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
  private void tickAllMaps(long deltaNanos) {
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

  private void publishSnapshot() {
    String mapId = activeMapId;
    if (mapId == null) {
      return;
    }

    long tick = tickCounter.incrementAndGet();
    latestSnapshot = snapshotAssembler.assemble(
        tick,
        mapId,
        worldState,
        activePlayer,
        factionSystem,
        activeNpcs,
        activeChests);
  }
}
