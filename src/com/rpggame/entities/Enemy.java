package com.rpggame.entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import com.rpggame.core.GamePanel;
import com.rpggame.world.Camera;
import com.rpggame.world.TileMap;
import com.rpggame.systems.EnemyManager;

/**
 * Classe base para todos os inimigos do jogo
 */
public abstract class Enemy {
  protected double x, y;
  protected double dx, dy;
  protected double speed;
  protected int width, height;

  // Atributos do inimigo
  protected int maxHealth;
  protected int currentHealth;
  protected int damage;
  protected int experienceReward;

  // Sprite e animação
  protected BufferedImage sprite;
  protected String spritePath;

  // Estado do inimigo
  protected boolean alive;
  protected boolean aggressive;
  protected double detectionRange;
  protected double attackRange;

  // Estado de congelamento
  protected boolean frozen;
  protected int freezeTimer;

  // Estado de medo (fear)
  protected boolean feared;
  protected int fearTimer;
  protected double fearDirectionX;
  protected double fearDirectionY;

  // Estado de encantamento (charm)
  protected boolean charmed;
  protected int charmTimer;

  // Estado de atordoamento (stun)
  protected boolean stunned;
  protected int stunTimer;

  // Timer para ataques
  protected int attackCooldown;
  protected final int ATTACK_COOLDOWN_TIME = 60; // frames

  // Referência para o jogador (para IA)
  protected Player target;

  // Referência para o mapa (para verificação de colisão)
  protected TileMap tileMap;

  // Referência para o EnemyManager (para atacar aliados quando encantado)
  protected EnemyManager enemyManager;

  // Alvo encantado (outro inimigo para atacar)
  protected Enemy charmedTarget;

  /**
   * Construtor da classe Enemy
   */
  public Enemy(double x, double y, String spritePath) {
    this.x = x;
    this.y = y;
    this.spritePath = spritePath;
    this.alive = true;
    this.aggressive = false;
    this.attackCooldown = 0;
    this.frozen = false;
    this.freezeTimer = 0;
    this.feared = false;
    this.fearTimer = 0;
    this.fearDirectionX = 0;
    this.fearDirectionY = 0;
    this.charmed = false;
    this.charmTimer = 0;
    this.stunned = false;
    this.stunTimer = 0;

    loadSprite();
    initializeStats();
  }

  /**
   * Carrega o sprite do inimigo
   */
  private void loadSprite() {
    System.out.println("Tentando carregar sprite: " + spritePath);

    boolean loaded = false;

    try {
      // Tentar carregar como recurso do classpath (funciona no JAR)
      InputStream is = getClass().getClassLoader().getResourceAsStream(spritePath);
      if (is != null) {
        sprite = ImageIO.read(is);
        is.close();
        if (sprite != null) {
          width = sprite.getWidth();
          height = sprite.getHeight();
          loaded = true;
          System.out.println("✅ Sprite carregado do JAR: " + spritePath);
        }
      } else {
        // Fallback: tentar carregar como arquivo externo (desenvolvimento)
        String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(spritePath);
        File spriteFile = new File(resolvedPath);

        if (spriteFile.exists()) {
          sprite = ImageIO.read(spriteFile);
          if (sprite != null) {
            width = sprite.getWidth();
            height = sprite.getHeight();
            loaded = true;
            System.out.println("✅ Sprite carregado do arquivo: " + resolvedPath);
          }
        }
      }
    } catch (IOException e) {
      System.out.println("❌ Falha ao carregar sprite: " + e.getMessage());
    }

    if (!loaded) {
      System.err.println("ERRO: Não foi possível carregar sprite do inimigo!");
      System.err.println("Sprite solicitado: " + spritePath);
      createDefaultSprite();
    }
  }

  /**
   * Cria um sprite padrão para o inimigo
   */
  private void createDefaultSprite() {
    width = 48;
    height = 48;
    sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = sprite.createGraphics();

    // Fundo vermelho para inimigo
    g.setColor(Color.RED);
    g.fillRect(0, 0, width, height);

    // Borda preta
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(2));
    g.drawRect(1, 1, width - 3, height - 3);

