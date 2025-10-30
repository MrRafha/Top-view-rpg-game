import java.awt.*;

/**
 * Sistema de mapeamento com diferentes tipos de tiles e suporte a mapas
 * personalizados
 */
public class TileMap {
  private final int TILE_SIZE = GamePanel.TILE_SIZE;
  private final int MAP_WIDTH = GamePanel.MAP_WIDTH;
  private final int MAP_HEIGHT = GamePanel.MAP_HEIGHT;

  // Mapa de tiles
  private TileType[][] map;

  // Sistema de fog of war
  private FogOfWar fogOfWar;

  public TileMap() {
    // Tentar carregar mapa personalizado, senão usar o padrão
    loadMap();

    // Inicializar fog of war
    fogOfWar = new FogOfWar(MAP_WIDTH, MAP_HEIGHT);

    // Criar mapa de exemplo se não existir
    MapLoader.createExampleMap();
  }

  private void loadMap() {
    // Tentar carregar o novo mapa 15x15 - verificar múltiplos caminhos
    map = MapLoader.loadMapFromFile("../maps/new_map_15x15.txt");
    if (map == null) {
      map = MapLoader.loadMapFromFile("maps/new_map_15x15.txt");
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
    // Atualizar fog of war
    fogOfWar.updateVisibility(player, map);

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

        // Escolher cor baseada no tipo
        Color tileColor = getTileColor(tileType, tileX, tileY);

        // Desenhar o tile
        g.setColor(tileColor);
        g.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);

        // Adicionar bordas para definir melhor os tiles
        g.setColor(getTileBorderColor(tileType));
        g.drawRect(screenX, screenY, TILE_SIZE - 1, TILE_SIZE - 1);
      }
    }

    // Renderizar fog of war
    fogOfWar.render(g, camera, map);
  }

  private Color getTileColor(TileType tileType, int x, int y) {
    switch (tileType) {
      case GRASS:
        // Variação sutil de cor para grama
        return (x + y) % 2 == 0 ? new Color(34, 139, 34) : new Color(0, 100, 0);
      case STONE:
        return new Color(105, 105, 105); // Cinza escuro
      case WALL:
        return new Color(139, 69, 19); // Marrom
      case WATER:
        return new Color(0, 100, 200); // Azul
      case DIRT:
        return new Color(101, 67, 33); // Marrom terra
      case SAND:
        return new Color(238, 203, 173); // Bege
      default:
        return new Color(34, 139, 34); // Verde padrão
    }
  }

  private Color getTileBorderColor(TileType tileType) {
    switch (tileType) {
      case GRASS:
        return new Color(0, 80, 0);
      case STONE:
        return new Color(80, 80, 80);
      case WALL:
        return new Color(100, 50, 10);
      case WATER:
        return new Color(0, 70, 140);
      case DIRT:
        return new Color(80, 50, 20);
      case SAND:
        return new Color(200, 170, 140);
      default:
        return new Color(0, 80, 0);
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
}