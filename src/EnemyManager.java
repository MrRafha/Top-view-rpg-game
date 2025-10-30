import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Gerenciador de inimigos do jogo.
 */
public class EnemyManager {
  private ArrayList<Enemy> enemies;
  private Player player;
  private TileMap tileMap;
  
  // Controle de população
  private static final int MIN_ENEMIES = 1;
  private static final int MAX_ENEMIES = 4;
  private int respawnTimer = 0;
  private static final int RESPAWN_DELAY = 300; // 5 segundos a 60 FPS

  /**
   * Construtor do EnemyManager.
   */
  public EnemyManager(Player player, TileMap tileMap) {
    this.enemies = new ArrayList<>();
    this.player = player;
    this.tileMap = tileMap;
  }

  /**
   * Adiciona um inimigo à lista.
   */
  public void addEnemy(Enemy enemy) {
    enemy.setTileMap(tileMap);
    enemies.add(enemy);
  }

  /**
   * Spawn de Goblins em posições específicas.
   */
  public void spawnGoblin(double x, double y) {
    Goblin goblin = new Goblin(x, y);
    addEnemy(goblin);
    System.out.println("Goblin spawnou em: (" + x + ", " + y + ")");
  }

  /**
   * Spawn de Goblin em posição aleatória de grama.
   */
  public void spawnGoblinOnGrass(TileMap tileMap) {
    Point grassPosition;
    int attempts = 0;
    
    do {
      // Usar spawn centrado considerando tamanho do Goblin (32x32 pixels)
      grassPosition = tileMap.getCenteredGrassPosition(32, 32);
      attempts++;
    } while (isTooCloseToPlayer(grassPosition.x, grassPosition.y) && attempts < 10);
    
    // Se após 10 tentativas ainda está perto, spawnar mesmo assim
    spawnGoblin(grassPosition.x, grassPosition.y);
  }
  
  /**
   * Verifica se uma posição está muito perto do jogador.
   */
  private boolean isTooCloseToPlayer(double x, double y) {
    if (player == null) return false;
    
    double distance = Math.sqrt(
        Math.pow(player.getX() - x, 2) + 
        Math.pow(player.getY() - y, 2)
    );
    
    return distance < 150; // Mínimo 150 pixels de distância
  }

  /**
   * Atualiza todos os inimigos.
   */
  public void update() {
    Iterator<Enemy> iterator = enemies.iterator();
    while (iterator.hasNext()) {
      Enemy enemy = iterator.next();

      if (enemy.isAlive()) {
        enemy.update(player);
      } else {
        // Remove inimigos mortos
        iterator.remove();
        System.out.println("Inimigo removido da lista");
      }
    }
    
    // Sistema de respawn automático
    manageEnemyPopulation();
  }
  
  /**
   * Gerencia a população de inimigos no mapa.
   */
  private void manageEnemyPopulation() {
    int currentCount = getAliveCount();
    
    // Se tem menos que o mínimo, fazer respawn imediato
    if (currentCount < MIN_ENEMIES) {
      spawnGoblinOnGrass(tileMap);
      respawnTimer = RESPAWN_DELAY; // Reset timer após spawn
      System.out.println("Respawn imediato! Inimigos: " + (currentCount + 1));
      return;
    }
    
    // Se tem menos que o máximo, considerar respawn após delay
    if (currentCount < MAX_ENEMIES) {
      respawnTimer--;
      
      if (respawnTimer <= 0) {
        // 50% de chance de spawnar a cada cycle do timer
        if (Math.random() < 0.5) {
          spawnGoblinOnGrass(tileMap);
          System.out.println("Respawn programado! Inimigos: " + (currentCount + 1));
        }
        
        // Reset timer
        respawnTimer = RESPAWN_DELAY;
      }
    } else {
      // Se já tem o máximo, não spawnar mais
      respawnTimer = RESPAWN_DELAY;
    }
  }

  /**
   * Renderiza todos os inimigos.
   */
  public void render(Graphics2D g, Camera camera) {
    for (Enemy enemy : enemies) {
      if (enemy.isAlive()) {
        enemy.render(g, camera);
      }
    }
  }

