import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

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
  
  // Timer para ataques
  protected int attackCooldown;
  protected final int ATTACK_COOLDOWN_TIME = 60; // frames
  
  // Referência para o jogador (para IA)
  protected Player target;
  
  // Referência para o mapa (para verificação de colisão)
  protected TileMap tileMap;
  
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
    
    loadSprite();
    initializeStats();
  }
  
  /**
   * Carrega o sprite do inimigo
   */
  private void loadSprite() {
    System.out.println("Tentando carregar sprite: " + spritePath);
    
    // Lista de caminhos possíveis para tentar
    String[] possiblePaths = {
        spritePath,
        "../" + spritePath,
        "../../" + spritePath,
        "./" + spritePath
    };
    
    boolean loaded = false;
    
    for (String path : possiblePaths) {
      try {
        File spriteFile = new File(path);
        System.out.println("Tentando caminho: " + spriteFile.getAbsolutePath());
        
        if (spriteFile.exists()) {
          sprite = ImageIO.read(spriteFile);
          if (sprite != null) {
            width = sprite.getWidth();
            height = sprite.getHeight();
            loaded = true;
            System.out.println("Sprite carregado com sucesso: " + path);
            break;
          }
        }
      } catch (IOException e) {
        System.out.println("Falha ao carregar de: " + path);
      }
    }
    
    if (!loaded) {
      System.err.println("ERRO: Não foi possível carregar sprite do inimigo de nenhum caminho!");
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
    if (!alive) return;
    
    this.target = player;
    
    // Atualizar cooldown de ataque
    if (attackCooldown > 0) {
      attackCooldown--;
    }
    
    // IA básica
    updateAI();
    
    // Atualizar posição com verificação de colisão
    updatePosition();
  }
  
  /**
   * IA básica do inimigo
   */
  protected void updateAI() {
    if (target == null) return;
    
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
   * Move o inimigo em direção ao jogador
   */
  protected void moveTowardsPlayer() {
    if (target == null) return;
    
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
    if (target == null) return Double.MAX_VALUE;
    
    double deltaX = target.getX() - x;
    double deltaY = target.getY() - y;
    return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
  }
  
  /**
   * Recebe dano
   */
  public void takeDamage(int damage) {
    currentHealth -= damage;
    if (currentHealth <= 0) {
      die();
    }
    
    // Ficar agressivo quando receber dano
    aggressive = true;
  }
  
  /**
   * Morre e concede experiência
   */
  protected void die() {
    alive = false;
    // TODO: Implementar sistema de experiência
    System.out.println("Inimigo morreu! XP: " + experienceReward);
  }
  
  /**
   * Renderiza o inimigo na tela
   */
  public void render(Graphics2D g, Camera camera) {
    if (!alive) return;
    
    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());
    
    // Desenhar sprite
    if (sprite != null) {
      g.drawImage(sprite, screenX, screenY, null);
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
  public double getX() { return x; }
  public double getY() { return y; }
  public int getWidth() { return width; }
  public int getHeight() { return height; }
  public boolean isAlive() { return alive; }
  public int getDamage() { return damage; }
  public int getExperienceReward() { return experienceReward; }
  
  /**
   * Define o mapa de tiles para verificação de colisão
   */
  public void setTileMap(TileMap tileMap) {
    this.tileMap = tileMap;
  }
  
  /**
   * Atualiza a posição do inimigo com verificação de colisão
   */
  private void updatePosition() {
    if (tileMap == null) {
      // Se não há mapa, mover sem restrições
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
    if (tileMap == null) return true;
    
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