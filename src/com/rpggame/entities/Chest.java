package com.rpggame.entities;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import com.rpggame.world.Camera;

/**
 * Baú que contém itens e requer um minigame para abrir.
 */
public class Chest {
  private double chestX;
  private double chestY;
  private int width = 48;
  private int height = 48;

  private BufferedImage closedSprite;
  private BufferedImage openedSprite;

  private boolean opened = false;
  private boolean playerNearby = false;

  // Itens que o baú pode conter
  private String[] possibleItems = {
      "health_potion",
      "mana_potion"
  };

  /**
   * Construtor do Chest.
   */
  public Chest(double x, double y) {
    this.chestX = x;
    this.chestY = y;
    loadSprites();
  }

  /**
   * Carrega sprites do baú.
   */
  private void loadSprites() {
    closedSprite = loadSpriteFile("sprites/ClosedChest.png");
    openedSprite = loadSpriteFile("sprites/OpenedChest.png");

    if (closedSprite != null && openedSprite != null) {
      System.out.println("✅ Sprites do baú carregados");
    }
  }

  /**
   * Carrega um sprite individual.
   */
  private BufferedImage loadSpriteFile(String path) {
    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream(path);
      if (is != null) {
        BufferedImage img = ImageIO.read(is);
        is.close();
        return img;
      }

      String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(path);
      java.io.File file = new java.io.File(resolvedPath);
      if (file.exists()) {
        return ImageIO.read(file);
      }
    } catch (IOException e) {
      System.err.println("❌ Erro ao carregar sprite: " + path);
    }
    return null;
  }

  /**
   * Atualiza o baú.
   */
  public void update(Player player) {
    if (opened) {
      return;
    }

    // Verificar se player está próximo
    double distance = Math.sqrt(
        Math.pow(player.getX() - chestX, 2)
            + Math.pow(player.getY() - chestY, 2));

    playerNearby = distance <= 70;
  }

  /**
   * Renderiza o baú.
   */
  public void render(Graphics2D g, Camera camera, com.rpggame.world.FogOfWar fogOfWar) {
    // Verificar se está no campo de visão
    int tileX = (int) (chestX / 48); // TILE_SIZE = 48
    int tileY = (int) (chestY / 48);

    if (fogOfWar != null && !fogOfWar.isVisible(tileX, tileY)) {
      return; // Não renderizar se não estiver visível
    }

    int screenX = (int) (chestX - camera.getX());
    int screenY = (int) (chestY - camera.getY());

    BufferedImage currentSprite = opened ? openedSprite : closedSprite;

    if (currentSprite != null) {
      g.drawImage(currentSprite, screenX, screenY, width, height, null);
    } else {
      // Fallback
      g.setColor(opened ? Color.GRAY : new Color(139, 69, 19));
      g.fillRect(screenX, screenY, width, height);
    }

    // Mostrar indicador de interação
    if (playerNearby && !opened) {
      g.setColor(Color.YELLOW);
      g.setFont(new Font("Arial", Font.BOLD, 12));
      String text = "[F] Abrir";
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(text);
      g.drawString(text, screenX + (width - textWidth) / 2, screenY - 5);
    }
  }

  /**
   * Verifica se player está próximo e pode interagir.
   */
  public boolean canInteract() {
    return playerNearby && !opened;
  }

  /**
   * Marca o baú como aberto.
   */
  public void open() {
    this.opened = true;
    System.out.println("✅ Baú aberto!");
  }

  /**
   * Retorna 2 itens aleatórios.
   */
  public String[] getRewards() {
    String[] rewards = new String[2];
    for (int i = 0; i < 2; i++) {
      rewards[i] = possibleItems[(int) (Math.random() * possibleItems.length)];
    }
    return rewards;
  }

  // Getters
  public double getX() {
    return chestX;
  }

  public double getY() {
    return chestY;
  }

  public boolean isOpened() {
    return opened;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