    // Desenhar um "G" para Goblin
    g.setColor(Color.WHITE);
    g.setFont(new Font("Arial", Font.BOLD, 20));
    FontMetrics fm = g.getFontMetrics();
    int textX = (width - fm.stringWidth("G")) / 2;
    int textY = (height + fm.getAscent()) / 2 - 2;
    g.drawString("G", textX, textY);

    g.dispose();

    System.out.println("Sprite padrão criado para inimigo: " + width + "x" + height + "px");
  }

  /**
   * Inicializa as estatísticas específicas do inimigo
   * Deve ser implementado pelas subclasses
   */
  protected abstract void initializeStats();

  /**
   * Atualiza a lógica do inimigo
   */
  public void update(Player player) {
    if (!alive)
      return;

    this.target = player;

    // Atualizar estado de congelamento
    if (frozen) {
      freezeTimer--;
      if (freezeTimer <= 0) {
        frozen = false;
        System.out.println("❄️ Inimigo descongelado!");
      }
      return; // Não fazer nada enquanto congelado
    }

    // Atualizar estado de encantamento (charm)
    if (charmed) {
      charmTimer--;
      if (charmTimer <= 0) {
        charmed = false;
        charmedTarget = null;
        System.out.println("💜 Encantamento dissipado!");
      } else {
        // Debug: mostrar que está encantado
        if (charmTimer % 60 == 0) { // A cada segundo
          System.out.println("💜 Inimigo está encantado! Tempo restante: " + (charmTimer / 60.0) + "s");
        }
      }
    }

    // Atualizar estado de atordoamento (stun)
    if (stunned) {
      stunTimer--;
      if (stunTimer <= 0) {
        stunned = false;
        System.out.println("💥 Inimigo recuperou do atordoamento!");
      }
      return; // Não fazer nada enquanto atordoado
    }

    // Atualizar estado de medo (fear)
    if (feared) {
      fearTimer--;
      if (fearTimer <= 0) {
        feared = false;
        dx = 0;
        dy = 0;
        System.out.println("💢 Inimigo recuperou coragem!");
      } else {
        // Continuar fugindo na direção definida
        dx = fearDirectionX;
        dy = fearDirectionY;
      }
    }

    // Atualizar cooldown de ataque
    if (attackCooldown > 0) {
      attackCooldown--;
    }

    // IA básica (sempre executar se encantado, ou se não estiver com medo)
    if (charmed || !feared) {
      if (charmed && charmTimer % 60 == 0) {
        System.out.println("💜 Chamando updateAI() para inimigo encantado...");
      }
      updateAI();
    } else {
      if (charmTimer % 60 == 0 && charmed) {
        System.out.println("⚠️ Inimigo encantado mas com medo! IA não executada.");
      }
    }

    // Atualizar posição com verificação de colisão
    updatePosition();
  }

  /**
   * IA básica do inimigo
   */
  protected void updateAI() {
    // Debug geral
    if (charmed && charmTimer % 60 == 0) {
      System.out.println("💜 updateAI() chamado! charmed=" + charmed + ", feared=" + feared);
    }

    // Se estiver encantado, atacar outros inimigos (VERIFICAR ANTES DO TARGET!)
    if (charmed) {
      System.out.println("💜 updateAI() detectou charmed=true, chamando updateCharmedAI()");
      updateCharmedAI();
      return;
    }

    // Apenas verificar target se NÃO estiver encantado
    if (target == null)
      return;

    double distanceToPlayer = getDistanceToPlayer();

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
    }
  }

  /**
   * IA quando encantado - atacar outros inimigos
   */
  protected void updateCharmedAI() {
    if (enemyManager == null) {
      System.out.println("⚠️ EnemyManager é null! Não pode procurar alvos.");
      return;
    }

    // Procurar inimigo mais próximo para atacar
    Enemy nearestEnemy = null;
    double nearestDistance = Double.MAX_VALUE;

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    System.out.println("💜 Procurando alvos... Total de inimigos: " + enemies.size());

    for (Enemy enemy : enemies) {
      if (enemy == this || !enemy.isAlive())
        continue;

      double dx = enemy.getX() - this.x;
      double dy = enemy.getY() - this.y;
      double distance = Math.sqrt(dx * dx + dy * dy);

      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearestEnemy = enemy;
      }
    }

    if (nearestEnemy != null) {
      charmedTarget = nearestEnemy;
      System.out.println("💜 Alvo encontrado! Distância: " + nearestDistance);

      // Mover em direção ao inimigo
      if (nearestDistance > attackRange) {
        double deltaX = nearestEnemy.getX() - x;
        double deltaY = nearestEnemy.getY() - y;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance > 0) {
          this.dx = (deltaX / distance) * speed;
          this.dy = (deltaY / distance) * speed;
          System.out.println("💜 Movendo em direção ao alvo... dx=" + this.dx + ", dy=" + this.dy);
        }
      } else {
        // Atacar o inimigo
        System.out.println("💜 Alcance de ataque! Atacando...");
        attackCharmedTarget();
      }
    } else {
      System.out.println("⚠️ Nenhum alvo encontrado!");
    }
  }

  /**
   * Ataca outro inimigo quando encantado
   */
  protected void attackCharmedTarget() {
    if (charmedTarget == null || !charmedTarget.isAlive())
      return;

    if (attackCooldown <= 0) {
      charmedTarget.takeDamageFromCharm(damage);
      attackCooldown = ATTACK_COOLDOWN_TIME;
      System.out.println("💜 Inimigo encantado atacou aliado causando " + damage + " de dano!");
    }

    dx = 0;
    dy = 0;
  }

  /**
   * Move o inimigo em direção ao jogador
   */
  protected void moveTowardsPlayer() {
    if (target == null)
      return;

    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > 0) {
      dx = (deltaX / distance) * speed;
      dy = (deltaY / distance) * speed;
    }
  }

  /**
   * Tenta atacar o jogador
   */
  protected void attemptAttack() {
    if (attackCooldown <= 0) {
      attack();
      attackCooldown = ATTACK_COOLDOWN_TIME;
    }

    // Parar movimento durante ataque
    dx = 0;
    dy = 0;
  }

  /**
   * Executa o ataque do inimigo
   * Deve ser implementado pelas subclasses
   */
  protected abstract void attack();

  /**
   * Calcula a distância até o jogador
   */
  private double getDistanceToPlayer() {
    if (target == null)
      return Double.MAX_VALUE;

    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
  }

  /**
   * Recebe dano
   */
  public void takeDamage(int damage) {
    // Não recebe dano enquanto congelado
    if (frozen) {
      System.out.println("❄️ Inimigo congelado não recebe dano!");
      return;
    }

    currentHealth -= damage;
    if (currentHealth <= 0) {
      die();
    }

    // Ficar agressivo quando receber dano
    aggressive = true;
  }

  /**
   * Recebe dano de NPCs (guardas) sem dar XP ao player
   */
  public void takeDamageFromNPC(int damage) {
    // Não recebe dano enquanto congelado
    if (frozen) {
      System.out.println("❄️ Inimigo congelado não recebe dano!");
      return;
    }

    currentHealth -= damage;
    if (currentHealth <= 0) {
      dieWithoutXP();
    }

    // Ficar agressivo quando receber dano
    aggressive = true;
  }

  /**
   * Recebe dano de outro inimigo encantado (não dá XP)
   */
  public void takeDamageFromCharm(int damage) {
    currentHealth -= damage;
    if (currentHealth <= 0) {
      dieWithoutXP();
    }
  }

  /**
   * Morre sem conceder experiência (morto por NPC)
   */
  protected void dieWithoutXP() {
    alive = false;
    System.out.println("Inimigo foi derrotado por um NPC!");
  }

  /**
   * Morre e concede experiência
   */
  protected void die() {
    alive = false;

    // Dar experiência ao jogador
    if (target != null) {
      target.gainExperience(experienceReward);
    }

    System.out.println("Inimigo morreu! XP: " + experienceReward);
  }

  /**
   * Renderiza o inimigo na tela
   */
  public void render(Graphics2D g, Camera camera) {
    if (!alive)
      return;

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Desenhar sprite
    if (sprite != null) {
      g.drawImage(sprite, screenX, screenY, null);

      // Se congelado, adicionar overlay azul
      if (frozen) {
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.setColor(new Color(150, 220, 255));
        g.fillRect(screenX, screenY, width, height);

        // Desenhar cristais de gelo
        g.setColor(new Color(200, 240, 255));
        g.setStroke(new BasicStroke(2));
        g.drawLine(screenX + width / 2, screenY, screenX + width / 2, screenY + height);
        g.drawLine(screenX, screenY + height / 2, screenX + width, screenY + height / 2);
        g.drawLine(screenX + width / 4, screenY + height / 4, screenX + 3 * width / 4, screenY + 3 * height / 4);
        g.drawLine(screenX + 3 * width / 4, screenY + height / 4, screenX + width / 4, screenY + 3 * height / 4);

        g.setComposite(oldComposite);
        g.setStroke(new BasicStroke(1));
      }

      // Se com medo, adicionar overlay amarelo pulsante
      if (feared) {
        Composite oldComposite = g.getComposite();
        float pulse = 0.3f + 0.2f * (float) Math.sin(fearTimer * 0.2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
        g.setColor(new Color(255, 255, 100));
        g.fillRect(screenX, screenY, width, height);

        // Desenhar símbolo de exclamação
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.setColor(new Color(255, 200, 0));
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("!", screenX + width / 2 - 4, screenY - 5);

        g.setComposite(oldComposite);
      }

      // Se encantado, adicionar overlay roxo brilhante
      if (charmed) {
        Composite oldComposite = g.getComposite();
        float pulse = 0.4f + 0.3f * (float) Math.sin(charmTimer * 0.15);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
        g.setColor(new Color(200, 100, 255));
        g.fillRect(screenX, screenY, width, height);

        // Desenhar símbolo de coração
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setColor(new Color(220, 150, 255));
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("♥", screenX + width / 2 - 5, screenY - 5);

        g.setComposite(oldComposite);
      }

      // Se atordoado, adicionar overlay amarelo escuro com estrelas
      if (stunned) {
        Composite oldComposite = g.getComposite();
        float pulse = 0.3f + 0.2f * (float) Math.sin(stunTimer * 0.3);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
        g.setColor(new Color(255, 200, 0));
        g.fillRect(screenX, screenY, width, height);

        // Desenhar estrelas girando
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setColor(new Color(255, 255, 100));
        g.setFont(new Font("Arial", Font.BOLD, 16));

        // Três estrelas girando ao redor da cabeça
        for (int i = 0; i < 3; i++) {
          double angle = (System.currentTimeMillis() * 0.003 + i * Math.PI * 2 / 3);
          int starX = screenX + width / 2 + (int) (Math.cos(angle) * 20) - 4;
          int starY = screenY - 10 + (int) (Math.sin(angle) * 15);
          g.drawString("★", starX, starY);
        }

        g.setComposite(oldComposite);
      }
    }

    // Desenhar barra de vida
    drawHealthBar(g, screenX, screenY);
  }

  /**
   * Desenha a barra de vida do inimigo
   */
  private void drawHealthBar(Graphics2D g, int screenX, int screenY) {
    int barWidth = width;
    int barHeight = 4;
    int barY = screenY - 8;

    // Fundo da barra (vermelho)
    g.setColor(Color.RED);
    g.fillRect(screenX, barY, barWidth, barHeight);

    // Vida atual (verde)
    g.setColor(Color.GREEN);
    int healthWidth = (int) ((double) currentHealth / maxHealth * barWidth);
    g.fillRect(screenX, barY, healthWidth, barHeight);

    // Borda da barra
    g.setColor(Color.WHITE);
    g.drawRect(screenX, barY, barWidth, barHeight);
  }

  // Getters
  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isAlive() {
    return alive;
  }

  public int getDamage() {
    return damage;
  }

  public int getExperienceReward() {
    return experienceReward;
  }

  /**
   * Define o mapa de tiles para verificação de colisão
   */
  public void setTileMap(TileMap tileMap) {
    this.tileMap = tileMap;
  }

  /**
   * Define o EnemyManager (para atacar aliados quando encantado)
   */
  public void setEnemyManager(EnemyManager manager) {
    this.enemyManager = manager;
  }

  /**
   * Aplica estado de medo (fear) ao inimigo
   */
  public void applyFear(double directionX, double directionY, int duration, double fleeSpeed) {
    this.feared = true;
    this.fearTimer = duration;
    this.fearDirectionX = directionX * fleeSpeed;
    this.fearDirectionY = directionY * fleeSpeed;
    System.out.println("💢 Inimigo com medo! Fugindo por " + (duration / 60.0) + " segundos");
  }

  /**
   * Aplica estado de encantamento (charm) ao inimigo
   */
  public void applyCharm(int duration) {
    this.charmed = true;
    this.charmTimer = duration;
    System.out.println("💜 Inimigo encantado! Atacará seus aliados por " + (duration / 60.0) + " segundos");
  }

  /**
   * Verifica se o inimigo está encantado
   */
  public boolean isCharmed() {
    return charmed;
  }

  /**
   * Aplica estado de atordoamento (stun) ao inimigo
   */
  public void applyStun(int duration) {
    this.stunned = true;
    this.stunTimer = duration;
    System.out.println("💥 Inimigo atordoado por " + (duration / 60.0) + " segundos!");
  }

  /**
   * Verifica se o inimigo está atordoado
   */
  public boolean isStunned() {
    return stunned;
  }

  /**
   * Atualiza a posição do inimigo com verificação de colisão
   */
  private void updatePosition() {
    // Verificar se é um Goblin em spawn safety
    boolean skipCollision = false;
    if (this instanceof Goblin) {
      Goblin goblin = (Goblin) this;
      skipCollision = goblin.isInSpawnSafety();
    }

    if (tileMap == null || skipCollision) {
      // Se não há mapa ou está em spawn safety, mover sem restrições
      x += dx;
      y += dy;
      return;
    }

    // Testar movimento horizontal
    double newX = x + dx;
    if (isValidPosition(newX, y)) {
      x = newX;
    } else {
      dx = 0; // Parar movimento horizontal se colidir
    }

    // Testar movimento vertical
    double newY = y + dy;
    if (isValidPosition(x, newY)) {
      y = newY;
    } else {
      dy = 0; // Parar movimento vertical se colidir
    }
  }

  /**
   * Verifica se uma posição é válida (não colide com obstáculos)
   */
  private boolean isValidPosition(double newX, double newY) {
    if (tileMap == null)
      return true;

    // Verificar os 4 cantos do inimigo
    int leftTile = (int) (newX / GamePanel.TILE_SIZE);
    int rightTile = (int) ((newX + width - 1) / GamePanel.TILE_SIZE);
    int topTile = (int) (newY / GamePanel.TILE_SIZE);
    int bottomTile = (int) ((newY + height - 1) / GamePanel.TILE_SIZE);

    // Verificar se todos os cantos estão em tiles válidos (grama)
    return tileMap.isWalkable(leftTile, topTile) &&
        tileMap.isWalkable(rightTile, topTile) &&
        tileMap.isWalkable(leftTile, bottomTile) &&
        tileMap.isWalkable(rightTile, bottomTile);
  }

  /**
   * Verifica colisão com retângulo
   */
  public Rectangle getBounds() {
    return new Rectangle((int) x, (int) y, width, height);
  }
}