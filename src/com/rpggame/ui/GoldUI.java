package com.rpggame.ui;

import java.awt.*;
import com.rpggame.entities.Player;

/**
 * UI para exibir o gold do jogador
 */
public class GoldUI {
  private Player player;
  private int x, y;

  // Cores
  private static final Color BG_COLOR = new Color(20, 20, 30, 200);
  private static final Color BORDER_COLOR = new Color(255, 215, 0);
  private static final Color TEXT_COLOR = new Color(255, 255, 255);
  private static final Color GOLD_COLOR = new Color(255, 215, 0);

  public GoldUI(Player player) {
    this.player = player;
  }

  /**
   * Renderiza o UI de gold
   */
  public void render(Graphics2D g) {
    if (player == null || !player.shouldShowGoldUI())
      return;

    // Atualizar posição
    this.x = g.getClipBounds().width - 150;
    this.y = 10;

    int gold = player.getGold();
    int width = 130;
    int height = 40;

    // Fundo semi-transparente
    g.setColor(BG_COLOR);
    g.fillRoundRect(x, y, width, height, 10, 10);

    // Borda dourada
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(x, y, width, height, 10, 10);

    // Ícone de moeda (círculo dourado)
    int iconX = x + 15;
    int iconY = y + 12;
    int iconSize = 18;

    // Sombra do ícone
    g.setColor(new Color(0, 0, 0, 100));
    g.fillOval(iconX + 2, iconY + 2, iconSize, iconSize);

    // Ícone de moeda
    g.setColor(GOLD_COLOR);
    g.fillOval(iconX, iconY, iconSize, iconSize);

    // Borda da moeda
    g.setColor(new Color(200, 170, 50));
    g.setStroke(new BasicStroke(2));
    g.drawOval(iconX, iconY, iconSize, iconSize);

    // Símbolo $ na moeda
    g.setColor(new Color(150, 120, 30));
    g.setFont(new Font("Arial", Font.BOLD, 14));
    g.drawString("$", iconX + 5, iconY + 14);

    // Texto de gold
    g.setColor(TEXT_COLOR);
    g.setFont(new Font("Arial", Font.BOLD, 16));
    String goldText = String.valueOf(gold);

    // Centralizar o texto
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(goldText);
    int textX = x + width - textWidth - 15;
    int textY = y + 26;

    // Sombra do texto
    g.setColor(new Color(0, 0, 0, 150));
    g.drawString(goldText, textX + 1, textY + 1);

    // Texto principal
    g.setColor(GOLD_COLOR);
    g.drawString(goldText, textX, textY);
  }
}
