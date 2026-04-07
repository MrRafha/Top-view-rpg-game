package com.rpggame.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * GameLauncher - Menu principal que oferece três caminhos:
 * 1. Jogar Solo (entrada direta em sessão local)
 * 2. Criar Servidor (inicia servidor e conecta)
 * 3. Entrar em Servidor (descobre e conecta a servidor existente)
 * 
 * Fase 7: Multiplayer Lobby
 */
public class GameLauncher extends JPanel {
  private static final int BUTTON_WIDTH = 300;
  private static final int BUTTON_HEIGHT = 60;
  private static final int BUTTON_SPACING = 30;

  private Rectangle soloButton;
  private Rectangle createServerButton;
  private Rectangle joinServerButton;

  private OnLaunchListener launchListener;

  public interface OnLaunchListener {
    void onSoloPlay();

    void onCreateServer();

    void onJoinServer();
  }

  public GameLauncher(OnLaunchListener listener) {
    this.launchListener = listener;
    setBackground(new Color(20, 20, 30));
    setFocusable(true);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        handleMouseClick(e);
      }
    });
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int width = getWidth();
    int height = getHeight();

    // Desenhar título
    g2d.setColor(new Color(200, 150, 100));
    g2d.setFont(new Font("Arial", Font.BOLD, 48));
    FontMetrics fm = g2d.getFontMetrics();
    String title = "Echoes of Forgotten Quests";
    int titleX = (width - fm.stringWidth(title)) / 2;
    g2d.drawString(title, titleX, 120);

    // Calcular posição dos botões
    int centerX = (width - BUTTON_WIDTH) / 2;
    int centerY = (height - (3 * BUTTON_HEIGHT + 2 * BUTTON_SPACING)) / 2;

    // Botões
    soloButton = new Rectangle(centerX, centerY, BUTTON_WIDTH, BUTTON_HEIGHT);
    createServerButton = new Rectangle(centerX, centerY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT);
    joinServerButton = new Rectangle(centerX, centerY + 2 * (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH,
        BUTTON_HEIGHT);

    // Desenhar botões
    drawButton(g2d, soloButton, "Jogar Solo", false);
    drawButton(g2d, createServerButton, "Criar Servidor", false);
    drawButton(g2d, joinServerButton, "Entrar em Servidor", false);

    // Desenhar subtítulo
    g2d.setColor(new Color(150, 150, 150));
    g2d.setFont(new Font("Arial", Font.PLAIN, 14));
    g2d.drawString("Escolha um modo de jogo", (width - fm.stringWidth("Escolha um modo de jogo")) / 2, height - 40);
  }

  private void drawButton(Graphics2D g2d, Rectangle rect, String text, boolean hovered) {
    // Fundo do botão
    Color bgColor = hovered ? new Color(80, 120, 150) : new Color(50, 80, 120);
    g2d.setColor(bgColor);

    RoundRectangle2D roundedRect = new RoundRectangle2D.Float(
        rect.x, rect.y, rect.width, rect.height, 15, 15);
    g2d.fill(roundedRect);

    // Borda
    g2d.setColor(new Color(150, 150, 150));
    g2d.setStroke(new BasicStroke(2));
    g2d.draw(roundedRect);

    // Texto
    g2d.setColor(Color.WHITE);
    g2d.setFont(new Font("Arial", Font.BOLD, 18));
    FontMetrics fm = g2d.getFontMetrics();
    int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
    int textY = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
    g2d.drawString(text, textX, textY);
  }

  private void handleMouseClick(MouseEvent e) {
    if (soloButton != null && soloButton.contains(e.getPoint())) {
      if (launchListener != null) {
        launchListener.onSoloPlay();
      }
    } else if (createServerButton != null && createServerButton.contains(e.getPoint())) {
      if (launchListener != null) {
        launchListener.onCreateServer();
      }
    } else if (joinServerButton != null && joinServerButton.contains(e.getPoint())) {
      if (launchListener != null) {
        launchListener.onJoinServer();
      }
    }
  }

  public static void showLauncher(JFrame frame, OnLaunchListener listener) {
    GameLauncher launcher = new GameLauncher(listener);
    frame.setContentPane(launcher);
    frame.setVisible(true);
  }
}
