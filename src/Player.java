import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Classe que representa o jogador
 */
public class Player {
  private double x, y;
  private double dx, dy;
  private double speed;

  private BufferedImage sprite;
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
  private ExperienceSystem experienceSystem;

  // Direção atual (para ataques direcionais)
  private double facing = 0; // 0 = direita, PI/2 = baixo, PI = esquerda, 3*PI/2 = cima

  // Lista de projéteis do jogador
  private ArrayList<Projectile> projectiles;

  // Lista de textos flutuantes
  private ArrayList<FloatingText> floatingTexts;

  // Referência para o mapa (para verificação de colisão)
  private TileMap tileMap;

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
    this.projectiles = new ArrayList<>();
    this.floatingTexts = new ArrayList<>();
    loadSprite(spritePath);
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
    loadSprite(spritePath);
  }

  private void loadSprite(String path) {
    try {
      // Tentar primeiro o caminho original
      sprite = ImageIO.read(new File(path));
    } catch (IOException e1) {
      try {
        // Tentar caminho relativo da pasta src
        String alternatePath = "../" + path;
        sprite = ImageIO.read(new File(alternatePath));
      } catch (IOException e2) {
        System.err.println("Erro ao carregar sprite do jogador: " + path);
        e2.printStackTrace();
        // Criar um retângulo simples como fallback
        sprite = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = sprite.getGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.dispose();
      }
    }
  }

  public void update() {
    // Resetar velocidade
    dx = 0;
    dy = 0;

    // Calcular movimento baseado nas teclas pressionadas
    if (up)
      dy = -speed;
    if (down)
      dy = speed;
    if (left)
      dx = -speed;
    if (right)
      dx = speed;

    // Movimento diagonal (normalizar velocidade)
    if ((up || down) && (left || right)) {
      dx *= 0.707; // sqrt(2)/2
      dy *= 0.707;
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

    // Processar ataque
    if (spacePressed && canAttack) {
      attack();
    }

    // Atualizar projéteis
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile p = projectiles.get(i);
      p.update();
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
  }

  public void render(Graphics2D g, Camera camera) {
    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Só renderizar se estiver na tela
    if (screenX > -WIDTH && screenX < Game.SCREEN_WIDTH &&
        screenY > -HEIGHT && screenY < Game.SCREEN_HEIGHT) {
      // Renderizar sprite com tamanho correto
      g.drawImage(sprite, screenX, screenY, WIDTH, HEIGHT, null);

      // DEBUG: Visualizar hitbox (descomente para debug)
      // g.setColor(Color.RED);
      // g.drawRect(screenX + HITBOX_OFFSET_X, screenY + HITBOX_OFFSET_Y,
      // HITBOX_WIDTH, HITBOX_HEIGHT);

      // Barra de vida removida - agora exibida na UI
    }

    // Renderizar projéteis
    for (Projectile p : projectiles) {
      p.render(g, camera);
    }

    // Renderizar textos flutuantes
    for (FloatingText ft : floatingTexts) {
      ft.render(g, camera);
    }
  }

  private void renderHealthBar(Graphics2D g, int screenX, int screenY) {
    int barWidth = WIDTH;
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

  private void attack() {
    if (!canAttack)
      return;

    canAttack = false;
    attackCooldown = ATTACK_COOLDOWN_TIME;

    // Posição inicial do projétil (centro do jogador)
    double startX = x + WIDTH / 2.0;
    double startY = y + HEIGHT / 2.0;

    // Calcular dano baseado nos atributos
    int baseDamage = 10;
    int bonusDamage = stats.getDamageBonus();
    int totalDamage = baseDamage + bonusDamage;

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
    int reducedDamage = (int) (damage * (1.0f - reduction));
    currentHealth = Math.max(0, currentHealth - reducedDamage);

    // Mostrar texto de dano
    FloatingText damageText = new FloatingText(x + WIDTH / 2, y, "-" + reducedDamage, Color.RED);
    floatingTexts.add(damageText);
  }

  public void heal(int amount) {
    currentHealth = Math.min(maxHealth, currentHealth + amount);
  }

  public boolean isAlive() {
    return currentHealth > 0;
  }

  public void setTileMap(TileMap tileMap) {
    this.tileMap = tileMap;
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

    // Verificar se todos os tiles são válidos e caminháveis
    return isValidTile(tileX1, tileY1) && tileMap.isWalkable(tileX1, tileY1) &&
        isValidTile(tileX2, tileY2) && tileMap.isWalkable(tileX2, tileY2) &&
        isValidTile(tileX3, tileY3) && tileMap.isWalkable(tileX3, tileY3) &&
        isValidTile(tileX4, tileY4) && tileMap.isWalkable(tileX4, tileY4);
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
}