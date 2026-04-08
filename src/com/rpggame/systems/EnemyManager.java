package com.rpggame.systems;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import com.rpggame.entities.*;
import com.rpggame.enemies.Golem.Golem;
import com.rpggame.factions.FactionSystem;
import com.rpggame.server.MapSimulation;
import com.rpggame.world.TileMap;
import com.rpggame.world.TileType;
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
  private String currentMapId;
  private FactionSystem factionSystem;

  // Sistema do boss Golem
  private boolean golemSpawned = false;
  private Golem activeGolem = null;

  // Controle de população
  private static final int MIN_ENEMIES = 1;
  private static final int MAX_ENEMIES = 4;
  private int respawnTimer = 0;
  private static final int RESPAWN_DELAY = 300; // 5 segundos a 60 FPS

  // Sistema de famílias
  private static final int MAX_FAMILIES = 3;
  private boolean familiesInitialized = false;

  // Sistema de respawn de famílias
  private java.util.Set<String> usedFamilyNames;
  private int familyRespawnTimer = 0;
  private static final int FAMILY_RESPAWN_DELAY = 3600; // 1 minuto (60fps * 60s)

  // Efeitos visuais de explosão do MAGIC_BOLT — lidos pelo GamePanel no render
  public static final class ExplosionEffect {
    public final double x, y;
    public int timer;
    public static final int DURATION = 18; // ~0.3s a 60fps
    public ExplosionEffect(double x, double y) { this.x = x; this.y = y; this.timer = DURATION; }
  }
  private final ArrayList<ExplosionEffect> explosionEffects = new ArrayList<>();

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
   * Define o ID do mapa atual
   */
  public void setCurrentMapId(String mapId) {
    this.currentMapId = mapId;
  }

  /** Conecta o sistema de facções — chamar após criar o FactionSystem */
  public void setFactionSystem(FactionSystem factionSystem) {
    this.factionSystem = factionSystem;
  }

  /**
   * Adiciona um inimigo à lista.
   */
  public void addEnemy(Enemy enemy) {
    enemy.setTileMap(tileMap);
    enemy.setEnemyManager(this); // Configurar referência ao EnemyManager
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
    // Tick do sistema de facções (reputação, suspeita, raids, territórios)
    if (factionSystem != null && player != null) {
      factionSystem.tick(currentMapId, player.getX(), player.getY());
      factionSystem.onEnteredGoblinArea(currentMapId);
    }

    // Atualizar conselho goblin
    goblinCouncil.update();

    // Verificar se é hora de convocar reunião
    if (goblinCouncil.shouldConveneCouncil(goblinFamilies)) {
      GoblinCouncil.CouncilDecision decision = goblinCouncil.conveneCouncil(goblinFamilies);

      // Se formou império, remover todas as outras famílias e adicionar novos membros
      if (decision == GoblinCouncil.CouncilDecision.GOBLIN_EMPIRE) {
        // Adicionar os novos goblins do império à lista de inimigos
        for (com.rpggame.entities.Goblin goblin : goblinCouncil.getAndClearNewEmpireGoblins()) {
          addEnemy(goblin);
        }

        // Remover as outras famílias
        removeNonEmpireFamilies();
      }
    }

    // Verificar se Golem morreu e retomar spawn de goblins
    if (activeGolem != null && !activeGolem.isAlive() && familyRespawnTimer == -1) {
      System.out.println("💀 Golem foi derrotado! Retomando spawn de goblins...");
      familyRespawnTimer = FAMILY_RESPAWN_DELAY;
      activeGolem = null;
    }

    // Atualizar timer de respawn de famílias APENAS no mapa de territórios goblin
    // Timer -1 = pausado (enquanto Golem está vivo)
    if (familyRespawnTimer > 0) {
      // Só decrementar timer se estiver no mapa correto (goblin_territories)
      if ("goblin_territories_25x25".equals(currentMapId)) {
        familyRespawnTimer--;

        // Debug: mostrar tempo restante a cada 60 frames (1 segundo)
        if (familyRespawnTimer % 60 == 0) {
          int secondsRemaining = familyRespawnTimer / 60;
          System.out.println("⏱️ Nova família em " + secondsRemaining + " segundos... (Famílias atuais: "
              + goblinFamilies.size() + "/" + MAX_FAMILIES + ")");
        }

        if (familyRespawnTimer == 0 && goblinFamilies.size() < MAX_FAMILIES) {
          // Não spawnar novas famílias se o império estiver ativo
          if (!goblinCouncil.isGoblinEmpireActive()) {
            System.out.println("🎯 Timer zerou! Chamando spawnNewFamily()...");
            spawnNewFamily();
          } else {
            System.out.println("👑 Império Goblin está ativo - novas famílias não podem surgir!");
            familyRespawnTimer = FAMILY_RESPAWN_DELAY; // Resetar timer para tentar depois
          }
        }
      }
    }

    // Atualizar lista de goblins para guerra
    updateGoblinWarLists();

    for (int i = enemies.size() - 1; i >= 0; i--) {
      Enemy enemy = enemies.get(i);

      if (enemy.isAlive()) {
        enemy.update(player);
        continue;
      }

      enemies.remove(i);
      System.out.println("Inimigo removido da lista");

      // Se for um goblin, remover da família, atualizar quest e reputação
      if (enemy instanceof Goblin) {
        Goblin goblin = (Goblin) enemy;
        GoblinFamily family = goblin.getFamily();
        if (family != null) {
          boolean familyDefeated = family.removeMember(goblin);
          if (familyDefeated) {
            handleFamilyDefeated(family);
          }
        }
        if (factionSystem != null) {
          factionSystem.onKilledGoblin();
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
   * Verifica colisão dos projéteis do jogador com inimigos.
   */
  private static final double MAGIC_BOLT_SPLASH_RADIUS = 80.0;

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
          if (Projectile.MAGIC_BOLT.equals(projectile.getType())) {
            // Mago: dano direto + explosão em área ao redor do ponto de impacto
            enemy.takeDamage(projectile.getDamage());
            double ex = projectile.getX();
            double ey = projectile.getY();
            explosionEffects.add(new ExplosionEffect(ex, ey));
            for (Enemy other : enemies) {
              if (other != enemy && other.isAlive()) {
                double dist = Math.hypot(other.getX() - ex, other.getY() - ey);
                if (dist <= MAGIC_BOLT_SPLASH_RADIUS) {
                  // Dano de splash: 60% do dano base, diminui com a distância
                  int splash = (int) (projectile.getDamage() * 0.6 * (1.0 - dist / MAGIC_BOLT_SPLASH_RADIUS));
                  if (splash > 0) other.takeDamage(splash);
                }
              }
            }
          } else {
            // Warrior / Hunter: dano simples sem splash
            enemy.takeDamage(projectile.getDamage());
          }

          projIterator.remove();
          break; // Projétil para no primeiro inimigo (não-perfurante)
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
        System.out.println("Jogador colidiu com inimigo!");

        // Empurrar jogador para longe do inimigo (knockback simples)
        double pushX = player.getX() - enemy.getX();
        double pushY = player.getY() - enemy.getY();
        double distance = Math.sqrt(pushX * pushX + pushY * pushY);

        if (distance > 0) {
          pushX = (pushX / distance) * 20; // força do empurrão
          pushY = (pushY / distance) * 20;
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
    if (familiesInitialized)
      return;

    // Não spawnar goblins em mapas de vila ou outros mapas seguros
    if ("village".equals(currentMapId) || "cave".equals(currentMapId) || "secret_area".equals(currentMapId)) {
      // Adicionar estruturas decorativas no vilarejo
      if ("village".equals(currentMapId)) {
        initializeVillageStructures();
      }
      familiesInitialized = true;
      System.out.println("🏞️ Mapa seguro (" + currentMapId + ") - sem goblins");
      return;
    }

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
   * Inicializa estruturas decorativas do mapa village
   */
  public void initializeVillageStructures() {
    if (!"village".equals(currentMapId)) {
      return;
    }

    System.out.println("Inicializando estruturas do vilarejo...");

    // Igreja - largura reduzida (144x192 pixels)
    structures.add(new Structure(466, 00, "Church", "sprites/curch.png", 144, 192, false));

    // Tenda de mercador - reduzida 10% (130x130 pixels)
    structures.add(new Structure(420, 300, "MarketTent", "sprites/MarketTend.png", 130, 130, false));

    // Casas - 2x2 tiles (96x96 pixels) - ajustado para proporção correta
    structures.add(new Structure(318, 185, "House", "sprites/House1.png", 96, 96, false)); // Casa1
    structures.add(new Structure(624, 474, "House", "sprites/House2.png", 96, 96, false)); // Casa2
    structures.add(new Structure(936, 474, "House", "sprites/House1.png", 96, 96, false)); // Casa3

    // Lâmpadas - 1x1 tile (48x48 pixels) - ajustado para tamanho de tile
    structures.add(new Structure(612, 612, "Lamp", "sprites/Lamp.png", 48, 48, false));
    structures.add(new Structure(480, 756, "Lamp", "sprites/Lamp.png", 48, 48, false));
    structures.add(new Structure(612, 936, "Lamp", "sprites/Lamp.png", 48, 48, false));
    structures.add(new Structure(612, 264, "Lamp", "sprites/Lamp.png", 48, 48, false));

    System.out.println("✅ " + structures.size() + " estruturas decorativas adicionadas ao vilarejo");
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
    // Verificar distância de outras cabanas (mínimo 400 pixels para territórios
    // distantes)
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

    // Verificar se não está perto dos guardas (posições: 480,144 e 672,144)
    double distanceToGuard1 = Math.sqrt(Math.pow(x - 480, 2) + Math.pow(y - 144, 2));
    double distanceToGuard2 = Math.sqrt(Math.pow(x - 672, 2) + Math.pow(y - 144, 2));
    if (distanceToGuard1 < 300 || distanceToGuard2 < 300) {
      return false; // Não spawnar cabanas perto dos guardas
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
          (spawnPos.x / 48) + ", " + (spawnPos.y / 48) + ") centro: (" +
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

      int targetTileX = hutTileX + (int) (Math.cos(angle) * radiusTiles);
      int targetTileY = hutTileY + (int) (Math.sin(angle) * radiusTiles);

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
   * Spawna uma nova família após uma ser derrotada
   */
  private void spawnNewFamily() {
    System.out.println("\n🔔 spawnNewFamily() CHAMADO! Famílias atuais: " + goblinFamilies.size() + "/" + MAX_FAMILIES);

    // Não spawnar em mapas seguros
    if ("village".equals(currentMapId) || "cave".equals(currentMapId)) {
      System.out.println("❌ Tentativa de spawn em mapa seguro (" + currentMapId + "). Cancelando.");
      familyRespawnTimer = 600; // Tentar novamente em 10 segundos
      return;
    }

    if (goblinFamilies.size() >= MAX_FAMILIES) {
      System.out.println("❌ Já temos " + MAX_FAMILIES + " famílias. Cancelando spawn.");
      return;
    }

    System.out.println("\n🆕 ===== NOVA FAMÍLIA GOBLIN CHEGANDO =====");

    // Encontrar posição para nova cabana
    ArrayList<Point> existingPositions = new ArrayList<>();
    for (GoblinFamily family : goblinFamilies) {
      existingPositions.add(family.getHutPosition());
    }

    ArrayList<Point> newHutPositions = findGoodHutPositions(tileMap, 1);

    if (newHutPositions.isEmpty()) {
      System.out.println("⚠️ Não foi possível encontrar posição válida para nova família");
      familyRespawnTimer = 600; // Tentar novamente em 10 segundos
      return;
    }

    Point hutPos = newHutPositions.get(0);

    // Criar cabana
    Structure hut = new Structure(hutPos.x, hutPos.y, "GoblinHut", "sprites/goblinHut.png");
    structures.add(hut);

    // Criar família com nome único
    String familyName = getFamilyName(goblinFamilies.size());
    GoblinFamily family = new GoblinFamily(hutPos, familyName);
    goblinFamilies.add(family);

    // Spawnar membros da família
    spawnFamilyMembers(family, tileMap);

    System.out.println("🏕️ " + familyName + " estabeleceu território em (" + hutPos.x + ", " + hutPos.y + ")");
    System.out.println("==========================================\n");

    // Pequena chance de começar em guerra com família existente (20%)
    if (!goblinFamilies.isEmpty() && random.nextDouble() < 0.2) {
      GoblinFamily enemy = goblinFamilies.get(random.nextInt(goblinFamilies.size()));
      if (enemy != family) {
        family.declareWarAgainst(enemy);
        System.out.println("⚔️ " + familyName + " já chegou em conflito com " + enemy.getFamilyName() + "!");
      }
    }
  }

  /**
   * Gera nome para família de forma aleatória sem repetição
   */
  private String getFamilyName(int index) {
    String[] allNames = {
        "Clã Pedra Negra",
        "Tribo Dente Afiado",
        "Família Garra Suja",
        "Bando Olho Vermelho",
        "Clã Sombra Verde",
        "Horda Osso Quebrado",
        "Tribo Sangue Podre",
        "Clã Veneno Noturno",
        "Bando Fogo Negro",
        "Família Lâmina Enferrujada",
        "Tribo Cranêo Rachado",
        "Clã Língua Venenosa",
        "Horda Grito Selvagem",
        "Bando Lua Sangrenta",
        "Família Espinho Negro",
        "Tribo Pântano Escuro",
        "Clã Chifre Retorcido",
        "Horda Presa Afiada",
        "Bando Cinza Sombria",
        "Família Caverna Profunda"
    };

    // Tentar encontrar um nome não usado
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

    // Escolher nome aleatório da lista disponível
    String chosenName = availableNames.get(random.nextInt(availableNames.size()));
    usedFamilyNames.add(chosenName);
    return chosenName;
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
   * Retorna o conselho goblin
   */
  public GoblinCouncil getGoblinCouncil() {
    return goblinCouncil;
  }

  /**
   * Callback quando uma estrutura é destruída pelo player
   */
  public void onStructureDestroyed(Structure structure) {
    Point structurePos = new Point((int) structure.getX(), (int) structure.getY());

    // Procurar qual família tinha cabana nesta posição
    for (GoblinFamily family : new java.util.ArrayList<>(goblinFamilies)) {
      Point hutPos = family.getHutPosition();
      if (hutPos.x == structurePos.x && hutPos.y == structurePos.y) {
        System.out.println("🏚️ Cabana de " + family.getFamilyName() + " foi destruída pelo jogador!");

        // Matar todos os goblins da família
        java.util.List<com.rpggame.entities.Goblin> familyMembers = new java.util.ArrayList<>();
        for (Enemy enemy : enemies) {
          if (enemy instanceof com.rpggame.entities.Goblin) {
            com.rpggame.entities.Goblin goblin = (com.rpggame.entities.Goblin) enemy;
            if (goblin.getFamily() == family) {
              familyMembers.add(goblin);
            }
          }
        }

        // Remover goblins da família
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
   * Lida com família derrotada - torna a cabana vulnerável
   */
  private void handleFamilyDefeated(GoblinFamily family) {
    System.out.println("🏴 " + family.getFamilyName() + " foi completamente derrotada!");

    // Verificar se é o Império Goblin
    boolean isEmpire = family.getFamilyName().equals("IMPÉRIO GOBLIN");

    if (isEmpire) {
      // Desfazer o império
      goblinCouncil.dissolveEmpire();
    } else {
      // Notificar o conselho goblin (apenas para famílias normais)
      goblinCouncil.registerFamilyDestroyed();
    }

    // Remover família da lista
    goblinFamilies.remove(family);

    // DEBUG: Verificar estado das famílias
    System.out.println("📊 Famílias restantes: " + goblinFamilies.size());
    System.out.println("🗺️ Mapa atual: " + currentMapId);
    System.out.println("🗿 Golem já foi spawnado? " + golemSpawned);

    // Verificar se todas as famílias foram derrotadas e spawnar o Golem
    // Aceitar tanto "goblin_territories" quanto "goblin_territories_25x25"
    boolean isGoblinMap = currentMapId != null && currentMapId.startsWith("goblin_territories");

    if (goblinFamilies.isEmpty() && !golemSpawned && isGoblinMap) {
      System.out.println("✅ TODAS AS CONDIÇÕES ATENDIDAS! Tentando spawnar Golem...");
      checkAndSpawnGolem();
    } else {
      System.out.println("❌ Condições para spawn do Golem não atendidas:");
      if (!goblinFamilies.isEmpty())
        System.out.println("   - Ainda há " + goblinFamilies.size() + " família(s)");
      if (golemSpawned)
        System.out.println("   - Golem já foi spawnado");
      if (!isGoblinMap)
        System.out.println("   - Mapa incorreto: " + currentMapId);
    }

    // Iniciar timer de respawn de nova família (3 minutos)
    // Mas NÃO respawnar se o império foi derrotado
    if (!isEmpire && goblinFamilies.size() < MAX_FAMILIES) {
      familyRespawnTimer = FAMILY_RESPAWN_DELAY;
      System.out.println("⏳ Nova família goblin aparecerá em 3 minutos...");
    } else if (isEmpire) {
      // Após império ser derrotado, permitir respawn de novas famílias normais
      familyRespawnTimer = FAMILY_RESPAWN_DELAY;
      System.out.println("⏳ Novas famílias goblin surgirão em 3 minutos...");
    }

    // Encontrar a cabana desta família e torná-la vulnerável (se ainda não foi
    // destruída)
    Point hutPos = family.getHutPosition();
    for (Structure structure : structures) {
      if (structure.getX() == hutPos.x && structure.getY() == hutPos.y && !structure.isDestroyed()) {
        structure.makeVulnerable();
        System.out.println("🏚️ A cabana de " + family.getFamilyName() + " agora está vulnerável!");
        break;
      }
    }
  }

  /**
   * Verifica se deve spawnar o Golem (50% de chance)
   */
  private void checkAndSpawnGolem() {
    if (random.nextDouble() < 0.5) {
      spawnGolem();
    } else {
      System.out.println("🗿 O Guardião do Equilíbrio não surgiu... o ecossistema permanece em paz.");
    }
  }

  /**
   * Verifica se uma posição está no campo de visão do player (sem paredes no
   * meio)
   */
  private boolean isPositionVisibleToPlayer(int x, int y) {
    if (player == null || tileMap == null)
      return false;

    double playerCenterX = player.getX() + player.getWidth() / 2.0;
    double playerCenterY = player.getY() + player.getHeight() / 2.0;
    double targetX = x + 32; // Centro do tile
    double targetY = y + 32;

    // Verificar distância
    double distance = Math.sqrt(
        Math.pow(targetX - playerCenterX, 2) +
            Math.pow(targetY - playerCenterY, 2));

    // Se está muito longe (fora da tela), não é visível
    if (distance > 600)
      return false;

    // Ray casting para verificar se há paredes no caminho
    int steps = (int) (distance / 8); // Verificar a cada 8 pixels
    double dx = (targetX - playerCenterX) / steps;
    double dy = (targetY - playerCenterY) / steps;

    for (int i = 0; i < steps; i++) {
      double checkX = playerCenterX + dx * i;
      double checkY = playerCenterY + dy * i;

      int tileX = (int) (checkX / GamePanel.TILE_SIZE);
      int tileY = (int) (checkY / GamePanel.TILE_SIZE);

      if (!tileMap.isWalkable(tileX, tileY)) {
        return false; // Há uma parede no caminho
      }
    }

    return true; // Caminho livre até o alvo
  }

  /**
   * Spawna o boss Golem em local fora do campo de visão do player
   */
  private void spawnGolem() {
    System.out.println("\n" +
        "═══════════════════════════════════════════════════\n" +
        "🗿 O GOLEM DESPERTA! 🗿\n" +
        "Guardião do Equilíbrio do Ecossistema\n" +
        "O desequilíbrio causado pela extinção dos goblins\n" +
        "despertou um antigo guardião de pedra...\n" +
        "═══════════════════════════════════════════════════\n");

    // Tentar encontrar posição fora do campo de visão
    int spawnX = 0, spawnY = 0;
    boolean foundSpot = false;
    int attempts = 0;
    int maxAttempts = 50;

    while (!foundSpot && attempts < maxAttempts) {
      // Gerar posição aleatória no mapa
      spawnX = random.nextInt(tileMap.getWidth() - 4) * GamePanel.TILE_SIZE;
      spawnY = random.nextInt(tileMap.getHeight() - 4) * GamePanel.TILE_SIZE;

      // Verificar se é caminhável
      int tileX = spawnX / GamePanel.TILE_SIZE;
      int tileY = spawnY / GamePanel.TILE_SIZE;

      if (tileMap.isWalkable(tileX, tileY) &&
          tileMap.isWalkable(tileX + 1, tileY) &&
          tileMap.isWalkable(tileX, tileY + 1) &&
          tileMap.isWalkable(tileX + 1, tileY + 1)) {

        // Verificar se NÃO está visível pelo player
        if (!isPositionVisibleToPlayer(spawnX, spawnY)) {
          foundSpot = true;
        }
      }
      attempts++;
    }

    // Fallback: centro do mapa se não encontrar spot
    if (!foundSpot) {
      spawnX = (tileMap.getWidth() / 2) * GamePanel.TILE_SIZE;
      spawnY = (tileMap.getHeight() / 2) * GamePanel.TILE_SIZE;
      System.out.println("⚠️ Não encontrou posição escondida, usando centro do mapa");
    } else {
      System.out.println("✅ Posição escondida encontrada após " + attempts + " tentativas");
    }

    activeGolem = new Golem(spawnX, spawnY);
    activeGolem.setTileMap(tileMap);
    activeGolem.setEnemyManager(this);

    addEnemy(activeGolem);
    golemSpawned = true;

    // PAUSAR spawn de goblins enquanto Golem está vivo
    familyRespawnTimer = -1; // Timer negativo = pausado

    System.out.println("✅ Golem criado e adicionado à lista de inimigos!");
    System.out.println("📍 Posição: (" + spawnX + ", " + spawnY + ")");
    System.out.println("⏸️ Spawn de goblins PAUSADO até Golem ser derrotado");
  }

  /**
   * Verifica colisões das pedras do Golem com o player
   */
  public void checkGolemStoneCollisions() {
    if (activeGolem == null || !activeGolem.isAlive()) {
      return;
    }

    // As pedras já verificam colisão internamente no update
    // Este método fica disponível para futuras expansões
  }

  /**
   * Retorna se o Golem está ativo
   */
  public boolean isGolemActive() {
    return activeGolem != null && activeGolem.isAlive();
  }

  /**
   * Retorna o Golem ativo
   */
  public Golem getActiveGolem() {
    return activeGolem;
  }

  /**
   * Remove todas as famílias exceto o Império Goblin
   */
  private void removeNonEmpireFamilies() {
    // Encontrar a família do império
    GoblinFamily empire = null;
    for (GoblinFamily family : goblinFamilies) {
      if (family.getFamilyName().equals("IMPÉRIO GOBLIN")) {
        empire = family;
        break;
      }
    }

    if (empire == null) {
      System.out.println("⚠️ Erro: Império não encontrado!");
      return;
    }

    // Criar lista temporária das famílias a serem removidas
    java.util.List<GoblinFamily> toRemove = new java.util.ArrayList<>();
    for (GoblinFamily family : goblinFamilies) {
      if (!family.getFamilyName().equals("IMPÉRIO GOBLIN")) {
        toRemove.add(family);
      }
    }

    // Eliminar todos os goblins das outras famílias
    for (GoblinFamily family : toRemove) {
      System.out.println("💀 Eliminando família: " + family.getFamilyName());

      // Destruir todos os membros
      for (com.rpggame.entities.Goblin goblin : family.getMembers()) {
        goblin.takeDamage(99999);
      }

      // Remover família
      goblinFamilies.remove(family);

      // Destruir a cabana desta família
      Point hutPos = family.getHutPosition();
      for (Structure structure : structures) {
        if (structure.getX() == hutPos.x && structure.getY() == hutPos.y && !structure.isDestroyed()) {
          structure.makeVulnerable();
          structure.takeDamage(99999); // Destruir imediatamente
          System.out.println("🏚️ Cabana de " + family.getFamilyName() + " destruída!");
          break;
        }
      }
    }

    System.out.println("✅ Apenas o " + empire.getFamilyName() + " permanece!");
    System.out.println("   Total de goblins no império: " + empire.getMembers().size());
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
   * Persiste o estado atual do mapa no MapSimulation antes de trocar de mapa.
   * Chame este método ANTES de chamar loadFromSimulation para o próximo mapa.
   */
  public void saveToSimulation(MapSimulation sim) {
    synchronized (sim) {
      sim.getEnemies().clear();
      sim.getEnemies().addAll(enemies);

      sim.getGoblinFamilies().clear();
      sim.getGoblinFamilies().addAll(goblinFamilies);

      sim.getStructures().clear();
      sim.getStructures().addAll(structures);

      sim.setFamiliesInitialized(familiesInitialized);
      sim.setRespawnTimer(respawnTimer);
      sim.setFamilyRespawnTimer(familyRespawnTimer);
    }

    System.out.println("💾 Estado salvo para mapa '" + sim.getMapId() + "': "
        + enemies.size() + " inimigos, " + goblinFamilies.size() + " famílias");
  }

  /**
   * Restaura o estado de simulação de um mapa previamente visitado.
   * Quando o mapa é visitado pela primeira vez (sim.isFamiliesInitialized() ==
   * false),
   * o chamador deve invocar initializeGoblinFamilies() em seguida.
   */
  public void loadFromSimulation(MapSimulation sim) {
    synchronized (sim) {
      enemies.clear();
      enemies.addAll(sim.getEnemies());

      goblinFamilies.clear();
      goblinFamilies.addAll(sim.getGoblinFamilies());

      structures.clear();
      structures.addAll(sim.getStructures());

      familiesInitialized = sim.isFamiliesInitialized();
      respawnTimer = sim.getRespawnTimer();
      familyRespawnTimer = sim.getFamilyRespawnTimer();
    }

    // Reconectar referências de TileMap e EnemyManager nos inimigos restaurados,
    // pois o TileMap pode ter sido recarregado desde a última visita.
    for (Enemy enemy : enemies) {
      enemy.setTileMap(tileMap);
      enemy.setEnemyManager(this);
    }

    System.out.println("📂 Estado restaurado para mapa '" + sim.getMapId() + "': "
        + enemies.size() + " inimigos, " + goblinFamilies.size() + " famílias");
  }

  /**
   * Limpa todos os inimigos para troca de mapa (sem persistência).
   * Prefira saveToSimulation + loadFromSimulation para preservar estado.
   */
  public void clearAllEnemies() {
    enemies.clear();
    goblinFamilies.clear();
    structures.clear();
    familiesInitialized = false;
    System.out.println("Todos os inimigos foram removidos");
  }

  /**
   * Retorna todos os goblins ativos para os guardas verificarem
   */
  public ArrayList<Goblin> getAllGoblins() {
    ArrayList<Goblin> goblins = new ArrayList<Goblin>();
    for (Enemy enemy : enemies) {
      if (enemy instanceof Goblin) {
        goblins.add((Goblin) enemy);
      }
    }
    return goblins;
  }

}
