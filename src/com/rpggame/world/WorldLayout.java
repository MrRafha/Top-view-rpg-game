package com.rpggame.world;

import java.awt.Point;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa o layout procedural do mundo como grafo de salas.
 */
public class WorldLayout {
  private final String startMapId;
  private final Map<String, Map<Direction, String>> connections;
  private final Map<String, Point> nodePositions;

  public WorldLayout(String startMapId) {
    this.startMapId = startMapId;
    this.connections = new LinkedHashMap<>();
    this.nodePositions = new HashMap<>();
  }

  public void connectBidirectional(String mapA, Direction fromA, String mapB, Direction fromB) {
    connections.computeIfAbsent(mapA, k -> new EnumMap<>(Direction.class)).put(fromA, mapB);
    connections.computeIfAbsent(mapB, k -> new EnumMap<>(Direction.class)).put(fromB, mapA);
  }

  public void setNodePosition(String mapId, int gridX, int gridY) {
    nodePositions.put(mapId, new Point(gridX, gridY));
  }

  public String getConnectedMap(String fromMapId, Direction direction) {
    Map<Direction, String> exits = connections.get(fromMapId);
    if (exits == null) {
      return null;
    }
    return exits.get(direction);
  }

  public Map<Direction, String> getConnectionsFrom(String mapId) {
    Map<Direction, String> exits = connections.get(mapId);
    if (exits == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(exits);
  }

  public Map<String, Map<Direction, String>> getAllConnections() {
    Map<String, Map<Direction, String>> snapshot = new LinkedHashMap<>();
    for (Map.Entry<String, Map<Direction, String>> entry : connections.entrySet()) {
      snapshot.put(entry.getKey(), Collections.unmodifiableMap(new EnumMap<>(entry.getValue())));
    }
    return Collections.unmodifiableMap(snapshot);
  }

  public Point getNodePosition(String mapId) {
    Point p = nodePositions.get(mapId);
    if (p == null) {
      return new Point(0, 0);
    }
    return new Point(p);
  }

  public Map<String, Point> getAllNodePositions() {
    Map<String, Point> snapshot = new HashMap<>();
    for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
      snapshot.put(entry.getKey(), new Point(entry.getValue()));
    }
    return Collections.unmodifiableMap(snapshot);
  }

  public String getStartMapId() {
    return startMapId;
  }
}