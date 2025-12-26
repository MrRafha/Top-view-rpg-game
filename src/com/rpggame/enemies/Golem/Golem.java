package com.rpggame.enemies.Golem;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import com.rpggame.entities.Enemy;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.core.GamePanel;

/**
 * Boss Golem - Guardião do Equilíbrio do Ecossistema
 * Aparece quando todas as famílias goblin são derrotadas
 */
public class Golem extends Enemy {

  // Sprites direcionais
  private BufferedImage spriteFront;
  private BufferedImage spriteBack;
  private BufferedImage spriteLeft;
  private BufferedImage spriteRight;
  private BufferedImage currentSprite;

  // Direção que o Golem está olhando
  private String facingDirection = "front"; // "front", "back", "left", "right"

  // Sistema de ataque
  private static final int ATTACK_WINDUP_TIME = 90; // 1.5 segundos de preparação
  private static final int ATTACK_RECOVERY_TIME = 180; // 3 segundos de cooldown
  private boolean preparingAttack = false;
  private int attackWindupTimer = 0;
  private double targetTileX = 0;
  private double targetTileY = 0;

  // Projéteis de pedra
  private java.util.List<GolemStone> activeStones;

  // Resistência a dano
  private static final double DAMAGE_RESISTANCE = 0.5; // 50% de resistência

  // Tempo de vida do boss
  private int aliveTime = 0;
  private static final int ENRAGE_TIME = 3600; // 1 minuto até enrage (60fps * 60s)
  private boolean enraged = false;

  /**
   * Construtor do Golem
   */
  public Golem(double x, double y) {
    super(x, y, "sprites/GOLEMFront.png");
    this.activeStones = new java.util.ArrayList<>();
    loadDirectionalSprites();
    this.currentSprite = spriteFront;
  }

  @Override
  protected void initializeStats() {
    this.maxHealth = 500;
    this.currentHealth = maxHealth;
    this.damage = 30;
    this.speed = 0.8; // Lento mas poderoso
    this.experienceReward = 500;
    this.width = 64;
    this.height = 64;
    this.detectionRange = 200.0;
    this.attackRange = 250.0; // Ataque de longo alcance
  }

  /**
   * Carrega todos os sprites direcionais do Golem
   */
  private void loadDirectionalSprites() {
    int loadedCount = 0;

    // Usar o mesmo método que a classe Enemy usa
    spriteFront = loadSpriteFile("sprites/GOLEMFront.png");
    if (spriteFront != null)
      loadedCount++;

    spriteBack = loadSpriteFile("sprites/GOLEMBack.png");
    if (spriteBack != null)
      loadedCount++;

    spriteLeft = loadSpriteFile("sprites/GOLEMLeft.png");
    if (spriteLeft != null)
      loadedCount++;

    spriteRight = loadSpriteFile("sprites/GOLEMRight.png");
    if (spriteRight != null)
      loadedCount++;

    if (loadedCount == 4) {
      System.out.println("✅ Todos os sprites direcionais do Golem carregados (" + loadedCount + "/4)");
    } else {
      System.err.println("⚠️ Apenas " + loadedCount + "/4 sprites do Golem foram carregados");
    }
  }

  /**
   * Carrega um arquivo de sprite (igual ao método da classe Enemy)
   */
  private BufferedImage loadSpriteFile(String path) {
    try {
      // Tentar carregar como recurso do classpath
      InputStream is = getClass().getClassLoader().getResourceAsStream(path);
      if (is != null) {
        BufferedImage img = ImageIO.read(is);
        is.close();
        if (img != null) {
          System.out.println("✅ " + path + " carregado do JAR");
          return img;
        }
      }

      // Fallback: tentar carregar como arquivo externo
      String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(path);
      java.io.File file = new java.io.File(resolvedPath);
      if (file.exists()) {
        BufferedImage img = ImageIO.read(file);
        if (img != null) {
          System.out.println("✅ " + path + " carregado do arquivo");
          return img;
        }
      }
    } catch (IOException e) {
      System.err.println("❌ Erro ao carregar " + path + ": " + e.getMessage());
    }

    System.err.println("❌ Não foi possível carregar: " + path);
    return null;
  }

