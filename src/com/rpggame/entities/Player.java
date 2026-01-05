package com.rpggame.entities;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import com.rpggame.world.*;
import com.rpggame.systems.*;
import com.rpggame.core.GamePanel;
import com.rpggame.core.Game;
import com.rpggame.items.Inventory;
import com.rpggame.items.consumables.HealthPotion;
import com.rpggame.items.consumables.ManaPotion;
import java.io.IOException;

/**
 * Classe que representa o jogador
 */
public class Player {
  private double x, y;
  private double dx, dy;
  private double speed;

  // Sistema de sprites para animação
  private BufferedImage currentSprite;
  private BufferedImage spriteRight1, spriteRight2;
  private BufferedImage spriteLeft1, spriteLeft2;

  // Controle de animação
  private int animationFrame = 0;
  // private int animationTimer = 0; // TODO: Implementar animação
  // private final int ANIMATION_SPEED = 15; // TODO: frames por troca de sprite
  private boolean facingLeft = false; // direção que o player está olhando
  private boolean isMoving = false;

  private final int WIDTH = 33; // Tamanho visual do sprite
  private final int HEIGHT = 48; // Tamanho visual do sprite

  // Hitbox menor para colisão (permite passar por corredores de 1 tile)
  private final int HITBOX_WIDTH = 28; // ~60% da largura do tile (48px)
  private final int HITBOX_HEIGHT = 40; // ~83% da altura do tile
  private final int HITBOX_OFFSET_X = (WIDTH - HITBOX_WIDTH) / 2; // Centralizar hitbox
  private final int HITBOX_OFFSET_Y = HEIGHT - HITBOX_HEIGHT; // Hitbox na parte inferior

  // Controles
  private boolean up, down, left, right;
  private boolean spacePressed = false;
  private boolean canAttack = true;
  private int attackCooldown = 0;
  private final int ATTACK_COOLDOWN_TIME = 30; // frames

  // Atributos do personagem
  private String playerClass;
  private CharacterStats stats;
  private int currentHealth;
  private int maxHealth;
  private int currentMana;
  private int maxMana;
  private int manaRegenTimer = 0; // Timer para regeneração de mana (60 frames = 1 segundo)
  private ExperienceSystem experienceSystem;
  private int lastLevelCheck; // Para detectar level ups

  // Controle de desbloqueio de habilidades
  private boolean skill2Unlocked = false;
  private boolean skill3Unlocked = false;
  private boolean skill4Unlocked = false;
  private int pendingSkillUnlock = 0; // 0 = nenhum, 2/3/4 = slot a desbloquear

  // Direção atual (para ataques direcionais)
  private double facing = 0; // 0 = direita, PI/2 = baixo, PI = esquerda, 3*PI/2 = cima

  // Lista de projéteis do jogador
  private ArrayList<Projectile> projectiles;

  // Lista de textos flutuantes
  private ArrayList<FloatingText> floatingTexts;

  // Referência para o mapa (para verificação de colisão)
  private TileMap tileMap;

  // Referência para o gerenciador de inimigos (para atacar estruturas)
  private EnemyManager enemyManager;

  // Controle de movimento durante diálogos
  private boolean inDialog = false;

  // Controle de movimento quando preso por inimigo
  private boolean grabbed = false;

  // Estado de Fúria Berserk
  private boolean berserkActive = false;

  // Gerenciador de habilidades
  private SkillManager skillManager;

  // Inventário do jogador
  private Inventory inventory;

  // Sistema de equipamento
  private com.rpggame.items.EquippableItem equippedWeapon;

  // Sistema de moedas
  private int gold = 0;
  private boolean showGoldUI = false;
  private int goldUITimer = 0;
  private static final int GOLD_UI_DISPLAY_TIME = 300; // 5 segundos em frames (60 fps)

  // Sistema de quests
  private QuestManager questManager;

  // Noclip mode (para debug/cheat)
  private boolean noclipEnabled = false;

  // Sistema de atordoamento
  private boolean stunned = false;
  private int stunTimer = 0;

  public Player(double x, double y, String spritePath) {
    this.x = x;
    this.y = y;
    this.speed = 4.0; // Aumentado de 3.0 para 4.0 para compensar o tamanho maior
    this.playerClass = "Warrior";
    this.stats = new CharacterStats("Warrior");
    this.maxHealth = stats.getMaxHealth();
    this.currentHealth = maxHealth;
    this.maxMana = stats.getMaxMana();
    this.currentMana = maxMana;
    this.experienceSystem = new ExperienceSystem();
    this.lastLevelCheck = 1; // Começar no nível 1
    this.projectiles = new ArrayList<>();
    this.floatingTexts = new ArrayList<>();
    this.skillManager = new SkillManager(this);
    this.inventory = new Inventory(); // Inicializa inventário com 20 slots
    this.questManager = new QuestManager(); // Inicializa gerenciador de quests
    loadSprite(spritePath);
    initializeStartingItems(); // Adiciona itens iniciais
  }

  public Player(double x, double y, String spritePath, String playerClass, CharacterStats stats) {
    this.x = x;
    this.y = y;
    this.playerClass = playerClass;
    this.stats = stats;
    this.maxHealth = stats.getMaxHealth();
    this.currentHealth = maxHealth;
    this.maxMana = stats.getMaxMana();
    this.currentMana = maxMana;
    this.experienceSystem = new ExperienceSystem();
    this.speed = 3.0; // Velocidade base fixa
    this.projectiles = new ArrayList<>();
    this.floatingTexts = new ArrayList<>();
    this.skillManager = new SkillManager(this);
    this.inventory = new Inventory(); // Inicializa inventário com 20 slots
    this.questManager = new QuestManager(); // Inicializa gerenciador de quests
    loadSprite(spritePath);
    initializeStartingItems(); // Adiciona itens iniciais
  }

