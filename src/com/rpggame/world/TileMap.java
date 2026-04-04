package com.rpggame.world;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;
import com.rpggame.core.GamePanel;
import com.rpggame.core.Game;
import com.rpggame.entities.Player;

/**
 * Sistema de mapeamento com diferentes tipos de tiles e suporte a mapas
 * personalizados
 */
public class TileMap {
  private static final Color GRASS_A = new Color(34, 139, 34);
  private static final Color GRASS_B = new Color(0, 100, 0);
  private static final Color STONE_A = new Color(128, 128, 128);
  private static final Color STONE_B = new Color(105, 105, 105);
  private static final Color WALL_COLOR = new Color(64, 64, 64);
  private static final Color WATER_A = new Color(0, 100, 200);
  private static final Color WATER_B = new Color(0, 120, 220);
  private static final Color DIRT_COLOR = new Color(101, 67, 33);
  private static final Color SAND_COLOR = new Color(238, 203, 173);

  private static final Color BORDER_GRASS = new Color(0, 80, 0);
  private static final Color BORDER_STONE = new Color(70, 70, 70);
  private static final Color BORDER_WALL = new Color(32, 32, 32);
  private static final Color BORDER_WATER = new Color(0, 70, 140);
  private static final Color BORDER_DIRT = new Color(80, 50, 20);
  private static final Color BORDER_SAND = new Color(200, 170, 140);

  private final int TILE_SIZE = GamePanel.TILE_SIZE;
  private final int MAP_WIDTH = GamePanel.MAP_WIDTH;
  private final int MAP_HEIGHT = GamePanel.MAP_HEIGHT;

  // Mapa de tiles
  private TileType[][] map;

  // Sistema de fog of war
  private FogOfWar fogOfWar;
  private int lastFogPlayerTileX = -1;
  private int lastFogPlayerTileY = -1;

  // Cache de sprites dos tiles
  private Map<TileType, BufferedImage> tileSprites;

  // Lista de portais no mapa
  private java.util.List<Portal> portals;

  public TileMap() {
    // Inicializar cache de sprites
    tileSprites = new HashMap<>();
    loadTileSprites();

    // Inicializar lista de portais
    portals = new java.util.ArrayList<>();

    // Tentar carregar mapa personalizado, senão usar o padrão
    loadMap();

    // Inicializar fog of war
    fogOfWar = new FogOfWar(MAP_WIDTH, MAP_HEIGHT);

    // Criar mapa de exemplo se não existir
    MapLoader.createExampleMap();

    // Configurar portais do mapa
    setupPortals();
  }

  /**
   * Carrega os sprites dos tiles da pasta sprites
   */
  private void loadTileSprites() {
    String[] spriteFiles = { "GRASS.png", "STONE.png", "BORDER.png", "Wather.png", "WalknableWather.png",
        "CaminhoGrama.png" };
    TileType[] tileTypes = { TileType.GRASS, TileType.STONE, TileType.WALL, TileType.WATER, TileType.WALKABLE_WATER,
        TileType.GRASS_PATH };

    for (int i = 0; i < spriteFiles.length; i++) {
      try {
        BufferedImage sprite = null;
        String resourcePath = "sprites/" + spriteFiles[i];

        // Tentar carregar como recurso do classpath (funciona no JAR)
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is != null) {
          sprite = ImageIO.read(is);
          is.close();
          System.out.println("✅ Sprite carregado do JAR: " + spriteFiles[i]);
        } else {
          // Fallback: tentar carregar como arquivo externo (desenvolvimento)
          String spritePath = ResourceResolver.getResourcePath(resourcePath);
          File spriteFile = new File(spritePath);
          if (spriteFile.exists()) {
            sprite = ImageIO.read(spriteFile);
            System.out.println("✅ Sprite carregado do arquivo: " + spriteFiles[i]);
          }
        }

        if (sprite != null) {
          // Redimensionar para o tamanho do tile
          BufferedImage scaledSprite = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2d = scaledSprite.createGraphics();
          g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
          g2d.drawImage(sprite, 0, 0, TILE_SIZE, TILE_SIZE, null);
          g2d.dispose();

          tileSprites.put(tileTypes[i], scaledSprite);
        } else {
          System.out.println("⚠️ Sprite não encontrado: " + resourcePath + " - usando cor padrão");
        }
      } catch (IOException e) {
        System.err.println("❌ Erro ao carregar sprite " + spriteFiles[i] + ": " + e.getMessage());
      }
    }

