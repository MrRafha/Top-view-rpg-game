package com.rpggame.entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Classe para NPCs com os quais o jogador pode interagir e conversar
 */
public class NPC {
  private double x, y;
  private int width, height;
  private String name;
  private String[] dialogLines; // Múltiplas linhas de diálogo
  private int currentDialogIndex = 0;

  // Sprite
  private BufferedImage sprite;
  private String spritePath;

  // Indicador de interação
  private boolean showInteractionPrompt = false;
  private static final int INTERACTION_RANGE = 60; // Pixels de distância para interagir

  /**
   * Construtor do NPC
   * 
   * @param x           Posição X no mundo
   * @param y           Posição Y no mundo
   * @param name        Nome do NPC
   * @param spritePath  Caminho do sprite
   * @param dialogLines Linhas de diálogo (array de strings)
   */
  public NPC(double x, double y, String name, String spritePath, String... dialogLines) {
    this.x = x;
    this.y = y;
    this.name = name;
    this.spritePath = spritePath;
    this.dialogLines = dialogLines;

    loadSprite();
  }

  /**
   * Carrega o sprite do NPC
   */
  private void loadSprite() {
    try {
      // Tentar carregar como recurso do classpath (funciona no JAR)
      InputStream is = getClass().getClassLoader().getResourceAsStream(spritePath);
      if (is != null) {
        sprite = ImageIO.read(is);
        is.close();
        width = sprite.getWidth();
        height = sprite.getHeight();
        System.out.println("✅ Sprite do NPC carregado do JAR: " + name);
      } else {
        // Fallback: tentar carregar como arquivo externo (desenvolvimento)
        String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(spritePath);
        File spriteFile = new File(resolvedPath);

        if (spriteFile.exists()) {
          sprite = ImageIO.read(spriteFile);
          width = sprite.getWidth();
          height = sprite.getHeight();
          System.out.println("✅ Sprite do NPC carregado do arquivo: " + name);
        } else {
          System.err.println("Sprite do NPC não encontrado: " + resolvedPath);
          createDefaultSprite();
        }
      }
    } catch (IOException e) {
      System.err.println("Erro ao carregar sprite do NPC: " + e.getMessage());
      createDefaultSprite();
    }
  }

  /**
   * Cria sprite padrão caso não encontre o arquivo
   */
  private void createDefaultSprite() {
    width = 48;
    height = 48;
    sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics g = sprite.getGraphics();

    // Desenhar NPC padrão (pessoa simples)
    g.setColor(new Color(100, 100, 200)); // Azul
    g.fillRect(14, 10, 20, 28); // Corpo
    g.setColor(new Color(255, 200, 150)); // Pele
    g.fillOval(16, 5, 16, 16); // Cabeça

    g.dispose();
  }

  /**
   * Atualiza o NPC (verifica proximidade do jogador)
   */
  public void update(Player player) {
    if (player == null)
      return;

    double distance = Math.sqrt(
        Math.pow(player.getX() - x, 2) +
            Math.pow(player.getY() - y, 2));

    showInteractionPrompt = distance <= INTERACTION_RANGE;
  }

  /**
   * Retorna a linha de diálogo atual
   */
  public String getCurrentDialog() {
    if (dialogLines == null || dialogLines.length == 0) {
      return "...";
    }
    return dialogLines[currentDialogIndex];
  }

  /**
   * Avança para próxima linha de diálogo
   * 
   * @return true se há mais diálogos, false se acabou
   */
  public boolean nextDialog() {
    currentDialogIndex++;
    if (currentDialogIndex >= dialogLines.length) {
      currentDialogIndex = 0; // Reinicia
      return false; // Não há mais diálogos
    }
    return true; // Há mais diálogos
  }

  /**
   * Reinicia o diálogo para o início
   */
  public void resetDialog() {
    currentDialogIndex = 0;
  }

  /**
   * Verifica se o jogador pode interagir com este NPC
   */
  public boolean canInteract() {
    return showInteractionPrompt;
  }

  // Getters
  public String getName() {
    return name;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public Rectangle getBounds() {
    return new Rectangle((int) x, (int) y, (int) (width * 1.5), (int) (height * 1.5));
  }
}
