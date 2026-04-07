package com.rpggame.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import com.rpggame.entities.Player;
import com.rpggame.entities.Chest;
import com.rpggame.enemies.mimic.Mimic;
import com.rpggame.npcs.NPC;
import com.rpggame.npcs.MerchantNPC;
import com.rpggame.npcs.GuardNPC;
import com.rpggame.npcs.VillagerNPC;
import com.rpggame.npcs.WiseManNPC;
import com.rpggame.world.*;
import com.rpggame.systems.*;
import com.rpggame.systems.MusicManager;
import com.rpggame.ui.CharacterScreen;
import com.rpggame.ui.DialogBox;
import com.rpggame.ui.SkillSlotUI;
import com.rpggame.ui.InventoryScreen;
import com.rpggame.ui.DeveloperConsole;
import com.rpggame.ui.QuestUI;
import com.rpggame.ui.GoldUI;
import com.rpggame.ui.QuestChoiceBox;
import com.rpggame.ui.ShopUI;
import com.rpggame.ui.WorldMapUI;
import com.rpggame.ui.LockpickingMinigame;
import com.rpggame.factions.FactionSystem;
import com.rpggame.factions.FactionType;
import com.rpggame.factions.TerritoryType;
import com.rpggame.server.InProcessTransport;
import com.rpggame.server.MapSimulation;
import com.rpggame.server.PlayerSimulation;
import com.rpggame.server.ServerLoop;
import com.rpggame.server.WorldState;
import com.rpggame.shared.InputPacket;
import com.rpggame.shared.WorldSnapshot;
import com.rpggame.client.ClientInput;
import com.rpggame.client.ClientNetwork;
import com.rpggame.client.GameClient;
import com.rpggame.render.SnapshotRenderSystem;

/**
 * Painel principal onde o jogo é renderizado
 */
public class GamePanel extends JPanel implements KeyListener, MouseListener, Runnable {
  public static final int TILE_SIZE = 48; // Aumentado para dar zoom
  public static final int MAP_WIDTH = 25; // Mapa maior 25x25 para territórios distantes
  public static final int MAP_HEIGHT = 25; // Novo mapa 15x15

  private Thread gameThread;
  private boolean running = false;

  private Player player;
  private TileMap tileMap;
  private Camera camera;
  private EnemyManager enemyManager;

  // Telas do jogo
  private CharacterScreen characterScreen;
  private boolean showingCharacterScreen = false;
  private DeveloperConsole developerConsole;
  private InventoryScreen inventoryScreen;

  // Sistema de NPCs e diálogos
  private java.util.ArrayList<NPC> npcs;
  private DialogBox dialogBox;
  private NPC currentTalkingNPC = null;
  private boolean showingDialog = false;
  private boolean waitingForQuestChoice = false; // Flag para aguardar escolha S/N
  private MerchantNPC merchantNPC; // Referência para o mercador (para a loja)

  // Sistema de UI de habilidades
  private SkillSlotUI skillSlotUI;

  // Sistema de UI de quests e gold
  private QuestUI questUI;
  private GoldUI goldUI;
  private QuestChoiceBox questChoiceBox;
  private ShopUI shopUI;
  private WorldMapUI worldMapUI;

  // Sistema de baús e minigame
  private java.util.ArrayList<Chest> chests;
  private LockpickingMinigame lockpickingMinigame;
  private boolean playingMinigame = false;
  private Chest currentChest = null;

  // Sistema de mapas e transições
  private MapManager mapManager;
  private MapTransition mapTransition;
  private static final int PORTAL_COOLDOWN_FRAMES = 30;
  private int portalCooldownFrames = 0;
  private boolean portalNeedsClear = false;

  // Sistema de facções
  private FactionSystem factionSystem;

  // Persistência de mundo: estado de todos os mapas sobrevive a trocas de mapa
  private WorldState worldState;
  private ServerLoop serverLoop;

  // Fase 3: separação de input
  private ClientInput clientInput;
  private PlayerSimulation playerSimulation;

  // Fase 4: cliente consome snapshots via fila em vez de acesso direto ao
  // ServerLoop
  private GameClient gameClient;

  // Fase 6/7: transporte TCP configuravel por modo de jogo
  // false = in-process (solo)
  // true = TCP (criar/entrar servidor)
  private final boolean useNetwork;
  private final String networkHost;
  private final int networkPort;
  private ClientNetwork clientNetwork;

  // Fase 5: segundo jogador local
  private Player player2;
  private ClientInput clientInput2;
  private PlayerSimulation playerSimulation2;
  private GameClient gameClient2;
  private boolean player2Enabled = false;

  private volatile WorldSnapshot previousWorldSnapshot;
  private volatile WorldSnapshot latestWorldSnapshot;
  private volatile long latestSnapshotNanos = 0L;

  // Sistema de música
  private MusicManager musicManager;

  // Sistema de morte
  private boolean playerDead = false;
  private boolean deathTransitionStarted = false;
  private boolean showingDeathScreen = false;
  private Rectangle newGameButton;

  // Debug - Visualização de campo de visão
  private boolean showVisionCones = false;

  // FPS
  private final int FPS = 60;
  private final long TARGET_TIME = 1000000000 / FPS;

  // Instrumentacao de performance
  private static final int PERF_SAMPLE_WINDOW = 600;
  private static final long PERF_LOG_INTERVAL_NANOS = 5_000_000_000L;
  private final long[] frameCpuSamples = new long[PERF_SAMPLE_WINDOW];
  private int frameCpuSampleCount = 0;
  private int frameCpuSampleIndex = 0;
  private int lateFrameCount = 0;
  private long fogUpdateNanosAccum = 0;
  private int fogUpdateCount = 0;
  private long lastPerfLogNanos = System.nanoTime();

  public GamePanel() {
    this(false, "127.0.0.1", 7777);
  }

