package com.rpggame.ui;

import com.rpggame.systems.Skill;
import com.rpggame.systems.SkillManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Sistema de UI para slots de habilidades com cooldown visual.
 */
public class SkillSlotUI {
  // Configurações dos slots
  private static final int SLOT_SIZE = 60;
  private static final int SLOT_SPACING = 10;
  private static final int SLOTS_COUNT = 4;
  private static final int MARGIN_RIGHT = 20;
  private static final int MARGIN_TOP = 100;

  // Cores
  private static final Color SLOT_EMPTY = new Color(40, 40, 40, 180);
  private static final Color SLOT_READY = new Color(0, 180, 0, 220);
  private static final Color SLOT_COOLDOWN = new Color(80, 80, 80, 200);
  private static final Color BORDER_COLOR = new Color(255, 255, 255, 150);
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color COOLDOWN_TEXT = new Color(255, 255, 100);

  private SkillManager skillManager;
  private int screenWidth;

  public SkillSlotUI(SkillManager skillManager, int screenWidth) {
    this.skillManager = skillManager;
    this.screenWidth = screenWidth;
  }

  /**
   * Renderiza os slots de habilidades na tela.
   */
  public void render(Graphics2D g) {
    if (skillManager == null) {
      return;
    }

    // Configurar renderização suave
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Calcular posição inicial dos slots (canto direito)
    int startX = screenWidth - MARGIN_RIGHT - SLOT_SIZE;
    int startY = MARGIN_TOP;

    // Renderizar cada slot
    for (int i = 1; i <= SLOTS_COUNT; i++) {
      int slotX = startX;
      int slotY = startY + ((SLOT_SIZE + SLOT_SPACING) * (i - 1));

      renderSlot(g, i, slotX, slotY);
    }
  }

  /**
   * Renderiza um slot individual.
   */
  private void renderSlot(Graphics2D g, int slotNumber, int x, int y) {
    Skill skill = skillManager.getSkill(slotNumber);

    // Determinar cor do slot baseada no estado
    Color slotColor = determineSlotColor(skill);

    // Desenhar fundo do slot com cantos arredondados
    RoundRectangle2D.Float slotRect = new RoundRectangle2D.Float(x, y, SLOT_SIZE, SLOT_SIZE, 8, 8);
    g.setColor(slotColor);
    g.fill(slotRect);

    // Desenhar borda
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(2));
    g.draw(slotRect);

    // Desenhar número do slot
    g.setFont(new Font("Arial", Font.BOLD, 12));
    g.setColor(TEXT_COLOR);
    g.drawString(String.valueOf(slotNumber), x + 5, y + 15);

    if (skill != null && skill.isLearned()) {
      // Se a habilidade está aprendida, mostrar informações
      renderSkillInfo(g, skill, x, y);
    } else if (skill != null) {
      // Slot tem habilidade mas não foi aprendida ainda
      renderLockedSkill(g, x, y);
    } else {
      // Slot vazio
      renderEmptySlot(g, x, y);
    }
  }

  /**
   * Determina a cor do slot baseada no estado da habilidade.
   */
  private Color determineSlotColor(Skill skill) {
    if (skill == null) {
      return SLOT_EMPTY;
    }

    if (!skill.isLearned()) {
      return SLOT_EMPTY;
    }

    if (skill.isOnCooldown()) {
      return SLOT_COOLDOWN;
    }

    return SLOT_READY;
  }

  /**
   * Renderiza informações da habilidade aprendida.
   */
  private void renderSkillInfo(Graphics2D g, Skill skill, int x, int y) {
    if (skill.isOnCooldown()) {
      // Mostrar cooldown
      int cooldownSeconds = skill.getCooldownInSeconds();

      // Desenhar círculo de progresso do cooldown
      renderCooldownProgress(g, skill, x, y);

      // Mostrar tempo restante no centro
      g.setFont(new Font("Arial", Font.BOLD, 16));
      g.setColor(COOLDOWN_TEXT);
      String cooldownText = String.valueOf(cooldownSeconds);
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(cooldownText);
      int textHeight = fm.getHeight();
      g.drawString(cooldownText,
          x + (SLOT_SIZE - textWidth) / 2,
          y + (SLOT_SIZE + textHeight) / 2 - 3);
    } else {
      // Habilidade pronta - mostrar ícone ou inicial
      renderReadySkill(g, skill, x, y);
    }
  }

  /**
   * Renderiza o progresso do cooldown como um círculo.
   */
  private void renderCooldownProgress(Graphics2D g, Skill skill, int x, int y) {
    int centerX = x + SLOT_SIZE / 2;
    int centerY = y + SLOT_SIZE / 2;
    int radius = 20;

    // Calcular progresso (0.0 a 1.0) - usando o cooldown total correto
    double totalCooldown = skill.getTotalCooldownTime();
    double remainingCooldown = skill.getCooldownRemaining();
    double progress = 1.0 - (remainingCooldown / totalCooldown);

    // Garantir que o progresso está no intervalo correto
    progress = Math.max(0.0, Math.min(1.0, progress));

    // Desenhar círculo de fundo
    g.setColor(new Color(0, 0, 0, 100));
    g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

    // Desenhar progresso do cooldown
    g.setColor(new Color(100, 255, 100, 150));
    g.setStroke(new BasicStroke(3));
    int angle = (int) (360 * progress);
    g.drawArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, -angle);
  }

  /**
   * Renderiza habilidade pronta para uso.
   */
  private void renderReadySkill(Graphics2D g, Skill skill, int x, int y) {
    // Mostrar primeira letra da habilidade no centro
    String initial = skill.getName().substring(0, 1).toUpperCase();

    g.setFont(new Font("Arial", Font.BOLD, 24));
    g.setColor(TEXT_COLOR);
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(initial);
    int textHeight = fm.getHeight();

    g.drawString(initial,
        x + (SLOT_SIZE - textWidth) / 2,
        y + (SLOT_SIZE + textHeight) / 2 - 5);

    // Adicionar pequeno brilho para indicar que está pronta
    g.setColor(new Color(255, 255, 255, 50));
    g.fillOval(x + 5, y + 5, SLOT_SIZE - 10, SLOT_SIZE - 10);
  }

  /**
   * Renderiza habilidade bloqueada (não aprendida).
   */
  private void renderLockedSkill(Graphics2D g, int x, int y) {
    g.setFont(new Font("Arial", Font.BOLD, 20));
    g.setColor(new Color(150, 150, 150));
    g.drawString("?", x + SLOT_SIZE / 2 - 5, y + SLOT_SIZE / 2 + 5);
  }

  /**
   * Renderiza slot vazio.
   */
  private void renderEmptySlot(Graphics2D g, int x, int y) {
    g.setColor(new Color(100, 100, 100, 100));
    g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
        new float[] { 5, 5 }, 0));
    g.drawRect(x + 10, y + 10, SLOT_SIZE - 20, SLOT_SIZE - 20);
  }

  /**
   * Atualiza o SkillManager (caso mude).
   */
  public void setSkillManager(SkillManager skillManager) {
    this.skillManager = skillManager;
  }
}