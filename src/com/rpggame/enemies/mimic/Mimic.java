package com.rpggame.enemies.mimic;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;

/**
 * Mimic - Inimigo disfarçado de baú.
 * Fica imóvel até o player se aproximar, então revela e ataca.
 */
public class Mimic extends Enemy {

  // Estados do Mimic
  private enum MimicState {
    DISGUISED, // Disfarçado como baú
    REVEALING, // Animação de revelação
    ATTACKING, // Ataque inicial
    ACTIVE // Perseguindo normalmente
  }

  private MimicState state = MimicState.DISGUISED;

  // Sprites
  private BufferedImage disguisedSprite; // Baú fechado
  private BufferedImage attack1Sprite; // Frame 1 do ataque
  private BufferedImage attack2Sprite; // Frame 2 do ataque
  private BufferedImage activeSprite; // Forma ativa

  // Sistema de detecção
  private static final double DETECTION_RADIUS = 150.0;

  // Sistema de ataque inicial
  private static final int REVEAL_TIME = 30; // 0.5 segundos de revelação
  private static final int ATTACK_WARN_TIME = 120; // 2 segundos de aviso
  private int stateTimer = 0;
  private boolean hasAttacked = false;

  // Animação de ataque
  private int attackAnimFrame = 0;
  private static final int ATTACK_ANIM_SPEED = 10;

  // Sistema de ataque com língua
  private boolean tongueAttacking = false;
  private int tongueAttackTimer = 0;
  private static final int TONGUE_ATTACK_DURATION = 60; // 1 segundo
  private double tongueTargetX = 0;
  private double tongueTargetY = 0;
  private double tongueLength = 0;
  private static final double MAX_TONGUE_LENGTH = 150.0;
  private static final double TONGUE_SPEED = 6.0; // Aumentado para garantir que atinja 80% (120 unidades) em 20 frames

  // Sistema de puxar pela língua
  private boolean pullingToPlayer = false;
  private static final double PULL_SPEED = 8.0; // Velocidade que o Mimic se puxa
  private double pullTargetX = 0;
  private double pullTargetY = 0;

  // Sistema de grab (prender player)
  private boolean playerGrabbed = false;
  private int escapeProgress = 0;
  private static final int ESCAPE_REQUIRED = 15; // Precisa apertar Space 15 vezes
  private int grabDamageTimer = 0;
  private static final int GRAB_DAMAGE_INTERVAL = 60; // 1 dano por segundo

  /**
   * Construtor do Mimic.
   */
  public Mimic(double x, double y) {
    super(x, y, "sprites/Mimic.png");
    loadAllSprites();
  }

  @Override
  protected void initializeStats() {
    this.maxHealth = 80;
    this.currentHealth = maxHealth;
    this.damage = 10; // Dano reduzido porque agora prende o player
    this.speed = 1.5;
    this.experienceReward = 100;
    this.width = 48;
    this.height = 48;
    this.detectionRange = DETECTION_RADIUS;
    this.attackRange = 50.0;
  }

  /**
   * Carrega todos os sprites do Mimic.
   */
  private void loadAllSprites() {
    disguisedSprite = loadSpriteFile("sprites/ClosedChest.png"); // Usa sprite do baú fechado
    attack1Sprite = loadSpriteFile("sprites/MimicAttack1.png");
    attack2Sprite = loadSpriteFile("sprites/MimicAttack2.png");
    activeSprite = loadSpriteFile("sprites/Mimic.png"); // Sprite do Mimic revelado

    if (disguisedSprite != null && attack1Sprite != null && attack2Sprite != null && activeSprite != null) {
      System.out.println("✅ Sprites do Mimic carregados");
    } else {
      System.err.println("❌ Erro ao carregar sprites do Mimic");
    }
  }