  /**
   * Renderiza apenas inimigos visíveis pelo jogador.
   */
  public void render(Graphics2D g, Camera camera, FogOfWar fogOfWar) {
    for (Enemy enemy : enemies) {
      if (enemy.isAlive() && isEnemyVisible(enemy, fogOfWar)) {
        enemy.render(g, camera);
      }
    }
  }

  /**
   * Verifica se um inimigo está visível pelo jogador
   */
  private boolean isEnemyVisible(Enemy enemy, FogOfWar fogOfWar) {
    if (fogOfWar == null)
      return true;

    // Calcular posição do inimigo em tiles
    int enemyTileX = (int) (enemy.getX() / GamePanel.TILE_SIZE);
    int enemyTileY = (int) (enemy.getY() / GamePanel.TILE_SIZE);

    // Verificar se o tile do inimigo está visível
    return fogOfWar.isVisible(enemyTileX, enemyTileY);
  }

  /**
   * Verifica colisão dos projéteis do jogador com inimigos.
   */
  public void checkProjectileCollisions(ArrayList<Projectile> projectiles) {
    for (Enemy enemy : enemies) {
      if (!enemy.isAlive())
        continue;

      Rectangle enemyBounds = enemy.getBounds();

      Iterator<Projectile> projIterator = projectiles.iterator();
      while (projIterator.hasNext()) {
        Projectile projectile = projIterator.next();
        Rectangle projBounds = projectile.getBounds();

        if (enemyBounds.intersects(projBounds)) {
          // Dano ao inimigo
          enemy.takeDamage(projectile.getDamage());

          // Remove projétil
          projIterator.remove();

          System.out.println("Projétil atingiu inimigo!");
          break; // Projétil só pode atingir um inimigo
        }
      }
    }
  }

  /**
   * Verifica colisão dos inimigos com o jogador.
   */
  public void checkPlayerCollisions() {
    Rectangle playerBounds = new Rectangle(
        (int) player.getX(),
        (int) player.getY(),
        player.getWidth(),
        player.getHeight());

    for (Enemy enemy : enemies) {
      if (!enemy.isAlive())
        continue;

      Rectangle enemyBounds = enemy.getBounds();

      if (playerBounds.intersects(enemyBounds)) {
        // TODO: Implementar sistema de dano ao jogador
        System.out.println("Jogador colidiu com inimigo!");

        // Empurrar jogador para longe do inimigo (knockback simples)
        double pushX = player.getX() - enemy.getX();
        double pushY = player.getY() - enemy.getY();
        double distance = Math.sqrt(pushX * pushX + pushY * pushY);

        if (distance > 0) {
          pushX = (pushX / distance) * 20; // força do empurrão
          pushY = (pushY / distance) * 20;

          // TODO: Aplicar knockback ao jogador
          System.out.println("Knockback aplicado!");
        }
      }
    }
  }

  /**
   * Spawna inimigos iniciais para teste.
   */
  public void spawnInitialEnemies() {
    // Spawnar alguns Goblins para teste (posições fixas temporárias)
    spawnGoblin(200, 150);
    spawnGoblin(300, 200);
    spawnGoblin(150, 300);
  }

  /**
   * Spawna inimigos iniciais em posições de grama válidas.
   */
  public void spawnInitialEnemies(TileMap tileMap) {
    // Spawnar número inicial de Goblins (entre MIN e MAX)
    int initialCount = MIN_ENEMIES + (int)(Math.random() * (MAX_ENEMIES - MIN_ENEMIES + 1));
    
    for (int i = 0; i < initialCount; i++) {
      spawnGoblinOnGrass(tileMap);
    }
    
    // Inicializar timer de respawn
    respawnTimer = RESPAWN_DELAY;
    
    System.out.println("Inimigos iniciais spawnados: " + initialCount);
  }

  /**
   * Retorna a lista de inimigos (para debugging).
   */
  public ArrayList<Enemy> getEnemies() {
    return enemies;
  }

  /**
   * Retorna o número de inimigos vivos.
   */
  public int getAliveCount() {
    int count = 0;
    for (Enemy enemy : enemies) {
      if (enemy.isAlive()) {
        count++;
      }
    }
    return count;
  }
}