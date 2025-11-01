/**
 * Enumeração dos diferentes tipos de tiles no jogo
 */
public enum TileType {
  GRASS(0, "Grass", true),
  STONE(1, "Stone", false),
  WALL(2, "Border", false),
  WATER(3, "Water", false),
  DIRT(4, "Terra", true),
  SAND(5, "Areia", true);

  private final int id;
  private final String name;
  private final boolean walkable;

  TileType(int id, String name, boolean walkable) {
    this.id = id;
    this.name = name;
    this.walkable = walkable;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean isWalkable() {
    return walkable;
  }

  // Método para converter ID em TileType
  public static TileType fromId(int id) {
    for (TileType type : values()) {
      if (type.id == id) {
        return type;
      }
    }
    return GRASS; // Default
  }

  // Método para converter char em TileType (para mapas .txt)
  public static TileType fromChar(char c) {
    switch (c) {
      case '.':
      case 'G':
        return GRASS;
      case '#':
      case 'S':
        return STONE;
      case 'W':
        return WALL;
      case '~':
      case 'T':
        return WATER;
      case 'd':
      case 'D':
        return DIRT;
      case 's':
      case 'A':
        return SAND;
      default:
        return GRASS;
    }
  }

  // Converter TileType para char (para salvar mapas)
  public char toChar() {
    switch (this) {
      case GRASS:
        return '.';
      case STONE:
        return '#';
      case WALL:
        return 'W';
      case WATER:
        return '~';
      case DIRT:
        return 'd';
      case SAND:
        return 's';
      default:
        return '.';
    }
  }
}