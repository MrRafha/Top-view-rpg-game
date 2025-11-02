package com.rpggame.entities;

import java.awt.*;
import com.rpggame.world.Camera;

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
  
  // Sistema de personalidade e família
  private GoblinPersonality personality;
  private GoblinFamily family;
  
  // Estados comportamentais
  private boolean fleeing = false;
  private double fearLevel = 0.0; // 0.0 a 1.0
  private int allyCheckTimer = 0;
  private static final int ALLY_CHECK_INTERVAL = 30; // Verifica aliados a cada 0.5s
  
  // Sistema de visão para stealth
  private double visionRange = 120.0; // Menor que detectionRange do player
  private double visionAngle = Math.PI / 3; // 60 graus (π/3 radianos)
  private double facingDirection = 0.0; // Direção que o goblin está olhando
  private boolean playerSpotted = false; // Se o player foi detectado
  private int alertTimer = 0; // Tempo em estado de alerta
  private static final int ALERT_DURATION = 180; // 3 segundos de alerta
  
  // Lista de todos os goblins (para guerra)
  private java.util.List<Goblin> allGoblins;
  
  // Sistema de efeitos visuais de ataque
  private boolean isPreparingAttack = false;
  private int attackPreparationTimer = 0;
  private static final int ATTACK_PREPARATION_TIME = 45; // 0.75 segundos de preparação
  
  private boolean isAttacking = false;
  private int attackEffectTimer = 0;
  private static final int ATTACK_EFFECT_DURATION = 15; // 0.25 segundos de efeito
  
  // Efeito visual do slash
  private double slashStartAngle = 0.0;
  private double slashEndAngle = 0.0;
  private int slashRadius = 40;

  /**
   * Construtor do Goblin com personalidade
   */
  public Goblin(double x, double y, GoblinPersonality personality) {
    super(x, y, personality.getSpritePath());
    
    // Inicializar personalidade ANTES que initializeStats seja chamado
    this.personality = personality;
    
    // Re-inicializar estatísticas agora que temos a personalidade
    initializeStats();

    // Definir centro da patrulha como posição inicial
    this.patrolCenterX = x;
    this.patrolCenterY = y;

    // Definir primeiro alvo de patrulha
    setNewPatrolTarget();
  }
  
  /**
   * Construtor legado do Goblin (personalidade comum)
   */
  public Goblin(double x, double y) {
    this(x, y, GoblinPersonality.COMMON);
  }

  /**
   * Inicializa as estatísticas específicas do Goblin baseadas na personalidade
   */
  @Override
  protected void initializeStats() {
    // Se personality ainda é null (primeira chamada do super), usar valores padrão temporários
    if (personality == null) {
      this.maxHealth = 25;
      this.currentHealth = maxHealth;
      this.damage = 8;
      this.speed = 1.5;
      this.experienceReward = 15;
      this.detectionRange = 80.0;
      this.attackRange = 35.0;
      return;
    }
    
    // Estatísticas baseadas na personalidade (segunda chamada após personality estar definida)
    this.maxHealth = personality.getBaseHealth();
    this.currentHealth = maxHealth;
    this.damage = personality.getBaseDamage();
    this.speed = 1.5 * personality.getSpeedMultiplier();
    this.experienceReward = 15;

    // Alcances ajustados por personalidade
    this.detectionRange = personality == GoblinPersonality.TIMID ? 60.0 : 
                         personality == GoblinPersonality.AGGRESSIVE ? 100.0 : 80.0;
    this.attackRange = 35.0;
  }

  /**
   * Ataque corpo a corpo do Goblin com sistema de preparação
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
      // Se não está preparando ataque, iniciar preparação
      if (!isPreparingAttack && !isAttacking) {
        startAttackPreparation();
      }
    }
  }
  
  /**
   * Inicia a preparação do ataque (telegraphing)
   */
  private void startAttackPreparation() {
    isPreparingAttack = true;
    attackPreparationTimer = ATTACK_PREPARATION_TIME;
    
    // Calcular ângulos para o slash baseado na direção do target
    if (target != null) {
      double angleToTarget = Math.atan2(target.getY() - y, target.getX() - x);
      slashStartAngle = angleToTarget - Math.PI / 4; // -45 graus
      slashEndAngle = angleToTarget + Math.PI / 4;   // +45 graus
    }
    
    System.out.println("Goblin preparando ataque! CUIDADO!");
  }
  
  /**
   * Executa o ataque após a preparação
   */
  private void executeAttack() {
    if (target == null) return;
    
    // Verificar se ainda está no alcance
    double distance = Math.sqrt(
        Math.pow(target.getX() - x, 2) +
            Math.pow(target.getY() - y, 2));
    
    if (distance <= attackRange) {
      // Realizar estocada na direção do player
      performLunge();
      
      // Causar dano ao jogador
      if (target instanceof Player) {
        Player player = (Player) target;
        player.takeDamage(damage);
        System.out.println("Goblin atacou o jogador! Dano: " + damage);
      }
      
      // Iniciar efeito visual de ataque
      isAttacking = true;
      attackEffectTimer = ATTACK_EFFECT_DURATION;
    }
    
    // Finalizar preparação
    isPreparingAttack = false;
    attackPreparationTimer = 0;
  }
  
  /**
   * Realiza uma estocada na direção do target
   */
  private void performLunge() {
    if (target == null) return;
    
    // Calcular direção para o target
    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    
    if (distance > 0) {
      // Estocada de 20 pixels na direção do target
      double lungeDistance = 20.0;
      double lungeX = (deltaX / distance) * lungeDistance;
      double lungeY = (deltaY / distance) * lungeDistance;
      
      // Aplicar movimento da estocada
      x += lungeX;
      y += lungeY;
    }
  }



  /**
   * Sobrescrevendo update para gerenciar efeitos visuais de ataque
   */
  @Override
  public void update(Player player) {
    // Chamar update da classe pai
    super.update(player);
    
    // Atualizar efeitos visuais de ataque
    updateAttackEffects();
  }
  
  /**
   * Atualiza os efeitos visuais de ataque
   */
  private void updateAttackEffects() {
    // Atualizar timer de preparação de ataque
    if (isPreparingAttack) {
      attackPreparationTimer--;
      if (attackPreparationTimer <= 0) {
        // Tempo de preparação acabou, executar ataque
        executeAttack();
      }
    }
    
    // Atualizar timer de efeito visual do ataque
    if (isAttacking) {
      attackEffectTimer--;
      if (attackEffectTimer <= 0) {
        isAttacking = false;
      }
    }
  }

  /**
   * IA específica do Goblin com personalidade e comportamento de família
   */
  @Override
  protected void updateAI() {
    // Atualizar direção de olhar baseado no movimento
    updateFacingDirection();
    
    // Atualizar timers
    allyCheckTimer--;
    
    // Verificar se há guerra e inimigos próximos
    if (family != null && family.isAtWar()) {
      Goblin nearestEnemy = findNearestEnemyGoblin();
      if (nearestEnemy != null) {
        // Priorizar ataque a goblin inimigo sobre player
        double distanceToEnemy = Math.sqrt(
          Math.pow(nearestEnemy.getX() - x, 2) +
          Math.pow(nearestEnemy.getY() - y, 2));
        
        if (distanceToEnemy > attackRange) {
          moveTowardsEnemyGoblin(nearestEnemy);
        } else {
          attackEnemyGoblin(nearestEnemy);
        }
        return; // Sair do método - foco no inimigo goblin
      }
    }
    
    // Sistema de detecção com stealth
    boolean playerDetected = detectPlayer();
    
    // Se não detectou o player, patrulhar
    if (!playerDetected || target == null) {
      patrol();
      return;
    }

    double distanceToPlayer = Math.sqrt(
        Math.pow(target.getX() - x, 2) +
            Math.pow(target.getY() - y, 2));
    
    // Comportamento baseado na personalidade
    switch (personality) {
      case TIMID:
        updateTimidBehavior(distanceToPlayer);
        break;
      case AGGRESSIVE:
        updateAggressiveBehavior(distanceToPlayer);
        break;
      case LEADER:
        updateLeaderBehavior(distanceToPlayer);
        break;
      case COMMON:
      default:
        updateCommonBehavior(distanceToPlayer);
        break;
    }
  }
  
  /**
   * Comportamento do goblin tímido
   */
  private void updateTimidBehavior(double distanceToPlayer) {
    boolean hasNearbyAllies = hasNearbyAllies();
    
    // Player já foi detectado pelo sistema de visão
    if (!hasNearbyAllies) {
      // Sozinho: fugir!
      fleeing = true;
      fearLevel = Math.min(1.0, fearLevel + 0.02);
    } else {
      // Com aliados: lutar!
      fleeing = false;
      fearLevel = Math.max(0.0, fearLevel - 0.01);
      aggressive = true;
    }
    
    if (fleeing) {
      fleeFromPlayer();
      // Parar de fugir se perder o player de vista
      if (!playerSpotted && alertTimer <= 0) {
        fleeing = false;
        fearLevel = Math.max(0.0, fearLevel - 0.05);
      }
    } else if (aggressive && hasNearbyAllies) {
      engagePlayer(distanceToPlayer);
    } else {
      patrol();
    }
  }
  
  /**
   * Comportamento do goblin agressivo
   */
  private void updateAggressiveBehavior(double distanceToPlayer) {
    // Mais agressivo que o normal - player já foi detectado
    aggressive = true;
    
    if (aggressive) {
      engagePlayer(distanceToPlayer);
      // Persegue por mais tempo - só para de ser agressivo se perder totalmente o player
      if (!playerSpotted && alertTimer <= 0) {
        aggressive = false;
      }
    }
  }
  
  /**
   * Comportamento do líder
   */
  private void updateLeaderBehavior(double distanceToPlayer) {
    if (family != null) {
      // Decisão baseada na família e território - player já detectado
      boolean shouldEngage = family.shouldPursuePlayer((Player)target);
      
      if (shouldEngage) {
        aggressive = true;
      }
    } else {
      // Sem família, comportamento padrão mais cauteloso - player já detectado
      aggressive = true;
    }
    
    if (aggressive) {
      engagePlayer(distanceToPlayer);
      // Líder desiste mais facilmente fora do território
      if (family != null && !family.isPlayerInTerritory((Player)target) && 
          distanceToPlayer > detectionRange * 2) {
        aggressive = false;
      }
    } else {
      patrol();
    }
  }
  
  /**
   * Comportamento do goblin comum
   */
  private void updateCommonBehavior(double distanceToPlayer) {
    // Player já foi detectado pelo sistema de visão
    aggressive = true;

    if (aggressive) {
      engagePlayer(distanceToPlayer);
      // Desistir se perder totalmente o player
      if (!playerSpotted && alertTimer <= 0) {
        aggressive = false;
      }
    }
  }
  
  /**
   * Lógica padrão de engajamento com o player
   */
  private void engagePlayer(double distanceToPlayer) {
    if (distanceToPlayer > attackRange) {
      moveTowardsPlayer();
    } else {
      attemptAttack();
    }
  }
  
  /**
   * Foge do player
   */
  private void fleeFromPlayer() {
    if (target == null) return;
    
    // Calcular direção oposta ao player
    double deltaX = x - target.getX();
    double deltaY = y - target.getY();
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    
    if (distance > 0) {
      // Velocidade aumentada pelo medo
      double fleeSpeed = speed * (1.0 + fearLevel * 0.5);
      dx = (deltaX / distance) * fleeSpeed;
      dy = (deltaY / distance) * fleeSpeed;
    }
  }
  
  /**
   * Verifica se há aliados próximos (mesma família)
   */
  private boolean hasNearbyAllies() {
    if (allyCheckTimer > 0 || family == null) {
      return false; // Usar cache para performance
    }
    
    allyCheckTimer = ALLY_CHECK_INTERVAL;
    
    for (Goblin ally : family.getMembers()) {
      if (ally != this) {
        double distance = Math.sqrt(
          Math.pow(ally.getX() - x, 2) + Math.pow(ally.getY() - y, 2)
        );
        if (distance <= 80.0) { // Aliado próximo
          return true;
        }
      }
    }
    return false;
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
  
  // Getters e Setters para família e personalidade
  public GoblinPersonality getPersonality() {
    return personality;
  }
  
  public GoblinFamily getFamily() {
    return family;
  }
  
  public void setFamily(GoblinFamily family) {
    this.family = family;
  }
  
  public boolean isFleeing() {
    return fleeing;
  }
  
  public double getFearLevel() {
    return fearLevel;
  }
  
  /**
   * Procura por goblins inimigos próximos durante guerra
   */
  private Goblin findNearestEnemyGoblin() {
    if (family == null || !family.isAtWar()) {
      return null;
    }
    
    Goblin nearestEnemy = null;
    double nearestDistance = Double.MAX_VALUE;
    
    // Procurar goblins inimigos em um raio de 200 pixels
    for (Goblin otherGoblin : getAllGoblins()) {
      if (otherGoblin != this && otherGoblin.getFamily() != null) {
        // Verificar se são de famílias inimigas
        if (family.isEnemyOf(otherGoblin.getFamily())) {
          double distance = Math.sqrt(
            Math.pow(otherGoblin.getX() - x, 2) +
            Math.pow(otherGoblin.getY() - y, 2));
          
          if (distance < 200 && distance < nearestDistance) {
            nearestDistance = distance;
            nearestEnemy = otherGoblin;
          }
        }
      }
    }
    
    return nearestEnemy;
  }
  
  /**
   * Obtém lista de todos os goblins (implementação via EnemyManager)
   */
  private java.util.List<Goblin> getAllGoblins() {
    // Esta lista será preenchida pelo EnemyManager
    return allGoblins != null ? allGoblins : new java.util.ArrayList<>();
  }
  
  /**
   * Define a lista de todos os goblins (chamado pelo EnemyManager)
   */
  public void setAllGoblins(java.util.List<Goblin> goblins) {
    this.allGoblins = goblins;
  }
  
  /**
   * Move em direção a um goblin inimigo
   */
  private void moveTowardsEnemyGoblin(Goblin enemy) {
    double deltaX = enemy.getX() - x;
    double deltaY = enemy.getY() - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    
    if (distance > 0) {
      dx = (deltaX / distance) * speed;
      dy = (deltaY / distance) * speed;
    }
  }
  
  /**
   * Ataca um goblin inimigo
   */
  private void attackEnemyGoblin(Goblin enemy) {
    if (attackCooldown > 0) {
      return;
    }
    
    double distance = Math.sqrt(
      Math.pow(enemy.getX() - x, 2) +
      Math.pow(enemy.getY() - y, 2));
    
    if (distance <= attackRange) {
      // Causar dano ao goblin inimigo
      int attackDamage = (int)(damage * personality.getStrengthMultiplier());
      enemy.takeDamage(attackDamage);
      attackCooldown = ATTACK_COOLDOWN_TIME;
      
      // Efeito visual do ataque
      System.out.println("*CLASH* " + personality + " goblin ataca goblin inimigo!");
    }
  }
  
  /**
   * Verifica se o player está no campo de visão do goblin
   */
  private boolean canSeePlayer() {
    if (target == null) return false;
    
    // Calcular distância ao player
    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    double distanceToPlayer = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    
    // Verificar se está dentro do alcance de visão
    if (distanceToPlayer > visionRange) {
      return false;
    }
    
    // Calcular ângulo para o player
    double angleToPlayer = Math.atan2(deltaY, deltaX);
    
    // Normalizar ângulos para [-π, π]
    double angleDifference = normalizeAngle(angleToPlayer - facingDirection);
    
    // Verificar se o player está dentro do cone de visão
    return Math.abs(angleDifference) <= visionAngle / 2;
  }
  
  /**
   * Normaliza um ângulo para o intervalo [-π, π]
   */
  private double normalizeAngle(double angle) {
    while (angle > Math.PI) angle -= 2 * Math.PI;
    while (angle < -Math.PI) angle += 2 * Math.PI;
    return angle;
  }
  
  /**
   * Atualiza a direção que o goblin está olhando baseado no movimento
   */
  private void updateFacingDirection() {
    if (Math.abs(dx) > 0.1 || Math.abs(dy) > 0.1) {
      facingDirection = Math.atan2(dy, dx);
    }
  }
  
  /**
   * Sistema de detecção com stealth
   */
  private boolean detectPlayer() {
    // Atualizar timer de alerta
    if (alertTimer > 0) {
      alertTimer--;
    }
    
    // Se pode ver o player, ativar detecção
    if (canSeePlayer()) {
      if (!playerSpotted) {
        playerSpotted = true;
        System.out.println("👁 " + personality + " goblin avistou o player!");
      }
      alertTimer = ALERT_DURATION;
      return true;
    }
    
    // Se estava em alerta mas não vê mais, manter alerta por um tempo
    if (alertTimer > 0) {
      return true;
    }
    
    // Perder o player
    if (playerSpotted) {
      playerSpotted = false;
      System.out.println("❓ " + personality + " goblin perdeu o player de vista");
    }
    
    return false;
  }
  
  /**
   * Renderiza o campo de visão do goblin (opcional para debug)
   */
  public void renderVisionCone(Graphics2D g, Camera camera) {
    if (!alive) return;
    
    // Posição na tela
    int screenX = (int)(x - camera.getX());
    int screenY = (int)(y - camera.getY());
    
    // Só renderizar se o goblin estiver na tela
    if (screenX < -100 || screenX > 900 || screenY < -100 || screenY > 700) {
      return;
    }
    
    // Cor do cone de visão
    if (playerSpotted) {
      g.setColor(new Color(255, 0, 0, 30)); // Vermelho se detectou player
    } else {
      g.setColor(new Color(255, 255, 0, 20)); // Amarelo normal
    }
    
    // Calcular pontos do cone
    int coneLength = (int)visionRange;
    double leftAngle = facingDirection - visionAngle / 2;
    double rightAngle = facingDirection + visionAngle / 2;
    
    int[] xPoints = {
      screenX + width/2,
      screenX + width/2 + (int)(Math.cos(leftAngle) * coneLength),
      screenX + width/2 + (int)(Math.cos(rightAngle) * coneLength)
    };
    
    int[] yPoints = {
      screenY + height/2,
      screenY + height/2 + (int)(Math.sin(leftAngle) * coneLength),
      screenY + height/2 + (int)(Math.sin(rightAngle) * coneLength)
    };
    
    // Desenhar cone de visão
    g.fillPolygon(xPoints, yPoints, 3);
    
    // Desenhar linha de direção
    g.setColor(playerSpotted ? Color.RED : Color.YELLOW);
    g.drawLine(
      screenX + width/2, 
      screenY + height/2,
      screenX + width/2 + (int)(Math.cos(facingDirection) * 30),
      screenY + height/2 + (int)(Math.sin(facingDirection) * 30)
    );
  }
  
  /**
   * Renderiza os efeitos visuais de ataque (preparação e execução)
   */
  public void renderAttackEffects(Graphics2D g, Camera camera) {
    int screenX = (int)(x - camera.getX());
    int screenY = (int)(y - camera.getY());
    
    // Só renderizar se o goblin estiver na tela
    if (screenX < -100 || screenX > 900 || screenY < -100 || screenY > 700) {
      return;
    }
    
    int centerX = screenX + width / 2;
    int centerY = screenY + height / 2;
    
    // Renderizar indicador de preparação de ataque
    if (isPreparingAttack) {
      renderAttackWarning(g, centerX, centerY);
    }
    
    // Renderizar efeito visual do ataque
    if (isAttacking) {
      renderAttackSlash(g, centerX, centerY);
    }
  }
  
  /**
   * Renderiza o aviso visual de que o goblin está prestes a atacar
   */
  private void renderAttackWarning(Graphics2D g, int centerX, int centerY) {
    // Calcular intensidade baseada no tempo restante
    float intensity = 1.0f - ((float)attackPreparationTimer / ATTACK_PREPARATION_TIME);
    
    // Cor vermelha piscante mais intensa conforme se aproxima do ataque
    int alpha = (int)(100 + 155 * intensity); // De 100 a 255
    Color warningColor = new Color(255, 0, 0, Math.min(255, alpha));
    
    // Efeito de pulso - círculo que cresce
    int pulseRadius = (int)(20 + 15 * intensity);
    g.setColor(warningColor);
    g.drawOval(centerX - pulseRadius, centerY - pulseRadius, 
               pulseRadius * 2, pulseRadius * 2);
    
    // Círculo interno mais sólido
    int innerRadius = (int)(5 + 10 * intensity);
    Color innerColor = new Color(255, 100, 100, Math.min(255, alpha + 50));
    g.setColor(innerColor);
    g.fillOval(centerX - innerRadius, centerY - innerRadius,
               innerRadius * 2, innerRadius * 2);
    
    // Indicadores direcionais mostrando onde será o ataque
    g.setStroke(new BasicStroke(2));
    g.setColor(new Color(255, 255, 0, Math.min(255, alpha)));
    
    // Linhas indicando a área de ataque
    int indicatorLength = slashRadius / 2;
    for (double angle = slashStartAngle; angle <= slashEndAngle; angle += Math.PI / 8) {
      int endX = centerX + (int)(Math.cos(angle) * indicatorLength);
      int endY = centerY + (int)(Math.sin(angle) * indicatorLength);
      g.drawLine(centerX, centerY, endX, endY);
    }
  }
  
  /**
   * Renderiza o efeito visual do slash do ataque
   */
  private void renderAttackSlash(Graphics2D g, int centerX, int centerY) {
    // Calcular intensidade baseada no tempo restante do efeito
    float intensity = (float)attackEffectTimer / ATTACK_EFFECT_DURATION;
    
    // Cor do slash - branco para amarelo
    Color slashColor = new Color(255, 255, (int)(100 * intensity), (int)(200 * intensity));
    g.setColor(slashColor);
    g.setStroke(new BasicStroke(4));
    
    // Desenhar múltiplas linhas para criar efeito de slash
    int numLines = 5;
    for (int i = 0; i < numLines; i++) {
      double angle = slashStartAngle + (slashEndAngle - slashStartAngle) * i / (numLines - 1);
      
      // Variar o comprimento das linhas para criar efeito mais natural
      int lineLength = slashRadius + (int)(Math.random() * 10 - 5);
      
      int endX = centerX + (int)(Math.cos(angle) * lineLength);
      int endY = centerY + (int)(Math.sin(angle) * lineLength);
      
      // Linha principal do slash
      g.drawLine(centerX, centerY, endX, endY);
      
      // Pequenas linhas perpendiculares para dar mais impacto
      if (i % 2 == 0) {
        double perpAngle = angle + Math.PI / 2;
        int perpLength = 8;
        int perpX1 = endX + (int)(Math.cos(perpAngle) * perpLength);
        int perpY1 = endY + (int)(Math.sin(perpAngle) * perpLength);
        int perpX2 = endX - (int)(Math.cos(perpAngle) * perpLength);
        int perpY2 = endY - (int)(Math.sin(perpAngle) * perpLength);
        
        g.setStroke(new BasicStroke(2));
        g.drawLine(perpX1, perpY1, perpX2, perpY2);
        g.setStroke(new BasicStroke(4));
      }
    }
    
    // Efeito de partículas ao redor do slash
    g.setColor(new Color(255, 200, 0, (int)(150 * intensity)));
    for (int i = 0; i < 8; i++) {
      double particleAngle = slashStartAngle + Math.random() * (slashEndAngle - slashStartAngle);
      int particleDistance = slashRadius + (int)(Math.random() * 20);
      int particleX = centerX + (int)(Math.cos(particleAngle) * particleDistance);
      int particleY = centerY + (int)(Math.sin(particleAngle) * particleDistance);
      
      g.fillOval(particleX - 2, particleY - 2, 4, 4);
    }
  }
}