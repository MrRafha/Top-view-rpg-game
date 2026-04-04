package com.rpggame.world;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Gera um layout procedural de mundo com regras de continuidade por direção.
 */
public class WorldGenerator {
  private static final String HUMAN_VILLAGE_ID = "village";
  private static final String GOBLIN_VILLAGE_ID = "goblin_village";

  public WorldLayout generate(Map<String, RoomTemplate> templates, String preferredStartMapId, long seed) {
    if (templates == null || templates.isEmpty()) {
      return new WorldLayout("village");
    }

    // Seed explícita para reproduzir layouts em testes e depuração quando
    // necessário.
    Random random = new Random(seed);
    String startMapId = templates.containsKey(preferredStartMapId)
        ? preferredStartMapId
        : templates.keySet().iterator().next();

    WorldLayout layout = new WorldLayout(startMapId);

    // connected = subconjunto já conectado ao grafo principal.
    Set<String> connected = new HashSet<>();
    connected.add(startMapId);
    layout.setNodePosition(startMapId, 0, 0);

    List<String> unconnected = new ArrayList<>(templates.keySet());
    unconnected.remove(startMapId);
    boolean hasGoblinVillage = templates.containsKey(GOBLIN_VILLAGE_ID) && !GOBLIN_VILLAGE_ID.equals(startMapId);

    // Regra de design: seguramos a vila goblin para conectar por último no ponto
    // mais distante.
    if (hasGoblinVillage) {
      unconnected.remove(GOBLIN_VILLAGE_ID);
    }

    // Constrói uma árvore de conexão garantindo que todo mapa esteja alcançável.
    while (!unconnected.isEmpty()) {
      String roomToAttach = popWeightedRoomId(unconnected, templates, random);
      RoomTemplate roomTemplate = templates.get(roomToAttach);

      List<String> anchors = new ArrayList<>(connected);
      Collections.shuffle(anchors, random);

      boolean linked = false;
      for (String anchorId : anchors) {
        RoomTemplate anchorTemplate = templates.get(anchorId);
        DirectionPair pair = findAvailableDirectionPair(layout, anchorId, anchorTemplate, roomTemplate, random);
        if (pair == null) {
          continue;
        }

        layout.connectBidirectional(anchorId, pair.anchorDirection, roomToAttach, pair.roomDirection);
        Point anchorPos = layout.getNodePosition(anchorId);
        Point roomPos = findFreePosition(layout, anchorPos, pair.anchorDirection);
        layout.setNodePosition(roomToAttach, roomPos.x, roomPos.y);

        connected.add(roomToAttach);
        linked = true;
        break;
      }

      if (!linked) {
        // Fallback mínimo para não quebrar o mundo se faltar par direcional compatível.
        String anchorId = connected.iterator().next();
        layout.connectBidirectional(anchorId, Direction.SOUTH, roomToAttach, Direction.NORTH);
        Point anchorPos = layout.getNodePosition(anchorId);
        layout.setNodePosition(roomToAttach, anchorPos.x, anchorPos.y + 1);
        connected.add(roomToAttach);
      }
    }

    Map<String, RoomTemplate> templatesWithoutGoblin = templates;
    if (hasGoblinVillage) {
      templatesWithoutGoblin = new LinkedHashMap<>(templates);
      templatesWithoutGoblin.remove(GOBLIN_VILLAGE_ID);
    }

    // Após garantir conectividade da parte principal, adicionamos algumas arestas
    // extras para variar rotas.
    addExtraConnections(layout, templatesWithoutGoblin, random);

    if (hasGoblinVillage) {
      attachGoblinVillageAsFarthest(layout, templates, startMapId, random);
    }

    if (!isReachable(layout, startMapId, templates.keySet())) {
      throw new IllegalStateException("Falha ao gerar layout conectado de mundo.");
    }

    return layout;
  }

