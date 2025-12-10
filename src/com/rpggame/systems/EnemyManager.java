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
  private GoblinCouncil goblinCouncil;

  // Controle de populaÃ§Ã£o
  private static final int MIN_ENEMIES = 1;
  private static final int MAX_ENEMIES = 4;
  private int respawnTimer = 0;
  private static final int RESPAWN_DELAY = 300; // 5 segundos a 60 FPS
  
  // Sistema de famÃ­lias
  private static final int MAX_FAMILIES = 3;
  private boolean familiesInitialized = false;
  
  // Sistema de respawn de famÃ­lias
  private java.util.Set<String> usedFamilyNames;
  private int familyRespawnTimer = 0;
  private static final int FAMILY_RESPAWN_DELAY = 3600; // 1 minuto (60fps * 60s)

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
    this.goblinCouncil = new GoblinCouncil();
    this.usedFamilyNames = new java.util.HashSet<>();
  }

  /**
   * Adiciona um inimigo Ã  lista.
   */
  public void addEnemy(Enemy enemy) {
    enemy.setTileMap(tileMap);
    enemies.add(enemy);
  }

  /**
   * Spawn de Goblins em posiÃ§Ãµes especÃ­ficas.
   */
  public void spawnGoblin(double x, double y) {
    Goblin goblin = new Goblin(x, y);
    addEnemy(goblin);
    System.out.println("Goblin spawnou em: (" + x + ", " + y + ")");
  }

  /**
   * Spawn de Goblin em posiÃ§Ã£o aleatÃ³ria de grama.
   */
  public void spawnGoblinOnGrass(TileMap tileMap) {
    Point grassPosition;
    int attempts = 0;

    do {
      // Usar spawn centrado considerando tamanho do Goblin (32x32 pixels)
      grassPosition = tileMap.getCenteredGrassPosition(32, 32);
      attempts++;
    } while (isTooCloseToPlayer(grassPosition.x, grassPosition.y) && attempts < 10);

    // Se apÃ³s 10 tentativas ainda estÃ¡ perto, spawnar mesmo assim
    spawnGoblin(grassPosition.x, grassPosition.y);
  }

  /**
   * Verifica se uma posiÃ§Ã£o estÃ¡ muito perto do jogador.
   */
  private boolean isTooCloseToPlayer(double x, double y) {
    if (player == null)
      return false;

    double distance = Math.sqrt(
        Math.pow(player.getX() - x, 2) +
            Math.pow(player.getY() - y, 2));

    return distance < 150; // MÃ­nimo 150 pixels de distÃ¢ncia
  }

  /**
   * Atualiza todos os inimigos.
   */
  public void update() {
    // Atualizar conselho goblin
    goblinCouncil.update();
    
    // Verificar se Ã© hora de convocar reuniÃ£o
    if (goblinCouncil.shouldConveneCouncil(goblinFamilies)) {
      goblinCouncil.conveneCouncil(goblinFamilies);
    }
    
    // Atualizar timer de respawn de famÃ­lias
    if (familyRespawnTimer > 0) {
      familyRespawnTimer--;
      
      // Debug: mostrar tempo restante a cada 60 frames (1 segundo)
      if (familyRespawnTimer % 60 == 0) {
        int secondsRemaining = familyRespawnTimer / 60;
        System.out.println("â±ï¸ Nova famÃ­lia em " + secondsRemaining + " segundos... (FamÃ­lias atuais: " + goblinFamilies.size() + "/" + MAX_FAMILIES + ")");
      }
      
      if (familyRespawnTimer == 0 && goblinFamilies.size() < MAX_FAMILIES) {
        System.out.println("ðŸŽ¯ Timer zerou! Chamando spawnNewFamily()...");
        spawnNewFamily();
      }
    }
    
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
        
        // Se for um goblin, remover da famÃ­lia
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

    // Sistema de respawn automÃ¡tico
    manageEnemyPopulation();
  }

  /**
   * Gerencia a populaÃ§Ã£o de inimigos no mapa.
   */
  private void manageEnemyPopulation() {
    // Se o sistema de famÃ­lias estÃ¡ ativo, nÃ£o fazer respawn automÃ¡tico
    if (familiesInitialized) {
      return;
    }
    
    int currentCount = getAliveCount();

    // Se tem menos que o mÃ­nimo, fazer respawn imediato
    if (currentCount < MIN_ENEMIES) {
      spawnGoblinOnGrass(tileMap);
      respawnTimer = RESPAWN_DELAY; // Reset timer apÃ³s spawn
      System.out.println("Respawn imediato! Inimigos: " + (currentCount + 1));
      return;
    }

    // Se tem menos que o mÃ¡ximo, considerar respawn apÃ³s delay
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
      // Se jÃ¡ tem o mÃ¡ximo, nÃ£o spawnar mais
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
   * Renderiza apenas inimigos visÃ­veis pelo jogador.
   */
  public void render(Graphics2D g, Camera camera, FogOfWar fogOfWar) {
    for (Enemy enemy : enemies) {
      if (enemy.isAlive() && isEnemyVisible(enemy, fogOfWar)) {
        enemy.render(g, camera);
      }
    }
  }

  /**
   * Renderiza cones de visÃ£o dos goblins (debug)
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
   * Verifica se um inimigo estÃ¡ visÃ­vel pelo jogador
   */
  private boolean isEnemyVisible(Enemy enemy, FogOfWar fogOfWar) {
    if (fogOfWar == null)
      return true;

    // Calcular posiÃ§Ã£o do inimigo em tiles
    int enemyTileX = (int) (enemy.getX() / GamePanel.TILE_SIZE);
    int enemyTileY = (int) (enemy.getY() / GamePanel.TILE_SIZE);

    // Verificar se o tile do inimigo estÃ¡ visÃ­vel
    return fogOfWar.isVisible(enemyTileX, enemyTileY);
  }

  /**
   * Verifica colisÃ£o dos projÃ©teis do jogador com inimigos.
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

          // Remove projÃ©til
          projIterator.remove();

          System.out.println("ProjÃ©til atingiu inimigo!");
          break; // ProjÃ©til sÃ³ pode atingir um inimigo
        }
      }
    }
  }

  /**
   * Verifica colisÃ£o dos inimigos com o jogador.
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
          pushX = (pushX / distance) * 20; // forÃ§a do empurrÃ£o
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
    // Spawnar alguns Goblins para teste (posiÃ§Ãµes fixas temporÃ¡rias)
    spawnGoblin(200, 150);
    spawnGoblin(300, 200);
    spawnGoblin(150, 300);
  }

  /**
   * Spawna inimigos iniciais em posiÃ§Ãµes de grama vÃ¡lidas.
   */
  public void spawnInitialEnemies(TileMap tileMap) {
    // Spawnar nÃºmero inicial de Goblins (entre MIN e MAX)
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
   * Retorna o nÃºmero de inimigos vivos.
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
   * Inicializa sistema de famÃ­lias de goblins
   */
  public void initializeGoblinFamilies(TileMap tileMap) {
    if (familiesInitialized) return;
    
    System.out.println("Inicializando famÃ­lias de goblins...");
    
    // Limpar inimigos existentes
    enemies.clear();
    
    // Encontrar posiÃ§Ãµes para cabanas
    ArrayList<Point> hutPositions = findGoodHutPositions(tileMap, MAX_FAMILIES);
    System.out.println("PosiÃ§Ãµes encontradas para cabanas: " + hutPositions.size());
    
    // Criar famÃ­lias
    for (int i = 0; i < hutPositions.size(); i++) {
      Point hutPos = hutPositions.get(i);
      
      // Criar cabana
      Structure hut = new Structure(hutPos.x, hutPos.y, "GoblinHut", "sprites/goblinHut.png");
      structures.add(hut);
      
      // Criar famÃ­lia
      String familyName = getFamilyName(i);
      GoblinFamily family = new GoblinFamily(hutPos, familyName);
      goblinFamilies.add(family);
      
      // Spawnar membros da famÃ­lia
      spawnFamilyMembers(family, tileMap);
      
      System.out.println("FamÃ­lia " + familyName + " criada em (" + hutPos.x + ", " + hutPos.y + ")");
    }
    
    // Configurar guerras entre famÃ­lias (chance aleatÃ³ria)
    setupFamilyWars();
    
    familiesInitialized = true;
    System.out.println("Sistema de famÃ­lias de goblins inicializado!");
  }
  
  /**
   * Encontra boas posiÃ§Ãµes para cabanas de goblins
   */
  private ArrayList<Point> findGoodHutPositions(TileMap tileMap, int count) {
    ArrayList<Point> positions = new ArrayList<>();
    int attempts = 0;
    int maxAttempts = count * 20;
    
    System.out.println("Procurando " + count + " posiÃ§Ãµes em mapa " + tileMap.getWidth() + "x" + tileMap.getHeight());
    
    while (positions.size() < count && attempts < maxAttempts) {
      // PosiÃ§Ã£o aleatÃ³ria alinhada com tiles (evitando bordas)
      int tileX = 2 + random.nextInt(tileMap.getWidth() - 6); // 2 tiles de margem de cada lado
      int tileY = 2 + random.nextInt(tileMap.getHeight() - 6);
      
      // Converter para coordenadas de pixel (canto superior esquerdo do tile)
      int x = tileX * 48;
      int y = tileY * 48;
      
      // Verificar se Ã© uma boa posiÃ§Ã£o
      if (isGoodHutPosition(x, y, positions, tileMap)) {
        positions.add(new Point(x, y));
        System.out.println("PosiÃ§Ã£o vÃ¡lida encontrada: tile (" + tileX + ", " + tileY + 
                          ") pixel (" + x + ", " + y + ")");
      }
      attempts++;
    }
    
    System.out.println("Tentativas: " + attempts + "/" + maxAttempts);
    
    return positions;
  }
  
  /**
   * Verifica se uma posiÃ§Ã£o Ã© boa para uma cabana
   */
  private boolean isGoodHutPosition(int x, int y, ArrayList<Point> existingPositions, TileMap tileMap) {
    // Verificar distÃ¢ncia de outras cabanas (mÃ­nimo 400 pixels para territÃ³rios distantes)
    for (Point existing : existingPositions) {
      double distance = Math.sqrt(Math.pow(x - existing.x, 2) + Math.pow(y - existing.y, 2));
      if (distance < 400) {
        return false;
      }
    }
    
    // Verificar se nÃ£o estÃ¡ muito perto do player spawn (centro do mapa)
    int centerX = (tileMap.getWidth() * 48) / 2;
    int centerY = (tileMap.getHeight() * 48) / 2;
    double distanceToCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
    if (distanceToCenter < 200) {
      return false;
    }
    
    // Verificar se o tile central da cabana (64x64px = ~1.3 tiles) Ã© de grama
    int tileX = x / 48;
    int tileY = y / 48;
    
    // Verificar o tile principal onde a cabana serÃ¡ colocada
    if (tileX >= 0 && tileX < tileMap.getWidth() && 
        tileY >= 0 && tileY < tileMap.getHeight()) {
      return tileMap.getTileAt(tileX, tileY) == TileType.GRASS;
    }
    
    return false; // Fora dos limites
  }
  
  /**
   * Spawna membros de uma famÃ­lia
   */
  private void spawnFamilyMembers(GoblinFamily family, TileMap tileMap) {
    Point hutPos = family.getHutPosition();
    
    // Definir composiÃ§Ã£o da famÃ­lia
    GoblinPersonality[] familyComposition = {
      GoblinPersonality.LEADER,
      GoblinPersonality.AGGRESSIVE,
      GoblinPersonality.COMMON,
      GoblinPersonality.TIMID
    };
    
    // Spawnar cada membro ao redor da cabana
    for (int i = 0; i < 4; i++) {
      Point spawnPos = findValidGrassSpawnPosition(hutPos, tileMap);
      
      // Criar goblin com personalidade especÃ­fica
      Goblin goblin = new Goblin(spawnPos.x, spawnPos.y, familyComposition[i]);
      family.addMember(goblin);
      addEnemy(goblin);
      
      System.out.println("  " + familyComposition[i] + " spawnou em tile (" + 
                        (spawnPos.x/48) + ", " + (spawnPos.y/48) + ") centro: (" + 
                        spawnPos.x + ", " + spawnPos.y + ")");
    }
  }
  
  /**
   * Encontra uma posiÃ§Ã£o vÃ¡lida de tile de grama ao redor da cabana
   */
  private Point findValidGrassSpawnPosition(Point hutPos, TileMap tileMap) {
    int maxAttempts = 50;
    int attempts = 0;
    
    while (attempts < maxAttempts) {
      // Calcular posiÃ§Ã£o ao redor da cabana
      double angle = random.nextDouble() * 2 * Math.PI;
      int radiusTiles = 2 + random.nextInt(4); // 2-5 tiles de distÃ¢ncia
      
      // Converter para coordenadas de tile
      int hutTileX = hutPos.x / 48;
      int hutTileY = hutPos.y / 48;
      
      int targetTileX = hutTileX + (int)(Math.cos(angle) * radiusTiles);
      int targetTileY = hutTileY + (int)(Math.sin(angle) * radiusTiles);
      
      // Verificar se estÃ¡ dentro dos limites do mapa
      if (targetTileX >= 0 && targetTileX < tileMap.getWidth() && 
          targetTileY >= 0 && targetTileY < tileMap.getHeight()) {
        
        // Verificar se Ã© tile de grama
        if (tileMap.getTileAt(targetTileX, targetTileY) == TileType.GRASS) {
          // Calcular posiÃ§Ã£o central do tile
          int centerX = (targetTileX * 48) + 24; // Centro do tile (48/2 = 24)
          int centerY = (targetTileY * 48) + 24;
          
          return new Point(centerX, centerY);
        }
      }
      
      attempts++;
    }
    
    // Fallback: usar posiÃ§Ã£o da cabana se nÃ£o encontrar tile vÃ¡lido
    System.out.println("  Aviso: NÃ£o foi possÃ­vel encontrar tile de grama vÃ¡lido, usando posiÃ§Ã£o da cabana");
    return new Point(hutPos.x + 48, hutPos.y + 48);
  }
  
  /**
   * Configura guerras entre famÃ­lias
   */
  private void setupFamilyWars() {
    for (int i = 0; i < goblinFamilies.size(); i++) {
      for (int j = i + 1; j < goblinFamilies.size(); j++) {
        // 30% chance de guerra entre duas famÃ­lias
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
   * Spawna uma nova famÃ­lia apÃ³s uma ser derrotada
   */
  private void spawnNewFamily() {
    System.out.println("\nðŸ”” spawnNewFamily() CHAMADO! FamÃ­lias atuais: " + goblinFamilies.size() + "/" + MAX_FAMILIES);
    
    if (goblinFamilies.size() >= MAX_FAMILIES) {
      System.out.println("âŒ JÃ¡ temos " + MAX_FAMILIES + " famÃ­lias. Cancelando spawn.");
      return;
    }
    
    System.out.println("\nðŸ†• ===== NOVA FAMÃLIA GOBLIN CHEGANDO =====");
    
    // Encontrar posiÃ§Ã£o para nova cabana
    ArrayList<Point> existingPositions = new ArrayList<>();
    for (GoblinFamily family : goblinFamilies) {
      existingPositions.add(family.getHutPosition());
    }
    
    ArrayList<Point> newHutPositions = findGoodHutPositions(tileMap, 1);
    
    if (newHutPositions.isEmpty()) {
      System.out.println("âš ï¸ NÃ£o foi possÃ­vel encontrar posiÃ§Ã£o vÃ¡lida para nova famÃ­lia");
      familyRespawnTimer = 600; // Tentar novamente em 10 segundos
      return;
    }
    
    Point hutPos = newHutPositions.get(0);
    
    // Criar cabana
    Structure hut = new Structure(hutPos.x, hutPos.y, "GoblinHut", "sprites/goblinHut.png");
    structures.add(hut);
    
    // Criar famÃ­lia com nome Ãºnico
    String familyName = getFamilyName(goblinFamilies.size());
    GoblinFamily family = new GoblinFamily(hutPos, familyName);
    goblinFamilies.add(family);
    
    // Spawnar membros da famÃ­lia
    spawnFamilyMembers(family, tileMap);
    
    System.out.println("ðŸ•ï¸ " + familyName + " estabeleceu territÃ³rio em (" + hutPos.x + ", " + hutPos.y + ")");
    System.out.println("==========================================\n");
    
    // Pequena chance de comeÃ§ar em guerra com famÃ­lia existente (20%)
    if (!goblinFamilies.isEmpty() && random.nextDouble() < 0.2) {
      GoblinFamily enemy = goblinFamilies.get(random.nextInt(goblinFamilies.size()));
      if (enemy != family) {
        family.declareWarAgainst(enemy);
        System.out.println("âš”ï¸ " + familyName + " jÃ¡ chegou em conflito com " + enemy.getFamilyName() + "!");
      }
    }
  }
  
  /**
   * Gera nome para famÃ­lia de forma aleatÃ³ria sem repetiÃ§Ã£o
   */
  private String getFamilyName(int index) {
    String[] allNames = {
      "ClÃ£ Pedra Negra",
      "Tribo Dente Afiado", 
      "FamÃ­lia Garra Suja",
      "Bando Olho Vermelho",
      "ClÃ£ Sombra Verde",
      "Horda Osso Quebrado",
      "Tribo Sangue Podre",
      "ClÃ£ Veneno Noturno",
      "Bando Fogo Negro",
      "FamÃ­lia LÃ¢mina Enferrujada",
      "Tribo CranÃªo Rachado",
      "ClÃ£ LÃ­ngua Venenosa",
      "Horda Grito Selvagem",
      "Bando Lua Sangrenta",
      "FamÃ­lia Espinho Negro",
      "Tribo PÃ¢ntano Escuro",
      "ClÃ£ Chifre Retorcido",
      "Horda Presa Afiada",
      "Bando Cinza Sombria",
      "FamÃ­lia Caverna Profunda"
    };
    
    // Tentar encontrar um nome nÃ£o usado
    java.util.List<String> availableNames = new java.util.ArrayList<>();
    for (String name : allNames) {
      if (!usedFamilyNames.contains(name)) {
        availableNames.add(name);
      }
    }
    
    // Se todos os nomes foram usados, resetar a lista
    if (availableNames.isEmpty()) {
      usedFamilyNames.clear();
      for (String name : allNames) {
        availableNames.add(name);
      }
    }
    
    // Escolher nome aleatÃ³rio da lista disponÃ­vel
    String chosenName = availableNames.get(random.nextInt(availableNames.size()));
    usedFamilyNames.add(chosenName);
    return chosenName;
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
   * Retorna famÃ­lias de goblins
   */
  public ArrayList<GoblinFamily> getGoblinFamilies() {
    return new ArrayList<>(goblinFamilies);
  }
  
  /**
   * Retorna estruturas para verificaÃ§Ã£o de ataques
   */
  public ArrayList<Structure> getStructures() {
    return new ArrayList<>(structures);
  }
  
  /**
   * Retorna o conselho goblin
   */
  public GoblinCouncil getGoblinCouncil() {
    return goblinCouncil;
  }
  
  /**
   * Callback quando uma estrutura Ã© destruÃ­da pelo player
   */
  public void onStructureDestroyed(Structure structure) {
    Point structurePos = new Point((int)structure.getX(), (int)structure.getY());
    
    // Procurar qual famÃ­lia tinha cabana nesta posiÃ§Ã£o
    for (GoblinFamily family : new java.util.ArrayList<>(goblinFamilies)) {
      Point hutPos = family.getHutPosition();
      if (hutPos.x == structurePos.x && hutPos.y == structurePos.y) {
        System.out.println("ðŸšï¸ Cabana de " + family.getFamilyName() + " foi destruÃ­da pelo jogador!");
        
        // Matar todos os goblins da famÃ­lia
        java.util.List<com.rpggame.entities.Goblin> familyMembers = new java.util.ArrayList<>();
        for (Enemy enemy : enemies) {
          if (enemy instanceof com.rpggame.entities.Goblin) {
            com.rpggame.entities.Goblin goblin = (com.rpggame.entities.Goblin) enemy;
            if (goblin.getFamily() == family) {
              familyMembers.add(goblin);
            }
          }
        }
        
        // Remover goblins da famÃ­lia
        for (com.rpggame.entities.Goblin goblin : familyMembers) {
          goblin.takeDamage(9999); // Matar instantaneamente
        }
        
        // Chamar handleFamilyDefeated
        handleFamilyDefeated(family);
        break;
      }
    }
  }
  
  /**
   * Lida com famÃ­lia derrotada - torna a cabana vulnerÃ¡vel
   */
  private void handleFamilyDefeated(GoblinFamily family) {
    System.out.println("ðŸ´ " + family.getFamilyName() + " foi completamente derrotada!");
    
    // Notificar o conselho goblin
    goblinCouncil.registerFamilyDestroyed();
    
    // Remover famÃ­lia da lista
    goblinFamilies.remove(family);
    
    // Iniciar timer de respawn de nova famÃ­lia (3 minutos)
    if (goblinFamilies.size() < MAX_FAMILIES) {
      familyRespawnTimer = FAMILY_RESPAWN_DELAY;
      System.out.println("â³ Nova famÃ­lia goblin aparecerÃ¡ em 3 minutos...");
    }
    
    // Encontrar a cabana desta famÃ­lia e tornÃ¡-la vulnerÃ¡vel (se ainda nÃ£o foi destruÃ­da)
    Point hutPos = family.getHutPosition();
    for (Structure structure : structures) {
      if (structure.getX() == hutPos.x && structure.getY() == hutPos.y && !structure.isDestroyed()) {
        structure.makeVulnerable();
        System.out.println("ðŸšï¸ A cabana de " + family.getFamilyName() + " agora estÃ¡ vulnerÃ¡vel!");
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
    
    // Passar a lista e o conselho para cada goblin
    for (Goblin goblin : allGoblins) {
      goblin.setAllGoblins(allGoblins);
      goblin.setGoblinCouncil(goblinCouncil);
    }
  }
  
  /**
   * Verifica se hÃ¡ linha de visÃ£o entre dois pontos (sem paredes no caminho)
   */
  private boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
    int tileX1 = (int)(x1 / GamePanel.TILE_SIZE);
    int tileY1 = (int)(y1 / GamePanel.TILE_SIZE);
    int tileX2 = (int)(x2 / GamePanel.TILE_SIZE);
    int tileY2 = (int)(y2 / GamePanel.TILE_SIZE);
    
    // Algoritmo de Bresenham para traÃ§ar linha entre os pontos
    int dx = Math.abs(tileX2 - tileX1);
    int dy = Math.abs(tileY2 - tileY1);
    int sx = tileX1 < tileX2 ? 1 : -1;
    int sy = tileY1 < tileY2 ? 1 : -1;
    int err = dx - dy;
    int x = tileX1;
    int y = tileY1;
    
    while (true) {
      // Verificar se o tile atual Ã© uma parede (exceto origem e destino)
      if ((x != tileX1 || y != tileY1) && (x != tileX2 || y != tileY2)) {
        if (!tileMap.isWalkable(x, y)) {
          return false; // HÃ¡ uma parede no caminho
        }
      }
      
      if (x == tileX2 && y == tileY2) {
        break; // Chegou ao destino
      }
      
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        x += sx;
      }
      if (e2 < dx) {
        err += dx;
        y += sy;
      }
    }
    
    return true; // Linha de visÃ£o clara
  }
  
  /**
   * Limpa todos os inimigos para troca de mapa
   */
  public void clearAllEnemies() {
    enemies.clear();
    goblinFamilies.clear();
    familiesInitialized = false;
    System.out.println("Todos os inimigos foram removidos");
  }
}
