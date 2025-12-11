package com.rpggame.systems.skills;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.util.ArrayList;

import com.rpggame.entities.Enemy;
import com.rpggame.entities.Player;
import com.rpggame.systems.EnemyManager;
import com.rpggame.systems.Skill;
import com.rpggame.world.Camera;

/**
 * Habilidade do Guerreiro: Golpe Horizontal
 * Um golpe em meia lua que atinge todos os inimigos numa área semicircular à
 * frente
 */
public class HorizontalSlashSkill extends Skill {
  private boolean isAnimating = false;
  private int animationTimer = 0;
  private static final int ANIMATION_DURATION = 30; // 0.5 segundos
  private static final double SKILL_RANGE = 75.0; // Diminuído pela metade
  private static final double SKILL_ANGLE = Math.PI; // 180 graus

  // Posição e direção do último ataque para renderização
  private double lastAttackX, lastAttackY, lastAttackDirection;

  public HorizontalSlashSkill() {
    super("Golpe Horizontal",
        "Um poderoso golpe em semicírculo que atinge todos os inimigos próximos",
        15, // 15 segundos de cooldown
        "Warrior");
  }

  @Override
  protected void performSkill(Player player) {
    // Guardar posição e direção para animação
    lastAttackX = player.getX();
    lastAttackY = player.getY();
    lastAttackDirection = player.getFacingDirection();

    // Iniciar animação
    isAnimating = true;
    animationTimer = ANIMATION_DURATION;

    // Encontrar e atacar inimigos na área
    attackEnemiesInRange(player);
  }

  /**
   * Ataca todos os inimigos na área do golpe horizontal
   */
  private void attackEnemiesInRange(Player player) {
    EnemyManager enemyManager = player.getEnemyManager();
    if (enemyManager == null)
      return;

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    int enemiesHit = 0;

    for (Enemy enemy : enemies) {
      if (enemy.isAlive() && isEnemyInSkillRange(player, enemy)) {
        // Dano baseado na Força (STR) - atributo principal do Guerreiro
        int damage = (int) (player.getStats().getStrength() * 2.0 + 15);
        enemy.takeDamage(damage);
        enemiesHit++;

        // TODO: Adicionar knockback quando o método for implementado na classe Enemy
      }
    }

    if (enemiesHit > 0) {
      System.out.println("⚔️ Golpe Horizontal atingiu " + enemiesHit + " inimigo(s)!");
    }
  }

  /**
   * Verifica se um inimigo está na área do golpe horizontal
   */
  private boolean isEnemyInSkillRange(Player player, Enemy enemy) {
    double dx = enemy.getX() - player.getX();
    double dy = enemy.getY() - player.getY();
    double distance = Math.sqrt(dx * dx + dy * dy);

    // Verificar distância
    if (distance > SKILL_RANGE) {
      return false;
    }

    // Verificar ângulo (semicírculo à frente do jogador)
    double angleToEnemy = Math.atan2(dy, dx);
    double facingDirection = player.getFacingDirection();

    // Normalizar diferença de ângulo
    double angleDifference = Math.abs(angleToEnemy - facingDirection);
    while (angleDifference > Math.PI) {
      angleDifference -= 2 * Math.PI;
    }
    angleDifference = Math.abs(angleDifference);

    return angleDifference <= SKILL_ANGLE / 2;
  }

  @Override
  public void update() {
    super.update();

    // Atualizar animação
    if (isAnimating) {
      animationTimer--;
      if (animationTimer <= 0) {
        isAnimating = false;
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (!isAnimating)
      return;

    // Posição na tela
    int screenX = (int) (lastAttackX - camera.getX());
    int screenY = (int) (lastAttackY - camera.getY());

    // Calcular intensidade da animação (fade out)
    float intensity = (float) animationTimer / ANIMATION_DURATION;

    // Cor do efeito (dourado/amarelo para guerreiro)
    Color slashColor = new Color(1.0f, 0.8f, 0.0f, intensity * 0.6f);
    g.setColor(slashColor);

    // Desenhar arco representando o golpe
    int radius = (int) SKILL_RANGE;
    double startAngle = Math.toDegrees(lastAttackDirection - SKILL_ANGLE / 2);
    double extent = Math.toDegrees(SKILL_ANGLE);

    // Criar e desenhar o arco
    Arc2D.Double arc = new Arc2D.Double(
        screenX - radius,
        screenY - radius,
        radius * 2,
        radius * 2,
        startAngle,
        extent,
        Arc2D.PIE);

    g.fill(arc);

    // Contorno mais escuro
    g.setColor(new Color(1.0f, 0.6f, 0.0f, intensity * 0.8f));
    g.draw(arc);

    // Linhas de movimento para dar sensação de velocidade
    for (int i = 0; i < 3; i++) {
      double lineAngle = lastAttackDirection - SKILL_ANGLE / 2 + (SKILL_ANGLE / 3) * i;
      int lineLength = (int) (radius * 0.8);

      int lineEndX = screenX + (int) (Math.cos(lineAngle) * lineLength);
      int lineEndY = screenY + (int) (Math.sin(lineAngle) * lineLength);

      g.setColor(new Color(1.0f, 1.0f, 1.0f, intensity * 0.7f));
      g.drawLine(screenX, screenY, lineEndX, lineEndY);
    }
  }
}