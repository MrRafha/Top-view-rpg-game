package com.rpggame.systems;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import com.rpggame.entities.*;
import com.rpggame.world.*;
import com.rpggame.core.GamePanel;

/**
 * Gerenciador de inimigos do jogo.
 */
public class EnemyManager {
  private ArrayList<Enemy> enemies;
  private ArrayList<GoblinFamily> goblinFamilies;
  private ArrayList<Structure> structures;
  private Player player;
  private TileMap tileMap;
  private Random random;

  // Controle de população
  private static final int MIN_ENEMIES = 1;
  private static final int MAX_ENEMIES = 4;
  private int respawnTimer = 0;
  private static final int RESPAWN_DELAY = 300; // 5 segundos a 60 FPS
  
  // Sistema de famílias
  private static final int MAX_FAMILIES = 3;
  private boolean familiesInitialized = false;

  /**
   * Construtor do EnemyManager.
   */
  public EnemyManager(Player player, TileMap tileMap) {
    this.enemies = new ArrayList<>();
    this.goblinFamilies = new ArrayList<>();
    this.structures = new ArrayList<>();
    this.player = player;
    this.tileMap = tileMap;
    this.random = new Random();
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
    if (player == null)
      return false;

    double distance = Math.sqrt(
        Math.pow(player.getX() - x, 2) +
            Math.pow(player.getY() - y, 2));

    return distance < 150; // Mínimo 150 pixels de distância
  }

  /**
   * Atualiza todos os inimigos.
   */
  public void update() {
    // Atualizar lista de goblins para guerra
    updateGoblinWarLists();
    
    Iterator<Enemy> iterator = enemies.iterator();
    while (iterator.hasNext()) {
      Enemy enemy = iterator.next();

      if (enemy.isAlive()) {
        enemy.update(player);
      } else {
        // Remove inimigos mortos
        iterator.remove();
        System.out.println("Inimigo removido da lista");
        
        // Se for um goblin, remover da família
        if (enemy instanceof Goblin) {
          Goblin goblin = (Goblin) enemy;
          GoblinFamily family = goblin.getFamily();
          if (family != null) {
            boolean familyDefeated = family.removeMember(goblin);
            if (familyDefeated) {
              handleFamilyDefeated(family);
            }
          }
        }
      }
    }

    // Sistema de respawn automático
    manageEnemyPopulation();
  }

