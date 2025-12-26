package com.rpggame.ui;

import java.awt.*;
import java.util.ArrayList;
import com.rpggame.systems.Quest;
import com.rpggame.systems.QuestManager;

/**
 * Interface para exibir quests ativas
 */
public class QuestUI {
  private QuestManager questManager;
  private boolean visible;
  private int x, y, width, height;

  // Cores
  private static final Color BG_COLOR = new Color(20, 20, 30, 220);
  private static final Color BORDER_COLOR = new Color(180, 150, 90);
  private static final Color TITLE_COLOR = new Color(255, 215, 0);
  private static final Color TEXT_COLOR = new Color(220, 220, 220);
  private static final Color PROGRESS_COLOR = new Color(100, 200, 100);
  private static final Color COMPLETED_COLOR = new Color(50, 255, 50);

  public QuestUI(QuestManager questManager) {
    this.questManager = questManager;
    this.visible = false;
    this.width = 500;
    this.height = 500;
  }

  /**
   * Atualiza a posição da UI (centralizada na tela)
   */
  public void updatePosition(int screenWidth, int screenHeight) {
    this.x = (screenWidth - width) / 2;
    this.y = (screenHeight - height) / 2;
  }

  /**
   * Alterna visibilidade da UI
   */
  public void toggle() {
    visible = !visible;
  }

  /**
   * Define visibilidade
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  /**
   * Verifica se está visível
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Renderiza a UI de quests
   */
  public void render(Graphics2D g) {
    // Só mostrar se estiver visível
    if (!visible) {
      return;
    }

    ArrayList<Quest> activeQuests = questManager.getActiveQuests();

    // Não mostrar se não há quests ativas
    if (activeQuests.isEmpty()) {
      return;
    }

    // Ajustar posição baseado na altura da tela
    int screenWidth = g.getClipBounds().width;
    int screenHeight = g.getClipBounds().height;
    updatePosition(screenWidth, screenHeight);

    // Overlay escuro de fundo
    g.setColor(new Color(0, 0, 0, 150));
    g.fillRect(0, 0, screenWidth, screenHeight);

    // Ajustar altura baseado no número de quests
    int questHeight = 100;
    int actualHeight = Math.min(80 + (activeQuests.size() * questHeight), height);

    // Fundo semi-transparente
    g.setColor(BG_COLOR);
    g.fillRoundRect(x, y, width, actualHeight, 15, 15);

    // Borda dourada
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(3));
    g.drawRoundRect(x, y, width, actualHeight, 15, 15);

    // Título
    g.setColor(TITLE_COLOR);
    g.setFont(new Font("Arial", Font.BOLD, 24));
    g.drawString("📜 Quests Ativas", x + 15, y + 35);

    // Instruções
    g.setColor(new Color(150, 150, 150));
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    g.drawString("Pressione Q ou ESC para fechar", x + 15, y + actualHeight - 15);

    // Linha separadora
    g.setColor(BORDER_COLOR);
    g.drawLine(x + 10, y + 50, x + width - 10, y + 50);

    // Renderizar cada quest
    int currentY = y + 70;
    g.setFont(new Font("Arial", Font.PLAIN, 14));

