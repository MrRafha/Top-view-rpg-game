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

  // FPS
  private final int FPS = 60;
  private final long TARGET_TIME = 1000000000 / FPS;

  public GamePanel() {
    setPreferredSize(new Dimension(Game.SCREEN_WIDTH, Game.SCREEN_HEIGHT));
    setBackground(Color.BLACK);
    setFocusable(true);
    addKeyListener(this);
    addMouseListener(this);

    // Garantir que o painel receba foco
    requestFocusInWindow();

    initializeGame();
    startGameLoop();
  }

  private void initializeGame() {
    // Criar o mapa de tiles
    tileMap = new TileMap();

    // Criar o jogador no centro do novo mapa 15x15 (usando sprite do Warrior por
    // padrão)
    // Posição central aproximada: (15*48)/2 = 360px
    player = new Player(360, 360, "sprites/WarriorPlayer.png");

    // Conectar o mapa ao jogador para verificação de colisão
    player.setTileMap(tileMap);

    // Criar a câmera
    camera = new Camera(0, 0);

    System.out.println("=== JOGO INICIALIZADO ===");
    System.out.println("Tamanho do player: " + player.getWidth() + "x" + player.getHeight());
    System.out.println("Tamanho dos tiles: " + TILE_SIZE + "px");
    System.out.println("Mapa: " + MAP_WIDTH + "x" + MAP_HEIGHT + " tiles");
    System.out.println("Controles: WASD ou setas para mover, ESPAÇO para atacar");
    System.out.println("========================");
  }

  public void setPlayerClass(String playerClass, String spritePath) {
    // Criar novo jogador com a classe selecionada no centro do mapa 15x15
    player = new Player(360, 360, spritePath);

    // Verificar se o tileMap já foi inicializado antes de conectar
    if (tileMap != null) {
      player.setTileMap(tileMap);
    } else {
      System.out.println("Aviso: TileMap ainda não foi inicializado quando setPlayerClass foi chamado");
    }
  }

  public void setPlayerClass(String playerClass, String spritePath, CharacterStats stats) {
    // Criar novo jogador com a classe e atributos selecionados no centro do mapa 15x15
    player = new Player(360, 360, spritePath, playerClass, stats);
    
    // Conectar o mapa ao jogador para verificação de colisão
    if (tileMap != null) {
      player.setTileMap(tileMap);
    } else {
      System.out.println("Aviso: TileMap ainda não foi inicializado quando setPlayerClass(com stats) foi chamado");
    }
  }

  private void startGameLoop() {
    gameThread = new Thread(this);
    running = true;
    gameThread.start();
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
    player.update();

    // Atualizar câmera para seguir o jogador
    camera.centerOnPlayer(player);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;

    // Aplicar antialiasing
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Renderizar o mapa
    tileMap.render(g2d, camera, player);

    // Renderizar o jogador
    player.render(g2d, camera);

    // Renderizar UI
    renderUI(g2d);
  }

  private void renderUI(Graphics2D g) {
    if (player == null)
      return;

    // Painel de informações do jogador (canto superior esquerdo)
    int panelX = 10;
    int panelY = 10;
    int panelWidth = 280;
    int panelHeight = 135;

    // Fundo do painel
    g.setColor(new Color(0, 0, 0, 150));
    g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
    g.setColor(new Color(100, 150, 200));
    g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);

    // Fonte para o texto
    g.setFont(new Font("Arial", Font.BOLD, 14));
    g.setColor(Color.WHITE);

    // Informações do jogador
    int textY = panelY + 20;
    int lineHeight = 15;

    g.drawString("Classe: " + player.getPlayerClass(), panelX + 10, textY);
    textY += lineHeight;

    CharacterStats stats = player.getStats();
    if (stats != null) {
      g.drawString("FOR:" + stats.getStrength() + " DEX:" + stats.getDexterity() + " INT:" + stats.getIntelligence(),
          panelX + 10, textY);
      textY += lineHeight;
      g.drawString("SAB:" + stats.getWisdom() + " CAR:" + stats.getCharisma() + " CON:" + stats.getConstitution(),
          panelX + 10, textY);
      textY += lineHeight;

      g.drawString("Dano: " + (10 + stats.getDamageBonus()) + " | Slots: " + stats.getInventorySlots(), panelX + 10,
          textY);
      textY += lineHeight;

      g.drawString("XP: +" + String.format("%.0f%%", (stats.getXpMultiplier() - 1) * 100) +
          " | Evasão: " + String.format("%.0f%%", stats.getEvasionChance() * 100), panelX + 10, textY);
      textY += lineHeight;

      g.drawString("Defesa: -" + String.format("%.0f%%", stats.getDamageReduction() * 100) + " dano recebido",
          panelX + 10, textY);
      textY += lineHeight;

      // Barra de vida detalhada
      g.drawString("Vida: " + player.getCurrentHealth() + "/" + player.getMaxHealth(), panelX + 10, textY);
    } // Instruções de controle (canto inferior direito)
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

  @Override
  public void keyPressed(KeyEvent e) {
    player.keyPressed(e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    player.keyReleased(e);
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
}