  /**
   * Carrega um sprite individual.
   */
  private BufferedImage loadSpriteFile(String path) {
    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream(path);
      if (is != null) {
        BufferedImage img = ImageIO.read(is);
        is.close();
        return img;
      }

      String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(path);
      java.io.File file = new java.io.File(resolvedPath);
      if (file.exists()) {
        return ImageIO.read(file);
      }
    } catch (IOException e) {
      System.err.println("❌ Erro ao carregar sprite: " + path);
    }
    return null;
  }

  @Override
  protected void updateAI() {
    if (target == null) {
      return;
    }

    double distanceToPlayer = Math.sqrt(
        Math.pow(target.getX() - x, 2)
            + Math.pow(target.getY() - y, 2));

    switch (state) {
      case DISGUISED:
        // Imóvel, apenas detectando
        if (distanceToPlayer <= DETECTION_RADIUS) {
          state = MimicState.REVEALING;
          stateTimer = REVEAL_TIME;
          System.out.println("👹 Mimic revelando! Player a " + distanceToPlayer + " unidades");
        }
        break;

      case REVEALING:
        // Animação de revelação
        stateTimer--;
        if (stateTimer <= 0) {
          state = MimicState.ATTACKING;
          stateTimer = ATTACK_WARN_TIME;
          System.out.println("⚠️ Mimic preparando ataque!");
        }
        break;

      case ATTACKING:
        // Aviso de ataque (2 segundos)
        stateTimer--;
        attackAnimFrame++;

        // Se está puxando pela língua
        if (pullingToPlayer) {
          updatePulling();
        } else {
          // Executar ataque no final do timer
          if (stateTimer <= 0 && !hasAttacked) {
            executeInitialAttack();
            hasAttacked = true;
          }

          // Processar ataque de língua
          if (tongueAttacking) {
            tongueAttackTimer++;

            // Primeira metade: língua estendendo
            if (tongueAttackTimer < TONGUE_ATTACK_DURATION / 2) {
              tongueLength += TONGUE_SPEED;
              if (tongueLength > MAX_TONGUE_LENGTH) {
                tongueLength = MAX_TONGUE_LENGTH;
              }

              // Verificar colisão com player no pico do ataque
              if (tongueLength >= MAX_TONGUE_LENGTH * 0.8) {
                System.out.println(
                    "🔍 [DEBUG PRÉ-CHECK] Língua atingiu 80%! Length: " + tongueLength + "/" + MAX_TONGUE_LENGTH);
                checkTongueCollision();
              }
            } else {
              // Segunda metade: língua retraindo (somente se não está puxando)
              if (!pullingToPlayer) {
                tongueLength -= TONGUE_SPEED;
                if (tongueLength <= 0) {
                  tongueLength = 0;
                  tongueAttacking = false;
                  state = MimicState.ACTIVE;
                  System.out.println("💥 Mimic completou ataque inicial!");
                }
              }
            }
          }
        }
        break;

      case ACTIVE:
        // Se player está preso, processar grab
        if (playerGrabbed) {
          updateGrab();
        } else if (pullingToPlayer) {
          // Se está puxando pela língua, continuar processando
          updatePulling();
        } else if (tongueAttacking) {
          // Se está atacando com língua, processar ataque
          tongueAttackTimer++;

          // Primeira metade: língua estendendo
          if (tongueAttackTimer < TONGUE_ATTACK_DURATION / 2) {
            tongueLength += TONGUE_SPEED;
            if (tongueLength > MAX_TONGUE_LENGTH) {
              tongueLength = MAX_TONGUE_LENGTH;
            }

            // Verificar colisão com player
            if (tongueLength >= MAX_TONGUE_LENGTH * 0.8) {
              System.out.println(
                  "🔍 [DEBUG PRÉ-CHECK ACTIVE] Língua atingiu 80%! Length: " + tongueLength + "/" + MAX_TONGUE_LENGTH);
              checkTongueCollision();
            }
          } else {
            // Segunda metade: língua retraindo (se não está puxando)
            if (!pullingToPlayer) {
              tongueLength -= TONGUE_SPEED;
              if (tongueLength <= 0) {
                tongueLength = 0;
                tongueAttacking = false;
                System.out.println("💥 Mimic retraiu a língua!");
              }
            }
          }
        } else {
          // Comportamento normal: perseguir e atacar com língua
          if (distanceToPlayer <= detectionRange) {
            moveTowardsPlayer();

            // Ataque de língua quando estiver no alcance
            if (distanceToPlayer <= MAX_TONGUE_LENGTH && attackCooldown <= 0) {
              executeInitialAttack();
              attackCooldown = ATTACK_COOLDOWN_TIME;
            }
          }
        }
        break;
      default:
        break;
    }
  }

  /**
   * Atualiza estado do grab (player preso).
   */
  private void updateGrab() {
    if (target == null) {
      releasePlayer();
      return;
    }

    System.out.println("🔍 [DEBUG] Update Grab - Escape: " + escapeProgress + "/" + ESCAPE_REQUIRED);

    // Aplicar dano periódico
    grabDamageTimer++;
    if (grabDamageTimer >= GRAB_DAMAGE_INTERVAL) {
      target.takeDamage(damage);
      grabDamageTimer = 0;
      System.out.println("💢 Mimic está esmagando o player! Dano: " + damage);
    }

    // Manter player na posição do Mimic
    target.setPosition(x, y);

    // Verificar se player escapou
    if (escapeProgress >= ESCAPE_REQUIRED) {
      releasePlayer();
      System.out.println("✅ Player escapou do Mimic!");
    }
  }

  /**
   * Atualiza o estado de puxar pela língua.
   */
  private void updatePulling() {
    if (target == null) {
      pullingToPlayer = false;
      tongueLength = 0;
      state = MimicState.ACTIVE;
      return;
    }

    // Calcular direção até o player
    double deltaX = pullTargetX - x;
    double deltaY = pullTargetY - y;
    double distanceToPlayer = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    System.out.println("🔍 [DEBUG] Puxando player... Distância: " + distanceToPlayer + " (precisa <= 30)");

    // Se chegou perto do player, ativa o grab
    if (distanceToPlayer <= 30) {
      playerGrabbed = true;
      escapeProgress = 0;
      grabDamageTimer = 0;
      pullingToPlayer = false;
      tongueLength = 0;
      state = MimicState.ACTIVE; // Muda para estado ativo mas com grab ativo

      System.out.println("🔍 [DEBUG] Mimic chegou perto! Ativando GRAB!");

      // Bloquear movimento do player
      if (target instanceof com.rpggame.entities.Player) {
        ((com.rpggame.entities.Player) target).setGrabbed(true);
        System.out.println("🔍 [DEBUG] setGrabbed(true) chamado no Player!");
      } else {
        System.out.println("❌ [DEBUG] Target NÃO é uma instância de Player!");
      }

      System.out.println("👾 Mimic engoliu o player! Aperte SPACE repetidamente para escapar!");
      return;
    }

    // Mover Mimic em direção ao player
    if (distanceToPlayer > 0) {
      double normalizedX = deltaX / distanceToPlayer;
      double normalizedY = deltaY / distanceToPlayer;

      x += normalizedX * PULL_SPEED;
      y += normalizedY * PULL_SPEED;

      // Retrair a língua gradualmente
      tongueLength = Math.max(0, distanceToPlayer);
    }
  }

  /**
   * Libera o player do grab.
   */
  private void releasePlayer() {
    playerGrabbed = false;
    escapeProgress = 0;
    grabDamageTimer = 0;

    // Liberar movimento do player
    if (target instanceof com.rpggame.entities.Player) {
      ((com.rpggame.entities.Player) target).setGrabbed(false);
    }
  }

  /**
   * Processa tentativa de escape (chamado quando player aperta Space).
   */
  public void processEscapeAttempt() {
    if (playerGrabbed) {
      escapeProgress++;
      System.out.println("🔨 Progresso de escape: " + escapeProgress + "/" + ESCAPE_REQUIRED);
    }
  }

  /**
   * Retorna se o player está preso.
   */
  public boolean isPlayerGrabbed() {
    return playerGrabbed;
  }

  /**
   * Executa o ataque inicial do Mimic.
   */
  private void executeInitialAttack() {
    if (target == null) {
      return;
    }

    // Salvar posição do player no momento do ataque
    tongueTargetX = target.getX();
    tongueTargetY = target.getY();

    // Iniciar ataque de língua
    tongueAttacking = true;
    tongueAttackTimer = 0;
    tongueLength = 0;

    System.out.println("👅 Mimic lançou a língua!");
  }

  /**
   * Verifica colisão da língua com o player.
   */
  private void checkTongueCollision() {
    if (target == null || tongueLength < MAX_TONGUE_LENGTH * 0.8) {
      return;
    }

    System.out.println("🔍 [DEBUG] Verificando colisão da língua - Length: " + tongueLength + "/" + MAX_TONGUE_LENGTH);

    // Calcular posição da ponta da língua
    double deltaX = tongueTargetX - x;
    double deltaY = tongueTargetY - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > 0) {
      double normalizedX = deltaX / distance;
      double normalizedY = deltaY / distance;

      double tongueEndX = x + normalizedX * tongueLength;
      double tongueEndY = y + normalizedY * tongueLength;

      // Verificar se a língua chegou perto da posição ALVO (onde o player estava)
      double distToTarget = Math.sqrt(
          Math.pow(tongueTargetX - tongueEndX, 2)
              + Math.pow(tongueTargetY - tongueEndY, 2));

      System.out.println("🔍 [DEBUG] Distância da língua ao alvo: " + distToTarget + " (precisa <= 60)");

      // Se a ponta da língua chegou perto do alvo, verificar se player ainda está lá
      if (distToTarget <= 60) {
        // Verificar se o player ATUAL está perto da posição alvo (hitbox maior)
        double playerDistToTarget = Math.sqrt(
            Math.pow(target.getX() - tongueTargetX, 2)
                + Math.pow(target.getY() - tongueTargetY, 2));

        System.out.println("🔍 [DEBUG] Player atual dist do alvo: " + playerDistToTarget + " (precisa <= 60)");

        if (playerDistToTarget <= 60) {
          // Player não se moveu, Mimic vai se puxar até ele!
          pullingToPlayer = true;
          pullTargetX = target.getX();
          pullTargetY = target.getY();
          tongueAttacking = false; // Para de estender a língua
          System.out.println("🪢 Mimic agarrou o player! Puxando-se pela língua!");
          System.out.println("🔍 [DEBUG] Pull ativado! Target: (" + pullTargetX + ", " + pullTargetY + ")");
        } else {
          // Player se moveu e esquivou!
          System.out.println("✅ Player esquivou da língua do Mimic!");
        }
        tongueLength = MAX_TONGUE_LENGTH; // Parar extensão
      }
    }
  }

  /**
   * Move em direção ao player.
   */
  @Override
  protected void moveTowardsPlayer() {
    if (target == null) {
      return;
    }

    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > 0) {
      dx = (deltaX / distance) * speed;
      dy = (deltaY / distance) * speed;
    }
  }

  /**
   * Ataca o player.
   */
  @Override
  public void attack() {
    attackPlayer();
  }

  /**
   * Ataca o player diretamente.
   */
  private void attackPlayer() {
    if (target != null) {
      target.takeDamage(damage);
      attackCooldown = ATTACK_COOLDOWN_TIME;
      System.out.println("👹 Mimic atacou o player!");
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (!alive) {
      return;
    }

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Escolher sprite baseado no estado
    BufferedImage currentSprite = disguisedSprite;

    switch (state) {
      case DISGUISED:
        currentSprite = disguisedSprite;
        break;

      case REVEALING:
        // Alternar entre baú fechado e Mimic revelado
        currentSprite = (stateTimer / 10) % 2 == 0 ? disguisedSprite : activeSprite;
        break;

      case ATTACKING:
        // Sempre mostrar Mimic revelado durante ataque
        currentSprite = activeSprite;

        // Desenhar aviso de perigo
        if (!tongueAttacking) {
          renderDangerWarning(g, screenX, screenY);
        }
        break;

      case ACTIVE:
        // Usar sprite de Mimic revelado quando ativo
        currentSprite = activeSprite;
        break;
      default:
        break;
    }

    // Renderizar sprite
    if (currentSprite != null) {
      g.drawImage(currentSprite, screenX, screenY, width, height, null);
    } else {
      // Fallback
      g.setColor(Color.YELLOW);
      g.fillRect(screenX, screenY, width, height);
    }

    // Renderizar língua se estiver atacando ou puxando
    if ((tongueAttacking || pullingToPlayer) && tongueLength > 0) {
      renderTongue(g, screenX, screenY);
    }

    // Renderizar barra de vida (apenas quando revelado)
    if (state != MimicState.DISGUISED) {
      renderHealthBar(g, screenX, screenY);
    }

  }

  /**
   * Renderiza a língua do Mimic durante ataque usando gráficos Swing.
   */
  private void renderTongue(Graphics2D g, int screenX, int screenY) {
    // Calcular direção da língua baseado no estado
    double targetX = pullingToPlayer ? pullTargetX : tongueTargetX;
    double targetY = pullingToPlayer ? pullTargetY : tongueTargetY;

    double deltaX = targetX - x;
    double deltaY = targetY - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > 0) {
      double normalizedX = deltaX / distance;
      double normalizedY = deltaY / distance;

      // Centro do Mimic
      int centerX = screenX + width / 2;
      int centerY = screenY + height / 2;

      // Posição da ponta da língua
      int tongueEndX = (int) (centerX + normalizedX * tongueLength);
      int tongueEndY = (int) (centerY + normalizedY * tongueLength);

      // Ativar anti-aliasing para suavizar
      g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

      // Desenhar corpo da língua (gradiente de grossura)
      if (tongueLength > 5) {
        // Calcular pontos para fazer a língua mais grossa na base
        double perpX = -normalizedY; // Perpendicular
        double perpY = normalizedX;

        // Base da língua (mais grossa)
        int baseThickness = 12;
        int tipThickness = 4;

        // Criar polígono para a língua
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        // Lado esquerdo da base
        xPoints[0] = (int) (centerX + perpX * baseThickness);
        yPoints[0] = (int) (centerY + perpY * baseThickness);

        // Lado direito da base
        xPoints[1] = (int) (centerX - perpX * baseThickness);
        yPoints[1] = (int) (centerY - perpY * baseThickness);

        // Lado direito da ponta
        xPoints[2] = (int) (tongueEndX - perpX * tipThickness);
        yPoints[2] = (int) (tongueEndY - perpY * tipThickness);

        // Lado esquerdo da ponta
        xPoints[3] = (int) (tongueEndX + perpX * tipThickness);
        yPoints[3] = (int) (tongueEndY + perpY * tipThickness);

        // Desenhar língua com gradiente de cor
        // Parte interna (rosa mais escuro)
        g.setColor(new Color(255, 100, 150, 200));
        g.fillPolygon(xPoints, yPoints, 4);

        // Borda da língua (mais escura)
        g.setColor(new Color(200, 50, 100, 220));
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawPolygon(xPoints, yPoints, 4);

        // Desenhar ponta da língua (círculo)
        g.setColor(new Color(255, 120, 160, 220));
        g.fillOval(tongueEndX - 6, tongueEndY - 6, 12, 12);

        g.setColor(new Color(200, 50, 100, 240));
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.drawOval(tongueEndX - 6, tongueEndY - 6, 12, 12);
      }

      // Desenhar indicador de alvo (onde a língua está mirando)
      if (tongueLength < MAX_TONGUE_LENGTH * 0.5) {
        int targetScreenX = (int) (tongueTargetX - (x - screenX));
        int targetScreenY = (int) (tongueTargetY - (y - screenY));

        g.setColor(new Color(255, 0, 0, 100));
        g.setStroke(new java.awt.BasicStroke(2, java.awt.BasicStroke.CAP_ROUND,
            java.awt.BasicStroke.JOIN_ROUND, 10, new float[] { 5, 5 }, 0));
        g.drawOval(targetScreenX - 20, targetScreenY - 20, 40, 40);
      }
    }
  }

  /**
   * Renderiza aviso de perigo durante ataque.
   */
  private void renderDangerWarning(Graphics2D g, int screenX, int screenY) {
    // Círculo vermelho pulsante
    float intensity = 1.0f - ((float) stateTimer / ATTACK_WARN_TIME);
    int alpha = (int) (100 + 155 * intensity);

    g.setColor(new Color(255, 0, 0, Math.min(255, alpha)));
    int warningSize = width + 20;
    g.fillOval(
        screenX - 10,
        screenY - 10,
        warningSize,
        warningSize);

    // Texto de aviso
    g.setColor(Color.YELLOW);
    g.setFont(new Font("Arial", Font.BOLD, 14));
    String warning = "CUIDADO!";
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(warning);
    g.drawString(warning, screenX + (width - textWidth) / 2, screenY - 15);
  }

  /**
   * Renderiza barra de vida.
   */
  private void renderHealthBar(Graphics2D g, int screenX, int screenY) {
    int barWidth = width;
    int barHeight = 6;
    int barY = screenY - 10;

    // Fundo da barra
    g.setColor(Color.DARK_GRAY);
    g.fillRect(screenX, barY, barWidth, barHeight);

    // Barra de vida
    double healthPercent = (double) currentHealth / maxHealth;
    int healthWidth = (int) (barWidth * healthPercent);

    g.setColor(healthPercent > 0.5 ? Color.GREEN : healthPercent > 0.25 ? Color.YELLOW : Color.RED);
    g.fillRect(screenX, barY, healthWidth, barHeight);

    // Borda
    g.setColor(Color.BLACK);
    g.drawRect(screenX, barY, barWidth, barHeight);
  }

  /**
   * Verifica se o Mimic está disfarçado.
   */
  public boolean isDisguised() {
    return state == MimicState.DISGUISED;
  }

  /**
   * Retorna o progresso de escape (0-15).
   */
  public int getEscapeProgress() {
    return escapeProgress;
  }

  /**
   * Força revelação (para debug ou outros sistemas).
   */
  public void forceReveal() {
    if (state == MimicState.DISGUISED) {
      state = MimicState.REVEALING;
      stateTimer = REVEAL_TIME;
    }
  }

  /**
   * Override do método die() para garantir que o player seja liberado ao morrer.
   */
  @Override
  protected void die() {
    // Liberar o player se estiver preso
    if (playerGrabbed) {
      releasePlayer();
      System.out.println("💀 Mimic morreu e liberou o player!");
    }

    // Chamar o método da classe pai
    super.die();
  }
}