  private void loadSprite(String path) {
    try {
      // Carregar sprites de animação baseado na classe
      String baseClass = playerClass.toLowerCase();
      String spritesPath = com.rpggame.world.ResourceResolver.getResourcePath("sprites/");

      // Usar o sistema de resolução de caminho
      loadAnimationSprites(spritesPath, baseClass);

      // Definir sprite inicial (direita)
      currentSprite = spriteRight1;

    } catch (IOException e) {
      System.err.println("Erro ao carregar sprites de animação do jogador: " + playerClass);
      e.printStackTrace();
      // Tentar carregar sprite original como fallback
      try {
        BufferedImage fallbackSprite = loadSpriteFromPath(path);
        if (fallbackSprite != null) {
          currentSprite = fallbackSprite;
        } else {
          throw new IOException("Não foi possível carregar: " + path);
        }
        // Usar o mesmo sprite para todas as direções como fallback
        spriteRight1 = spriteRight2 = spriteLeft1 = spriteLeft2 = currentSprite;
      } catch (IOException e2) {
        System.err.println("Erro ao carregar sprite fallback: " + path);
        // Criar um retângulo simples como fallback final
        currentSprite = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = currentSprite.getGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.dispose();
        spriteRight1 = spriteRight2 = spriteLeft1 = spriteLeft2 = currentSprite;
      }
    }
  }

  private BufferedImage loadSpriteFromPath(String path) throws IOException {
    // Tentar carregar como recurso do classpath (funciona no JAR)
    InputStream is = getClass().getClassLoader().getResourceAsStream(path);
    if (is != null) {
      BufferedImage img = ImageIO.read(is);
      is.close();
      System.out.println("✅ Sprite carregado do JAR: " + path);
      return img;
    }

    // Fallback: tentar carregar como arquivo externo (desenvolvimento)
    File file = new File(path);
    if (file.exists()) {
      System.out.println("✅ Sprite carregado do arquivo: " + path);
      return ImageIO.read(file);
    }

    return null;
  }

  private void loadAnimationSprites(String basePath, String className) throws IOException {
    // Nomes dos arquivos baseados na classe
    String classCapitalized = className.substring(0, 1).toUpperCase() + className.substring(1);

    // Sprites para direita
    spriteRight1 = loadSpriteFromPath(basePath + classCapitalized + "Player.png");
    spriteRight2 = loadSpriteFromPath(basePath + classCapitalized + "Player2.png");

    // Sprites para esquerda
    spriteLeft1 = loadSpriteFromPath(basePath + classCapitalized + "PlayerLeft.png");
    spriteLeft2 = loadSpriteFromPath(basePath + classCapitalized + "PlayerLeft2.png");

    if (spriteRight1 == null || spriteRight2 == null || spriteLeft1 == null || spriteLeft2 == null) {
      throw new IOException("Falha ao carregar um ou mais sprites de " + className);
    }

    System.out.println("Sprites de animação carregados para " + className);
  }

  public void update() {
    // Processar atordoamento
    if (stunned) {
      stunTimer--;
      if (stunTimer <= 0) {
        stunned = false;
        System.out.println("✅ Player recuperou do atordoamento!");
      }
      // Bloquear movimento enquanto atordoado
      dx = 0;
      dy = 0;
      up = down = left = right = false;

      // Ainda processar cooldowns e outras coisas
      processTimers();
      return; // Não processar mais nada
    }

    // Verificar se está preso pelo Mimic ou outro inimigo
    if (grabbed) {
      System.out.println("🔒 [DEBUG Player] Movimento BLOQUEADO - Player está grabbed!");
      // Bloquear completamente o movimento
      dx = 0;
      dy = 0;
      up = down = left = right = false;

      // Ainda processar cooldowns e outras coisas
      processTimers();
      return; // Não processar mais nada
    }

    // Resetar velocidade
    dx = 0;
    dy = 0;

    // Calcular velocidade efetiva (dobra se estiver em fúria berserk)
    double effectiveSpeed = berserkActive ? speed * 2.0 : speed;

    // Só permite movimento se não estiver em diálogo ou preso
    if (!inDialog && !grabbed) {
      // Calcular movimento baseado nas teclas pressionadas
      if (up)
        dy = -effectiveSpeed;
      if (down)
        dy = effectiveSpeed;
      if (left) {
        dx = -effectiveSpeed;
        facingLeft = true; // Virado para a esquerda
      }
      if (right) {
        dx = effectiveSpeed;
        facingLeft = false; // Virado para a direita
      }
    }

    // Movimento diagonal (normalizar velocidade)
    if ((up || down) && (left || right)) {
      dx *= 0.707; // sqrt(2)/2
      dy *= 0.707;
    }

    // Verificar se está se movendo
    isMoving = (dx != 0 || dy != 0);

    // Atualizar animação
    if (isMoving) {
      animationFrame++;
      // Alternar entre frame 1 e 2 a cada 10 updates (velocidade da animação)
      int frameIndex = (animationFrame / 10) % 2;

      if (facingLeft) {
        currentSprite = (frameIndex == 0) ? spriteLeft1 : spriteLeft2;
      } else {
        currentSprite = (frameIndex == 0) ? spriteRight1 : spriteRight2;
      }
    }

    // Atualizar direção baseada no movimento
    if (dx != 0 || dy != 0) {
      facing = Math.atan2(dy, dx);
    }

    // Verificar colisão e atualizar posição
    updatePositionWithCollision(dx, dy);

    // Atualizar cooldown de ataque
    if (attackCooldown > 0) {
      attackCooldown--;
      if (attackCooldown == 0) {
        canAttack = true;
      }
    }

    // Processar ataque (só se não estiver em diálogo ou preso)
    if (spacePressed && canAttack && !inDialog && !grabbed) {
      attack();
    }

    // Atualizar projéteis
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile p = projectiles.get(i);
      p.update();
      // Verificar colisão com paredes
      if (tileMap != null) {
        p.checkWallCollision(tileMap);
      }
      if (!p.isActive()) {
        projectiles.remove(i);
      }
    }