    for (Quest quest : activeQuests) {
      renderQuest(g, quest, currentY);
      currentY += questHeight;
    }
  }

  /**
   * Renderiza uma quest individual
   */
  private void renderQuest(Graphics2D g, Quest quest, int yPos) {
    // Nome da quest
    g.setColor(quest.isCompleted() ? COMPLETED_COLOR : TEXT_COLOR);
    g.setFont(new Font("Arial", Font.BOLD, 14));
    String questName = quest.getName();
    if (quest.isCompleted()) {
      questName = "✓ " + questName;
    }
    g.drawString(questName, x + 15, yPos);

    // Descrição (com suporte a quebra de linha)
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    g.setColor(new Color(180, 180, 180));
    String desc = quest.getDescription();

    // Renderizar descrição com quebra de linha
    int descY = yPos + 17;
    int lineHeight = 14;
    int maxWidth = width - 30;
    FontMetrics fm = g.getFontMetrics();

    // Suportar quebra manual (\n) e automática
    String[] lines = desc.split("\\n");
    for (String line : lines) {
      if (fm.stringWidth(line) > maxWidth) {
        // Quebra automática se a linha for muito longa
        String[] words = line.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
          String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
          if (fm.stringWidth(testLine) > maxWidth && currentLine.length() > 0) {
            g.drawString(currentLine.toString(), x + 15, descY);
            descY += lineHeight;
            currentLine = new StringBuilder(word);
          } else {
            currentLine = new StringBuilder(testLine);
          }
        }
        if (currentLine.length() > 0) {
          g.drawString(currentLine.toString(), x + 15, descY);
          descY += lineHeight;
        }
      } else {
        g.drawString(line, x + 15, descY);
        descY += lineHeight;
      }
    }

    // Progresso (ajustado para a posição após a descrição)
    if (quest.getType().toString().equals("KILL")) {
      String progressText = "Progresso: " + quest.getProgressString();
      g.setColor(quest.isCompleted() ? COMPLETED_COLOR : PROGRESS_COLOR);
      g.drawString(progressText, x + 15, yPos + 50);

      // Barra de progresso
      int barWidth = width - 40;
      int barHeight = 8;
      int barX = x + 15;
      int barY = yPos + 56;

      // Fundo da barra
      g.setColor(new Color(40, 40, 40));
      g.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);

      // Progresso da barra
      float progress = (float) quest.getCurrentAmount() / quest.getTargetAmount();
      int fillWidth = (int) (barWidth * progress);
      g.setColor(quest.isCompleted() ? COMPLETED_COLOR : new Color(100, 180, 255));
      g.fillRoundRect(barX, barY, fillWidth, barHeight, 4, 4);

      // Borda da barra
      g.setColor(new Color(80, 80, 80));
      g.drawRoundRect(barX, barY, barWidth, barHeight, 4, 4);
    }
  }

  /**
   * Renderiza um mini indicador de quests (sempre visível no canto)
   */
  public void renderMiniIndicator(Graphics2D g, int screenWidth, int screenHeight) {
    ArrayList<Quest> activeQuests = questManager.getActiveQuests();

    if (activeQuests.isEmpty()) {
      return;
    }

    // Posição no canto superior direito
    int miniX = screenWidth - 200;
    int miniY = 50;
    int miniWidth = 180;
    int miniHeight = 30 + (activeQuests.size() * 25);

    // Fundo compacto
    g.setColor(new Color(20, 20, 30, 180));
    g.fillRoundRect(miniX, miniY, miniWidth, miniHeight, 10, 10);

    // Borda
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(1.5f));
    g.drawRoundRect(miniX, miniY, miniWidth, miniHeight, 10, 10);

    // Título mini
    g.setColor(TITLE_COLOR);
    g.setFont(new Font("Arial", Font.BOLD, 12));
    g.drawString("Quests (" + activeQuests.size() + ")", miniX + 10, miniY + 17);

    // Listar quests compactamente
    int currentY = miniY + 30;
    g.setFont(new Font("Arial", Font.PLAIN, 11));

    for (Quest quest : activeQuests) {
      g.setColor(quest.isCompleted() ? COMPLETED_COLOR : TEXT_COLOR);
      String shortName = quest.getName();
      if (shortName.length() > 20) {
        shortName = shortName.substring(0, 17) + "...";
      }
      String text = (quest.isCompleted() ? "✓ " : "• ") + shortName;
      g.drawString(text, miniX + 10, currentY);

      // Progresso compacto
      if (quest.getType().toString().equals("KILL")) {
        g.setColor(quest.isCompleted() ? COMPLETED_COLOR : PROGRESS_COLOR);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString(quest.getProgressString(), miniX + miniWidth - 40, currentY);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
      }

      currentY += 25;
    }
  }
}