  /**
   * Gerencia a população de inimigos no mapa.
   */
  private void manageEnemyPopulation() {
    // Se o sistema de famílias está ativo, não fazer respawn automático
    if (familiesInitialized) {
      return;
    }
    
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
   * Renderiza cones de visão dos goblins (debug)
   */
  public void renderVisionCones(Graphics2D g, Camera camera) {
    for (Enemy enemy : enemies) {
      if (enemy instanceof Goblin && enemy.isAlive()) {
        ((Goblin) enemy).renderVisionCone(g, camera);
      }
    }
  }
  
  /**
   * Renderiza efeitos visuais de ataque dos goblins
   */
  public void renderAttackEffects(Graphics2D g, Camera camera) {
    for (Enemy enemy : enemies) {
      if (enemy instanceof Goblin && enemy.isAlive()) {
        ((Goblin) enemy).renderAttackEffects(g, camera);
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
    int initialCount = MIN_ENEMIES + (int) (Math.random() * (MAX_ENEMIES - MIN_ENEMIES + 1));

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
  
  /**
   * Inicializa sistema de famílias de goblins
   */
  public void initializeGoblinFamilies(TileMap tileMap) {
    if (familiesInitialized) return;
    
    System.out.println("Inicializando famílias de goblins...");
    
    // Limpar inimigos existentes
    enemies.clear();
    
    // Encontrar posições para cabanas
    ArrayList<Point> hutPositions = findGoodHutPositions(tileMap, MAX_FAMILIES);
    System.out.println("Posições encontradas para cabanas: " + hutPositions.size());
    
    // Criar famílias
    for (int i = 0; i < hutPositions.size(); i++) {
      Point hutPos = hutPositions.get(i);
      
      // Criar cabana
      Structure hut = new Structure(hutPos.x, hutPos.y, "GoblinHut", "sprites/goblinHut.png");
      structures.add(hut);
      
      // Criar família
      String familyName = getFamilyName(i);
      GoblinFamily family = new GoblinFamily(hutPos, familyName);
      goblinFamilies.add(family);
      
      // Spawnar membros da família
      spawnFamilyMembers(family, tileMap);
      
      System.out.println("Família " + familyName + " criada em (" + hutPos.x + ", " + hutPos.y + ")");
    }
    
    // Configurar guerras entre famílias (chance aleatória)
    setupFamilyWars();
    
    familiesInitialized = true;
    System.out.println("Sistema de famílias de goblins inicializado!");
  }
  
  /**
   * Encontra boas posições para cabanas de goblins
   */
  private ArrayList<Point> findGoodHutPositions(TileMap tileMap, int count) {
    ArrayList<Point> positions = new ArrayList<>();
    int attempts = 0;
    int maxAttempts = count * 20;
    
    System.out.println("Procurando " + count + " posições em mapa " + tileMap.getWidth() + "x" + tileMap.getHeight());
    
    while (positions.size() < count && attempts < maxAttempts) {
      // Posição aleatória alinhada com tiles (evitando bordas)
      int tileX = 2 + random.nextInt(tileMap.getWidth() - 6); // 2 tiles de margem de cada lado
      int tileY = 2 + random.nextInt(tileMap.getHeight() - 6);
      
      // Converter para coordenadas de pixel (canto superior esquerdo do tile)
      int x = tileX * 48;
      int y = tileY * 48;
      
      // Verificar se é uma boa posição
      if (isGoodHutPosition(x, y, positions, tileMap)) {
        positions.add(new Point(x, y));
        System.out.println("Posição válida encontrada: tile (" + tileX + ", " + tileY + 
                          ") pixel (" + x + ", " + y + ")");
      }
      attempts++;
    }
    
    System.out.println("Tentativas: " + attempts + "/" + maxAttempts);
    
    return positions;
  }
  
  /**
   * Verifica se uma posição é boa para uma cabana
   */
  private boolean isGoodHutPosition(int x, int y, ArrayList<Point> existingPositions, TileMap tileMap) {
    // Verificar distância de outras cabanas (mínimo 400 pixels para territórios distantes)
    for (Point existing : existingPositions) {
      double distance = Math.sqrt(Math.pow(x - existing.x, 2) + Math.pow(y - existing.y, 2));
      if (distance < 400) {
        return false;
      }
    }
    
    // Verificar se não está muito perto do player spawn (centro do mapa)
    int centerX = (tileMap.getWidth() * 48) / 2;
    int centerY = (tileMap.getHeight() * 48) / 2;
    double distanceToCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
    if (distanceToCenter < 200) {
      return false;
    }
    
    // Verificar se o tile central da cabana (64x64px = ~1.3 tiles) é de grama
    int tileX = x / 48;
    int tileY = y / 48;
    
    // Verificar o tile principal onde a cabana será colocada
    if (tileX >= 0 && tileX < tileMap.getWidth() && 
        tileY >= 0 && tileY < tileMap.getHeight()) {
      return tileMap.getTileAt(tileX, tileY) == TileType.GRASS;
    }
    
    return false; // Fora dos limites
  }
  
  /**
   * Spawna membros de uma família
   */
  private void spawnFamilyMembers(GoblinFamily family, TileMap tileMap) {
    Point hutPos = family.getHutPosition();
    
    // Definir composição da família
    GoblinPersonality[] familyComposition = {
      GoblinPersonality.LEADER,
      GoblinPersonality.AGGRESSIVE,
      GoblinPersonality.COMMON,
      GoblinPersonality.TIMID
    };
    
    // Spawnar cada membro ao redor da cabana
    for (int i = 0; i < 4; i++) {
      Point spawnPos = findValidGrassSpawnPosition(hutPos, tileMap);
      
      // Criar goblin com personalidade específica
      Goblin goblin = new Goblin(spawnPos.x, spawnPos.y, familyComposition[i]);
      family.addMember(goblin);
      addEnemy(goblin);
      
      System.out.println("  " + familyComposition[i] + " spawnou em tile (" + 
                        (spawnPos.x/48) + ", " + (spawnPos.y/48) + ") centro: (" + 
                        spawnPos.x + ", " + spawnPos.y + ")");
    }
  }
  
  /**
   * Encontra uma posição válida de tile de grama ao redor da cabana
   */
  private Point findValidGrassSpawnPosition(Point hutPos, TileMap tileMap) {
    int maxAttempts = 50;
    int attempts = 0;
    
    while (attempts < maxAttempts) {
      // Calcular posição ao redor da cabana
      double angle = random.nextDouble() * 2 * Math.PI;
      int radiusTiles = 2 + random.nextInt(4); // 2-5 tiles de distância
      
      // Converter para coordenadas de tile
      int hutTileX = hutPos.x / 48;
      int hutTileY = hutPos.y / 48;
      
      int targetTileX = hutTileX + (int)(Math.cos(angle) * radiusTiles);
      int targetTileY = hutTileY + (int)(Math.sin(angle) * radiusTiles);
      
      // Verificar se está dentro dos limites do mapa
      if (targetTileX >= 0 && targetTileX < tileMap.getWidth() && 
          targetTileY >= 0 && targetTileY < tileMap.getHeight()) {
        
        // Verificar se é tile de grama
        if (tileMap.getTileAt(targetTileX, targetTileY) == TileType.GRASS) {
          // Calcular posição central do tile
          int centerX = (targetTileX * 48) + 24; // Centro do tile (48/2 = 24)
          int centerY = (targetTileY * 48) + 24;
          
          return new Point(centerX, centerY);
        }
      }
      
      attempts++;
    }
    
    // Fallback: usar posição da cabana se não encontrar tile válido
    System.out.println("  Aviso: Não foi possível encontrar tile de grama válido, usando posição da cabana");
    return new Point(hutPos.x + 48, hutPos.y + 48);
  }
  
  /**
   * Configura guerras entre famílias
   */
  private void setupFamilyWars() {
    for (int i = 0; i < goblinFamilies.size(); i++) {
      for (int j = i + 1; j < goblinFamilies.size(); j++) {
        // 30% chance de guerra entre duas famílias
        if (random.nextDouble() < 0.3) {
          GoblinFamily family1 = goblinFamilies.get(i);
          GoblinFamily family2 = goblinFamilies.get(j);
          family1.declareWarAgainst(family2);
          System.out.println("Guerra declarada entre " + family1.getFamilyName() + 
                           " e " + family2.getFamilyName());
        }
      }
    }
  }
  
  /**
   * Gera nome para família
   */
  private String getFamilyName(int index) {
    String[] names = {
      "Clã Pedra Negra",
      "Tribo Dente Afiado", 
      "Família Garra Suja",
      "Bando Olho Vermelho",
      "Clã Sombra Verde"
    };
    
    if (index < names.length) {
      return names[index];
    }
    return "Família " + (index + 1);
  }
  
  /**
   * Renderiza estruturas
   */
  public void renderStructures(Graphics2D g, Camera camera) {
    for (Structure structure : structures) {
      structure.render(g, camera);
    }
  }
  
  /**
   * Retorna famílias de goblins
   */
  public ArrayList<GoblinFamily> getGoblinFamilies() {
    return new ArrayList<>(goblinFamilies);
  }
  
  /**
   * Retorna estruturas para verificação de ataques
   */
  public ArrayList<Structure> getStructures() {
    return new ArrayList<>(structures);
  }
  
  /**
   * Lida com família derrotada - torna a cabana vulnerável
   */
  private void handleFamilyDefeated(GoblinFamily family) {
    System.out.println("🏴 " + family.getFamilyName() + " foi completamente derrotada!");
    
    // Encontrar a cabana desta família e torná-la vulnerável
    Point hutPos = family.getHutPosition();
    for (Structure structure : structures) {
      if (structure.getX() == hutPos.x && structure.getY() == hutPos.y) {
        structure.makeVulnerable();
        break;
      }
    }
  }
  
  /**
   * Atualiza lista de goblins para cada goblin (para sistema de guerra)
   */
  private void updateGoblinWarLists() {
    java.util.List<Goblin> allGoblins = new java.util.ArrayList<>();
    
    // Coletar todos os goblins vivos
    for (Enemy enemy : enemies) {
      if (enemy instanceof Goblin && enemy.isAlive()) {
        allGoblins.add((Goblin) enemy);
      }
    }
    
    // Passar a lista para cada goblin
    for (Goblin goblin : allGoblins) {
      goblin.setAllGoblins(allGoblins);
    }
  }
}