package com.rpggame.enemies.Goblins;

import com.rpggame.entities.Enemy;

/**
 * Classe do inimigo Goblin - inimigo básico corpo a corpo
 */
public class Goblin extends Enemy {

  /**
   * Construtor do Goblin
   */
  public Goblin(double x, double y) {
    super(x, y, "sprites/CommonGoblin.png");
  }

  /**
   * Inicializa as estatísticas específicas do Goblin
   */
  @Override
  protected void initializeStats() {
    // Estatísticas do Goblin
    this.maxHealth = 25;
    this.currentHealth = maxHealth;
    this.damage = 8;
    this.speed = 1.5;
    this.experienceReward = 15;

    // Alcances
    this.detectionRange = 80.0;
    this.attackRange = 35.0;
  }

  /**
   * Ataque corpo a corpo do Goblin
   */
  @Override
  protected void attack() {
    if (target == null)
      return;

    // Verificar se o jogador está no alcance
    double distance = Math.sqrt(
        Math.pow(target.getX() - x, 2) +
            Math.pow(target.getY() - y, 2));

    if (distance <= attackRange) {
      // TODO: Implementar sistema de dano ao jogador
      System.out.println("Goblin atacou o jogador! Dano: " + damage);

      // Efeito visual de ataque (opcional)
      createAttackEffect();
    }
  }

  /**
   * Cria um efeito visual para o ataque
   */
  private void createAttackEffect() {
    // TODO: Implementar efeito visual de ataque
    // Por enquanto apenas debug
    System.out.println("*SLASH* Goblin ataca!");
  }

  /**
   * IA específica do Goblin (mais agressiva)
   */
  @Override
  protected void updateAI() {
    super.updateAI();

    // Goblin é mais agressivo - persegue mais longe
    if (target != null) {
      double distanceToPlayer = Math.sqrt(
          Math.pow(target.getX() - x, 2) +
              Math.pow(target.getY() - y, 2));

      // Desistir apenas se muito longe
      if (distanceToPlayer > detectionRange * 2) {
        aggressive = false;
        dx = 0;
        dy = 0;
      }
    }
  }
}