package com.rpggame.items;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Cajado Velho - Arma para Mago
 * +5 de dano
 */
public class OldStaff extends EquippableItem {

  public OldStaff() {
    super(
        "Cajado Velho",
        "Um cajado antigo imbuído com\nmagia residual.\n+5 de dano mágico\nRequer: 5 Inteligência\nPreço: 15 Gold",
        createSprite(),
        5, // damageBonus
        0, // requiredStrength
        0, // requiredDexterity
        5, // requiredIntelligence
        15); // goldCost
  }

  private static BufferedImage createSprite() {
    BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = sprite.createGraphics();

    // Haste de madeira marrom
    g.setColor(new Color(100, 70, 40));
    g.fillRect(14, 10, 4, 22);

    // Cristal roxo no topo
    g.setColor(new Color(150, 80, 200));
    int[] xPoints = { 16, 12, 16, 20 };
    int[] yPoints = { 5, 10, 15, 10 };
    g.fillPolygon(xPoints, yPoints, 4);

    // Brilho no cristal
    g.setColor(new Color(200, 150, 255, 180));
    g.fillOval(14, 8, 4, 4);

    g.dispose();
    return sprite;
  }
}
