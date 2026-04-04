package com.rpggame.entities;

import com.rpggame.core.GamePanel;

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
  private boolean cachedHasNearbyAllies = false;
  private static final double ALLY_SUPPORT_RADIUS = GamePanel.TILE_SIZE * 3.0; // ~3 tiles
  private static final double ALLY_SUPPORT_RADIUS_SQ = ALLY_SUPPORT_RADIUS * ALLY_SUPPORT_RADIUS;
  private static final int ALLY_CHECK_INTERVAL = 30; // Verifica aliados a cada 0.5s

  // Sistema de visão para stealth
  private double visionRange = 120.0; // Menor que detectionRange do player
  private double visionAngle = Math.PI / 3; // 60 graus (π/3 radianos)
  private double circularDetectionRange = 60.0; // Área circular de detecção próxima
  private double facingDirection = 0.0; // Direção que o goblin está olhando
  private boolean playerSpotted = false; // Se o player foi detectado
  private int alertTimer = 0; // Tempo em estado de alerta
  private static final int ALERT_DURATION = 180; // 3 segundos de alerta

  // Lista de todos os goblins (para guerra)
  private java.util.List<Goblin> allGoblins;

  // Referência ao conselho goblin
  private com.rpggame.systems.GoblinCouncil goblinCouncil;

  // Sistema de efeitos visuais de ataque
  private boolean isPreparingAttack = false;
  private int attackPreparationTimer = 0;
  private static final int ATTACK_PREPARATION_TIME = 45; // 0.75 segundos de preparação

  private boolean isAttacking = false;
  private int attackEffectTimer = 0;
  private static final int ATTACK_EFFECT_DURATION = 15; // 0.25 segundos de efeito

  // Sistema de spawn safety (evitar spawnar preso em paredes)
  private boolean inSpawnSafety = true;
  private int spawnSafetyTimer = 0;
  private static final int SPAWN_SAFETY_DURATION = 60; // 1 segundo a 60 FPS

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

    // Iniciar spawn safety
    this.inSpawnSafety = true;
    this.spawnSafetyTimer = SPAWN_SAFETY_DURATION;
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
    // Se personality ainda é null (primeira chamada do super), usar valores padrão
    // temporários
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

    // Estatísticas baseadas na personalidade (segunda chamada após personality
    // estar definida)
    this.maxHealth = personality.getBaseHealth();
    this.currentHealth = maxHealth;
    this.damage = personality.getBaseDamage();
    this.speed = 1.5 * personality.getSpeedMultiplier();
    this.experienceReward = 15;

    // Alcances ajustados por personalidade
    this.detectionRange = personality == GoblinPersonality.TIMID ? 60.0
        : personality == GoblinPersonality.AGGRESSIVE ? 100.0 : 80.0;
    this.attackRange = 35.0;

    // Área circular de detecção baseada na personalidade
    this.circularDetectionRange = personality == GoblinPersonality.TIMID ? 40.0 // Tímidos: área menor
        : personality == GoblinPersonality.AGGRESSIVE ? 80.0 // Agressivos: área maior
            : personality == GoblinPersonality.LEADER ? 90.0 // Líderes: área ainda maior
                : 60.0; // Padrão para COMMON
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

    System.out.println("Goblin preparando ataque! CUIDADO!");
  }

  /**
   * Executa o ataque após a preparação
   */
  private void executeAttack() {
    if (target == null)
      return;

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
    if (target == null)
      return;

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
    // Processar spawn safety primeiro
    if (inSpawnSafety) {
      handleSpawnSafety();
      spawnSafetyTimer--;
      if (spawnSafetyTimer <= 0) {
        inSpawnSafety = false;
      }
    }

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
    // IMPORTANTE: Se estiver encantado, usar IA da classe base para atacar aliados
    if (charmed) {
      System.out.println("💜 Goblin.updateAI() detectou charmed, chamando super.updateAI()");
      super.updateAI();
      return;
    }

    // Atualizar direção de olhar baseado no movimento
    updateFacingDirection();

    // Atualizar timers
    if (allyCheckTimer > 0) {
      allyCheckTimer--;
    }

    // Verificar decisões do conselho goblin
    boolean allianceActive = goblinCouncil != null && goblinCouncil.isAllianceAgainstPlayerActive();

    // Se aliança contra player está ativa, ignorar guerras entre famílias
    if (allianceActive) {
      // Focar apenas no player durante a aliança
      boolean playerDetected = detectPlayer();
      if (playerDetected && target != null) {
        double distanceToPlayer = Math.sqrt(
            Math.pow(target.getX() - x, 2) +
                Math.pow(target.getY() - y, 2));

        aggressive = true;
        engagePlayer(distanceToPlayer);
      } else {
        patrol();
      }
      return;
    }

    // Verificar se há guerra e inimigos próximos (apenas se não houver aliança
    // contra player)
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

    // Se tem família, seguir decisão do líder
    if (family != null) {
      boolean shouldEngage = family.shouldPursuePlayer((Player) target);

      if (!shouldEngage) {
        // Líder decidiu não perseguir: recuar para patrulha em vez de fuga permanente
        fleeing = false;
        aggressive = false;
        fearLevel = Math.max(0.0, fearLevel - 0.02);
        patrol();
        return;
      }
    }

    // Player já foi detectado pelo sistema de visão
    if (!hasNearbyAllies) {
      // Sozinho: fugir!
      fleeing = true;
      aggressive = false;
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
    } else if (aggressive) {
      engagePlayer(distanceToPlayer);
    } else {
      patrol();
    }
  }

  /**
   * Comportamento do goblin agressivo
   */
  private void updateAggressiveBehavior(double distanceToPlayer) {
    // Se tem família, considerar decisão do líder (mas é mais teimoso)
    if (family != null) {
      boolean shouldEngage = family.shouldPursuePlayer((Player) target);

      if (!shouldEngage && !family.isPlayerInTerritory((Player) target)) {
        // Fora do território e líder decidiu não perseguir
        // Agressivo persegue um pouco mais, mas eventualmente desiste
        if (distanceToPlayer > detectionRange * 1.5) {
          aggressive = false;
          return;
        }
      }
    }

    // Player já foi detectado pelo sistema de visão
    aggressive = true;

    if (aggressive) {
      engagePlayer(distanceToPlayer);
      // Persegue por mais tempo - só para de ser agressivo se perder totalmente o
      // player
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
      boolean shouldEngage = family.shouldPursuePlayer((Player) target);

      if (shouldEngage) {
        if (!aggressive) {
          // Log quando líder decide perseguir
          boolean inTerritory = family.isPlayerInTerritory((Player) target);
          System.out.println("⚔️ Líder de " + family.getFamilyName() + " decidiu perseguir o jogador! " +
              (inTerritory ? "(Dentro do território)" : "(Fora do território)"));
        }
        aggressive = true;
      }
    } else {
      // Sem família, comportamento padrão mais cauteloso - player já detectado
      aggressive = true;
    }

    if (aggressive) {
      engagePlayer(distanceToPlayer);
      // Líder desiste mais facilmente fora do território
      if (family != null && !family.isPlayerInTerritory((Player) target) &&
          distanceToPlayer > detectionRange * 2) {
        System.out
            .println("🏳️ Líder de " + family.getFamilyName() + " desistiu da perseguição (muito longe do território)");
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
    // Se tem família, seguir decisão do líder
    if (family != null) {
      boolean shouldEngage = family.shouldPursuePlayer((Player) target);

      if (shouldEngage) {
        aggressive = true;
      } else {
        aggressive = false;
        patrol();
        return;
      }
    } else {
      // Sem família, comportamento padrão
      aggressive = true;
    }

    if (aggressive) {
      engagePlayer(distanceToPlayer);
      // Desistir se perder totalmente o player ou líder decidir recuar
      if (!playerSpotted && alertTimer <= 0) {
        aggressive = false;
      }
      if (family != null && !family.isPlayerInTerritory((Player) target) &&
          distanceToPlayer > detectionRange * 2) {
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
    if (target == null)
      return;

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
    if (family == null) {
      cachedHasNearbyAllies = false;
      return false;
    }

    if (allyCheckTimer <= 0) {
      cachedHasNearbyAllies = false;
      allyCheckTimer = ALLY_CHECK_INTERVAL;

      for (Goblin ally : family.getMembers()) {
        if (ally != this) {
          double allyDx = ally.getX() - x;
          double allyDy = ally.getY() - y;
          double distanceSq = allyDx * allyDx + allyDy * allyDy;
          if (distanceSq <= ALLY_SUPPORT_RADIUS_SQ) { // Aliado próximo (~3 tiles)
            cachedHasNearbyAllies = true;
            break;
          }
        }
      }
    }

    return cachedHasNearbyAllies;
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
    allyCheckTimer = 0;
    cachedHasNearbyAllies = false;
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
   * Define o conselho goblin (chamado pelo EnemyManager)
   */
  public void setGoblinCouncil(com.rpggame.systems.GoblinCouncil council) {
    this.goblinCouncil = council;
  }

  /**
   * Retorna se o goblin está em período de spawn safety
   */
  public boolean isInSpawnSafety() {
    return inSpawnSafety;
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
      int attackDamage = (int) (damage * personality.getStrengthMultiplier());
      enemy.takeDamage(attackDamage);
      attackCooldown = ATTACK_COOLDOWN_TIME;

      // Efeito visual do ataque
      System.out.println("*CLASH* " + personality + " goblin ataca goblin inimigo!");
    }
  }

  /**
   * Retorna o dano do goblin (aplicando multiplicador tecnológico se ativo)
   */
  @Override
  public int getDamage() {
    double baseDamage = damage;

    // Aplicar multiplicador de avanço tecnológico se ativo
    if (goblinCouncil != null && goblinCouncil.isTechnologicalAdvanceActive()) {
      baseDamage *= goblinCouncil.getStrengthMultiplier();
    }

    return (int) baseDamage;
  }

  /**
   * Verifica se o player está no campo de visão do goblin
   */
  private boolean canSeePlayer() {
    if (target == null)
      return false;

    // Calcular distância ao player
    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    double distanceToPlayer = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    // Área circular de detecção próxima (360 graus) - mais difícil de escapar
    if (distanceToPlayer <= circularDetectionRange) {
      return true; // Detectado em qualquer direção quando muito próximo
    }

    // Verificar se está dentro do alcance de visão normal (cone)
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
    while (angle > Math.PI)
      angle -= 2 * Math.PI;
    while (angle < -Math.PI)
      angle += 2 * Math.PI;
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
   * Lida com o período de spawn safety onde o goblin pode atravessar paredes
   * e se move automaticamente para longe delas
   */
  private void handleSpawnSafety() {
    if (tileMap == null)
      return;

    // Verificar tiles ao redor para detectar paredes próximas
    int tileSize = GamePanel.TILE_SIZE;
    int currentTileX = (int) (x / tileSize);
    int currentTileY = (int) (y / tileSize);

    // Direção de escape (vetor que aponta para longe de paredes)
    double escapeX = 0.0;
    double escapeY = 0.0;

    // Verificar tiles em um raio de 2 tiles ao redor
    for (int dy = -2; dy <= 2; dy++) {
      for (int dx = -2; dx <= 2; dx++) {
        int checkX = currentTileX + dx;
        int checkY = currentTileY + dy;

        if (!tileMap.isWalkable(checkX, checkY)) {
          // Encontrou uma parede, calcular vetor de repulsão
          double wallCenterX = checkX * tileSize + tileSize / 2.0;
          double wallCenterY = checkY * tileSize + tileSize / 2.0;

          double deltaX = x - wallCenterX;
          double deltaY = y - wallCenterY;
          double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

          if (distance > 0 && distance < tileSize * 2) {
            // Peso inversamente proporcional à distância (mais forte quando mais perto)
            double weight = (tileSize * 2 - distance) / (tileSize * 2);
            escapeX += (deltaX / distance) * weight;
            escapeY += (deltaY / distance) * weight;
          }
        }
      }
    }

    // Se detectou paredes próximas, mover para longe delas
    if (escapeX != 0.0 || escapeY != 0.0) {
      double magnitude = Math.sqrt(escapeX * escapeX + escapeY * escapeY);
      if (magnitude > 0) {
        // Normalizar e aplicar velocidade de escape (mais rápido que velocidade normal)
        double escapeSpeed = speed * 3.0; // 3x mais rápido durante spawn safety
        dx = (escapeX / magnitude) * escapeSpeed;
        dy = (escapeY / magnitude) * escapeSpeed;

        // Aplicar movimento (sem verificação de colisão durante spawn safety)
        x += dx;
        y += dy;
      }
    }
  }

  /**
   * Retorna o tipo do inimigo para o sistema de quests
   */
  @Override
  protected String getEnemyType() {
    return "Goblin";
  }
}
