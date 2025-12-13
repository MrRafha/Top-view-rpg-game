package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;
import com.rpggame.entities.Enemy;

/**
 * Habilidade: Intimidação Colossal
 * Intimida todos os inimigos em uma área de 3x3 tiles, fazendo-os fugir por 2
 * segundos
 */
public class IntimidatingShoutSkill extends Skill {

  // Efeito visual
  private boolean isActive = false;
  private int effectTimer = 0;
  private static final int EFFECT_DURATION = 20; // ~0.33 segundos de animação
  private double playerX, playerY;
  private float effectRadius = 0;
  private static final float MAX_EFFECT_RADIUS = 144; // 3 tiles (48 * 3)

  public IntimidatingShoutSkill() {
    super("Intimidação Colossal",
        "Intimida inimigos em 3x3 tiles, fazendo-os fugir por 2 segundos",
        10, // 10 segundos de cooldown
        "Warrior",
        30); // 30 de mana
  }

  @Override
  protected void performSkill(Player player) {
    // Guardar posição do player para o efeito visual
    playerX = player.getX() + player.getWidth() / 2;
    playerY = player.getY() + player.getHeight() / 2;

    // Ativar efeito visual
    isActive = true;
    effectTimer = EFFECT_DURATION;
    effectRadius = 0;

    // Calcular área de efeito (3x3 tiles = 144 pixels de raio)
    double intimidateRange = 144;

    // Pegar inimigos do EnemyManager
    EnemyManager enemyManager = player.getEnemyManager();
    if (enemyManager == null) {
      System.out.println("⚠️ EnemyManager não encontrado!");
      return;
    }

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    int intimidatedCount = 0;

    // Intimidar todos os inimigos na área
    for (Enemy enemy : enemies) {
      if (!enemy.isAlive())
        continue;

      double dx = enemy.getX() - playerX;
      double dy = enemy.getY() - playerY;
      double distance = Math.sqrt(dx * dx + dy * dy);

      if (distance <= intimidateRange) {
        // Fazer inimigo fugir
        intimidateEnemy(enemy, dx, dy, distance);
        intimidatedCount++;
      }
    }

    System.out.println("💢 INTIMIDAÇÃO COLOSSAL! " + intimidatedCount + " inimigos intimidados!");
  }

  /**
   * Faz um inimigo fugir do player
   */
  private void intimidateEnemy(Enemy enemy, double dx, double dy, double distance) {
    // Calcular direção de fuga (oposta ao player)
    double fleeX = dx / distance;
    double fleeY = dy / distance;

    // Aplicar velocidade de fuga por 2 segundos (120 frames)
    double fleeSpeed = 3.0; // Velocidade de fuga
    int fleeDuration = 120; // 2 segundos a 60 FPS

    // Aplicar estado de medo ao inimigo
    enemy.applyFear(fleeX, fleeY, fleeDuration, fleeSpeed);
  }

  @Override
  public void update() {
    super.update();

    // Atualizar efeito visual
    if (isActive && effectTimer > 0) {
      effectTimer--;

      // Expandir o raio do efeito
      float progress = 1.0f - (effectTimer / (float) EFFECT_DURATION);
      effectRadius = MAX_EFFECT_RADIUS * progress;

      if (effectTimer <= 0) {
        isActive = false;
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (!isActive)
      return;

    // Calcular posição na tela
    int screenX = (int) (playerX - camera.getX());
    int screenY = (int) (playerY - camera.getY());

    // Calcular intensidade (fade out)
    float intensity = effectTimer / (float) EFFECT_DURATION;

    // Desenhar onda vermelha expandindo
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.5f));

    // Onda externa (mais clara)
    g.setColor(new Color(255, 50, 50, 150));
    int radius = (int) effectRadius;
    g.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

    // Onda interna (mais escura)
    g.setColor(new Color(200, 0, 0, 200));
    int innerRadius = (int) (effectRadius * 0.7f);
    g.fillOval(screenX - innerRadius, screenY - innerRadius, innerRadius * 2, innerRadius * 2);

    // Resetar composite
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
  }
}
