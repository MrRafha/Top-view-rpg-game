package com.rpggame.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import com.rpggame.entities.Player;
import com.rpggame.npcs.NPC;
import com.rpggame.npcs.MerchantNPC;
import com.rpggame.npcs.GuardNPC;
import com.rpggame.npcs.VillagerNPC;
import com.rpggame.npcs.WiseManNPC;
import com.rpggame.world.*;
import com.rpggame.systems.*;
import com.rpggame.ui.CharacterScreen;
import com.rpggame.ui.DialogBox;
import com.rpggame.ui.SkillSlotUI;

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

  // Sistema de NPCs e diálogos
  private java.util.ArrayList<NPC> npcs;
  private DialogBox dialogBox;
  private NPC currentTalkingNPC = null;
  private boolean showingDialog = false;

  // Sistema de UI de habilidades
  private SkillSlotUI skillSlotUI;

  // Sistema de mapas e transições
  private MapManager mapManager;
  private MapTransition mapTransition;

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

  public GamePanel() {
    setPreferredSize(new Dimension(Game.SCREEN_WIDTH, Game.SCREEN_HEIGHT));
    setBackground(Color.BLACK);
    setFocusable(true);
    addKeyListener(this);
    addMouseListener(this);

    // Garantir que use layout null por padrão para renderização custom
    setLayout(null);

    // Garantir que o painel receba foco
    requestFocusInWindow();

    initializeGame();
    // startGameLoop() será chamado quando setPlayerClass for executado
  }

  private void initializeGame() {
    // Inicializar sistema de mapas primeiro
    mapManager = new MapManager();

    // Criar o mapa de tiles
    tileMap = new TileMap();

    // Carregar o mapa inicial correto baseado no MapManager
    MapManager.MapData initialMap = mapManager.getCurrentMap();
    if (initialMap != null) {
      tileMap.reloadMap(initialMap.getFilePath(), mapManager.getCurrentMapId());
      System.out.println("✅ Mapa inicial carregado: " + initialMap.getName());
    }

    // Criar a câmera
    camera = new Camera(0, 0);

    // Inicializar sistema de diálogos
    dialogBox = new DialogBox();
    npcs = new java.util.ArrayList<>();

    // Inicializar sistema de transições
    mapTransition = new MapTransition();

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
      // Posição inicial fixa no mapa village
      player = new Player(638, 260, spritePath);
      player.setTileMap(tileMap);

      // Reinicializar o gerenciador de inimigos com o novo player
      enemyManager = new EnemyManager(player, tileMap);
      player.setEnemyManager(enemyManager); // Conectar player ao enemy manager
      enemyManager.setCurrentMapId(mapManager.getCurrentMapId());
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
      // Posição inicial fixa no mapa village (x:638, y:260)
      player = new Player(638, 260, spritePath, playerClass, stats);
      player.setTileMap(tileMap);

      // Criar o gerenciador de inimigos com o novo player
      enemyManager = new EnemyManager(player, tileMap);
      player.setEnemyManager(enemyManager); // Conectar player ao enemy manager
      enemyManager.setCurrentMapId(mapManager.getCurrentMapId());
      enemyManager.initializeGoblinFamilies(tileMap);

      // Inicializar UI de slots de habilidades
      if (player.getSkillManager() != null) {
        skillSlotUI = new SkillSlotUI(player.getSkillManager(), Game.SCREEN_WIDTH);
      }

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
    long startTime, elapsed, wait;

    while (running) {
      startTime = System.nanoTime();

      update();
      repaint();

      elapsed = System.nanoTime() - startTime;
      wait = TARGET_TIME - elapsed;

      if (wait > 0) {
        try {
          Thread.sleep(wait / 1000000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void update() {
    // Só atualizar se o player foi criado e não estiver na tela de características
    if (player == null || showingCharacterScreen)
      return;

    // Verificar se player morreu
    if (!playerDead && player != null && !player.isAlive()) {
      playerDead = true;
      deathTransitionStarted = false;
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

    player.update();

    // Verificar desbloqueio de habilidade pendente
    if (player.getPendingSkillUnlock() > 0 && !showingDialog) {
      showSkillUnlockDialog(player.getPendingSkillUnlock());
    }

    // Atualizar NPCs
    updateNPCs();

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

    // Renderizar estruturas (cabanas)
    if (enemyManager != null) {
      enemyManager.renderStructures(g2d, camera);
    }

    // Renderizar inimigos (apenas os visíveis)
    if (enemyManager != null) {
      enemyManager.render(g2d, camera, tileMap.getFogOfWar());
    }

    // Renderizar cones de visão (debug)
    if (showVisionCones && enemyManager != null) {
      enemyManager.renderVisionCones(g2d, camera);
    }

    // Renderizar efeitos visuais de ataque dos goblins
    if (enemyManager != null) {
      enemyManager.renderAttackEffects(g2d, camera);
    }

    // Renderizar NPCs
    renderNPCs(g2d);

    // Renderizar o jogador
    player.render(g2d, camera);

    // Renderizar habilidades do jogador (efeitos visuais)
    if (player.getSkillManager() != null) {
      player.getSkillManager().render(g2d, camera);
    }

    // Renderizar UI
    renderUI(g2d);

    // Renderizar DialogBox se estiver mostrando
    if (showingDialog && dialogBox != null) {
      String npcName = currentTalkingNPC != null ? currentTalkingNPC.getName() : "Sistema";
      dialogBox.render(g2d, npcName, getWidth(), getHeight());
    }

    // Renderizar transição de mapa (sempre por último, em cima de tudo)
    if (mapTransition != null && mapTransition.isTransitioning()) {
      mapTransition.render(g2d, getWidth(), getHeight());
    }

    // Renderizar tela de morte (se ativa)
    if (showingDeathScreen) {
      renderDeathScreen(g2d);
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

    // Botão "Novo Jogo"
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

    // Texto do botão
    g.setFont(new Font("Arial", Font.BOLD, 24));
    String buttonText = "Novo Jogo";
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

    // Barra de Vida
    drawBar(g, "VIDA", barX, barY, barWidth, barHeight,
        player.getCurrentHealth(), player.getMaxHealth(),
        Color.RED, Color.DARK_GRAY);

    // Barra de Mana
    drawBar(g, "MANA", barX, barY + barSpacing, barWidth, barHeight,
        player.getCurrentMana(), player.getMaxMana(),
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

    // Se player ainda não foi criado, ignorar input
    if (player == null) {
      return;
    }

    // Tecla E para interagir com NPCs
    if (e.getKeyCode() == KeyEvent.VK_E) {
      interactWithNearbyNPC();
      return;
    }

    // Tecla C para abrir tela de características
    if (e.getKeyCode() == KeyEvent.VK_C) {
      openCharacterScreen();
      return;
    }

    // Tecla V para ativar/desativar modo debug
    if (e.getKeyCode() == KeyEvent.VK_V) {
      showVisionCones = !showVisionCones;
      System.out.println("� Modo Debug: " + (showVisionCones ? "ATIVADO" : "DESATIVADO") +
          " (Campo de visão, contadores de inimigos, posição do player)");
      return;
    }

    player.keyPressed(e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // Se player ainda não foi criado, ignorar input
    if (player != null) {
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

    // Verificar clique no botão "Novo Jogo" na tela de morte
    if (showingDeathScreen && newGameButton != null) {
      if (newGameButton.contains(e.getPoint())) {
        restartGame();
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
   * Reinicia o jogo voltando para a tela de criação de personagem
   */
  private void restartGame() {
    System.out.println("🔄 Reiniciando jogo...");

    // Parar o game loop
    running = false;

    // Resetar estados
    playerDead = false;
    deathTransitionStarted = false;
    showingDeathScreen = false;
    newGameButton = null;

    // Limpar referências
    player = null;
    enemyManager = null;

    // Fechar a janela atual e voltar para tela de criação
    SwingUtilities.invokeLater(() -> {
      JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
      if (topFrame != null) {
        topFrame.dispose();
      }

      // Criar nova janela com tela de criação
      Game.main(new String[] {});
    });
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
      npcs.add(new MerchantNPC(500, 400));
      npcs.add(new VillagerNPC(300, 300));
      npcs.add(new WiseManNPC(900, 500));
      System.out.println("🏘️ NPCs da vila criados: " + npcs.size());
    } else if ("goblin_territories".equals(currentMapId)) {
      // Territórios Goblin: Guards protegendo a entrada da vila (ao redor do spawn
      // tile 12,3)
      npcs.add(new GuardNPC(480, 144)); // Esquerda do spawn (tile 10, 3)
      npcs.add(new GuardNPC(672, 144)); // Direita do spawn (tile 14, 3)
      System.out.println("⚔️ Guards dos territórios criados: " + npcs.size());
    }
    // Outros mapas podem não ter NPCs
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
    }
  }

  /**
   * Renderiza NPCs
   */
  private void renderNPCs(Graphics2D g) {
    for (NPC npc : npcs) {
      npc.render(g, camera);
    }
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
            dialogBox.setText(currentTalkingNPC.getCurrentDialog());
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
    currentTalkingNPC = null;
    dialogBox.reset();
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
    if (player == null || tileMap == null || mapTransition.isTransitioning()) {
      return;
    }

    // Calcular posição do jogador em tiles
    int playerTileX = (int) (player.getX() / TILE_SIZE);
    int playerTileY = (int) (player.getY() / TILE_SIZE);

    // Verificar se há portal nesta posição
    Portal portal = tileMap.getPortalAt(playerTileX, playerTileY);

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

    // Obter dados do mapa de destino
    MapManager.MapData targetMap = mapManager.getMap(portal.getTargetMapId());

    // Usar spawn point do mapa de destino
    int spawnX = targetMap.getDefaultSpawnX();
    int spawnY = targetMap.getDefaultSpawnY();

    // Iniciar transição
    mapTransition.startTransition(
        targetMap.getFilePath(),
        spawnX,
        spawnY);
  }

  /**
   * Troca efetivamente o mapa (chamado no meio da transição)
   */
  private void changeMap(String mapPath, int playerX, int playerY) {
    System.out.println("🔄 Trocando mapa...");

    // Determinar ID do mapa baseado no caminho
    String mapId;
    if (mapPath.contains("village")) {
      mapId = "village";
    } else if (mapPath.contains("goblin_territories")) {
      mapId = "goblin_territories";
    } else if (mapPath.contains("cave") || mapPath.contains("new_map")) {
      mapId = "cave";
    } else {
      mapId = "goblin_territories"; // Padrão
    }

    // Recarregar mapa com ID
    tileMap.reloadMap(mapPath, mapId);

    // Reposicionar player
    if (player != null) {
      player.setPosition(playerX, playerY);
    }

    // Reinicializar fog of war
    tileMap.getFogOfWar().resetFog();

    // Atualizar mapa atual no MapManager
    mapManager.setCurrentMap(mapId);

    // Reinicializar inimigos
    if (enemyManager != null) {
      enemyManager.clearAllEnemies();
      enemyManager.setCurrentMapId(mapManager.getCurrentMapId());
      enemyManager.initializeGoblinFamilies(tileMap);
    }

    // Limpar NPCs antigos e criar novos
    npcs.clear();
    createExampleNPCs();

    System.out.println("✅ Mapa trocado com sucesso!");
  }

}