  public GamePanel(boolean useNetwork, String networkHost, int networkPort) {
    this.useNetwork = useNetwork;
    this.networkHost = networkHost;
    this.networkPort = networkPort;

    setPreferredSize(new Dimension(Game.SCREEN_WIDTH, Game.SCREEN_HEIGHT));
    setBackground(Color.BLACK);
    setFocusable(true);
    addKeyListener(this);
    addMouseListener(this);

    // Garantir que use layout null por padrão para renderização custom
    setLayout(null);

    // Ajusta elementos de UI quando a janela muda de tamanho.
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        updateResponsiveUiLayout();
      }
    });

    // Garantir que o painel receba foco
    requestFocusInWindow();

    initializeGame();
    // startGameLoop() será chamado quando setPlayerClass for executado
  }

  private void initializeGame() {
    // Inicializar sistema de mapas primeiro
    mapManager = new MapManager();

    // Inicializar sistema de música
    musicManager = new MusicManager();

    // Criar o mapa de tiles
    tileMap = new TileMap();
    tileMap.setMapManager(mapManager);

    // Carregar o mapa inicial correto baseado no MapManager
    MapManager.MapData initialMap = mapManager.getCurrentMap();
    if (initialMap != null) {
      tileMap.reloadMap(initialMap.getFilePath(), mapManager.getCurrentMapId());
      System.out.println("✅ Mapa inicial carregado: " + initialMap.getName());

      // Iniciar música do mapa inicial
      if (musicManager != null) {
        musicManager.playMusicForMap(mapManager.getCurrentMapId());
      }
    }

    // Criar a câmera
    camera = new Camera(0, 0);

    // Inicializar sistema de diálogos
    dialogBox = new DialogBox();
    questChoiceBox = new QuestChoiceBox();
    npcs = new java.util.ArrayList<>();

    // Inicializar sistema de baús
    chests = new java.util.ArrayList<>();
    lockpickingMinigame = new LockpickingMinigame();

    // Inicializar sistema de transições
    mapTransition = new MapTransition();

    // Inicializar mapa mundi
    worldMapUI = new WorldMapUI(mapManager);

    // Criar NPCs de exemplo
    createExampleNPCs();

    // Não criar player aqui - será criado quando setPlayerClass for chamado
    // Isso evita conflitos quando o jogo é iniciado através da tela de criação de
    // personagem

    System.out.println("=== SISTEMA INICIALIZADO ===");
    System.out.println("TileMap criado");
    System.out.println("Tamanho dos tiles: " + TILE_SIZE + "px");
    System.out.println("Mapa: " + MAP_WIDTH + "x" + MAP_HEIGHT + " tiles");
    System.out.println("Aguardando criação do personagem...");
    System.out.println("========================");
  }

  public void setPlayerClass(String playerClass, String spritePath) {
    // Verificar se o tileMap já foi inicializado antes de criar o player
    if (tileMap != null) {
      // Posição inicial fixa no mapa village (x:558, y:217)
      player = new Player(558, 217, spritePath);
      player.setTileMap(tileMap);

      // Reinicializar o gerenciador de inimigos com o novo player
      enemyManager = new EnemyManager(player, tileMap);
      player.setEnemyManager(enemyManager); // Conectar player ao enemy manager
      enemyManager.setCurrentMapId(mapManager.getCurrentMapId());
      buildFactionSystem();
      initWorldState();
      enemyManager.initializeGoblinFamilies(tileMap);
    } else {
      // Fallback para posição central se tileMap ainda não foi inicializado
      player = new Player(360, 360, spritePath);
      System.out.println("Aviso: TileMap ainda não foi inicializado quando setPlayerClass foi chamado");
    }
  }

  public void setPlayerClass(String playerClass, String spritePath, CharacterStats stats) {
    // Conectar o mapa ao jogador para verificação de colisão
    if (tileMap != null) {
      // Posição inicial fixa no mapa village (x:558, y:217)
      player = new Player(558, 217, spritePath, playerClass, stats);
      player.setTileMap(tileMap);

      // Criar o gerenciador de inimigos com o novo player
      enemyManager = new EnemyManager(player, tileMap);
      player.setEnemyManager(enemyManager); // Conectar player ao enemy manager
      enemyManager.setCurrentMapId(mapManager.getCurrentMapId());
      buildFactionSystem();
      initWorldState();
      enemyManager.initializeGoblinFamilies(tileMap);

      // Inicializar UI de slots de habilidades
      if (player.getSkillManager() != null) {
        int currentWidth = getWidth() > 0 ? getWidth() : Game.SCREEN_WIDTH;
        skillSlotUI = new SkillSlotUI(player.getSkillManager(), currentWidth);
      }

      // Inicializar UI de quests e gold
      questUI = new QuestUI(player.getQuestManager());
      goldUI = new GoldUI(player);

      // Inicializar tela de inventário
      inventoryScreen = new InventoryScreen(player.getInventory(), player);
      int currentWidth = getWidth() > 0 ? getWidth() : Game.SCREEN_WIDTH;
      int currentHeight = getHeight() > 0 ? getHeight() : Game.SCREEN_HEIGHT;
      inventoryScreen.updateLayout(currentWidth, currentHeight);

      // Inicializar console de desenvolvedor
      developerConsole = new DeveloperConsole(player);

      // Iniciar o loop do jogo se ainda não estiver rodando
      if (gameThread == null || !gameThread.isAlive()) {
        startGameLoop();
      }

      System.out.println("=== PERSONAGEM CRIADO ===");
      System.out.println("Classe: " + playerClass);
      System.out.println("Stats: " + stats.toString());
      System.out.println("Posição inicial: 638, 260 (Village)");
      System.out.println("Controles: WASD para mover, ESPAÇO para atacar, C para características");
      System.out.println("========================");
    } else {
      System.err.println("ERRO: TileMap não foi inicializado!");
    }
  }

  /**
   * Constrói e conecta o FactionSystem com base nos mapas conhecidos.
   * Deve ser chamado após criar o EnemyManager e antes de
   * initializeGoblinFamilies.
   */
  private void buildFactionSystem() {
    factionSystem = new FactionSystem();

    // Registrar todos os mapas com seus tipos de território
    factionSystem.registerMap("village", FactionType.HUMANS, TerritoryType.HUMAN_SETTLEMENT);
    factionSystem.registerMap("goblin_village", FactionType.GOBLINS, TerritoryType.GOBLIN_TERRITORY);
    factionSystem.registerMap("goblin_territories", FactionType.GOBLINS, TerritoryType.GOBLIN_TERRITORY);
    factionSystem.registerMap("secret_area", FactionType.NEUTRAL, TerritoryType.SPECIAL_AREA);
    factionSystem.registerMap("neutral_1", FactionType.NEUTRAL, TerritoryType.NEUTRAL_WILDS);
    factionSystem.registerMap("neutral_2", FactionType.NEUTRAL, TerritoryType.NEUTRAL_WILDS);
    factionSystem.registerMap("neutral_3", FactionType.NEUTRAL, TerritoryType.NEUTRAL_WILDS);
    factionSystem.registerMap("neutral_4", FactionType.NEUTRAL, TerritoryType.NEUTRAL_WILDS);

    // Conexões de adjacência (baseadas nas entradas dos templates)
    factionSystem.connect("village", "secret_area");
    factionSystem.connect("village", "goblin_territories");
    factionSystem.connect("goblin_territories", "goblin_village");

    // Valor estratégico — vila humana é o alvo mais valioso para goblins
    factionSystem.setStrategicValue("village", 80);
    factionSystem.setStrategicValue("goblin_territories", 50);

    factionSystem.buildLeaderBrain();

    // Conectar ao EnemyManager e QuestManager
    enemyManager.setFactionSystem(factionSystem);
    if (player != null && player.getQuestManager() != null) {
      player.getQuestManager().setFactionSystem(factionSystem);
    }

    System.out.println("[FactionSystem] Inicializado com " + 8 + " mapas registrados.");
  }

  /**
   * Cria o WorldState e o ServerLoop, e registra o mapa inicial como ativo.
   * Deve ser chamado após buildFactionSystem() e antes de
   * initializeGoblinFamilies().
   */
  private void initWorldState() {
    // Parar loop anterior (reinício de partida)
    if (serverLoop != null) {
      serverLoop.stop();
    }

    worldState = new WorldState();
    serverLoop = new ServerLoop(worldState);

    // Marcar mapa inicial como ativo (jogador começa aqui)
    String startMapId = mapManager.getCurrentMapId();
    worldState.getOrCreate(startMapId).setHasActivePlayers(true);

    serverLoop.start();
    System.out.println("[WorldState] Iniciado. Mapa ativo: " + startMapId);

    // Fase 3+4: criar pipeline de input e cliente com transporte compartilhado
    clientInput = new ClientInput("player-1");
    InProcessTransport p1Transport = serverLoop.getTransport();
    clientInput.setTransport(p1Transport);
    gameClient = new GameClient(p1Transport);
    if (player != null) {
      playerSimulation = new PlayerSimulation(player);
      serverLoop.setPlayerSimulation(playerSimulation);
    }

    // Fase 6/7: se useNetwork=true, conecta via TCP ao servidor escolhido
    if (useNetwork) {
      clientNetwork = new ClientNetwork(p1Transport, "player-1",
          player != null ? player.getPlayerClass() : "Unknown");
      clientNetwork.setConnectionListener(() -> {
        System.err.println("[GamePanel] Conexao com o servidor perdida!");
        // Aqui poderia exibir tela de reconexao — por ora apenas loga
      });
      try {
        clientNetwork.connect(networkHost, networkPort);
      } catch (java.io.IOException e) {
        System.err.println("[GamePanel] Nao foi possivel conectar ao servidor "
            + networkHost + ":" + networkPort + " — " + e.getMessage());
        clientNetwork = null;
      }
    }
  }

  /**
   * Fase 5: ativa o segundo jogador local.
   * Deve ser chamado após initWorldState(). Cria Player2, conecta pipeline
   * de input dedicado e registra no ServerLoop.
   *
   * @param spritePath sprite do segundo player (pode ser o mesmo de P1)
   */
  public void enablePlayer2(String spritePath) {
    if (serverLoop != null && !player2Enabled) {
      // Criar Player2 deslocado para não sobrepor P1
      player2 = new Player(558 + 64, 217, spritePath);
      player2.setPlayerId("player-2");
      player2.setTileMap(tileMap);
      player2.setEnemyManager(enemyManager);

      // Pipeline de input dedicado para P2 (IJKL + Enter)
      clientInput2 = new ClientInput("player-2");
      playerSimulation2 = new PlayerSimulation(player2);

      // Registrar no servidor — retorna transporte dedicado para P2
      InProcessTransport transport2 = serverLoop.registerPlayer(playerSimulation2);
      clientInput2.setTransport(transport2);
      gameClient2 = new GameClient(transport2);

      player2Enabled = true;
      System.out.println("[Fase 5] Player 2 ativado.");
    }
  }

  private void startGameLoop() {
    if (gameThread == null || !gameThread.isAlive()) {
      gameThread = new Thread(this);
      running = true;
      gameThread.start();
      System.out.println("Game loop iniciado");
    }
  }

  @Override
  public void run() {
    long nextFrameTime = System.nanoTime();

    while (running) {
      long frameStart = System.nanoTime();

      update();
      repaint();

      long frameCpuNanos = System.nanoTime() - frameStart;

      nextFrameTime += TARGET_TIME;
      long sleepNanos = nextFrameTime - System.nanoTime();

      if (sleepNanos > 0) {
        try {
          long sleepMillis = sleepNanos / 1_000_000L;
          int sleepNanoPart = (int) (sleepNanos % 1_000_000L);
          Thread.sleep(sleepMillis, sleepNanoPart);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          running = false;
        }
      } else {
        // Se atrasou, evita acumular lag indefinidamente e ressincroniza o relógio.
        nextFrameTime = frameStart;
      }

      recordPerformanceMetrics(frameCpuNanos, sleepNanos <= 0);
    }
  }

  private void recordPerformanceMetrics(long frameCpuNanos, boolean lateFrame) {
    frameCpuSamples[frameCpuSampleIndex] = frameCpuNanos;
    frameCpuSampleIndex = (frameCpuSampleIndex + 1) % PERF_SAMPLE_WINDOW;
    if (frameCpuSampleCount < PERF_SAMPLE_WINDOW) {
      frameCpuSampleCount++;
    }

    if (lateFrame) {
      lateFrameCount++;
    }

    long now = System.nanoTime();
    if (frameCpuSampleCount < PERF_SAMPLE_WINDOW || now - lastPerfLogNanos < PERF_LOG_INTERVAL_NANOS) {
      return;
    }

    long[] sorted = Arrays.copyOf(frameCpuSamples, frameCpuSampleCount);
    Arrays.sort(sorted);

    double avgMs = 0.0;
    for (long sample : sorted) {
      avgMs += sample / 1_000_000.0;
    }
    avgMs /= frameCpuSampleCount;

    double p95Ms = sorted[(int) Math.floor((frameCpuSampleCount - 1) * 0.95)] / 1_000_000.0;
    double p99Ms = sorted[(int) Math.floor((frameCpuSampleCount - 1) * 0.99)] / 1_000_000.0;
    double latePct = (lateFrameCount * 100.0) / frameCpuSampleCount;
    double fogAvgMs = fogUpdateCount > 0 ? (fogUpdateNanosAccum / 1_000_000.0) / fogUpdateCount : 0.0;

    System.out.printf("📊 PERF | frameCPU avg=%.2fms p95=%.2fms p99=%.2fms late=%.1f%% | fogAvg=%.3fms updates=%d%n",
        avgMs, p95Ms, p99Ms, latePct, fogAvgMs, fogUpdateCount);

    lateFrameCount = 0;
    fogUpdateNanosAccum = 0;
    fogUpdateCount = 0;
    lastPerfLogNanos = now;
  }

  private void requestUiRefresh() {
    if (!running) {
      repaint();
    }
  }

  private void updateResponsiveUiLayout() {
    int currentWidth = getWidth() > 0 ? getWidth() : Game.SCREEN_WIDTH;
    int currentHeight = getHeight() > 0 ? getHeight() : Game.SCREEN_HEIGHT;

    if (inventoryScreen != null) {
      inventoryScreen.updateLayout(currentWidth, currentHeight);
    }

    if (skillSlotUI != null) {
      skillSlotUI.setScreenWidth(currentWidth);
    }
  }

  private void update() {
    if (portalCooldownFrames > 0) {
      portalCooldownFrames--;
    }

    // Só atualizar se o player foi criado e não estiver na tela de características
    if (player == null || showingCharacterScreen)
      return;

    // Verificar se player morreu
    if (!playerDead && player != null && !player.isAlive()) {
      playerDead = true;
      deathTransitionStarted = false;
      // Parar a música quando o player morre
      if (musicManager != null) {
        musicManager.stopMusic();
      }
      System.out.println("💀 Player morreu!");
    }

    // Se player está morto, iniciar transição de morte
    if (playerDead && !deathTransitionStarted) {
      mapTransition.startTransition("", 0, 0); // Transição vazia, só para efeito visual
      deathTransitionStarted = true;
    }

    // Atualizar transição de mapa ou morte
    if (mapTransition.isTransitioning()) {
      boolean shouldChangeMap = mapTransition.update();

      if (shouldChangeMap && !playerDead) {
        // Momento de trocar o mapa (tela totalmente preta) - apenas se não for morte
        changeMap(mapTransition.getTargetMapPath(),
            mapTransition.getPlayerSpawnX(),
            mapTransition.getPlayerSpawnY());
      } else if (shouldChangeMap && playerDead) {
        // Tela totalmente preta - mostrar tela de morte
        showingDeathScreen = true;
      }

      // Não atualizar gameplay durante transição
      return;
    }

    // Não atualizar se estiver na tela de morte
    if (showingDeathScreen) {
      return;
    }

    // Fase 3: aplicar InputPacket antes de atualizar o player
    if (clientInput != null && playerSimulation != null) {
      InputPacket packet = clientInput.buildPacket();
      playerSimulation.applyInput(packet);
      if (serverLoop != null) {
        serverLoop.submitInput(packet);
      }
    }

    player.update();

    // Fase 5: atualizar P2 se ativo
    if (player2Enabled && player2 != null) {
      if (clientInput2 != null && playerSimulation2 != null) {
        InputPacket packet2 = clientInput2.buildPacket();
        playerSimulation2.applyInput(packet2);
      }
      player2.update();
      if (gameClient2 != null) {
        gameClient2.pollSnapshot();
      }
    }

    // Atualizar fog apenas no ciclo de update para manter paintComponent leve
    long fogStart = System.nanoTime();
    tileMap.updateFogOfWar(player);
    fogUpdateNanosAccum += (System.nanoTime() - fogStart);
    fogUpdateCount++;

    // Verificar desbloqueio de habilidade pendente
    if (player.getPendingSkillUnlock() > 0 && !showingDialog) {
      showSkillUnlockDialog(player.getPendingSkillUnlock());
    }

    // Atualizar NPCs
    updateNPCs();

    // Atualizar baús
    updateChests();

    // Atualizar minigame se estiver ativo
    if (playingMinigame && lockpickingMinigame != null) {
      lockpickingMinigame.update();
    }

    // Atualizar inimigos
    if (enemyManager != null) {
      enemyManager.update();

      // Verificar colisões
      enemyManager.checkProjectileCollisions(player.getProjectiles());
      enemyManager.checkPlayerCollisions();
    }

    // Atualizar câmera para seguir o jogador
    camera.centerOnPlayer(player);

    // Verificar se player está sobre um portal
    checkPortalCollision();

    // Publicar contexto para que o ServerLoop gere snapshots consistentes.
    if (serverLoop != null && mapManager != null) {
      serverLoop.updateSnapshotContext(mapManager.getCurrentMapId(), player, factionSystem, npcs, chests);
    }

    // Fase 4: consumir snapshot via GameClient (fila) em vez de acesso direto ao
    // ServerLoop
    if (gameClient != null) {
      gameClient.pollSnapshot();
      latestWorldSnapshot = gameClient.getLatestSnapshot();
      previousWorldSnapshot = gameClient.getPreviousSnapshot();
      latestSnapshotNanos = gameClient.getLatestSnapshotNanos();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Se estiver mostrando a tela de características, não renderizar o jogo
    if (showingCharacterScreen) {
      return;
    }

    // Se player ainda não foi criado, mostrar tela de loading
    if (player == null) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.setColor(Color.WHITE);
      g2d.setFont(new Font("Arial", Font.BOLD, 24));
      g2d.drawString("Aguardando criação do personagem...", 300, 400);
      return;
    }

    Graphics2D g2d = (Graphics2D) g;

    // Aplicar antialiasing
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Renderizar o mapa
    tileMap.render(g2d, camera, player);

    WorldSnapshot snapshot = latestWorldSnapshot;
    WorldSnapshot previousSnapshot = previousWorldSnapshot;
    double interpolationAlpha = SnapshotRenderSystem.computeInterpolationAlpha(latestSnapshotNanos);

    // Renderizacao orientada a snapshot (Fase 2).
    if (enemyManager != null) {
      SnapshotRenderSystem.renderStructures(g2d, camera, enemyManager.getStructures());
    }
    SnapshotRenderSystem.renderEnemies(g2d, camera, previousSnapshot, snapshot, interpolationAlpha,
        tileMap.getFogOfWar());
    SnapshotRenderSystem.renderNpcs(g2d, camera, snapshot);
    SnapshotRenderSystem.renderChests(g2d, camera, snapshot, tileMap.getFogOfWar());
    SnapshotRenderSystem.renderProjectiles(g2d, camera, previousSnapshot, snapshot, interpolationAlpha);
    SnapshotRenderSystem.renderPlayer(g2d, camera, previousSnapshot, snapshot, interpolationAlpha);

    // Render de debug continua local no cliente.
    if (showVisionCones) {
      renderVisionCones(g2d);
    }

    // Renderizar habilidades do jogador (efeitos visuais)
    if (player.getSkillManager() != null) {
      player.getSkillManager().render(g2d, camera);
    }

    // Renderizar UI
    renderUI(g2d);

    // Renderizar minigame por cima de tudo se estiver ativo
    if (playingMinigame && lockpickingMinigame != null) {
      lockpickingMinigame.render(g2d, getWidth(), getHeight());
    }

    // Renderizar DialogBox se estiver mostrando
    if (showingDialog && dialogBox != null) {
      String npcName = currentTalkingNPC != null ? currentTalkingNPC.getName() : "Sistema";
      dialogBox.render(g2d, npcName, getWidth(), getHeight());

      // Renderizar caixa de escolha de quest sobre o diálogo
      if (waitingForQuestChoice && questChoiceBox != null) {
        questChoiceBox.render(g2d, getWidth(), getHeight());
      }
    }

    // Renderizar inventário se estiver visível
    if (inventoryScreen != null && inventoryScreen.isInventoryVisible()) {
      inventoryScreen.render(g2d);
    }

    // Renderizar janela de quests se estiver visível (por cima do inventário)
    if (questUI != null && questUI.isVisible()) {
      questUI.render(g2d);
    }

    // Renderizar loja se estiver visível
    if (shopUI != null && shopUI.isVisible()) {
      shopUI.render(g2d);
    }

    // Renderizar mapa mundi por cima do gameplay, sem bloquear o loop
    if (worldMapUI != null && worldMapUI.isVisible()) {
      worldMapUI.render(g2d, getWidth(), getHeight());
    }

    // Renderizar transição de mapa (sempre por último, em cima de tudo)
    if (mapTransition != null && mapTransition.isTransitioning()) {
      mapTransition.render(g2d, getWidth(), getHeight());
    }

    // Renderizar indicador de escape se player estiver preso
    renderEscapeIndicator(g2d);

    // Renderizar tela de morte (se ativa)
    if (showingDeathScreen) {
      renderDeathScreen(g2d);
    }
  }

  /**
   * Renderiza indicador de progresso de escape quando player está preso no Mimic.
   */
  private void renderEscapeIndicator(Graphics2D g) {
    if (enemyManager == null)
      return;

    for (com.rpggame.entities.Enemy enemy : enemyManager.getEnemies()) {
      if (enemy instanceof com.rpggame.enemies.mimic.Mimic) {
        com.rpggame.enemies.mimic.Mimic mimic = (com.rpggame.enemies.mimic.Mimic) enemy;
        if (mimic.isPlayerGrabbed()) {
          // Fundo semi-transparente
          g.setColor(new Color(0, 0, 0, 150));
          int boxWidth = 400;
          int boxHeight = 80;
          int boxX = (getWidth() - boxWidth) / 2;
          int boxY = getHeight() / 2 - 100;
          g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

          // Texto de instrução
          g.setColor(Color.RED);
          g.setFont(new Font("Arial", Font.BOLD, 24));
          String text = "APERTE SPACE PARA ESCAPAR!";
          FontMetrics fm = g.getFontMetrics();
          int textWidth = fm.stringWidth(text);
          g.drawString(text, (getWidth() - textWidth) / 2, boxY + 30);

          // Barra de progresso
          int barWidth = 300;
          int barHeight = 20;
          int barX = (getWidth() - barWidth) / 2;
          int barY = boxY + 50;

          // Fundo da barra
          g.setColor(Color.DARK_GRAY);
          g.fillRect(barX, barY, barWidth, barHeight);

          // Progresso (pegar do método público)
          int progress = mimic.getEscapeProgress();
          double progressPercent = Math.min(1.0, progress / 15.0);
          int progressWidth = (int) (barWidth * progressPercent);

          g.setColor(new Color(0, 255, 0));
          g.fillRect(barX, barY, progressWidth, barHeight);

          // Borda da barra
          g.setColor(Color.WHITE);
          g.setStroke(new BasicStroke(2));
          g.drawRect(barX, barY, barWidth, barHeight);

          break;
        }
      }
    }
  }

  /**
   * Renderiza a tela de morte
   */
  private void renderDeathScreen(Graphics2D g) {
    // Fundo preto
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, getWidth(), getHeight());

    // Antialiasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Texto "Você morreu" em vermelho sangue
    Color bloodRed = new Color(139, 0, 0); // Vermelho escuro/sangue
    g.setColor(bloodRed);
    g.setFont(new Font("Arial", Font.BOLD, 72));

    String deathText = "Você morreu";
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(deathText);
    int textX = (getWidth() - textWidth) / 2;
    int textY = getHeight() / 2 - 50;

    g.drawString(deathText, textX, textY);

    // Botão "Renascer" - Fase 8: Respawn mechanic
    int buttonWidth = 200;
    int buttonHeight = 50;
    int buttonX = (getWidth() - buttonWidth) / 2;
    int buttonY = textY + 80;

    // Armazenar área do botão para detecção de clique
    if (newGameButton == null) {
      newGameButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
    }

    // Desenhar botão
    g.setColor(new Color(60, 60, 60));
    g.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

    // Texto do botão - "Renascer" ao invés de "Novo Jogo"
    g.setFont(new Font("Arial", Font.BOLD, 24));
    String buttonText = "Renascer";
    fm = g.getFontMetrics();
    textWidth = fm.stringWidth(buttonText);
    int buttonTextX = buttonX + (buttonWidth - textWidth) / 2;
    int buttonTextY = buttonY + ((buttonHeight - fm.getHeight()) / 2) + fm.getAscent();

    g.drawString(buttonText, buttonTextX, buttonTextY);
  }

  private void renderUI(Graphics2D g) {
    if (player == null)
      return;

    // Barras de vida e mana (canto superior esquerdo)
    drawHealthAndManaBars(g, player);

    // Slots de habilidades (canto superior direito)
    if (skillSlotUI != null) {
      skillSlotUI.render(g);
    }

    // UI de Gold (canto superior direito)
    if (goldUI != null) {
      goldUI.render(g);
    }

    // Instruções de controle removidas para interface mais limpa
  }

  /**
   * Desenha as barras de vida e mana no canto superior esquerdo
   */
  private void drawHealthAndManaBars(Graphics2D g, Player player) {
    int barX = 20;
    int barY = 20;
    int barWidth = 200;
    int barHeight = 20;
    int barSpacing = 30;

    WorldSnapshot snapshot = latestWorldSnapshot;
    WorldSnapshot.SnapshotPlayer snapshotPlayer = getPrimarySnapshotPlayer(snapshot);

    int currentHealth = snapshotPlayer != null ? snapshotPlayer.getHp() : player.getCurrentHealth();
    int maxHealth = snapshotPlayer != null ? snapshotPlayer.getMaxHp() : player.getMaxHealth();
    int currentMana = snapshotPlayer != null ? snapshotPlayer.getMana() : player.getCurrentMana();
    int maxMana = snapshotPlayer != null ? snapshotPlayer.getMaxMana() : player.getMaxMana();

    // Barra de Vida
    drawBar(g, "VIDA", barX, barY, barWidth, barHeight,
        currentHealth, maxHealth,
        Color.RED, Color.DARK_GRAY);

    // Barra de Mana
    drawBar(g, "MANA", barX, barY + barSpacing, barWidth, barHeight,
        currentMana, maxMana,
        Color.BLUE, Color.DARK_GRAY);

    // Barra de XP
    ExperienceSystem expSys = player.getExperienceSystem();
    drawXpBar(g, barX, barY + (barSpacing * 2), barWidth, barHeight - 5, expSys);

    // Classe e nível do jogador abaixo das barras
    g.setFont(new Font("Arial", Font.BOLD, 12));
    g.setColor(Color.WHITE);
    g.drawString("Classe: " + player.getPlayerClass() + " | Nível: " + expSys.getCurrentLevel(),
        barX, barY + (barSpacing * 3) + 5);

    // Informações de debug (só aparece quando modo debug está ativo - tecla V)
    if (enemyManager != null && showVisionCones) {
      g.setFont(new Font("Arial", Font.PLAIN, 10));
      g.setColor(Color.LIGHT_GRAY);

      // Quantidade de inimigos
      g.drawString("Inimigos: " + enemyManager.getAliveCount(),
          barX, barY + (barSpacing * 3) + 25);

      // Posição do player (X, Y)
      g.drawString("Player X: " + (int) player.getX() + " Y: " + (int) player.getY(),
          barX, barY + (barSpacing * 3) + 40);

      // Posição do tile do player
      int tileX = (int) player.getX() / TILE_SIZE;
      int tileY = (int) player.getY() / TILE_SIZE;
      g.drawString("Tile: T " + tileX + " - L " + tileY,
          barX, barY + (barSpacing * 3) + 55);

      // Mostrar decisão do conselho goblin se houver
      com.rpggame.systems.GoblinCouncil council = enemyManager.getGoblinCouncil();
      if (council != null) {
        int yOffset = barY + (barSpacing * 3) + (showVisionCones ? 75 : 40);

        if (council.isAllianceAgainstPlayerActive()) {
          g.setFont(new Font("Arial", Font.BOLD, 12));
          g.setColor(new Color(255, 100, 100));
          int timeLeft = council.getAllianceTimeRemaining() / 60; // Converter frames para segundos
          g.drawString("⚔️ ALIANÇA GOBLIN ATIVA! (" + timeLeft + "s)", barX, yOffset);
        } else if (council.isGoblinEmpireActive()) {
          g.setFont(new Font("Arial", Font.BOLD, 12));
          g.setColor(new Color(255, 215, 0));
          g.drawString("👑 IMPÉRIO GOBLIN FORMADO!", barX, yOffset);
        } else if (council.isTechnologicalAdvanceActive()) {
          g.setFont(new Font("Arial", Font.BOLD, 12));
          g.setColor(new Color(100, 255, 100));
          g.drawString("🔧 AVANÇO TECNOLÓGICO ATIVO! (x2 Força)", barX, yOffset);
        }
      }
    }

    // Console de desenvolvedor (renderizar antes da death screen)
    if (developerConsole != null && developerConsole.isVisible()) {
      developerConsole.render((Graphics2D) g, getWidth(), getHeight());
    }
  }

  private WorldSnapshot.SnapshotPlayer getPrimarySnapshotPlayer(WorldSnapshot snapshot) {
    if (snapshot == null || snapshot.getPlayers().isEmpty()) {
      return null;
    }
    return snapshot.getPlayers().get(0);
  }

  /**
   * Desenha uma barra de recurso (vida, mana, etc.)
   */
  private void drawBar(Graphics2D g, String label, int x, int y, int width, int height,
      int current, int max, Color fillColor, Color bgColor) {
    // Fundo da barra
    g.setColor(bgColor);
    g.fillRect(x, y, width, height);

    // Borda da barra
    g.setColor(Color.WHITE);
    g.drawRect(x, y, width, height);

    // Preenchimento da barra
    if (max > 0) {
      int fillWidth = (int) ((double) current / max * width);
      g.setColor(fillColor);
      g.fillRect(x + 1, y + 1, fillWidth - 1, height - 2);
    }

    // Texto da barra
    g.setFont(new Font("Arial", Font.BOLD, 12));
    g.setColor(Color.WHITE);
    String text = label + ": " + current + "/" + max;
    FontMetrics fm = g.getFontMetrics();
    int textX = x + (width - fm.stringWidth(text)) / 2;
    int textY = y + (height + fm.getAscent()) / 2 - 2;
    g.drawString(text, textX, textY);
  }

  /**
   * Desenha a barra de experiência
   */
  private void drawXpBar(Graphics2D g, int x, int y, int width, int height,
      ExperienceSystem expSys) {
    // Fundo da barra
    g.setColor(Color.DARK_GRAY);
    g.fillRect(x, y, width, height);

    // Borda da barra
    g.setColor(Color.WHITE);
    g.drawRect(x, y, width, height);

    // Preenchimento da barra baseado na porcentagem
    float progress = expSys.getProgressPercentage();
    int fillWidth = (int) (progress * width);

    // Cor do XP (dourado)
    g.setColor(new Color(255, 215, 0)); // Dourado
    g.fillRect(x + 1, y + 1, fillWidth - 1, height - 2);

    // Texto da barra
    g.setFont(new Font("Arial", Font.BOLD, 10));
    g.setColor(Color.WHITE);
    String text = "XP: " + expSys.getCurrentXp() + "/" + expSys.getXpToNextLevel();
    FontMetrics fm = g.getFontMetrics();
    int textX = x + (width - fm.stringWidth(text)) / 2;
    int textY = y + (height + fm.getAscent()) / 2 - 2;
    g.drawString(text, textX, textY);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    // Se estiver mostrando tela de características, passa o evento para ela
    if (showingCharacterScreen && characterScreen != null) {
      characterScreen.keyPressed(e);
      return;
    }

    // Mapa mundi: atalho global durante o jogo
    if (worldMapUI != null) {
      if (e.getKeyCode() == KeyEvent.VK_M) {
        worldMapUI.toggle();
        repaint();
        return;
      }

      if (worldMapUI.isVisible() && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        worldMapUI.hide();
        repaint();
        return;
      }
    }

    // Se inventário estiver aberto, passa eventos para ele
    if (inventoryScreen != null && inventoryScreen.isInventoryVisible()) {
      inventoryScreen.keyPressed(e);
      return;
    }

    // Se player ainda não foi criado, ignorar input
    if (player == null) {
      return;
    }

    // Tecla ' (aspas) para abrir console de desenvolvedor
    if (e.getKeyCode() == KeyEvent.VK_QUOTE) {
      if (developerConsole != null) {
        developerConsole.toggle();
        requestUiRefresh();
      }
      return;
    }

    // Se console está aberto, delegar input para ele
    if (developerConsole != null && developerConsole.isVisible()) {
      boolean needsRepaint = developerConsole.keyPressed(e);
      if (needsRepaint) {
        requestUiRefresh();
      }
      return;
    }

    // Tecla F para baús e minigame
    if (e.getKeyCode() == KeyEvent.VK_F) {
      if (playingMinigame && lockpickingMinigame != null) {
        // Está jogando minigame - tentar abrir baú
        boolean success = lockpickingMinigame.handleInput(KeyEvent.VK_F);
        if (success && currentChest != null) {
          // Sucesso! Abrir baú e dar recompensas
          currentChest.open();
          String[] rewards = currentChest.getRewards();
          System.out.println("✅ Baú aberto! Recompensas: " + rewards[0] + ", " + rewards[1]);

          // Adicionar itens ao inventário do player
          if (player != null) {
            for (String reward : rewards) {
              if ("health_potion".equals(reward)) {
                player.getInventory().addItem(new com.rpggame.items.consumables.HealthPotion(player, 50), 1);
              } else if ("mana_potion".equals(reward)) {
                player.getInventory().addItem(new com.rpggame.items.consumables.ManaPotion(player, 30), 1);
              }
            }
          }

          playingMinigame = false;
          currentChest = null;
        } else if (lockpickingMinigame.isFinished() && !success) {
          // Falhou - reiniciar minigame
          System.out.println("❌ Falhou no minigame! Tente novamente.");
          lockpickingMinigame.reset();
        }
        requestUiRefresh();
        return;
      } else {
        // Verificar se há baú próximo para interagir
        checkChestInteraction();
        return;
      }
    }

    // Tecla E para interagir com NPCs
    if (e.getKeyCode() == KeyEvent.VK_E) {
      interactWithNearbyNPC();
      return;
    }

    // Sistema de escolha de quest com setas e Enter
    if (waitingForQuestChoice && currentTalkingNPC instanceof MerchantNPC) {
      MerchantNPC merchant = (MerchantNPC) currentTalkingNPC;

      // Setas para navegar entre Sim/Não
      if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
        questChoiceBox.selectPrevious();
        requestUiRefresh();
        return;
      } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
        questChoiceBox.selectNext();
        requestUiRefresh();
        return;
      }

      // Enter ou Space para confirmar escolha
      if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
        if (questChoiceBox.isYesSelected()) {
          // Aceitar quest
          merchant.acceptQuest(player);
          waitingForQuestChoice = false;
          questChoiceBox.hide();
          endDialog();
        } else {
          // Recusar quest
          merchant.declineQuest(player);
          waitingForQuestChoice = false;
          questChoiceBox.hide();
          // Mostrar diálogo de recusa
          currentTalkingNPC.resetDialog();
          dialogBox.setText(currentTalkingNPC.getCurrentDialog());
        }
        requestUiRefresh();
        return;
      }
    }

    // Tecla C para abrir tela de características
    if (e.getKeyCode() == KeyEvent.VK_C)

    {
      openCharacterScreen();
      return;
    }

    // Tecla I para abrir inventário
    if (e.getKeyCode() == KeyEvent.VK_I) {
      if (inventoryScreen != null) {
        inventoryScreen.toggleVisibility();

        // Mostrar GoldUI quando abrir inventário, esconder quando fechar
        if (player != null) {
          if (inventoryScreen.isInventoryVisible()) {
            player.forceShowGoldUI();
          } else {
            player.hideGoldUI();
          }
        }

        requestUiRefresh();
      }
      return;
    }

    // Tecla V para toggle de debug (vision cones)
    if (e.getKeyCode() == KeyEvent.VK_V) {
      showVisionCones = !showVisionCones;
      requestUiRefresh();
      return;
    }

    // Tecla Q para abrir janela de quests
    if (e.getKeyCode() == KeyEvent.VK_Q) {
      if (questUI != null) {
        questUI.updatePosition(getWidth(), getHeight());
        questUI.toggle();
        requestUiRefresh();
      }
      return;
    }

    // Tecla L para abrir loja (apenas se estiver próximo do mercador e loja
    // desbloqueada)
    if (e.getKeyCode() == KeyEvent.VK_L) {
      if (merchantNPC != null && merchantNPC.isShopUnlocked() && merchantNPC.canInteract()) {
        if (shopUI != null) {
          shopUI.updatePosition(getWidth(), getHeight());
          shopUI.show();
          requestUiRefresh();
        }
      } else if (merchantNPC != null && !merchantNPC.isShopUnlocked() && merchantNPC.canInteract()) {
        System.out.println("🏪 Complete a quest do mercador para desbloquear a loja!");
      }
      return;
    }

    // Tecla ESC para fechar telas abertas
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      boolean closedSomething = false;

      // Fechar tela de características se estiver aberta
      if (showingCharacterScreen) {
        showingCharacterScreen = false;
        if (characterScreen != null) {
          characterScreen.setVisible(false);
        }
        closedSomething = true;
      }

      // Fechar loja se estiver aberta
      if (shopUI != null && shopUI.isVisible()) {
        shopUI.hide();
        closedSomething = true;
      }

      // Fechar questUI se estiver aberta
      if (questUI != null && questUI.isVisible()) {
        questUI.setVisible(false);
        closedSomething = true;
      }

      // Fechar inventário se estiver aberto
      if (inventoryScreen != null && inventoryScreen.isInventoryVisible()) {
        inventoryScreen.toggleVisibility();
        // Esconder GoldUI quando fechar inventário com ESC
        if (player != null) {
          player.hideGoldUI();
        }
        closedSomething = true;
      }

      if (closedSomething) {
        requestUiRefresh();
        return;
      }
    }

    // Delegar para shopUI se estiver visível
    if (shopUI != null && shopUI.isVisible()) {
      shopUI.keyPressed(e);
      requestUiRefresh();
      return;
    }

    // Verificar se player está preso no Mimic ANTES de qualquer outra ação
    if (e.getKeyCode() == KeyEvent.VK_SPACE && enemyManager != null) {
      for (com.rpggame.entities.Enemy enemy : enemyManager.getEnemies()) {
        if (enemy instanceof com.rpggame.enemies.mimic.Mimic) {
          com.rpggame.enemies.mimic.Mimic mimic = (com.rpggame.enemies.mimic.Mimic) enemy;
          if (mimic.isPlayerGrabbed()) {
            mimic.processEscapeAttempt();
            System.out.println("🎮 Player apertou Space! Progresso: " + mimic.getEscapeProgress() + "/15");
            requestUiRefresh();
            return; // Não processar ataque do player
          }
        }
      }
    }

    // Delegar para ClientInput (Fase 3: input via InputPacket)
    if (clientInput != null) {
      clientInput.onKeyPressed(e);
    } else {
      // Fallback enquanto clientInput não estiver inicializado
      player.keyPressed(e);
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (player == null)
      return;
    if (clientInput != null) {
      clientInput.onKeyReleased(e);
    } else {
      player.keyReleased(e);
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  // MouseListener methods - para garantir foco quando clicado
  @Override
  public void mouseClicked(MouseEvent e) {
    requestFocusInWindow();

    // Verificar clique no botão "Renascer" na tela de morte - Fase 8
    if (showingDeathScreen && newGameButton != null) {
      if (newGameButton.contains(e.getPoint())) {
        respawnPlayer();
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    requestFocusInWindow();
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  // Getter para o mapa (usado pelo Player para verificar colisões)
  public TileMap getTileMap() {
    return tileMap;
  }

  /**
   * Abre a tela de características do personagem.
   */
  public void openCharacterScreen() {
    if (player != null && !showingCharacterScreen) {
      // Criar e configurar a tela de características
      characterScreen = new CharacterScreen(this, player);

      // Criar inventoryScreen se ainda não existe
      if (inventoryScreen == null) {
        inventoryScreen = new InventoryScreen(player.getInventory(), player);
        int currentWidth = getWidth() > 0 ? getWidth() : Game.SCREEN_WIDTH;
        int currentHeight = getHeight() > 0 ? getHeight() : Game.SCREEN_HEIGHT;
        inventoryScreen.updateLayout(currentWidth, currentHeight);
      }

      showingCharacterScreen = true;

      // Mostrar a tela (não pausar o thread do jogo)
      removeAll();
      setLayout(new BorderLayout());
      add(characterScreen, BorderLayout.CENTER);
      revalidate();
      repaint();

      // Dar foco para a tela de características
      SwingUtilities.invokeLater(() -> {
        characterScreen.requestFocusInWindow();
      });

      System.out.println("Tela de características aberta - jogo pausado");
    }
  }

  /**
   * Fase 8: Respawn Mechanic
   * Ressuscita o jogador com seu personagem atual:
   * - Reseta health para máximo
   * - Retorna à posição de spawn
   * - Reseta XP do level atual (mas mantém o level)
   * - Preserva: class, level, inventário, quests, gold, reputação
   */
  private void respawnPlayer() {
    if (player == null) {
      return;
    }

    System.out.println("🔄 Ressuscitando jogador no spawn...");

    // Ressuscitar o jogador - restaurar health ao máximo
    player.heal(player.getMaxHealth());

    // Retornar à posição de spawn (0, 0 como ponto de spawn padrão)
    // Em futuras implementações, isso pode ser obtido do mapa/mundo
    player.setPosition(0, 0);

    // Reseta XP do level atual (Fase 8: "zerar apenas a XP do level atual")
    if (player.getExperienceSystem() != null) {
      player.getExperienceSystem().resetCurrentLevelXp();
    }

    // Remover flags de morte
    playerDead = false;
    deathTransitionStarted = false;
    showingDeathScreen = false;
    newGameButton = null;

    // Reiniciar o game loop se foi pausado
    if (!running) {
      startGameLoop();
    }

    System.out.println("✅ Jogador ressuscitado com sucesso no spawn!");
  }

  /**
   * Fecha a tela de características e volta ao jogo.
   */
  public void closeCharacterScreen() {
    if (showingCharacterScreen) {
      showingCharacterScreen = false;

      // Remover a tela de características
      removeAll();

      // Restaurar layout null para renderização custom do jogo
      setLayout(null);

      // Limpar referência primeiro
      characterScreen = null;

      // Revalidar e repintar para voltar ao jogo normal
      revalidate();
      repaint();

      // Dar foco de volta ao painel do jogo - importante para capturar teclas
      SwingUtilities.invokeLater(() -> {
        setFocusable(true);
        requestFocusInWindow();
        grabFocus();
        setFocusable(true);
      });

      System.out.println("Tela de características fechada - foco restaurado");
    }
  }

  /**
   * Cria NPCs de acordo com o mapa atual
   */
  private void createExampleNPCs() {
    String currentMapId = mapManager.getCurrentMapId();

    if ("village".equals(currentMapId)) {
      // Vila: Mercador, Aldeão, Sábio
      merchantNPC = new MerchantNPC(500, 400);
      npcs.add(merchantNPC);
      npcs.add(new VillagerNPC(300, 300));
      npcs.add(new WiseManNPC(900, 500));

      // Inicializar ShopUI com o inventário do mercador
      if (player != null && shopUI == null) {
        shopUI = new ShopUI(merchantNPC.getShopInventory(), player);
        shopUI.updatePosition(Game.SCREEN_WIDTH, Game.SCREEN_HEIGHT);
      }

      System.out.println("🏘️ NPCs da vila criados: " + npcs.size());
    } else if ("goblin_territories".equals(currentMapId)) {
      // Territórios Goblin: Guards protegendo a entrada da vila (ao redor do spawn
      // tile 12,3)
      npcs.add(new GuardNPC(480, 144)); // Esquerda do spawn (tile 10, 3)
      npcs.add(new GuardNPC(672, 144)); // Direita do spawn (tile 14, 3)
      System.out.println("⚔️ Guards dos territórios criados: " + npcs.size());
    } else if ("secret_area".equals(currentMapId)) {
      // Área secreta: sem NPCs, mas com Mimic e Baú
      System.out.println("🌿 Área secreta - sem NPCs");

      // Spawnar 1 Mimic e 1 Baú
      if (enemyManager != null) {
        spawnMimicAndChest();
      }
    }
    // Outros mapas podem não ter NPCs
  }

  /**
   * Spawna 1 Mimic e 1 Baú no mapa secret_area.
   */
  private void spawnMimicAndChest() {
    // Limpar listas primeiro
    chests.clear();

    // Coordenadas para spawnar (centro do mapa aproximadamente)
    // Mimic na posição (300, 400)
    Mimic mimic = new Mimic(300, 400);
    enemyManager.addEnemy(mimic);
    System.out.println("👹 Mimic spawnado em (300, 400)");

    // Baú na posição (600, 400) - distante do mimic para criar confusão
    Chest chest = new Chest(600, 400);
    chests.add(chest);
    System.out.println("📦 Baú spawnado em (600, 400)");
  }

  /**
   * Atualiza todos os baús.
   */
  private void updateChests() {
    if (player == null) {
      return;
    }

    for (Chest chest : chests) {
      chest.update(player);
    }
  }

  /**
   * Verifica interação com baús próximos.
   */
  private void checkChestInteraction() {
    for (Chest chest : chests) {
      if (chest.canInteract()) {
        // Iniciar minigame
        currentChest = chest;
        playingMinigame = true;
        lockpickingMinigame.reset();
        System.out.println("🎮 Iniciando minigame de lockpicking!");
        requestUiRefresh();
        return;
      }
    }
  }

  /*
   * Atualiza NPCs
   */
  private void updateNPCs() {
    for (NPC npc : npcs) {
      npc.update(player);

      // Se for um guarda, atualizar comportamento de combate
      if (npc instanceof GuardNPC && enemyManager != null) {
        ((GuardNPC) npc).updateGuardBehavior(enemyManager.getAllGoblins());
      }
    }

    if (showingDialog && dialogBox != null) {
      dialogBox.update();
      questChoiceBox.update();
    }
  }

  private void renderVisionCones(Graphics2D g) {
    // Debug visual legado removido da camada de simulacao na Fase 2.
  }

  /**
   * Tenta interagir com NPCs pr�ximos
   */
  private void interactWithNearbyNPC() {
    if (showingDialog) {
      if (dialogBox.isTextComplete()) {
        // Se é um diálogo de desbloqueio de habilidade
        if (skillUnlockDialogs != null) {
          currentSkillUnlockIndex++;
          if (currentSkillUnlockIndex < skillUnlockDialogs.length) {
            dialogBox.setText(skillUnlockDialogs[currentSkillUnlockIndex]);
          } else {
            endDialog();
          }
        }
        // Se é diálogo com NPC
        else if (currentTalkingNPC != null) {
          boolean hasMore = currentTalkingNPC.nextDialog();
          if (hasMore) {
            String newDialog = currentTalkingNPC.getCurrentDialog();
            dialogBox.setText(newDialog);

            // Verificar se é uma pergunta de quest (contém "(S/N)")
            if (newDialog != null && newDialog.contains("(S/N)")) {
              waitingForQuestChoice = true;
              questChoiceBox.show();
            }
          } else {
            endDialog();
          }
        }
      } else {
        dialogBox.skipAnimation();
      }
    } else {
      for (NPC npc : npcs) {
        if (npc.canInteract()) {
          startDialog(npc);
          break;
        }
      }
    }
  }

  /**
   * Inicia diálogo com NPC
   */
  private void startDialog(NPC npc) {
    currentTalkingNPC = npc;
    showingDialog = true;
    npc.resetDialog();

    // Se é o MerchantNPC, verificar status das quests
    if (npc instanceof MerchantNPC && player != null) {
      MerchantNPC merchant = (MerchantNPC) npc;

      // Verificar se a quest foi completada
      if (merchant.completeQuest(player)) {
        // Quest completada! Diálogos já foram atualizados no método completeQuest
      }
      // Verificar status da quest ativa
      else {
        merchant.checkQuestStatus(player);

        // Se a quest ainda não foi criada e não foi oferecida, criar e oferecer
        Quest goblinQuest = player.getQuestManager().getQuestById("merchant_goblin_hunt");
        if (goblinQuest == null && !merchant.isQuestGiven() && !merchant.isQuestOffered()) {
          merchant.createGoblinQuest(player);
          // Atualizar diálogos para mostrar a oferta da quest
          merchant.updateDialogues(merchant.getQuestOfferDialogues());
          merchant.setQuestOffered(true);
        }
        // Se a quest existe mas não foi aceita ainda, oferecer novamente
        else if (goblinQuest != null && goblinQuest.isAvailable() && !merchant.isQuestGiven()) {
          merchant.updateDialogues(merchant.getQuestOfferDialogues());
        }
      }
    }

    dialogBox.setText(npc.getCurrentDialog());

    // Informar ao jogador que está em diálogo (bloquear movimento)
    if (player != null) {
      player.setInDialog(true);
    }

    System.out.println("💬 Iniciando conversa com: " + npc.getName());
  }

  // Sistema de diálogo multi-etapas para desbloqueio de habilidades
  private String[] skillUnlockDialogs;
  private int currentSkillUnlockIndex = 0;

  /**
   * Mostra diálogo de desbloqueio de habilidade
   */
  private void showSkillUnlockDialog(int slot) {
    showingDialog = true;
    currentSkillUnlockIndex = 0;

    // Criar diálogos multi-etapas
    skillUnlockDialogs = new String[] {
        "Você sente toda a experiência acumulada ressoando em você...",
        "Seu corpo e mente se fortalecem com o conhecimento adquirido.",
        "HABILIDADE DESBLOQUEADA!",
        "Slot " + slot + " agora está disponível! Use a tecla " + slot + " para ativar."
    };

    dialogBox.setText(skillUnlockDialogs[0]);

    // Bloquear movimento do player
    if (player != null) {
      player.setInDialog(true);
    }

    System.out.println(" Mostrando diálogo de desbloqueio de habilidade - Slot " + slot);
  }

  /**
   * Encerra diálogo
   */
  private void endDialog() {
    showingDialog = false;
    waitingForQuestChoice = false;
    currentTalkingNPC = null;
    dialogBox.reset();
    questChoiceBox.hide();
    skillUnlockDialogs = null;
    currentSkillUnlockIndex = 0;

    // Limpar desbloqueio pendente se houver
    if (player != null && player.getPendingSkillUnlock() > 0) {
      player.clearPendingSkillUnlock();
    }

    // Informar ao jogador que não está mais em diálogo (liberar movimento)
    if (player != null) {
      player.setInDialog(false);
    }

    System.out.println("💬 Conversa encerrada");
  }

  /**
   * Verifica se o jogador está sobre um portal
   */
  private void checkPortalCollision() {
    if (player == null || tileMap == null || mapTransition.isTransitioning() || portalCooldownFrames > 0) {
      return;
    }

    // Calcular posição do jogador em tiles
    int playerTileX = (int) (player.getX() / TILE_SIZE);
    int playerTileY = (int) (player.getY() / TILE_SIZE);

    // Verificar se há portal nesta posição
    Portal portal = tileMap.getPortalAt(playerTileX, playerTileY);

    // Só rearma o sistema de portal quando o jogador sair totalmente do tile de
    // portal.
    if (portalNeedsClear) {
      if (portal == null) {
        portalNeedsClear = false;
      }
      return;
    }

    if (portal != null) {
      System.out.println("🚪 Player entrou no portal: " + portal.getName());
      triggerPortalTransition(portal);
    }
  }

  /**
   * Inicia transição para outro mapa via portal
   */
  private void triggerPortalTransition(Portal portal) {
    // Verificar se o mapa de destino existe
    if (!mapManager.hasMap(portal.getTargetMapId())) {
      System.err.println("❌ Mapa de destino não encontrado: " + portal.getTargetMapId());
      return;
    }

    // Iniciar transição
    // Usa spawn definido no próprio portal para respeitar o sentido da
    // entrada/saída entre salas.
    mapTransition.startTransition(
        mapManager.getMap(portal.getTargetMapId()).getFilePath(),
        portal.getTargetX(),
        portal.getTargetY());

    // Evita dupla ativação do mesmo portal durante os primeiros frames da
    // transição.
    portalCooldownFrames = PORTAL_COOLDOWN_FRAMES;
    portalNeedsClear = true;
  }

  /**
   * Troca efetivamente o mapa (chamado no meio da transição)
   */
  private void changeMap(String mapPath, int playerX, int playerY) {
    System.out.println("🔄 Trocando mapa...");

    // Determinar ID do mapa via MapManager (compatível com geração procedural)
    String mapId = mapManager.findMapIdByFilePath(mapPath);
    if (mapId == null) {
      // Fallback de segurança para não interromper a transição caso o arquivo não
      // esteja registrado.
      mapId = mapManager.getCurrentMapId();
    }

    // Recarregar mapa com ID
    tileMap.reloadMap(mapPath, mapId);

    // Reposicionar player
    if (player != null) {
      player.setPosition(playerX, playerY);
      movePlayerOffPortalIfNeeded();
    }

    // Bloqueio curto para impedir reentrada imediata ao final da troca de mapa.
    portalCooldownFrames = PORTAL_COOLDOWN_FRAMES;
    portalNeedsClear = true;

    // Reinicializar fog of war
    tileMap.getFogOfWar().resetFog();

    // Capturar mapa anterior ANTES de atualizar o MapManager
    String previousMapId = mapManager.getCurrentMapId();

    // Atualizar mapa atual no MapManager
    mapManager.setCurrentMap(mapId);

    // Tocar música do novo mapa
    if (musicManager != null) {
      musicManager.playMusicForMap(mapId);
    }

    // Persistir e restaurar estado de inimigos via WorldState
    if (enemyManager != null && worldState != null) {
      // Salvar estado do mapa que estamos deixando
      MapSimulation previousSim = worldState.getOrCreate(previousMapId);
      enemyManager.saveToSimulation(previousSim);
      previousSim.setHasActivePlayers(false);

      // Carregar (ou inicializar pela primeira vez) o novo mapa
      enemyManager.setCurrentMapId(mapId);
      MapSimulation nextSim = worldState.getOrCreate(mapId);
      nextSim.setHasActivePlayers(true);

      if (nextSim.isFamiliesInitialized()) {
        // Mapa já foi visitado — restaurar estado persistido
        enemyManager.loadFromSimulation(nextSim);
      } else {
        // Primeira visita — inicializar do zero e salvar imediatamente
        enemyManager.loadFromSimulation(nextSim); // limpa listas internas
        enemyManager.initializeGoblinFamilies(tileMap);
        enemyManager.saveToSimulation(nextSim);
      }
    } else if (enemyManager != null) {
      // Fallback sem WorldState (não deve ocorrer em uso normal)
      enemyManager.clearAllEnemies();
      enemyManager.setCurrentMapId(mapManager.getCurrentMapId());
      enemyManager.initializeGoblinFamilies(tileMap);
    }

    // Limpar NPCs antigos e criar novos
    npcs.clear();
    createExampleNPCs();

    System.out.println("✅ Mapa trocado com sucesso!");
  }

  /**
   * Se o jogador surgir exatamente sobre um portal, desloca para um tile vizinho
   * seguro.
   */
  private void movePlayerOffPortalIfNeeded() {
    if (player == null || tileMap == null) {
      return;
    }

    int playerTileX = (int) (player.getX() / TILE_SIZE);
    int playerTileY = (int) (player.getY() / TILE_SIZE);
    if (tileMap.getPortalAt(playerTileX, playerTileY) == null) {
      portalNeedsClear = false;
      return;
    }

    int[][] offsets = {
        { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 },
        { 0, -2 }, { 2, 0 }, { 0, 2 }, { -2, 0 }
    };

    for (int[] offset : offsets) {
      int nx = playerTileX + offset[0];
      int ny = playerTileY + offset[1];

      if (nx < 0 || ny < 0 || nx >= tileMap.getWidth() || ny >= tileMap.getHeight()) {
        continue;
      }
      if (!tileMap.isWalkable(nx, ny)) {
        continue;
      }
      if (tileMap.getPortalAt(nx, ny) != null) {
        continue;
      }

      player.setPosition(nx * TILE_SIZE, ny * TILE_SIZE);
      portalNeedsClear = false;
      return;
    }

    // Se não houver tile seguro próximo, apenas mantém cooldown mais longo para
    // evitar loop.
    portalCooldownFrames = Math.max(portalCooldownFrames, PORTAL_COOLDOWN_FRAMES * 2);
  }

}
