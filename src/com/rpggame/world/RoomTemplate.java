package com.rpggame.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Define um template de sala/mapa com entradas direcionais.
 */
public class RoomTemplate {
  private final String id;
  private final String mapFile;
  private final String displayName;
  private final RoomType roomType;
  private final boolean unique;
  private final int weight;
  private final int spawnTileX;
  private final int spawnTileY;
  private final Map<Direction, List<EntrancePoint>> entrances;

  public RoomTemplate(String id, String mapFile, String displayName, RoomType roomType,
      boolean unique, int weight, int spawnTileX, int spawnTileY) {
    this.id = id;
    this.mapFile = mapFile;
    this.displayName = displayName;
    this.roomType = roomType;
    this.unique = unique;
    this.weight = weight;
    this.spawnTileX = spawnTileX;
    this.spawnTileY = spawnTileY;
    this.entrances = new EnumMap<>(Direction.class);
  }

  public RoomTemplate addEntrance(Direction direction, int tileX, int tileY, String label) {
    entrances.computeIfAbsent(direction, k -> new ArrayList<>())
        .add(new EntrancePoint(direction, tileX, tileY, label));
    return this;
  }

  public boolean hasEntrance(Direction direction) {
    return entrances.containsKey(direction) && !entrances.get(direction).isEmpty();
  }

  public List<EntrancePoint> getEntrances(Direction direction) {
    List<EntrancePoint> points = entrances.get(direction);
    if (points == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(points);
  }

  public Map<Direction, List<EntrancePoint>> getAllEntrances() {
    Map<Direction, List<EntrancePoint>> snapshot = new EnumMap<>(Direction.class);
    for (Map.Entry<Direction, List<EntrancePoint>> entry : entrances.entrySet()) {
      snapshot.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
    }
    return Collections.unmodifiableMap(snapshot);
  }

  public String getId() {
    return id;
  }

  public String getMapFile() {
    return mapFile;
  }

  public String getDisplayName() {
    return displayName;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public boolean isUnique() {
    return unique;
  }

  public int getWeight() {
    return weight;
  }

  public int getSpawnTileX() {
    return spawnTileX;
  }

  public int getSpawnTileY() {
    return spawnTileY;
  }

  /**
   * Ponto de entrada/saída no tile map para uma direção específica.
   */
  public static class EntrancePoint {
    private final Direction direction;
    private final int tileX;
    private final int tileY;
    private final String label;

    public EntrancePoint(Direction direction, int tileX, int tileY, String label) {
      this.direction = direction;
      this.tileX = tileX;
      this.tileY = tileY;
      this.label = label;
    }

    public Direction getDirection() {
      return direction;
    }

    public int getTileX() {
      return tileX;
    }

    public int getTileY() {
      return tileY;
    }

    public String getLabel() {
      return label;
    }
  }
}