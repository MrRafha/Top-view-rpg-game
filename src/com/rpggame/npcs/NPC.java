package com.rpggame.npcs;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.world.ResourceResolver;

/**
 * Classe base abstrata para NPCs
 * Cada NPC específico deve estender esta classe e definir seu próprio diálogo
 */
public abstract class NPC {
  protected double x, y;
  protected int width, height;
  protected String name;
  protected String[] dialogLines;
  protected int currentDialogIndex = 0;

  // Sprite
  protected BufferedImage sprite;
  protected String spritePath;

  // Indicador de interação
  protected boolean showInteractionPrompt = false;
  protected static final int INTERACTION_RANGE = 60;

  /**
   * Construtor base do NPC
   */
  public NPC(double x, double y, String name, String spritePath) {
    this.x = x;
    this.y = y;
    this.name = name;
    this.spritePath = spritePath;
    this.dialogLines = initializeDialogues();

    loadSprite();
  }

  /**
   * Método abstrato que cada NPC deve implementar para definir seus diálogos
   */
  protected abstract String[] initializeDialogues();

  /**
   * Carrega o sprite do NPC
   */
  private void loadSprite() {
    try {
      String resolvedPath = ResourceResolver.getResourcePath(spritePath);
      File spriteFile = new File(resolvedPath);

      if (spriteFile.exists()) {
        sprite = ImageIO.read(spriteFile);
        width = sprite.getWidth();
        height = sprite.getHeight();
        System.out.println("✅ Sprite do NPC carregado: " + name);
      } else {
        System.err.println("❌ Sprite do NPC não encontrado: " + resolvedPath);
        createDefaultSprite();
      }
    } catch (IOException e) {
      System.err.println("❌ Erro ao carregar sprite do NPC: " + e.getMessage());
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
    Graphics2D g = sprite.createGraphics();

    g.setColor(new Color(100, 100, 200));
    g.fillRect(14, 10, 20, 28);
    g.setColor(new Color(255, 200, 150));
    g.fillOval(16, 5, 16, 16);

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
   * Renderiza o NPC
   */
  public void render(Graphics2D g, Camera camera) {
    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Desenhar sprite com escala maior (1.5x)
    if (sprite != null) {
      int scaledWidth = (int) (width * 1.5);
      int scaledHeight = (int) (height * 1.5);
      g.drawImage(sprite, screenX, screenY, scaledWidth, scaledHeight, null);
    }

    if (showInteractionPrompt) {
      drawInteractionPrompt(g, screenX, screenY);
    }
  }

  /**
   * Desenha o indicador de interação acima do NPC
   */
  private void drawInteractionPrompt(Graphics2D g, int screenX, int screenY) {
    int promptY = screenY - 15;
    int promptX = screenX + (int) (width * 1.5) / 2; // Ajustado para o tamanho escalado

    g.setColor(new Color(0, 0, 0, 180));
    g.fillRoundRect(promptX - 12, promptY - 12, 24, 20, 5, 5);

    g.setColor(Color.WHITE);
    g.drawRoundRect(promptX - 12, promptY - 12, 24, 20, 5, 5);

    g.setFont(new Font("Arial", Font.BOLD, 14));
    g.drawString("E", promptX - 5, promptY + 4);
  }

  public String getCurrentDialog() {
    if (dialogLines == null || dialogLines.length == 0) {
      return "...";
    }
    return dialogLines[currentDialogIndex];
  }

  public boolean nextDialog() {
    currentDialogIndex++;
    if (currentDialogIndex >= dialogLines.length) {
      currentDialogIndex = 0;
      return false;
    }
    return true;
  }

  public void resetDialog() {
    currentDialogIndex = 0;
  }

  public boolean canInteract() {
    return showInteractionPrompt;
  }

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
