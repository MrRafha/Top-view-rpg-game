package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;
import com.rpggame.core.GamePanel;

/**
 * Habilidade: Salto Veloz
 * Dá um dash para frente na distância de 3 tiles
 */
public class QuickDashSkill extends Skill {

  private boolean isDashing = false;
  private int dashTimer = 0;
  private static final int DASH_DURATION = 15; // ~0.25 segundos
  private double startX, startY;
  private double targetX, targetY;
  private double currentProgress = 0;

  // Efeito visual do trail
  private static final int MAX_TRAIL_POSITIONS = 5;
  private double[] trailX = new double[MAX_TRAIL_POSITIONS];
  private double[] trailY = new double[MAX_TRAIL_POSITIONS];
  private int trailIndex = 0;

  public QuickDashSkill() {
    super("Salto Veloz",
        "Dash de 3 tiles saltando sobre obstáculos",
        6, // 6 segundos de cooldown
        "Archer",
        30); // 30 de mana
  }

  @Override
  protected void performSkill(Player player) {
    // Calcular direção que o player está olhando
    double facing = player.getFacingDirection();

    // Guardar posição inicial
    startX = player.getX();
    startY = player.getY();

    // Calcular posição final (3 tiles = 144 pixels)
    double dashDistance = GamePanel.TILE_SIZE * 3; // 3 tiles
    targetX = startX + Math.cos(facing) * dashDistance;
    targetY = startY + Math.sin(facing) * dashDistance;

    // Iniciar dash
    isDashing = true;
    dashTimer = DASH_DURATION;
    currentProgress = 0;

    // Limpar trail
    for (int i = 0; i < MAX_TRAIL_POSITIONS; i++) {
      trailX[i] = startX;
      trailY[i] = startY;
    }
    trailIndex = 0;

    System.out.println("💨 SALTO VELOZ! Dashando 3 tiles!");
  }

  @Override
  public void update() {
    super.update();

    if (isDashing && dashTimer > 0) {
      dashTimer--;

      // Calcular progresso (0.0 a 1.0)
      currentProgress = 1.0 - (dashTimer / (float) DASH_DURATION);

      if (dashTimer <= 0) {
        isDashing = false;
      }
    }
  }

  /**
   * Aplica o movimento do dash ao player
   * Deve ser chamado pelo Player durante seu update
   */
  public void applyDashMovement(Player player) {
    if (!isDashing)
      return;

    // Interpolar posição entre início e fim
    double newX = startX + (targetX - startX) * currentProgress;
    double newY = startY + (targetY - startY) * currentProgress;

    // Atualizar trail
    trailX[trailIndex] = player.getX();
    trailY[trailIndex] = player.getY();
    trailIndex = (trailIndex + 1) % MAX_TRAIL_POSITIONS;

    // Aplicar nova posição (ignorando colisões durante o dash)
    try {
      java.lang.reflect.Field xField = player.getClass().getDeclaredField("x");
      java.lang.reflect.Field yField = player.getClass().getDeclaredField("y");
      xField.setAccessible(true);
      yField.setAccessible(true);
      xField.set(player, newX);
      yField.set(player, newY);
    } catch (Exception e) {
      System.err.println("Erro ao aplicar dash: " + e.getMessage());
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (!isDashing)
      return;

    // Desenhar trail atrás do player
    for (int i = 0; i < MAX_TRAIL_POSITIONS; i++) {
      int index = (trailIndex - i - 1 + MAX_TRAIL_POSITIONS) % MAX_TRAIL_POSITIONS;

      int screenX = (int) (trailX[index] - camera.getX());
      int screenY = (int) (trailY[index] - camera.getY());

      // Calcular transparência baseada na posição no trail
      float alpha = (1.0f - (i / (float) MAX_TRAIL_POSITIONS)) * 0.5f;

      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
      g.setColor(new Color(100, 255, 100));
      g.fillOval(screenX, screenY, 33, 48); // Tamanho do player

      // Desenhar linhas de velocidade
      g.setColor(new Color(200, 255, 200));
      for (int j = 0; j < 3; j++) {
        int lineOffset = j * 5;
        g.drawLine(screenX - 10 - lineOffset, screenY + 24,
            screenX - 5 - lineOffset, screenY + 24);
      }
    }

    // Resetar composite
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
  }

  /**
   * Verifica se está executando dash
   */
  public boolean isDashing() {
    return isDashing;
  }
}
