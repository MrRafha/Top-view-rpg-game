package com.rpggame.items;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Arco Velho - Arma para Caçador
 * +5 de dano
 */
public class OldBow extends EquippableItem {

  public OldBow() {
    super(
        "Arco Velho",
        "Um arco de madeira resistente.\nAinda tem boa precisão.\n+5 de dano\nRequer: 5 Destreza\nPreço: 15 Gold",
        createSprite(),
        5, // damageBonus
        0, // requiredStrength
        5, // requiredDexterity
        0, // requiredIntelligence
        15); // goldCost
  }

  private static BufferedImage createSprite() {
    BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = sprite.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Arco de madeira (curva)
    g.setColor(new Color(120, 80, 40));
    g.setStroke(new BasicStroke(3));
    g.drawArc(8, 4, 16, 24, -30, 240);

    // Corda
    g.setColor(new Color(200, 200, 180));
    g.setStroke(new BasicStroke(1));
    g.drawLine(11, 7, 11, 25);

    // Flecha decorativa
    g.setColor(new Color(180, 140, 90));
    g.setStroke(new BasicStroke(2));
    g.drawLine(20, 16, 12, 16);

    // Ponta da flecha
    g.setColor(new Color(150, 150, 150));
    int[] xPoints = { 20, 24, 20 };
    int[] yPoints = { 14, 16, 18 };
    g.fillPolygon(xPoints, yPoints, 3);

    g.dispose();
    return sprite;
  }
}