    // PORTAL usa o mesmo sprite da GRASS
    if (tileSprites.containsKey(TileType.GRASS)) {
      tileSprites.put(TileType.PORTAL, tileSprites.get(TileType.GRASS));
      System.out.println("✅ Tile PORTAL configurado (usa sprite GRASS)");
    }
  }

  private void loadMap() {
    // Lista de mapas para tentar carregar em ordem de preferência
    String[] mapFiles = {
        "maps/goblin_territories_25x25.txt",
        "maps/enhanced_map_15x15.txt",
        "maps/new_map_15x15.txt",
        "maps/example.txt"
    };

    // Tentar carregar cada mapa usando o sistema de resolução de caminho
    for (String mapFile : mapFiles) {
      String resolvedPath = ResourceResolver.getResourcePath(mapFile);
      map = MapLoader.loadMapFromFile(resolvedPath);
      if (map != null) {
        System.out.println("Mapa carregado com sucesso: " + resolvedPath);
        break;
      }
    }

    // Se não conseguiu carregar, tentar mapa personalizado
    if (map == null || map.length != MAP_HEIGHT ||
        (map.length > 0 && map[0].length != MAP_WIDTH)) {
      map = MapLoader.loadMapFromFile("custom_map.txt");
    }

    // Se ainda não conseguiu carregar ou dimensões não batem, usar mapa padrão
    if (map == null || map.length != MAP_HEIGHT ||
        (map.length > 0 && map[0].length != MAP_WIDTH)) {
      map = MapLoader.generateDefaultMap();
    }
  }

  public void render(Graphics2D g, Camera camera, Player player) {
    // Calcular quais tiles estão visíveis na tela
    int startX = Math.max(0, (int) (camera.getX() / TILE_SIZE));
    int endX = Math.min(MAP_WIDTH, (int) ((camera.getX() + Game.SCREEN_WIDTH) / TILE_SIZE) + 1);
    int startY = Math.max(0, (int) (camera.getY() / TILE_SIZE));
    int endY = Math.min(MAP_HEIGHT, (int) ((camera.getY() + Game.SCREEN_HEIGHT) / TILE_SIZE) + 1);

    // Renderizar apenas os tiles visíveis
    for (int tileY = startY; tileY < endY; tileY++) {
      for (int tileX = startX; tileX < endX; tileX++) {
        int screenX = (int) (tileX * TILE_SIZE - camera.getX());
        int screenY = (int) (tileY * TILE_SIZE - camera.getY());

        // Obter tipo do tile
        TileType tileType = map[tileY][tileX];

        // Verificar se existe sprite para este tipo de tile
        BufferedImage tileSprite = tileSprites.get(tileType);

        if (tileSprite != null) {
          // Usar sprite se disponível
          g.drawImage(tileSprite, screenX, screenY, null);
        } else {
          // Fallback para cores sólidas se sprite não estiver disponível
          Color tileColor = getTileColor(tileType, tileX, tileY);
          g.setColor(tileColor);
          g.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);

          // Adicionar bordas para definir melhor os tiles
          g.setColor(getTileBorderColor(tileType));
          g.drawRect(screenX, screenY, TILE_SIZE - 1, TILE_SIZE - 1);
        }
      }
    }

    // Renderizar fog of war
    fogOfWar.render(g, camera, map);
  }

  /**
   * Atualiza fog of war apenas quando o jogador muda de tile.
   */
  public void updateFogOfWar(Player player) {
    if (player == null || fogOfWar == null || map == null) {
      return;
    }

    int playerTileX = (int) (player.getX() / TILE_SIZE);
    int playerTileY = (int) (player.getY() / TILE_SIZE);

    if (playerTileX == lastFogPlayerTileX && playerTileY == lastFogPlayerTileY) {
      return;
    }

    fogOfWar.updateVisibility(player, map);
    lastFogPlayerTileX = playerTileX;
    lastFogPlayerTileY = playerTileY;
  }

  private Color getTileColor(TileType tileType, int x, int y) {
    switch (tileType) {
      case GRASS:
        // Variação sutil de cor para grama (GRASS)
        return (x + y) % 2 == 0 ? GRASS_A : GRASS_B;
      case STONE:
        // Pedra cinza com textura rochosa (STONE - não caminhável)
        return (x + y) % 2 == 0 ? STONE_A : STONE_B;
      case WALL:
        // Borda/Parede mais escura e sólida (BORDER)
        return WALL_COLOR; // Cinza escuro para bordas
      case WATER:
        // Água com variação azul (mantém como WATER)
        return (x + y) % 2 == 0 ? WATER_A : WATER_B;
      case DIRT:
        return DIRT_COLOR; // Marrom terra
      case SAND:
        return SAND_COLOR; // Bege
      default:
        return GRASS_A; // Verde padrão
    }
  }

  private Color getTileBorderColor(TileType tileType) {
    switch (tileType) {
      case GRASS:
        return BORDER_GRASS; // Borda verde escura para grama
      case STONE:
        return BORDER_STONE; // Borda cinza mais escura para pedras
      case WALL:
        return BORDER_WALL; // Borda preta para bordas/paredes
      case WATER:
        return BORDER_WATER; // Borda azul escura para água
      case DIRT:
        return BORDER_DIRT;
      case SAND:
        return BORDER_SAND;
      default:
        return BORDER_GRASS;
    }
  }

  // Getters
  public TileType[][] getMap() {
    return map;
  }

  public FogOfWar getFogOfWar() {
    return fogOfWar;
  }

  // Verificar se um tile é transitável
  public boolean isWalkable(int tileX, int tileY) {
    if (tileX < 0 || tileX >= MAP_WIDTH || tileY < 0 || tileY >= MAP_HEIGHT) {
      return false;
    }
    return map[tileY][tileX].isWalkable();
  }

  // Encontrar uma posição aleatória de grama para spawn do player
  public Point getRandomGrassPosition() {
    java.util.List<Point> grassTiles = new java.util.ArrayList<>();

    // Encontrar todos os tiles de grama
    for (int y = 0; y < MAP_HEIGHT; y++) {
      for (int x = 0; x < MAP_WIDTH; x++) {
        if (map[y][x] == TileType.GRASS) {
          grassTiles.add(new Point(x, y));
        }
      }
    }

    // Se não encontrou grama, retorna uma posição padrão
    if (grassTiles.isEmpty()) {
      return new Point(MAP_WIDTH / 2, MAP_HEIGHT / 2);
    }

    // Escolher uma posição aleatória
    java.util.Random random = new java.util.Random();
    Point selectedTile = grassTiles.get(random.nextInt(grassTiles.size()));

    // Converter coordenadas do tile para coordenadas do mundo (pixels)
    return new Point(selectedTile.x * TILE_SIZE + TILE_SIZE / 2,
        selectedTile.y * TILE_SIZE + TILE_SIZE / 2);
  }

  /**
   * Obtém posição centrada no tile considerando o tamanho do objeto
   */
  public Point getCenteredGrassPosition(int objectWidth, int objectHeight) {
    java.util.List<Point> grassTiles = new java.util.ArrayList<>();

    // Encontrar todos os tiles de grama
    for (int y = 0; y < MAP_HEIGHT; y++) {
      for (int x = 0; x < MAP_WIDTH; x++) {
        if (map[y][x] == TileType.GRASS) {
          grassTiles.add(new Point(x, y));
        }
      }
    }

    // Se não encontrou grama, retorna uma posição padrão
    if (grassTiles.isEmpty()) {
      int centerX = (MAP_WIDTH / 2) * TILE_SIZE + (TILE_SIZE - objectWidth) / 2;
      int centerY = (MAP_HEIGHT / 2) * TILE_SIZE + (TILE_SIZE - objectHeight) / 2;
      return new Point(centerX, centerY);
    }

    // Escolher uma posição aleatória
    java.util.Random random = new java.util.Random();
    Point selectedTile = grassTiles.get(random.nextInt(grassTiles.size()));

    // Centralizar o objeto no tile considerando seu tamanho
    int centerX = selectedTile.x * TILE_SIZE + (TILE_SIZE - objectWidth) / 2;
    int centerY = selectedTile.y * TILE_SIZE + (TILE_SIZE - objectHeight) / 2;

    return new Point(centerX, centerY);
  }

  /**
   * Getters para dimensões do mapa
   */
  public int getWidth() {
    return MAP_WIDTH;
  }

  public int getHeight() {
    return MAP_HEIGHT;
  }

  /**
   * Retorna o tipo de tile em uma coordenada específica
   */
  public TileType getTileAt(int x, int y) {
    if (x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT) {
      return map[y][x];
    }
    return TileType.WALL; // Retorna parede se fora dos limites
  }

  /**
   * Configura os portais do mapa atual
   */
  /**
   * Configura portais do mapa baseado no ID do mapa atual
   */
  public void setupPortals(String currentMapId) {
    portals.clear();

    // Procurar tiles PORTAL e WALKABLE_WATER no mapa e criar portais
    // automaticamente
    for (int y = 0; y < MAP_HEIGHT; y++) {
      for (int x = 0; x < MAP_WIDTH; x++) {
        boolean isPortalTile = map[y][x] == TileType.PORTAL;
        boolean isWaterlilyPortal = false;

        // No village, a vitória régia na linha 15 (y=15) é um portal
        if ("village".equals(currentMapId) && map[y][x] == TileType.WALKABLE_WATER) {
          if (y == 15 && x == 0) {
            isWaterlilyPortal = true;
          }
        }

        // Na secret area, o WALKABLE_WATER ou PORTAL na direita é a saída
        if ("secret_area".equals(currentMapId) && map[y][x] == TileType.WALKABLE_WATER) {
          if (y == 15 && x >= 23) {
            isWaterlilyPortal = true;
          }
        }

        if (isPortalTile || isWaterlilyPortal) {
          // Definir destino do portal baseado no mapa atual e posição

          if ("village".equals(currentMapId)) {
            // Village tem dois portais
            if ((y >= 15 && y <= 16 && x <= 2) || isWaterlilyPortal) {
              // Portal da vitória régia (canto superior esquerdo) -> Secret Area
              // Spawna do lado direito na secret area (onde está o portal de volta)
              portals.add(new Portal(x, y, "secret_area", 23, 15, "Passagem Secreta"));
              System.out.println("🌀 Portal Village encontrado em (" + x + ", " + y + ") -> Secret Area");
            } else {
              // Portal sul -> Goblin Territories
              portals.add(new Portal(x, y, "goblin_territories", 0, 0, "Portal dos Territórios"));
              System.out.println("🌀 Portal Village encontrado em (" + x + ", " + y + ") -> Goblin Territories");
            }
          } else if ("secret_area".equals(currentMapId)) {
            // Secret Area -> Village (volta pelo mesmo portal da vitória régia)
            // Spawna ao lado do portal da vitória régia
            portals.add(new Portal(x, y, "village", 2, 15, "Portal da Vila"));
            System.out.println("🌀 Portal Secret Area encontrado em (" + x + ", " + y + ") -> Village");
          } else {
            // Goblin Territories -> Village
            portals.add(new Portal(x, y, "village", 0, 0, "Portal da Vila"));
            System.out.println("🌀 Portal Goblin encontrado em (" + x + ", " + y + ") -> Village");
          }
        }
      }
    }
  }

  /**
   * Versão antiga do setupPortals (sem parâmetro) - chama a nova versão com ID
   * padrão
   */
  private void setupPortals() {
    setupPortals("goblin_territories"); // Mapa inicial padrão
  }

  /**
   * Verifica se o jogador esta sobre um portal
   */
  public Portal getPortalAt(int tileX, int tileY) {
    for (Portal portal : portals) {
      if (portal.isPlayerOn(tileX, tileY)) {
        return portal;
      }
    }
    return null;
  }

  /**
   * Adiciona um portal manualmente
   */
  public void addPortal(Portal portal) {
    portals.add(portal);
    System.out.println("➕ " + portal);
  }

  /**
   * Recarrega o mapa com novo arquivo e ID do mapa
   */
  public void reloadMap(String mapPath, String mapId) {
    try {
      map = MapLoader.loadMapFromFile(mapPath);
      fogOfWar = new FogOfWar(MAP_WIDTH, MAP_HEIGHT);
      lastFogPlayerTileX = -1;
      lastFogPlayerTileY = -1;
      setupPortals(mapId);
      System.out.println("🗺️ Mapa recarregado: " + mapPath);
    } catch (Exception e) {
      System.err.println("❌ Erro ao recarregar mapa: " + e.getMessage());
    }
  }
}
