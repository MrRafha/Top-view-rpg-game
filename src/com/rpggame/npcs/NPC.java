package com.rpggame.npcs;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

  // Sistema de quests
  protected boolean hasQuestAvailable = false;
  protected boolean hasQuestCompleted = false;

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
        String resolvedPath = ResourceResolver.getResourcePath(spritePath);
        File spriteFile = new File(resolvedPath);

        if (spriteFile.exists()) {
          sprite = ImageIO.read(spriteFile);
          width = sprite.getWidth();
          height = sprite.getHeight();
          System.out.println("✅ Sprite do NPC carregado do arquivo: " + name);
        } else {
          System.err.println("❌ Sprite do NPC não encontrado: " + resolvedPath);
          createDefaultSprite();
        }
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

    // Desenhar indicador de quest
    if (hasQuestAvailable || hasQuestCompleted) {
      drawQuestIndicator(g, screenX, screenY);
    }

    if (showInteractionPrompt) {
      drawInteractionPrompt(g, screenX, screenY);
    }
  }

  /**
   * Desenha o indicador de quest acima do NPC
   */
  private void drawQuestIndicator(Graphics2D g, int screenX, int screenY) {
    // Posicionar à esquerda do NPC
    int indicatorX = screenX - 25; // 25 pixels à esquerda
    int indicatorY = screenY + (int) (height * 1.5) / 2; // Centralizado verticalmente
    int indicatorSize = 20;

    // Sombra do ícone
    g.setColor(new Color(0, 0, 0, 100));
    g.fillOval(indicatorX - indicatorSize / 2 + 2, indicatorY - indicatorSize / 2 + 2, indicatorSize, indicatorSize);

    // Fundo do ícone
    if (hasQuestCompleted) {
      // Dourado para quest completa
      g.setColor(new Color(255, 215, 0));
    } else {
      // Amarelo para quest disponível
      g.setColor(new Color(255, 255, 0));
    }
    g.fillOval(indicatorX - indicatorSize / 2, indicatorY - indicatorSize / 2, indicatorSize, indicatorSize);

    // Borda do ícone
    g.setColor(new Color(150, 120, 0));
    g.setStroke(new BasicStroke(2));
    g.drawOval(indicatorX - indicatorSize / 2, indicatorY - indicatorSize / 2, indicatorSize, indicatorSize);

    // Símbolo de exclamação ou interrogação
    g.setColor(Color.BLACK);
    g.setFont(new Font("Arial", Font.BOLD, 16));
    if (hasQuestCompleted) {
      g.drawString("?", indicatorX - 5, indicatorY + 6);
    } else {
      g.drawString("!", indicatorX - 4, indicatorY + 6);
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

  /**
   * Método de interação - deve ser sobrescrito por NPCs específicos
   */
  public void interact(Player player) {
    // NPCs específicos devem sobrescrever este método
    currentDialogIndex = 0;
  }

  /**
   * Define se o NPC tem uma quest disponível
   */
  public void setHasQuestAvailable(boolean hasQuest) {
    this.hasQuestAvailable = hasQuest;
  }

  /**
   * Define se o NPC tem uma quest completa
   */
  public void setHasQuestCompleted(boolean hasQuestCompleted) {
    this.hasQuestCompleted = hasQuestCompleted;
  }
}