  @Override
  public void takeDamage(int damage) {
    if (!alive)
      return;

    // Aplicar resistência de 50%
    int reducedDamage = (int) (damage * DAMAGE_RESISTANCE);
    currentHealth -= reducedDamage;

    System.out.println("🗿 Golem recebeu " + reducedDamage + " de dano (resistiu " + (damage - reducedDamage)
        + ") - HP: " + currentHealth + "/" + maxHealth);

    if (currentHealth <= 0) {
      alive = false;
      System.out.println("💀 O Golem, guardião do equilíbrio, foi derrotado!");
    }

    // Enrage quando chega em 30% de vida
    if (!enraged && currentHealth <= maxHealth * 0.3) {
      enrage();
    }
  }

  /**
   * Ativa modo enrage - ataques mais rápidos e agressivos
   */
  private void enrage() {
    enraged = true;
    speed *= 1.3;
    System.out.println("🔥 O GOLEM ENFURECEU! Ele está mais rápido e agressivo!");
  }

  @Override
  protected void updateAI() {
    aliveTime++;

    // Enrage por tempo se passar de 1 minuto
    if (!enraged && aliveTime >= ENRAGE_TIME) {
      enrage();
    }

    // Atualizar direção baseada no movimento
    updateFacingDirection();

    if (target == null)
      return;

    double distanceToPlayer = Math.sqrt(
        Math.pow(target.getX() - x, 2) +
            Math.pow(target.getY() - y, 2));

    // Se está preparando ataque, não se move
    if (preparingAttack) {
      attackWindupTimer--;
      if (attackWindupTimer <= 0) {
        executeStoneThrow();
        preparingAttack = false;
        attackCooldown = enraged ? ATTACK_RECOVERY_TIME / 2 : ATTACK_RECOVERY_TIME;
      }
      return;
    }

    // Manter distância do player (kiting)
    if (distanceToPlayer < 100) {
      // Muito perto, recuar
      moveAwayFromPlayer();
    } else if (distanceToPlayer > 150) {
      // Muito longe, aproximar
      moveTowardsPlayer();
    }

    // Atacar se estiver no alcance e cooldown acabou
    if (distanceToPlayer <= attackRange && attackCooldown <= 0) {
      prepareStoneThrow();
    }
  }

  /**
   * Prepara o ataque de pedra
   */
  private void prepareStoneThrow() {
    preparingAttack = true;
    attackWindupTimer = enraged ? ATTACK_WINDUP_TIME / 2 : ATTACK_WINDUP_TIME;

    // Prever posição do jogador
    if (target != null) {
      targetTileX = target.getX();
      targetTileY = target.getY();
    }

    System.out.println("🗿 Golem está preparando para lançar uma pedra!");
  }

  /**
   * Executa o lançamento da pedra
   */
  private void executeStoneThrow() {
    if (target == null)
      return;

    // Criar projétil de pedra
    GolemStone stone = new GolemStone(
        x + width / 2,
        y + height / 2,
        targetTileX,
        targetTileY,
        damage,
        (Player) target);

    activeStones.add(stone);

    System.out.println("💎 Golem lançou uma pedra!");
  }