  private DirectionPair findAvailableDirectionPair(WorldLayout layout, String anchorId,
      RoomTemplate anchorTemplate, RoomTemplate roomTemplate, Random random) {
    List<DirectionPair> candidates = new ArrayList<>();

    for (Direction anchorDirection : Direction.values()) {
      if (!anchorTemplate.hasEntrance(anchorDirection)) {
        continue;
      }
      if (layout.getConnectedMap(anchorId, anchorDirection) != null) {
        continue;
      }

      Direction requiredRoomDirection = anchorDirection.opposite();
      if (!roomTemplate.hasEntrance(requiredRoomDirection)) {
        continue;
      }

      candidates.add(new DirectionPair(anchorDirection, requiredRoomDirection));
    }

    if (candidates.isEmpty()) {
      return null;
    }

    return candidates.get(random.nextInt(candidates.size()));
  }

  private String popWeightedRoomId(List<String> remainingRoomIds, Map<String, RoomTemplate> templates, Random random) {
    // Sorteio ponderado: tipos de sala com maior peso entram mais cedo no layout.
    int totalWeight = 0;
    for (String roomId : remainingRoomIds) {
      RoomTemplate template = templates.get(roomId);
      totalWeight += getEffectiveWeight(template);
    }

    int roll = random.nextInt(Math.max(1, totalWeight));
    int cumulative = 0;

    for (int i = 0; i < remainingRoomIds.size(); i++) {
      String roomId = remainingRoomIds.get(i);
      cumulative += getEffectiveWeight(templates.get(roomId));
      if (roll < cumulative) {
        remainingRoomIds.remove(i);
        return roomId;
      }
    }

    return remainingRoomIds.remove(remainingRoomIds.size() - 1);
  }

  private int getEffectiveWeight(RoomTemplate template) {
    if (template == null) {
      return 1;
    }

    // Ajuste de peso por tipo para controlar frequência sem depender apenas do
    // valor cru do template.
    int base = Math.max(1, template.getWeight());
    switch (template.getRoomType()) {
      case NEUTRAL:
        return base + 2;
      case HOSTILE:
        return base + 1;
      case SPECIAL:
        return Math.max(1, base - 2);
      case SAFE:
      default:
        return base;
    }
  }

  private Point findFreePosition(WorldLayout layout, Point anchorPos, Direction direction) {
    // Evita sobrepor nós no mapa mundi quando múltiplas conexões seguem a mesma
    // direção.
    Point candidate = offsetPoint(anchorPos, direction);
    int guard = 0;

    while (isOccupied(layout, candidate) && guard < 12) {
      candidate = offsetPoint(candidate, direction);
      guard++;
    }

    return candidate;
  }

  private boolean isOccupied(WorldLayout layout, Point point) {
    for (Point existing : layout.getAllNodePositions().values()) {
      if (existing.x == point.x && existing.y == point.y) {
        return true;
      }
    }
    return false;
  }

  private void addExtraConnections(WorldLayout layout, Map<String, RoomTemplate> templates, Random random) {
    // Limite simples para evitar grafo denso demais e manter leitura visual do
    // mapa.
    int maxExtras = Math.max(1, templates.size() / 3);
    int added = 0;

    List<String> mapIds = new ArrayList<>(templates.keySet());
    Collections.shuffle(mapIds, random);

    for (String fromMapId : mapIds) {
      if (added >= maxExtras) {
        break;
      }

      RoomTemplate fromTemplate = templates.get(fromMapId);
      List<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
      Collections.shuffle(directions, random);

      for (Direction fromDirection : directions) {
        if (!fromTemplate.hasEntrance(fromDirection)) {
          continue;
        }
        if (layout.getConnectedMap(fromMapId, fromDirection) != null) {
          continue;
        }

        String candidateMapId = pickConnectionCandidate(layout, templates, fromMapId, fromDirection, random);
        if (candidateMapId == null) {
          continue;
        }

        layout.connectBidirectional(fromMapId, fromDirection, candidateMapId, fromDirection.opposite());
        added++;
        if (added >= maxExtras) {
          break;
        }
      }
    }
  }

