package com.rpggame.npcs;

import com.rpggame.entities.Goblin;
import java.awt.*;
import java.util.ArrayList;

/**
 * NPC Guarda Real - Protege a área e oferece informações sobre segurança
 */
public class GuardNPC extends NPC {

  private static final int GUARD_RANGE = 150; // Alcance de detecção de goblins
  private static final int GUARD_ATTACK_RANGE = 50; // Alcance de ataque (menor para movimento)
  private static final double GUARD_SPEED = 2.5; // Velocidade de movimento do guarda

  // Estados do guarda
  private enum GuardState {
    PATROLLING, PURSUING, ATTACKING, RETURNING
  }

  private GuardState currentState = GuardState.PATROLLING;

  // Posição original para retorno
  private double originalX, originalY;

  // Sistema de ataque
  private int attackCooldown = 0;
  private final int ATTACK_COOLDOWN_TIME = 60; // frames entre ataques
  private Goblin targetGoblin = null;

  // Efeitos visuais de ataque (similar ao goblin)
  private boolean isPreparingAttack = false;
  private int attackPreparationTimer = 0;
  private static final int ATTACK_PREPARATION_TIME = 30; // 0.5 segundos de preparação

  private boolean isAttacking = false;
  private int attackEffectTimer = 0;
  private static final int ATTACK_EFFECT_DURATION = 15; // 0.25 segundos de efeito

  private int logTimer = 0; // Timer para controlar logs de debug

  public GuardNPC(double x, double y) {
    super(x, y, "Guarda Real", "sprites/GuardNPC.png");
    this.originalX = x;
    this.originalY = y;
  }

  @Override
  protected String[] initializeDialogues() {
    return new String[] {
        "Alto lá! Esta área está sob minha proteção.",
        "Cuidado com os goblins que vagam por estas terras.",
        "Eles ficaram mais organizados recentemente...",
        "Ouvi dizer que formaram famílias e até realizam reuniões!",
        "Mantenha-se vigilante, guerreiro."
    };
  }

  /**
   * Atualiza o guarda, incluindo detecção e ataque de goblins
   */
  public void updateGuardBehavior(ArrayList<Goblin> goblins) {
    // Atualizar timers
    if (attackCooldown > 0)
      attackCooldown--;
    if (attackPreparationTimer > 0)
      attackPreparationTimer--;
    if (attackEffectTimer > 0)
      attackEffectTimer--;
    if (logTimer > 0)
      logTimer--;

    // Atualizar estados de ataque
    if (isPreparingAttack && attackPreparationTimer <= 0) {
      executeAttack();
    }
    if (isAttacking && attackEffectTimer <= 0) {
      isAttacking = false;
    }

    // Debug removido - sistema funcionando

    // Máquina de estados do guarda
    switch (currentState) {
      case PATROLLING:
        handlePatrolling(goblins);
        break;
      case PURSUING:
        handlePursuing();
        break;
      case ATTACKING:
        handleAttacking();
        break;
      case RETURNING:
        handleReturning();
        break;
    }
  }

  private void handlePatrolling(ArrayList<Goblin> goblins) {
    // Procurar goblins próximos
    Goblin closestGoblin = findClosestGoblin(goblins);

    if (closestGoblin != null) {
      targetGoblin = closestGoblin;
      currentState = GuardState.PURSUING;
    }
  }

  private void handlePursuing() {
    if (targetGoblin == null || !targetGoblin.isAlive()) {
      currentState = GuardState.RETURNING;
      return;
    }

    double distance = Math.sqrt(
        Math.pow(targetGoblin.getX() - x, 2) +
            Math.pow(targetGoblin.getY() - y, 2));

    if (distance <= GUARD_ATTACK_RANGE) {
      // Próximo o suficiente para atacar
      currentState = GuardState.ATTACKING;
      startAttackSequence();
    } else if (distance > GUARD_RANGE * 2) {
      // Goblin muito longe, desistir
      currentState = GuardState.RETURNING;
      targetGoblin = null;
    } else {
      // Mover em direção ao goblin
      moveTowards(targetGoblin.getX(), targetGoblin.getY());
    }
  }

  private void handleAttacking() {
    if (targetGoblin == null || !targetGoblin.isAlive()) {
      currentState = GuardState.RETURNING;
      return;
    }

    if (!isPreparingAttack && !isAttacking && attackCooldown == 0) {
      startAttackSequence();
    } else if (!isPreparingAttack && !isAttacking) {
      // Esperar cooldown
      double distance = Math.sqrt(
          Math.pow(targetGoblin.getX() - x, 2) +
              Math.pow(targetGoblin.getY() - y, 2));

      if (distance > GUARD_ATTACK_RANGE) {
        currentState = GuardState.PURSUING;
      }
    }
  }