  /**
   * Move para longe do player
   */
  private void moveAwayFromPlayer() {
    if (target == null)
      return;

    double deltaX = x - target.getX();
    double deltaY = y - target.getY();
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > 0) {
      dx = (deltaX / distance) * speed;
      dy = (deltaY / distance) * speed;
    }
  }

  /**
   * Atualiza a direção que o Golem está olhando
   */
  private void updateFacingDirection() {
    if (target == null)
      return;

    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;

    // Determinar direção baseada no maior delta
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      if (deltaX > 0) {
        facingDirection = "right";
        currentSprite = spriteRight;
      } else {
        facingDirection = "left";
        currentSprite = spriteLeft;
      }
    } else {
      if (deltaY > 0) {
        facingDirection = "front";
        currentSprite = spriteFront;
      } else {
        facingDirection = "back";
        currentSprite = spriteBack;
      }
    }
  }

  @Override
  public void update(Player player) {
    super.update(player);

    // Atualizar pedras ativas
    java.util.Iterator<GolemStone> iterator = activeStones.iterator();
    while (iterator.hasNext()) {
      GolemStone stone = iterator.next();
      stone.update();

      if (!stone.isActive()) {
        iterator.remove();
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (!alive)
      return;

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Renderizar sprite atual
    if (currentSprite != null) {
      g.drawImage(currentSprite, screenX, screenY, width, height, null);
    } else {
      // Fallback - retângulo cinza
      g.setColor(Color.GRAY);
      g.fillRect(screenX, screenY, width, height);
    }

    // Renderizar indicador de preparação de ataque
    if (preparingAttack) {
      renderAttackWarning(g, camera);
    }

    // Renderizar pedras ativas (usar cópia para evitar
    // ConcurrentModificationException)
    for (GolemStone stone : new java.util.ArrayList<>(activeStones)) {
      stone.render(g, camera);
    }

    // Renderizar barra de vida
    renderHealthBar(g, screenX, screenY);

    // Renderizar indicador de enrage
    if (enraged) {
      g.setColor(new Color(255, 0, 0, 150));
      g.drawOval(screenX - 10, screenY - 10, width + 20, height + 20);
      g.setFont(new Font("Arial", Font.BOLD, 12));
      g.setColor(Color.RED);
      g.drawString("ENFURECIDO!", screenX, screenY - 15);
    }
  }

  /**
   * Renderiza o aviso visual de onde a pedra vai cair
   */
  private void renderAttackWarning(Graphics2D g, Camera camera) {
    int targetScreenX = (int) (targetTileX - camera.getX());
    int targetScreenY = (int) (targetTileY - camera.getY());

    // Intensidade do aviso aumenta conforme o tempo passa
    float intensity = 1.0f - ((float) attackWindupTimer / ATTACK_WINDUP_TIME);
    int alpha = (int) (100 + 155 * intensity);

    // Área de impacto 2x2 tiles
    int tileSize = GamePanel.TILE_SIZE;
    int areaSize = tileSize * 2;

    // Desenhar área de perigo
    g.setColor(new Color(255, 0, 0, Math.min(255, alpha)));
    g.fillRect(
        targetScreenX - areaSize / 2,
        targetScreenY - areaSize / 2,
        areaSize,
        areaSize);

    // Desenhar borda piscante
    g.setColor(new Color(255, 255, 0, Math.min(255, alpha + 50)));
    g.setStroke(new BasicStroke(3));
    g.drawRect(
        targetScreenX - areaSize / 2,
        targetScreenY - areaSize / 2,
        areaSize,
        areaSize);

    // Desenhar símbolo de alerta
    g.setFont(new Font("Arial", Font.BOLD, 24));
    g.setColor(new Color(255, 255, 0, Math.min(255, alpha + 100)));
    String warning = "!";
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(warning);
    g.drawString(warning, targetScreenX - textWidth / 2, targetScreenY + 8);
  }

  /**
   * Renderiza a barra de vida do boss
   */
  private void renderHealthBar(Graphics2D g, int screenX, int screenY) {
    int barWidth = 80;
    int barHeight = 8;
    int barX = screenX + (width - barWidth) / 2;
    int barY = screenY - 15;

    // Fundo da barra
    g.setColor(Color.BLACK);
    g.fillRect(barX, barY, barWidth, barHeight);

    // Barra de vida
    int healthWidth = (int) ((double) currentHealth / maxHealth * barWidth);
    Color healthColor = enraged ? Color.RED : Color.GREEN;
    if (!enraged && currentHealth < maxHealth * 0.5) {
      healthColor = Color.YELLOW;
    }

    g.setColor(healthColor);
    g.fillRect(barX, barY, healthWidth, barHeight);

    // Borda
    g.setColor(Color.WHITE);
    g.drawRect(barX, barY, barWidth, barHeight);

    // Texto de HP
    g.setFont(new Font("Arial", Font.BOLD, 10));
    g.setColor(Color.WHITE);
    String hpText = currentHealth + "/" + maxHealth;
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(hpText);
    g.drawString(hpText, barX + (barWidth - textWidth) / 2, barY - 2);

    // Nome do boss
    g.setFont(new Font("Arial", Font.BOLD, 12));
    g.setColor(new Color(255, 215, 0)); // Dourado
    String bossName = "GOLEM - Guardião do Equilíbrio";
    textWidth = fm.stringWidth(bossName);
    g.drawString(bossName, screenX + (width - textWidth) / 2, screenY - 25);
  }

  @Override
  protected void attack() {
    // Ataque é gerenciado pelo sistema de pedras
  }

  /**
   * Retorna as pedras ativas (para verificação de colisão)
   */
  public java.util.List<GolemStone> getActiveStones() {
    return activeStones;
  }

  @Override
  protected String getEnemyType() {
    return "Golem";
  }

  /**
   * Verifica se o Golem está preparando ataque
   */
  public boolean isPreparingAttack() {
    return preparingAttack;
  }
}