  private String pickConnectionCandidate(WorldLayout layout, Map<String, RoomTemplate> templates,
      String fromMapId, Direction fromDirection, Random random) {
    List<String> candidates = new ArrayList<>();
    Direction requiredDirection = fromDirection.opposite();

    for (Map.Entry<String, RoomTemplate> entry : templates.entrySet()) {
      String targetMapId = entry.getKey();
      RoomTemplate targetTemplate = entry.getValue();

      if (targetMapId.equals(fromMapId)) {
        continue;
      }
      if (!targetTemplate.hasEntrance(requiredDirection)) {
        continue;
      }
      if (layout.getConnectedMap(targetMapId, requiredDirection) != null) {
        continue;
      }

      candidates.add(targetMapId);
    }

    if (candidates.isEmpty()) {
      return null;
    }

    return candidates.get(random.nextInt(candidates.size()));
  }

  private void attachGoblinVillageAsFarthest(WorldLayout layout, Map<String, RoomTemplate> templates,
      String startMapId, Random random) {
    RoomTemplate goblinTemplate = templates.get(GOBLIN_VILLAGE_ID);
    if (goblinTemplate == null) {
      return;
    }

    Map<String, Integer> distances = computeDistancesFromStart(layout, startMapId);
    List<String> anchors = new ArrayList<>(distances.keySet());
    anchors.sort((a, b) -> Integer.compare(distances.get(b), distances.get(a)));

    for (String anchorId : anchors) {
      RoomTemplate anchorTemplate = templates.get(anchorId);
      if (anchorTemplate == null) {
        continue;
      }

      DirectionPair pair = findAvailableDirectionPair(layout, anchorId, anchorTemplate, goblinTemplate, random);
      if (pair == null) {
        continue;
      }

      layout.connectBidirectional(anchorId, pair.anchorDirection, GOBLIN_VILLAGE_ID, pair.roomDirection);
      Point anchorPos = layout.getNodePosition(anchorId);
      Point goblinPos = findFreePosition(layout, anchorPos, pair.anchorDirection);
      layout.setNodePosition(GOBLIN_VILLAGE_ID, goblinPos.x, goblinPos.y);
      return;
    }

    // Fallback defensivo: se não encontrar par perfeito, ainda conecta no mais
    // distante possível.
    String fallbackAnchor = anchors.isEmpty() ? startMapId : anchors.get(0);
    Point anchorPos = layout.getNodePosition(fallbackAnchor);
    layout.connectBidirectional(fallbackAnchor, Direction.SOUTH, GOBLIN_VILLAGE_ID, Direction.NORTH);
    layout.setNodePosition(GOBLIN_VILLAGE_ID, anchorPos.x, anchorPos.y + 1);
  }

  private Map<String, Integer> computeDistancesFromStart(WorldLayout layout, String startMapId) {
    Map<String, Integer> distances = new LinkedHashMap<>();
    Queue<String> queue = new ArrayDeque<>();
    queue.add(startMapId);
    distances.put(startMapId, 0);

    while (!queue.isEmpty()) {
      String current = queue.poll();
      int base = distances.get(current);

      for (String next : layout.getConnectionsFrom(current).values()) {
        if (distances.containsKey(next)) {
          continue;
        }
        distances.put(next, base + 1);
        queue.add(next);
      }
    }

    return distances;
  }

  private boolean isReachable(WorldLayout layout, String startMapId, Set<String> expectedMaps) {
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new ArrayDeque<>();
    queue.add(startMapId);
    visited.add(startMapId);

    while (!queue.isEmpty()) {
      String current = queue.poll();
      for (String next : layout.getConnectionsFrom(current).values()) {
        if (visited.add(next)) {
          queue.add(next);
        }
      }
    }

    return visited.containsAll(expectedMaps);
  }

  private Point offsetPoint(Point base, Direction direction) {
    switch (direction) {
      case NORTH:
        return new Point(base.x, base.y - 1);
      case SOUTH:
        return new Point(base.x, base.y + 1);
      case EAST:
        return new Point(base.x + 1, base.y);
      case WEST:
      default:
        return new Point(base.x - 1, base.y);
    }
  }

  private static class DirectionPair {
    private final Direction anchorDirection;
    private final Direction roomDirection;

    private DirectionPair(Direction anchorDirection, Direction roomDirection) {
      this.anchorDirection = anchorDirection;
      this.roomDirection = roomDirection;
    }
  }
}