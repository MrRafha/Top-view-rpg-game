import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

/**
 * Painel principal onde o jogo é renderizado
 */
public class GamePanel extends JPanel implements KeyListener, MouseListener, Runnable {
  public static final int TILE_SIZE = 48; // Aumentado para dar zoom
  public static final int MAP_WIDTH = 15; // Novo mapa 15x15
  public static final int MAP_HEIGHT = 15; // Novo mapa 15x15

  private Thread gameThread;
  private boolean running = false;

  private Player player;
  private TileMap tileMap;
  private Camera camera;
  private EnemyManager enemyManager;

  // Telas do jogo
  private CharacterScreen characterScreen;
  private boolean showingCharacterScreen = false;

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
    // Criar o mapa de tiles
    tileMap = new TileMap();

    // Criar a câmera
    camera = new Camera(0, 0);

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
      // Encontrar uma posição centrada de grama para spawn do jogador
      Point spawnPosition = tileMap.getCenteredGrassPosition(33, 48); // Tamanho do player
      player = new Player(spawnPosition.x, spawnPosition.y, spritePath);
      player.setTileMap(tileMap);

      // Reinicializar o gerenciador de inimigos com o novo player
      enemyManager = new EnemyManager(player, tileMap);
      enemyManager.spawnInitialEnemies(tileMap);
    } else {
      // Fallback para posição central se tileMap ainda não foi inicializado
      player = new Player(360, 360, spritePath);
      System.out.println("Aviso: TileMap ainda não foi inicializado quando setPlayerClass foi chamado");
    }
  }

  public void setPlayerClass(String playerClass, String spritePath, CharacterStats stats) {
    // Conectar o mapa ao jogador para verificação de colisão
    if (tileMap != null) {
      // Encontrar uma posição aleatória de grama para spawn do jogador (centralizada
      // no tile)
      Point spawnPosition = tileMap.getCenteredGrassPosition(33, 48); // Player dimensions
      player = new Player(spawnPosition.x, spawnPosition.y, spritePath, playerClass, stats);
      player.setTileMap(tileMap);

      // Criar o gerenciador de inimigos com o novo player
      enemyManager = new EnemyManager(player, tileMap);
      enemyManager.spawnInitialEnemies(tileMap);

      // Iniciar o loop do jogo se ainda não estiver rodando
      if (gameThread == null || !gameThread.isAlive()) {
        startGameLoop();
      }

      System.out.println("=== PERSONAGEM CRIADO ===");
      System.out.println("Classe: " + playerClass);
      System.out.println("Stats: " + stats.toString());
      System.out.println("Posição: " + spawnPosition.x + ", " + spawnPosition.y);
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

    player.update();

    // Atualizar inimigos
    if (enemyManager != null) {
      enemyManager.update();

      // Verificar colisões
      enemyManager.checkProjectileCollisions(player.getProjectiles());
      enemyManager.checkPlayerCollisions();
    }

    // Atualizar câmera para seguir o jogador
    camera.centerOnPlayer(player);
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

    // Renderizar inimigos (apenas os visíveis)
    if (enemyManager != null) {
      enemyManager.render(g2d, camera, tileMap.getFogOfWar());
    }

    // Renderizar o jogador
    player.render(g2d, camera);

    // Renderizar UI
    renderUI(g2d);
  }

  private void renderUI(Graphics2D g) {
    if (player == null)
      return;

    // Barras de vida e mana (canto superior esquerdo)
    drawHealthAndManaBars(g, player);

    // Instruções de controle (canto inferior direito)
    String[] instructions = {
        "WASD - Movimento",
        "ESPAÇO - Atacar"
    };

    g.setFont(new Font("Arial", Font.PLAIN, 12));
    g.setColor(new Color(200, 200, 200));

    int instrX = Game.SCREEN_WIDTH - 150;
    int instrY = Game.SCREEN_HEIGHT - 50;

    for (int i = 0; i < instructions.length; i++) {
      g.drawString(instructions[i], instrX, instrY + (i * 15));
    }
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

    // Informação de inimigos (debug)
    if (enemyManager != null) {
      g.setFont(new Font("Arial", Font.PLAIN, 10));
      g.setColor(Color.LIGHT_GRAY);
      g.drawString("Inimigos: " + enemyManager.getAliveCount() + "/4",
          barX, barY + (barSpacing * 3) + 25);
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

    // Tecla C para abrir tela de características
    if (e.getKeyCode() == KeyEvent.VK_C) {
      openCharacterScreen();
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
}