  private void handleReturning() {
    double distanceToBase = Math.sqrt(
        Math.pow(originalX - x, 2) +
            Math.pow(originalY - y, 2));

    if (distanceToBase <= 5) {
      // Chegou na posição original
      x = originalX;
      y = originalY;
      currentState = GuardState.PATROLLING;
      targetGoblin = null;
    } else {
      // Mover em direção à posição original
      moveTowards(originalX, originalY);
    }
  }

  private Goblin findClosestGoblin(ArrayList<Goblin> goblins) {
    Goblin closestGoblin = null;
    double closestDistance = Double.MAX_VALUE;

    for (Goblin goblin : goblins) {
      if (goblin.isAlive()) {
        double distance = Math.sqrt(
            Math.pow(goblin.getX() - x, 2) +
                Math.pow(goblin.getY() - y, 2));

        if (distance <= GUARD_RANGE && distance < closestDistance) {
          closestGoblin = goblin;
          closestDistance = distance;
        }
      }
    }

    return closestGoblin;
  }

  private void moveTowards(double targetX, double targetY) {
    double dx = targetX - x;
    double dy = targetY - y;
    double distance = Math.sqrt(dx * dx + dy * dy);

    if (distance > 0) {
      // Normalizar e aplicar velocidade
      dx = (dx / distance) * GUARD_SPEED;
      dy = (dy / distance) * GUARD_SPEED;

      x += dx;
      y += dy;
    }
  }

  private void startAttackSequence() {
    isPreparingAttack = true;
    attackPreparationTimer = ATTACK_PREPARATION_TIME;
    System.out.println("⚡ " + name + " preparando ataque!");
  }

  private void executeAttack() {
    if (targetGoblin != null && targetGoblin.isAlive()) {
      isPreparingAttack = false;
      isAttacking = true;
      attackEffectTimer = ATTACK_EFFECT_DURATION;
      attackCooldown = ATTACK_COOLDOWN_TIME;

      // Atacar sem dar XP ao player
      attackGoblinWithoutXP(targetGoblin);

      System.out.println("⚔️ " + name + " EXECUTOU ATAQUE!");
    }
  }

  /**
   * Ataca um goblin sem dar XP ao player
   */
  private void attackGoblinWithoutXP(Goblin goblin) {
    int damage = 60; // Dano do guarda (maior que goblin normal)

    // Usar o método especial que não dá XP
    goblin.takeDamageFromNPC(damage);

    if (!goblin.isAlive()) {
      System.out.println("💀 " + name + " derrotou um goblin!");
      currentState = GuardState.RETURNING;
    }
  }

  /**
   * Renderiza o guarda com indicador visual quando está em combate
   */
  @Override
  public void render(Graphics2D g, com.rpggame.world.Camera camera) {
    // Renderizar NPC normalmente
    super.render(g, camera);

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Efeito visual de preparação de ataque
    if (isPreparingAttack) {
      // Círculo pulsante vermelho
      int pulseRadius = 15 + (int) (Math.sin(attackPreparationTimer * 0.3) * 5);
      g.setColor(new Color(255, 0, 0, 100));
      g.fillOval(screenX + width / 2 - pulseRadius, screenY + height / 2 - pulseRadius,
          pulseRadius * 2, pulseRadius * 2);

      // Texto de aviso
      g.setColor(Color.RED);
      g.setFont(new Font("Arial", Font.BOLD, 12));
      g.drawString("PREPARANDO!", screenX - 15, screenY - 30);
    }

    // Efeito visual de ataque
    if (isAttacking) {
      // Flash branco intenso
      g.setColor(new Color(255, 255, 255, 150));
      g.fillOval(screenX - 10, screenY - 10, width + 20, height + 20);

      // Raios de energia
      for (int i = 0; i < 8; i++) {
        double angle = (i * Math.PI * 2) / 8;
        int rayLength = 25;
        int endX = screenX + width / 2 + (int) (Math.cos(angle) * rayLength);
        int endY = screenY + height / 2 + (int) (Math.sin(angle) * rayLength);

        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(255, 255, 0, 200));
        g.drawLine(screenX + width / 2, screenY + height / 2, endX, endY);
      }
    }

    // Indicador de estado baseado no estado atual
    String stateIndicator = "";
    Color stateColor = Color.WHITE;

    switch (currentState) {
      case PATROLLING:
        if (targetGoblin != null) {
          stateIndicator = "?";
          stateColor = Color.YELLOW;
        }
        break;
      case PURSUING:
        stateIndicator = "!";
        stateColor = Color.ORANGE;
        break;
      case ATTACKING:
        stateIndicator = "⚔";
        stateColor = Color.RED;
        break;
      case RETURNING:
        stateIndicator = "←";
        stateColor = Color.BLUE;
        break;
    }

    if (!stateIndicator.isEmpty()) {
      g.setColor(new Color(0, 0, 0, 180));
      g.fillRoundRect(screenX + width / 2 - 10, screenY - 25, 20, 18, 5, 5);
      g.setColor(stateColor);
      g.setFont(new Font("Arial", Font.BOLD, 14));
      g.drawString(stateIndicator, screenX + width / 2 - 5, screenY - 10);
    }
  }
}
