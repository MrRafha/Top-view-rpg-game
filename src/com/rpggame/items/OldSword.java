package com.rpggame.items;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Espada Velha - Arma para Guerreiro
 * +5 de dano
 */
public class OldSword extends EquippableItem {

  public OldSword() {
    super(
        "Espada Velha",
        "Uma espada desgastada pelo tempo.\nAinda pode ser útil em batalha.\n+5 de dano\nRequer: 5 Força\nPreço: 15 Gold",
        createSprite(),
        5, // damageBonus
        5, // requiredStrength
        0, // requiredDexterity
        0, // requiredIntelligence
        15); // goldCost
  }

  private static BufferedImage createSprite() {
    BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = sprite.createGraphics();

    // Lâmina cinza
    g.setColor(new Color(150, 150, 150));
    g.fillRect(14, 5, 4, 20);

    // Guarda dourada
    g.setColor(new Color(180, 140, 0));
    g.fillRect(8, 24, 16, 3);

    // Cabo marrom
    g.setColor(new Color(120, 80, 40));
    g.fillRect(13, 27, 6, 5);

    // Brilho na lâmina
    g.setColor(new Color(200, 200, 200));
    g.fillRect(15, 8, 1, 10);

    g.dispose();
    return sprite;
  }
}
