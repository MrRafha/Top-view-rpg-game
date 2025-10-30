import java.awt.*;

/**
 * Classe do inimigo Goblin - inimigo básico corpo a corpo
 */
public class Goblin extends Enemy {

  // Variáveis para patrulha
  private double patrolCenterX, patrolCenterY;
  private double patrolRadius = 100.0;
  private double patrolTargetX, patrolTargetY;
  private int patrolTimer = 0;
  private final int PATROL_CHANGE_TIME = 120; // Muda direção a cada 2 segundos

  /**
   * Construtor do Goblin
   */
  public Goblin(double x, double y) {
    super(x, y, "sprites/CommonGoblin.png");

    // Definir centro da patrulha como posição inicial
    this.patrolCenterX = x;
    this.patrolCenterY = y;

    // Definir primeiro alvo de patrulha
    setNewPatrolTarget();
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
    if (target == null) {
      patrol();
      return;
    }

    double distanceToPlayer = Math.sqrt(
        Math.pow(target.getX() - x, 2) +
            Math.pow(target.getY() - y, 2));

    // Detectar jogador
    if (distanceToPlayer <= detectionRange) {
      aggressive = true;
    }

    if (aggressive) {
      // Mover em direção ao jogador
      if (distanceToPlayer > attackRange) {
        moveTowardsPlayer();
      } else {
        // Atacar se estiver no alcance
        attemptAttack();
      }

      // Desistir se muito longe
      if (distanceToPlayer > detectionRange * 2.5) {
        aggressive = false;
      }
    } else {
      // Patrulhar quando não está perseguindo
      patrol();
    }
  }

  /**
   * Define um novo alvo de patrulha aleatório
   */
  private void setNewPatrolTarget() {
    double angle = Math.random() * 2 * Math.PI;
    double distance = Math.random() * patrolRadius;

    patrolTargetX = patrolCenterX + Math.cos(angle) * distance;
    patrolTargetY = patrolCenterY + Math.sin(angle) * distance;

    patrolTimer = PATROL_CHANGE_TIME;
  }

  /**
   * Executa comportamento de patrulha
   */
  private void patrol() {
    patrolTimer--;

    // Verificar se chegou próximo ao alvo ou se é hora de mudar
    double distanceToTarget = Math.sqrt(
        Math.pow(patrolTargetX - x, 2) +
            Math.pow(patrolTargetY - y, 2));

    if (distanceToTarget < 20 || patrolTimer <= 0) {
      setNewPatrolTarget();
    }

    // Mover em direção ao alvo de patrulha
    double deltaX = patrolTargetX - x;
    double deltaY = patrolTargetY - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > 0) {
      dx = (deltaX / distance) * speed * 0.5; // Patrulha mais devagar
      dy = (deltaY / distance) * speed * 0.5;
    }
  }
}