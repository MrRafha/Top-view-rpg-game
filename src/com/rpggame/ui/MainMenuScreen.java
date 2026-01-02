package com.rpggame.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.rpggame.systems.MusicManager;

/**
 * Tela de menu principal do jogo
 */
public class MainMenuScreen extends JPanel {
  private JFrame parentFrame;
  private MusicManager musicManager;
  private boolean musicEnabled = true;

  // Botões
  private Rectangle playButton;
  private Rectangle exitButton;
  private Rectangle musicToggleButton;

  // Estados de hover
  private boolean playHover = false;
  private boolean exitHover = false;
  private boolean musicHover = false;

  // Cores do tema
  private static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
  private static final Color TITLE_COLOR = new Color(220, 180, 100);
  private static final Color BUTTON_COLOR = new Color(50, 50, 70);
  private static final Color BUTTON_HOVER_COLOR = new Color(70, 70, 100);
  private static final Color BUTTON_TEXT_COLOR = new Color(220, 220, 220);
  private static final Color MUSIC_ICON_COLOR = new Color(200, 150, 50);

  public MainMenuScreen(JFrame frame) {
    this.parentFrame = frame;
    this.musicManager = new MusicManager();

    setLayout(null);
    setPreferredSize(new Dimension(1024, 800));
    setBackground(BACKGROUND_COLOR);
    setFocusable(true);

    // Inicializar posições dos botões
    int buttonWidth = 200;
    int buttonHeight = 60;
    int centerX = 512; // Metade de 1024
    int startY = 400;

    playButton = new Rectangle(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight);
    exitButton = new Rectangle(centerX - buttonWidth / 2, startY + 100, buttonWidth, buttonHeight);

    // Botão de música no canto inferior esquerdo
    musicToggleButton = new Rectangle(20, 720, 60, 60);

    // Adicionar listeners
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        handleMouseClick(e.getX(), e.getY());
      }
    });

    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        handleMouseMove(e.getX(), e.getY());
      }
    });

    // Iniciar música do menu
    musicManager.playMusicByPath("songs/MainOST.wav");

    System.out.println("🎮 Menu Principal iniciado");
  }

  private void handleMouseMove(int x, int y) {
    boolean needsRepaint = false;

    boolean newPlayHover = playButton.contains(x, y);
    boolean newExitHover = exitButton.contains(x, y);
    boolean newMusicHover = musicToggleButton.contains(x, y);

    if (newPlayHover != playHover || newExitHover != exitHover || newMusicHover != musicHover) {
      playHover = newPlayHover;
      exitHover = newExitHover;
      musicHover = newMusicHover;
      needsRepaint = true;
    }

    if (needsRepaint) {
      repaint();
    }
  }

  private void handleMouseClick(int x, int y) {
    // Botão Jogar
    if (playButton.contains(x, y)) {
      System.out.println("▶️ Iniciando jogo...");
      startGame();
    }

    // Botão Sair
    if (exitButton.contains(x, y)) {
      System.out.println("👋 Saindo do jogo...");
      exitGame();
    }

    // Botão de música
    if (musicToggleButton.contains(x, y)) {
      toggleMusic();
    }
  }

  private void startGame() {
    // Parar música do menu
    musicManager.stopMusic();

    // Trocar para tela de criação de personagem
    parentFrame.getContentPane().removeAll();
    com.rpggame.ui.CombinedCharacterScreen characterScreen = new com.rpggame.ui.CombinedCharacterScreen(parentFrame);
    parentFrame.add(characterScreen);
    parentFrame.revalidate();
    parentFrame.repaint();
    characterScreen.requestFocusInWindow();
  }

  private void exitGame() {
    musicManager.cleanup();
    System.exit(0);
  }

  private void toggleMusic() {
    musicEnabled = !musicEnabled;

    if (musicEnabled) {
      musicManager.resumeMusic();
      System.out.println("🎵 Música ativada");
    } else {
      musicManager.pauseMusic();
      System.out.println("🔇 Música desativada");
    }

    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;

    // Ativar anti-aliasing
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Desenhar título do jogo
    drawTitle(g2d);

    // Desenhar botões
    drawButton(g2d, playButton, "Jogar", playHover);
    drawButton(g2d, exitButton, "Sair", exitHover);

    // Desenhar botão de música
    drawMusicButton(g2d);
  }

  private void drawTitle(Graphics2D g) {
    // Título principal
    Font titleFont = new Font("Serif", Font.BOLD, 56);
    g.setFont(titleFont);
    g.setColor(TITLE_COLOR);

    String title = "Echoes of Forgotten Quests";
    FontMetrics fm = g.getFontMetrics();
    int titleWidth = fm.stringWidth(title);
    int titleX = (1024 - titleWidth) / 2;

    // Sombra do título
    g.setColor(new Color(0, 0, 0, 150));
    g.drawString(title, titleX + 3, 153);

    // Título
    g.setColor(TITLE_COLOR);
    g.drawString(title, titleX, 150);

    // Subtítulo
    Font subtitleFont = new Font("Sans-serif", Font.ITALIC, 18);
    g.setFont(subtitleFont);
    g.setColor(new Color(180, 180, 180));
    String subtitle = "Um RPG de aventura épica";
    int subtitleWidth = g.getFontMetrics().stringWidth(subtitle);
    g.drawString(subtitle, (1024 - subtitleWidth) / 2, 200);
  }

  private void drawButton(Graphics2D g, Rectangle button, String text, boolean hover) {
    // Cor de fundo do botão
    if (hover) {
      g.setColor(BUTTON_HOVER_COLOR);
    } else {
      g.setColor(BUTTON_COLOR);
    }

    // Desenhar botão com bordas arredondadas
    g.fillRoundRect(button.x, button.y, button.width, button.height, 15, 15);

    // Borda do botão
    g.setColor(TITLE_COLOR);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(button.x, button.y, button.width, button.height, 15, 15);

    // Texto do botão
    Font buttonFont = new Font("Sans-serif", Font.BOLD, 24);
    g.setFont(buttonFont);
    g.setColor(BUTTON_TEXT_COLOR);

    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(text);
    int textX = button.x + (button.width - textWidth) / 2;
    int textY = button.y + ((button.height - fm.getHeight()) / 2) + fm.getAscent();

    g.drawString(text, textX, textY);
  }

  private void drawMusicButton(Graphics2D g) {
    // Fundo do botão
    if (musicHover) {
      g.setColor(BUTTON_HOVER_COLOR);
    } else {
      g.setColor(BUTTON_COLOR);
    }
    g.fillRoundRect(musicToggleButton.x, musicToggleButton.y,
        musicToggleButton.width, musicToggleButton.height, 10, 10);

    // Borda
    g.setColor(TITLE_COLOR);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(musicToggleButton.x, musicToggleButton.y,
        musicToggleButton.width, musicToggleButton.height, 10, 10);

    // Ícone de nota musical
    int centerX = musicToggleButton.x + musicToggleButton.width / 2;
    int centerY = musicToggleButton.y + musicToggleButton.height / 2;

    if (musicEnabled) {
      g.setColor(MUSIC_ICON_COLOR);
    } else {
      g.setColor(new Color(100, 100, 100));
    }

    // Desenhar nota musical (♪)
    Font musicFont = new Font("Serif", Font.BOLD, 36);
    g.setFont(musicFont);
    String note = "♪";
    FontMetrics fm = g.getFontMetrics();
    int noteWidth = fm.stringWidth(note);
    g.drawString(note, centerX - noteWidth / 2, centerY + fm.getAscent() / 2);

    // Desenhar X se desativado
    if (!musicEnabled) {
      g.setColor(new Color(200, 50, 50));
      g.setStroke(new BasicStroke(3));
      int margin = 10;
      g.drawLine(musicToggleButton.x + margin, musicToggleButton.y + margin,
          musicToggleButton.x + musicToggleButton.width - margin,
          musicToggleButton.y + musicToggleButton.height - margin);
      g.drawLine(musicToggleButton.x + musicToggleButton.width - margin, musicToggleButton.y + margin,
          musicToggleButton.x + margin, musicToggleButton.y + musicToggleButton.height - margin);
    }
  }
}
