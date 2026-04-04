package com.rpggame.world;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Gerencia múltiplos mapas e suas conexões via portais
 */
public class MapManager {
  private Map<String, MapData> maps;
  // Catálogo base usado pelo gerador procedural.
  private Map<String, RoomTemplate> roomTemplates;
  // Grafo final do mundo com conexões entre salas.
  private WorldLayout worldLayout;
  // Estado de progressão: quais salas o jogador já visitou.
  private Set<String> discoveredMapIds;
  private String currentMapId;

  public MapManager() {
    this.maps = new LinkedHashMap<>();
    this.discoveredMapIds = new LinkedHashSet<>();
    initializeMaps();
  }

  /**
   * Inicializa os mapas disponíveis no jogo
   */
  private void initializeMaps() {
    // 1) Carrega templates fixos (metadados + entradas) para cada sala disponível.
    roomTemplates = WorldTemplateRegistry.createDefaultTemplates();

    // 2) Espelha templates em MapData para manter compatibilidade com sistemas
    // existentes.
    for (RoomTemplate template : roomTemplates.values()) {
      maps.put(template.getId(), new MapData(
          template.getMapFile(),
          template.getDisplayName(),
          template.getSpawnTileX(),
          template.getSpawnTileY(),
          template.getRoomType()));
    }

    // 3) Gera o layout procedural conectando as salas respeitando direções de
    // entrada/saída.
    WorldGenerator generator = new WorldGenerator();
    worldLayout = generator.generate(roomTemplates, "village", System.currentTimeMillis());

    // 4) Define início e já marca a sala inicial como descoberta.
    currentMapId = worldLayout.getStartMapId();
    discoveredMapIds.add(currentMapId);
    System.out.println("🗺️ MapManager inicializado com " + maps.size() + " mapas");
    System.out.println("🧩 Layout procedural gerado com início em: " + currentMapId);
  }

  /**
   * Obtém dados de um mapa
   */
  public MapData getMap(String mapId) {
    return maps.get(mapId);
  }

  /**
   * Obtém o mapa atual
   */
  public MapData getCurrentMap() {
    return maps.get(currentMapId);
  }

  /**
   * Define o mapa atual
   */
  public void setCurrentMap(String mapId) {
    if (maps.containsKey(mapId)) {
      currentMapId = mapId;
      // Toda troca de mapa consolida descoberta para progressão do mapa mundi.
      discoveredMapIds.add(mapId);
      System.out.println("📍 Mapa atual: " + mapId);
    } else {
      System.err.println("❌ Mapa não encontrado: " + mapId);
    }
  }

  /**
   * Verifica se um mapa existe
   */
  public boolean hasMap(String mapId) {
    return maps.containsKey(mapId);
  }

  /**
   * Busca o mapId correspondente a um caminho de arquivo de mapa.
   */
  public String findMapIdByFilePath(String filePath) {
    for (Map.Entry<String, MapData> entry : maps.entrySet()) {
      if (entry.getValue().getFilePath().equals(filePath)) {
        return entry.getKey();
      }
    }
    return null;
  }

  /**
   * Retorna uma visão somente leitura dos mapas registrados.
   */
  public Map<String, MapData> getAllMaps() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(maps));
  }

  /**
   * Retorna o layout procedural atual do mundo.
   */
  public WorldLayout getWorldLayout() {
    return worldLayout;
  }

  /**
   * Retorna um template de sala pelo id do mapa.
   */
  public RoomTemplate getRoomTemplate(String mapId) {
    return roomTemplates.get(mapId);
  }

  /**
   * Retorna todos os templates registrados.
   */
  public Map<String, RoomTemplate> getRoomTemplates() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(roomTemplates));
  }

  /**
   * Retorna os mapas já descobertos pelo jogador.
   */
  public Set<String> getDiscoveredMapIds() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(discoveredMapIds));
  }

  /**
   * Verifica se um mapa já foi descoberto.
   */
  public boolean isMapDiscovered(String mapId) {
    return discoveredMapIds.contains(mapId);
  }

  public String getCurrentMapId() {
    return currentMapId;
  }

  /**
   * Classe interna para armazenar dados de um mapa
   */
  public static class MapData {
    private String filePath;
    private String name;
    private int spawnTileX;
    private int spawnTileY;
    private RoomType roomType;

    public MapData(String filePath, String name, int spawnTileX, int spawnTileY, RoomType roomType) {
      this.filePath = filePath;
      this.name = name;
      this.spawnTileX = spawnTileX;
      this.spawnTileY = spawnTileY;
      this.roomType = roomType;
    }

    public String getFilePath() {
      return filePath;
    }

    public String getName() {
      return name;
    }

    public int getSpawnTileX() {
      return spawnTileX;
    }

    public int getSpawnTileY() {
      return spawnTileY;
    }

    public RoomType getRoomType() {
      return roomType;
    }

    // Para compatibilidade, retorna pixels
    public int getDefaultSpawnX() {
      return spawnTileX * 48; // TILE_SIZE = 48
    }

    public int getDefaultSpawnY() {
      return spawnTileY * 48;
    }
  }
}
