package com.rpggame.ui;

import java.awt.*;

/**
 * Caixa de escolha para aceitar ou recusar quests
 * Aparece sobre o DialogBox com opções Sim/Não e uma seta indicando a seleção
 */
public class QuestChoiceBox {
  private static final int BOX_WIDTH = 180;
  private static final int BOX_HEIGHT = 100;
  private static final int OPTION_SPACING = 35;

  // Cores
  private static final Color BACKGROUND_COLOR = new Color(20, 20, 20, 240);
  private static final Color BORDER_COLOR = new Color(255, 215, 0); // Dourado
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color ARROW_COLOR = new Color(255, 215, 0);

  // Fontes
  private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 16);
  private static final Font OPTION_FONT = new Font("Arial", Font.PLAIN, 18);

  // Estado
  private boolean visible = false;
  private int selectedOption = 0; // 0 = Sim, 1 = Não

  // Animação da seta
  private int arrowAnimation = 0;

  public void show() {
    visible = true;
    selectedOption = 0; // Sempre começa em "Sim"
  }

  public void hide() {
    visible = false;
  }

  public boolean isVisible() {
    return visible;
  }

  public void selectNext() {
    selectedOption = (selectedOption + 1) % 2;
  }

  public void selectPrevious() {
    selectedOption = (selectedOption - 1 + 2) % 2;
  }

  public boolean isYesSelected() {
    return selectedOption == 0;
  }

  public void update() {
    if (visible) {
      arrowAnimation++;
    }
  }

  /**
   * Renderiza a caixa de escolha de quest
   */
  public void render(Graphics2D g, int screenWidth, int screenHeight) {
    if (!visible) {
      return;
    }

    // Ativar anti-aliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Posição da caixa (centralizada, ligeiramente acima do DialogBox)
    int boxX = (screenWidth - BOX_WIDTH) / 2;
    int boxY = screenHeight - 200;

    // Desenhar sombra
    g.setColor(new Color(0, 0, 0, 150));
    g.fillRoundRect(boxX + 5, boxY + 5, BOX_WIDTH, BOX_HEIGHT, 15, 15);

    // Desenhar fundo
    g.setColor(BACKGROUND_COLOR);
    g.fillRoundRect(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 15, 15);

    // Desenhar borda dourada
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(2.5f));
    g.drawRoundRect(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 15, 15);

    // Desenhar título "Aceitar Quest?"
    g.setFont(TITLE_FONT);
    g.setColor(TEXT_COLOR);
    String title = "Aceitar Quest?";
    FontMetrics titleMetrics = g.getFontMetrics();
    int titleWidth = titleMetrics.stringWidth(title);
    int titleX = boxX + (BOX_WIDTH - titleWidth) / 2;
    int titleY = boxY + 25;
    g.drawString(title, titleX, titleY);

    // Desenhar opções
    g.setFont(OPTION_FONT);
    FontMetrics optionMetrics = g.getFontMetrics();

    String[] options = { "Sim", "Não" };
    int optionStartY = boxY + 50;

    for (int i = 0; i < options.length; i++) {
      int optionY = optionStartY + (i * OPTION_SPACING);

      // Calcular posição centralizada para a opção
      int optionWidth = optionMetrics.stringWidth(options[i]);
      int optionX = boxX + (BOX_WIDTH - optionWidth) / 2;

      // Desenhar opção
      if (i == selectedOption) {
        // Opção selecionada - cor dourada
        g.setColor(ARROW_COLOR);
      } else {
        // Opção não selecionada - cor normal
        g.setColor(TEXT_COLOR);
      }
      g.drawString(options[i], optionX, optionY);

      // Desenhar seta na opção selecionada
      if (i == selectedOption) {
        g.setColor(ARROW_COLOR);
        // Animação de movimento da seta
        int arrowOffset = (int) (Math.sin(arrowAnimation * 0.1) * 3);
        int arrowX = optionX - 25 + arrowOffset;

        // Desenhar seta ">"
        int[] xPoints = { arrowX, arrowX + 10, arrowX };
        int[] yPoints = { optionY - 12, optionY - 7, optionY - 2 };
        g.fillPolygon(xPoints, yPoints, 3);
      }
    }
  }
}