    // Atualizar textos flutuantes
    for (int i = floatingTexts.size() - 1; i >= 0; i--) {
      FloatingText ft = floatingTexts.get(i);
      ft.update();
      if (!ft.isActive()) {
        floatingTexts.remove(i);
      }
    }

    // Atualizar habilidades
    if (skillManager != null) {
      skillManager.update();

      // Verificar se está usando QuickDashSkill
      try {
        com.rpggame.systems.Skill dashSkill = getSkillInSlot(2);
        if (dashSkill instanceof com.rpggame.systems.skills.QuickDashSkill) {
          com.rpggame.systems.skills.QuickDashSkill quickDash = (com.rpggame.systems.skills.QuickDashSkill) dashSkill;
          quickDash.applyDashMovement(this);
        }
      } catch (Exception e) {
        // Ignorar se não houver habilidade no slot 2
      }
    }

    // Verificar level up e restaurar vida
    checkLevelUpAndRestoreHealth();

    // Regenerar mana baseado na sabedoria
    regenerateMana();

    // Atualizar timer do GoldUI
    updateGoldUI();
  }

  /**
   * Verifica se houve level up e restaura a vida ao máximo
   */
  private void checkLevelUpAndRestoreHealth() {
    if (experienceSystem.shouldRestoreHealth(lastLevelCheck)) {
      currentHealth = maxHealth; // Restaurar vida ao máximo
      int newLevel = experienceSystem.getCurrentLevel();
      lastLevelCheck = newLevel;

      // Mostrar texto de level up
      FloatingText levelUpText = new FloatingText(x + WIDTH / 2, y - 20,
          "LEVEL UP!", Color.YELLOW);
      floatingTexts.add(levelUpText);

      // Mostrar texto de vida restaurada
      FloatingText healText = new FloatingText(x + WIDTH / 2, y - 35,
          "VIDA RESTAURADA!", Color.GREEN);
      floatingTexts.add(healText);

      // Verificar desbloqueio de habilidades
      checkSkillUnlock(newLevel);
    }
  }

  /**
   * Verifica se o nível atual desbloqueia uma nova habilidade
   */
  private void checkSkillUnlock(int level) {
    if (level == 5 && !skill2Unlocked) {
      skill2Unlocked = true;
      pendingSkillUnlock = 2;
      // Aprender a habilidade do slot 2
      if (skillManager != null) {
        skillManager.learnSkill(2);
      }
      System.out.println("✨ Level 5 alcançado! Slot 2 desbloqueado!");
    } else if (level == 7 && !skill3Unlocked) {
      skill3Unlocked = true;
      pendingSkillUnlock = 3;
      // Aprender a habilidade do slot 3
      if (skillManager != null) {
        skillManager.learnSkill(3);
      }
      System.out.println("✨ Level 7 alcançado! Slot 3 desbloqueado!");
    } else if (level == 10 && !skill4Unlocked) {
      skill4Unlocked = true;
      pendingSkillUnlock = 4;
      // Aprender a habilidade do slot 4
      if (skillManager != null) {
        skillManager.learnSkill(4);
      }
      System.out.println("✨ Level 10 alcançado! Slot 4 desbloqueado!");
    }
  }

  public void render(Graphics2D g, Camera camera) {
    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Só renderizar se estiver na tela
    if (screenX > -WIDTH && screenX < Game.SCREEN_WIDTH &&
        screenY > -HEIGHT && screenY < Game.SCREEN_HEIGHT) {
      // Renderizar sprite atual com tamanho correto
      BufferedImage spriteToRender = (currentSprite != null) ? currentSprite : spriteRight1;
      g.drawImage(spriteToRender, screenX, screenY, WIDTH, HEIGHT, null);

      // Renderizar efeito de atordoamento
      if (stunned) {
        renderStunEffect(g, screenX + WIDTH / 2, screenY);
      }

      // DEBUG: Visualizar hitbox (descomente para debug)
      // g.setColor(Color.RED);
      // g.drawRect(screenX + HITBOX_OFFSET_X, screenY + HITBOX_OFFSET_Y,
      // HITBOX_WIDTH, HITBOX_HEIGHT);

      // Barra de vida removida - agora exibida na UI
    }

    // Renderizar projéteis (usar cópia para evitar ConcurrentModificationException)
    for (Projectile p : new ArrayList<>(projectiles)) {
      p.render(g, camera);
    }

    // Renderizar textos flutuantes (usar cópia para evitar
    // ConcurrentModificationException)
    for (FloatingText ft : new ArrayList<>(floatingTexts)) {
      ft.render(g, camera);
    }
  }

  // TODO: Implementar renderização de barra de vida
  /*
   * private void renderHealthBar(Graphics2D g, int screenX, int screenY) {
   * int barWidth = WIDTH;
   * int barHeight = 4;
   * int barY = screenY - 8;
   * 
   * // Fundo da barra (vermelho)
   * g.setColor(Color.RED);
   * g.fillRect(screenX, barY, barWidth, barHeight);
   * 
   * // Vida atual (verde)
   * g.setColor(Color.GREEN);
   * int healthWidth = (int) ((double) currentHealth / maxHealth * barWidth);
   * g.fillRect(screenX, barY, healthWidth, barHeight);
   * 
   * // Borda da barra
   * g.setColor(Color.WHITE);
   * g.drawRect(screenX, barY, barWidth, barHeight);
   * }
   */

  private void attack() {
    if (!canAttack)
      return;

    canAttack = false;
    attackCooldown = ATTACK_COOLDOWN_TIME;

    // Posição inicial do projétil (centro do jogador)
    double startX = x + WIDTH / 2.0;
    double startY = y + HEIGHT / 2.0;

    // Calcular dano baseado nos atributos e equipamento
    int baseDamage = 10;
    int bonusDamage = stats.getDamageBonus();
    int weaponBonus = getTotalDamageBonus(); // Bônus da arma equipada
    int totalDamage = baseDamage + bonusDamage + weaponBonus;

    // Debug do dano
    if (weaponBonus > 0) {
      System.out.println("🗡️ Dano total: " + totalDamage + " (Base: " + baseDamage + " + Atributo: " + bonusDamage
          + " + Arma: " + weaponBonus + ")");
    }

    // Criar projétil baseado na classe
    Projectile projectile = null;
    switch (playerClass.toLowerCase()) {
      case "mage":
        projectile = new Projectile(startX, startY, facing, Projectile.MAGIC_BOLT, totalDamage);
        break;
      case "hunter":
        projectile = new Projectile(startX, startY, facing, Projectile.ARROW, totalDamage);
        break;
      case "warrior":
        // Guerreiro faz um ataque corpo a corpo à frente
        double slashX = startX + Math.cos(facing) * 30;
        double slashY = startY + Math.sin(facing) * 30;
        projectile = new Projectile(slashX, slashY, facing, Projectile.SWORD_SLASH, totalDamage);
        break;
    }

    if (projectile != null) {
      projectiles.add(projectile);
    }

    // Verificar se há estruturas vulneráveis próximas para atacar diretamente
    checkAndAttackNearbyStructures(totalDamage);
  }

  /**
   * Verifica e ataca estruturas vulneráveis próximas
   */
  private void checkAndAttackNearbyStructures(int damage) {
    if (enemyManager == null)
      return;

    // Alcance de ataque corpo a corpo para estruturas
    double structureAttackRange = 80.0;

    // Verificar estruturas próximas
    for (Structure structure : enemyManager.getStructures()) {
      if (structure.isVulnerable() && !structure.isDestroyed()) {
        double distance = structure.distanceTo(x + WIDTH / 2, y + HEIGHT / 2);

        if (distance <= structureAttackRange) {
          // Atacar a estrutura
          boolean destroyed = structure.takeDamage(damage);

          // Criar texto de dano
          FloatingText damageText = new FloatingText(
              structure.getX() + structure.getWidth() / 2,
              structure.getY() + structure.getHeight() / 2,
              "-" + damage, Color.ORANGE);
          floatingTexts.add(damageText);

          if (destroyed) {
            // Dar XP por destruir a cabana
            int xpReward = 100; // XP por destruir cabana
            boolean leveledUp = experienceSystem.addExperience(xpReward);

            // Mostrar XP ganho
            FloatingText xpText = new FloatingText(
                structure.getX() + structure.getWidth() / 2,
                structure.getY() + structure.getHeight() / 2 - 20,
                "+" + xpReward + " XP", Color.CYAN);
            floatingTexts.add(xpText);

            System.out.println("Cabana destruída! +" + xpReward + " XP");

            // Notificar EnemyManager que a cabana foi destruída
            enemyManager.onStructureDestroyed(structure);

            if (leveledUp) {
              System.out.println("Level up ao destruir cabana!");
            }
          }

          break; // Atacar apenas uma estrutura por vez
        }
      }
    }
  }

  public void keyPressed(KeyEvent e) {
    int key = e.getKeyCode();

    switch (key) {
      case KeyEvent.VK_W:
      case KeyEvent.VK_UP:
        up = true;
        break;
      case KeyEvent.VK_S:
      case KeyEvent.VK_DOWN:
        down = true;
        break;
      case KeyEvent.VK_A:
      case KeyEvent.VK_LEFT:
        left = true;
        break;
      case KeyEvent.VK_D:
      case KeyEvent.VK_RIGHT:
        right = true;
        break;
      case KeyEvent.VK_SPACE:
        spacePressed = true;
        break;
      case KeyEvent.VK_1:
        if (skillManager != null) {
          skillManager.useSkill(1);
        }
        break;
      case KeyEvent.VK_2:
        if (skillManager != null) {
          skillManager.useSkill(2);
        }
        break;
      case KeyEvent.VK_3:
        if (skillManager != null) {
          skillManager.useSkill(3);
        }
        break;
      case KeyEvent.VK_4:
        if (skillManager != null) {
          skillManager.useSkill(4);
        }
        break;
    }
  }

  public void keyReleased(KeyEvent e) {
    int key = e.getKeyCode();

    switch (key) {
      case KeyEvent.VK_W:
      case KeyEvent.VK_UP:
        up = false;
        break;
      case KeyEvent.VK_S:
      case KeyEvent.VK_DOWN:
        down = false;
        break;
      case KeyEvent.VK_A:
      case KeyEvent.VK_LEFT:
        left = false;
        break;
      case KeyEvent.VK_D:
      case KeyEvent.VK_RIGHT:
        right = false;
        break;
      case KeyEvent.VK_SPACE:
        spacePressed = false;
        break;
    }
  }

  // Getters
  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public void setPosition(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public int getWidth() {
    return WIDTH;
  }

  public int getHeight() {
    return HEIGHT;
  }

  public String getPlayerClass() {
    return playerClass;
  }

  public CharacterStats getStats() {
    return stats;
  }

  public int getCurrentHealth() {
    return currentHealth;
  }

  public int getMaxHealth() {
    return maxHealth;
  }

  public int getCurrentMana() {
    return currentMana;
  }

  public int getMaxMana() {
    return maxMana;
  }

  public ExperienceSystem getExperienceSystem() {
    return experienceSystem;
  }

  public void gainExperience(int xp) {
    boolean leveledUp = experienceSystem.addExperience(xp);

    if (leveledUp) {
      // Mostrar texto de level up
      FloatingText levelUpText = new FloatingText(x + WIDTH / 2, y - 20,
          "LEVEL UP!", Color.YELLOW);
      floatingTexts.add(levelUpText);

      // Atualizar vida e mana máximas baseadas nos novos stats
      maxHealth = stats.getMaxHealth();
      maxMana = stats.getMaxMana();

      // Restaurar vida e mana completamente ao subir de nível
      currentHealth = maxHealth;
      currentMana = maxMana;
    }

    // Mostrar XP ganho
    FloatingText xpText = new FloatingText(x + WIDTH / 2, y - 10,
        "+" + xp + " XP", Color.GREEN);
    floatingTexts.add(xpText);
  }

  public ArrayList<Projectile> getProjectiles() {
    return projectiles;
  }

  // Setters
  public void takeDamage(int damage) {
    // Verificar evasão baseada na destreza
    float evasionChance = stats.getEvasionChance();
    if (Math.random() < evasionChance) {
      // Ataque evadido - não recebe dano
      FloatingText evasionText = new FloatingText(x + WIDTH / 2, y, "EVADIDO!", Color.CYAN);
      floatingTexts.add(evasionText);
      return;
    }

    // Aplicar redução de dano baseada na constituição
    float reduction = stats.getDamageReduction();

    // Se estiver em Fúria Berserk, reduz 50% adicional do dano
    if (berserkActive) {
      reduction += 0.5f;
      reduction = Math.min(0.95f, reduction); // Máximo de 95% de redução total
    }

    int reducedDamage = (int) (damage * (1.0f - reduction));
    currentHealth = Math.max(0, currentHealth - reducedDamage);

    // Mostrar texto de dano (com cor diferente se em berserk)
    Color damageColor = berserkActive ? new Color(255, 100, 0) : Color.RED;
    FloatingText damageText = new FloatingText(x + WIDTH / 2, y, "-" + reducedDamage, damageColor);
    floatingTexts.add(damageText);
  }

  public void heal(int amount) {
    currentHealth = Math.min(maxHealth, currentHealth + amount);
  }

  /**
   * Restaura mana do jogador.
   */
  public void restoreMana(int amount) {
    currentMana = Math.min(maxMana, currentMana + amount);
  }

  /**
   * Tenta aplicar atordoamento no player
   * Retorna true se foi atordoado, false se for imune (berserk)
   */
  public boolean applyStun(int duration) {
    if (berserkActive) {
      FloatingText immuneText = new FloatingText(x + WIDTH / 2, y - 10, "IMUNE!", new Color(255, 200, 0));
      floatingTexts.add(immuneText);
      System.out.println("⚔️ Fúria Berserk: IMUNE a atordoamento!");
      return false;
    }

    // Aplicar atordoamento
    stunned = true;
    stunTimer = duration;

    // Parar movimento
    dx = 0;
    dy = 0;
    up = down = left = right = false;

    // Feedback visual
    FloatingText stunText = new FloatingText(x + WIDTH / 2, y - 10, "ATORDOADO!", new Color(255, 255, 0));
    floatingTexts.add(stunText);

    System.out.println("💫 Player foi atordoado por " + (duration / 60.0) + " segundos!");
    return true;
  }

  /**
   * Tenta aplicar medo no player
   * Retorna true se foi amedrontado, false se for imune (berserk)
   */
  public boolean applyFear(int duration) {
    if (berserkActive) {
      FloatingText immuneText = new FloatingText(x + WIDTH / 2, y - 10, "IMUNE!", new Color(255, 200, 0));
      floatingTexts.add(immuneText);
      System.out.println("⚔️ Fúria Berserk: IMUNE a medo!");
      return false;
    }
    // Player não tem sistema de fear implementado ainda, mas retorna true para
    // indicar que seria amedrontado
    return true;
  }

  /**
   * Consome mana do jogador
   */
  public void consumeMana(int amount) {
    currentMana = Math.max(0, currentMana - amount);

    // Mostrar texto de mana consumida
    FloatingText manaText = new FloatingText(x + WIDTH / 2, y - 5,
        "-" + amount + " MP", new Color(100, 150, 255));
    floatingTexts.add(manaText);
  }

  /**
   * Regenera mana baseado na sabedoria do jogador
   */
  private void regenerateMana() {
    if (currentMana >= maxMana) {
      manaRegenTimer = 0;
      return;
    }

    manaRegenTimer++;

    // Regenerar a cada 1 segundo (60 frames)
    if (manaRegenTimer >= 60) {
      manaRegenTimer = 0;

      // Quantidade de mana regenerada baseada na sabedoria
      float manaRegen = stats.getManaRegen();
      int manaToRegen = (int) Math.ceil(manaRegen);

      currentMana = Math.min(maxMana, currentMana + manaToRegen);
    }
  }

  public boolean isAlive() {
    return currentHealth > 0;
  }

  public void setTileMap(TileMap tileMap) {
    this.tileMap = tileMap;
  }

  public void setEnemyManager(EnemyManager enemyManager) {
    this.enemyManager = enemyManager;
  }

  public EnemyManager getEnemyManager() {
    return enemyManager;
  }

  public SkillManager getSkillManager() {
    return skillManager;
  }

  public TileMap getTileMap() {
    return tileMap;
  }

  /**
   * Obtém uma habilidade do slot especificado
   */
  public com.rpggame.systems.Skill getSkillInSlot(int slot) {
    if (skillManager != null) {
      return skillManager.getSkill(slot);
    }
    return null;
  }

  /**
   * Obtém a direção que o jogador está olhando
   */
  public double getFacingDirection() {
    return facing;
  }

  private void updatePositionWithCollision(double dx, double dy) {
    // Calcular nova posição
    double newX = x + dx;
    double newY = y + dy;

    // Verificar limites do mapa (usando a hitbox para cálculos precisos)
    newX = Math.max(-HITBOX_OFFSET_X,
        Math.min(newX, GamePanel.MAP_WIDTH * GamePanel.TILE_SIZE - HITBOX_WIDTH - HITBOX_OFFSET_X));
    newY = Math.max(-HITBOX_OFFSET_Y,
        Math.min(newY, GamePanel.MAP_HEIGHT * GamePanel.TILE_SIZE - HITBOX_HEIGHT - HITBOX_OFFSET_Y));

    // Verificar colisão com tiles (checkar os 4 cantos do jogador)
    if (canMoveToPosition(newX, newY)) {
      x = newX;
      y = newY;
    } else {
      // Tentar movimento apenas no eixo X
      if (dx != 0 && canMoveToPosition(x + dx, y)) {
        x = Math.max(-HITBOX_OFFSET_X,
            Math.min(x + dx, GamePanel.MAP_WIDTH * GamePanel.TILE_SIZE - HITBOX_WIDTH - HITBOX_OFFSET_X));
      }
      // Tentar movimento apenas no eixo Y
      else if (dy != 0 && canMoveToPosition(x, y + dy)) {
        y = Math.max(-HITBOX_OFFSET_Y,
            Math.min(y + dy, GamePanel.MAP_HEIGHT * GamePanel.TILE_SIZE - HITBOX_HEIGHT - HITBOX_OFFSET_Y));
      }
    }
  }

  private boolean canMoveToPosition(double newX, double newY) {
    // Noclip bypass - permite atravessar qualquer coisa
    if (noclipEnabled) {
      return true;
    }

    // Se o tileMap não estiver inicializado, permitir movimento (para
    // compatibilidade)
    if (tileMap == null) {
      return true;
    }

    // Usar hitbox menor para colisão (permite passar por corredores de 1 tile)
    int tileSize = GamePanel.TILE_SIZE;

    // Calcular posição da hitbox
    double hitboxX = newX + HITBOX_OFFSET_X;
    double hitboxY = newY + HITBOX_OFFSET_Y;

    // Verificar os 4 cantos da hitbox
    // Canto superior esquerdo
    int tileX1 = (int) (hitboxX / tileSize);
    int tileY1 = (int) (hitboxY / tileSize);

    // Canto superior direito
    int tileX2 = (int) ((hitboxX + HITBOX_WIDTH - 1) / tileSize);
    int tileY2 = (int) (hitboxY / tileSize);

    // Canto inferior esquerdo
    int tileX3 = (int) (hitboxX / tileSize);
    int tileY3 = (int) ((hitboxY + HITBOX_HEIGHT - 1) / tileSize);

    // Canto inferior direito
    int tileX4 = (int) ((hitboxX + HITBOX_WIDTH - 1) / tileSize);
    int tileY4 = (int) ((hitboxY + HITBOX_HEIGHT - 1) / tileSize);

    // Verificar se todos os tiles são válidos e caminháveis (ou congelados)
    boolean tilesWalkable = isValidTile(tileX1, tileY1) && isTileWalkableOrFrozen(tileX1, tileY1) &&
        isValidTile(tileX2, tileY2) && isTileWalkableOrFrozen(tileX2, tileY2) &&
        isValidTile(tileX3, tileY3) && isTileWalkableOrFrozen(tileX3, tileY3) &&
        isValidTile(tileX4, tileY4) && isTileWalkableOrFrozen(tileX4, tileY4);

    if (!tilesWalkable) {
      return false;
    }

    // Verificar colisão com estruturas (igreja, casas, tendas, lâmpadas, etc)
    if (enemyManager != null) {
      for (Structure structure : enemyManager.getStructures()) {
        if (structure.isDestroyed()) {
          continue; // Estruturas destruídas não bloqueiam
        }

        // Colisão AABB (Axis-Aligned Bounding Box) - retângulo vs retângulo
        double structX = structure.getX();
        double structY = structure.getY();
        double structWidth = structure.getWidth();
        double structHeight = structure.getHeight();

        // Verificar se há sobreposição entre as hitboxes
        if (hitboxX < structX + structWidth &&
            hitboxX + HITBOX_WIDTH > structX &&
            hitboxY < structY + structHeight &&
            hitboxY + HITBOX_HEIGHT > structY) {
          return false; // Colisão detectada
        }
      }
    }

    return true;
  }

  /**
   * Verifica se um tile é caminhável ou está congelado (gelo sobre água/lava)
   */
  private boolean isTileWalkableOrFrozen(int tileX, int tileY) {
    // Se o tile é naturalmente caminhável, retorna true
    if (tileMap.isWalkable(tileX, tileY)) {
      return true;
    }

    // Se não é caminhável, verificar se está congelado (apenas para Mage)
    if (skillManager != null) {
      com.rpggame.systems.Skill skill = skillManager.getSkillBySlot(2);
      if (skill instanceof com.rpggame.systems.skills.FreezingSkill) {
        com.rpggame.systems.skills.FreezingSkill freezingSkill = (com.rpggame.systems.skills.FreezingSkill) skill;
        if (freezingSkill.isTileFrozen(tileX, tileY)) {
          return true; // Tile congelado permite passagem
        }
      }
    }

    return false;
  }

  private boolean isValidTile(int tileX, int tileY) {
    return tileX >= 0 && tileX < GamePanel.MAP_WIDTH &&
        tileY >= 0 && tileY < GamePanel.MAP_HEIGHT;
  }

  /**
   * Atualiza vida e mana máximas baseado nos novos atributos.
   */
  public void updateStatsFromAttributes() {
    int newMaxHealth = stats.getMaxHealth();
    int newMaxMana = stats.getMaxMana();

    // Manter proporção da vida/mana atual
    double healthRatio = (double) currentHealth / maxHealth;
    double manaRatio = (double) currentMana / maxMana;

    maxHealth = newMaxHealth;
    maxMana = newMaxMana;

    // Aplicar proporção aos valores atuais (não pode exceder o novo máximo)
    currentHealth = Math.min(maxHealth, (int) (maxHealth * healthRatio));
    currentMana = Math.min(maxMana, (int) (maxMana * manaRatio));

    System.out.println("Stats atualizados - Vida: " + currentHealth + "/" + maxHealth +
        " | Mana: " + currentMana + "/" + maxMana);
  }

  /**
   * Define se o jogador está em diálogo (não pode se mover)
   */
  public void setInDialog(boolean inDialog) {
    this.inDialog = inDialog;
  }

  /**
   * Retorna se o jogador está em diálogo
   */
  public boolean isInDialog() {
    return inDialog;
  }

  /**
   * Define se o jogador está sendo preso por um inimigo (não pode se mover)
   */
  public void setGrabbed(boolean grabbed) {
    System.out.println("🔍 [DEBUG Player] setGrabbed chamado! Valor: " + grabbed + " | Anterior: " + this.grabbed);
    this.grabbed = grabbed;
  }

  /**
   * Retorna se o jogador está sendo preso
   */
  public boolean isGrabbed() {
    return grabbed;
  }

  /**
   * Define o estado de Fúria Berserk
   */
  public void setBerserkActive(boolean active) {
    this.berserkActive = active;
  }

  /**
   * Retorna se está em Fúria Berserk
   */
  public boolean isBerserkActive() {
    return berserkActive;
  }

  /**
   * Retorna qual slot de habilidade está pendente para desbloqueio (0 = nenhum)
   */
  public int getPendingSkillUnlock() {
    return pendingSkillUnlock;
  }

  /**
   * Limpa o desbloqueio pendente de habilidade
   */
  public void clearPendingSkillUnlock() {
    pendingSkillUnlock = 0;
  }

  /**
   * Retorna o inventário do jogador
   */
  public Inventory getInventory() {
    return inventory;
  }

  /**
   * Inicializa itens iniciais no inventário.
   */
  private void initializeStartingItems() {
    if (inventory != null) {
      // Adiciona 3 poções de vida
      inventory.addItem(new HealthPotion(this, 50), 3);

      // Adiciona 3 poções de mana
      inventory.addItem(new ManaPotion(this, 30), 3);

      System.out.println("✅ Inventário iniciado com itens básicos");
    }
  }

  /**
   * Adiciona um texto flutuante na tela do jogador.
   */
  public void addFloatingText(String text, Color color) {
    FloatingText ft = new FloatingText(x + WIDTH / 2, y - 20, text, color);
    floatingTexts.add(ft);
  }

  /**
   * Ativa/desativa modo noclip (atravessar paredes)
   */
  public void setNoclip(boolean enabled) {
    this.noclipEnabled = enabled;
  }

  /**
   * Retorna se noclip está ativo
   */
  public boolean isNoclipEnabled() {
    return noclipEnabled;
  }

  /**
   * Define velocidade do player
   */
  public void setSpeed(double newSpeed) {
    this.speed = newSpeed;
  }

  /**
   * Retorna velocidade atual do player
   */
  public double getSpeed() {
    return speed;
  }

  /**
   * Adiciona gold ao jogador
   */
  public void addGold(int amount) {
    gold += amount;
    showGoldUI = true;
    goldUITimer = GOLD_UI_DISPLAY_TIME;
    addFloatingText("+" + amount + " Gold", new Color(255, 215, 0));
    System.out.println("💰 +" + amount + " gold (Total: " + gold + ")");
  }

  /**
   * Remove gold do jogador
   */
  public boolean removeGold(int amount) {
    if (gold >= amount) {
      gold -= amount;
      return true;
    }
    return false;
  }

  /**
   * Retorna quantidade de gold
   */
  public int getGold() {
    return gold;
  }

  /**
   * Força a exibição do UI de gold (ex: quando abre inventário)
   */
  public void showGoldUI() {
    showGoldUI = true;
    goldUITimer = GOLD_UI_DISPLAY_TIME;
  }

  /**
   * Verifica se deve mostrar o UI de gold
   */
  public boolean shouldShowGoldUI() {
    return showGoldUI;
  }

  /**
   * Atualiza o timer do UI de gold
   */
  public void updateGoldUI() {
    if (goldUITimer > 0) {
      goldUITimer--;
      if (goldUITimer <= 0) {
        showGoldUI = false;
      }
    }
  }

  /**
   * Retorna o gerenciador de quests
   */
  public QuestManager getQuestManager() {
    return questManager;
  }

  /**
   * Força a exibição do UI de gold
   */
  public void forceShowGoldUI() {
    showGoldUI = true;
    goldUITimer = GOLD_UI_DISPLAY_TIME;
  }

  /**
   * Esconde o UI de gold
   */
  public void hideGoldUI() {
    showGoldUI = false;
    goldUITimer = 0;
  }

  /**
   * Equipa uma arma
   */
  public void equipWeapon(com.rpggame.items.EquippableItem weapon) {
    if (weapon != null && weapon.canEquip(this)) {
      equippedWeapon = weapon;
      System.out.println("⚔️ " + weapon.getName() + " equipado! (+" + weapon.getDamageBonus() + " dano)");
    } else if (weapon != null) {
      System.out.println("❌ Você não pode equipar " + weapon.getName());
      System.out.println("   Requisitos: " + weapon.getMissingRequirements(this));
    }
  }

  /**
   * Desequipa a arma atual
   */
  public void unequipWeapon() {
    if (equippedWeapon != null) {
      System.out.println("⚔️ " + equippedWeapon.getName() + " desequipado");
      equippedWeapon = null;
    }
  }

  /**
   * Retorna a arma equipada
   */
  public com.rpggame.items.EquippableItem getEquippedWeapon() {
    return equippedWeapon;
  }

  /**
   * Retorna o bônus de dano total (equipamento + outros)
   */
  public int getTotalDamageBonus() {
    int bonus = 0;
    if (equippedWeapon != null) {
      bonus += equippedWeapon.getDamageBonus();
    }
    return bonus;
  }

  /**
   * Processa todos os timers do player
   */
  private void processTimers() {
    // Atualizar cooldown de ataque
    if (attackCooldown > 0) {
      attackCooldown--;
      if (attackCooldown == 0) {
        canAttack = true;
      }
    }

    // Atualizar projéteis
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile p = projectiles.get(i);
      p.update();
      if (tileMap != null) {
        p.checkWallCollision(tileMap);
      }
      if (!p.isActive()) {
        projectiles.remove(i);
      }
    }

    // Atualizar textos flutuantes
    for (int i = floatingTexts.size() - 1; i >= 0; i--) {
      FloatingText ft = floatingTexts.get(i);
      ft.update();
      if (!ft.isActive()) {
        floatingTexts.remove(i);
      }
    }

    // Regeneração de mana
    manaRegenTimer++;
    if (manaRegenTimer >= 60) {
      int regenAmount = (int) stats.getManaRegen();
      if (regenAmount > 0) {
        restoreMana(regenAmount);
      }
      manaRegenTimer = 0;
    }

    // Timer de UI de gold
    if (showGoldUI && goldUITimer > 0) {
      goldUITimer--;
      if (goldUITimer <= 0) {
        showGoldUI = false;
      }
    }
  }

  /**
   * Renderiza o efeito visual de atordoamento (estrelas girando)
   */
  private void renderStunEffect(Graphics2D g, int centerX, int centerY) {
    // Estrelas amarelas girando ao redor da cabeça do player
    int numStars = 3;
    double radius = 25;

    // Usar stunTimer para animação de rotação
    double angleOffset = (stunTimer / 10.0) * Math.PI * 2; // Gira conforme o tempo

    g.setColor(new Color(255, 255, 0));
    g.setFont(new Font("Arial", Font.BOLD, 20));

    for (int i = 0; i < numStars; i++) {
      double angle = (Math.PI * 2 / numStars) * i + angleOffset;
      int starX = centerX + (int) (Math.cos(angle) * radius);
      int starY = centerY + (int) (Math.sin(angle) * radius);

      g.drawString("★", starX - 6, starY + 6);
    }

    // Indicador de tempo restante
    if (stunTimer > 0) {
      float alpha = Math.min(1.0f, stunTimer / 30.0f);
      g.setColor(new Color(255, 255, 255, (int) (200 * alpha)));
      g.setFont(new Font("Arial", Font.BOLD, 10));
      String timeText = String.format("%.1fs", stunTimer / 60.0);
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(timeText);
      g.drawString(timeText, centerX - textWidth / 2, centerY - 35);
    }
  }

}