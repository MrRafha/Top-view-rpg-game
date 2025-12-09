package com.rpggame.world;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia múltiplos mapas e suas conexões via portais
 */
public class MapManager {
  private Map<String, MapData> maps;
  private String currentMapId;
  
  public MapManager() {
    this.maps = new HashMap<>();
    initializeMaps();
  }
  
  /**
   * Inicializa os mapas disponíveis no jogo
   */
  private void initializeMaps() {
    // Mapa principal (territórios de goblins)
    maps.put("goblin_territories", new MapData(
      "maps/goblin_territories_25x25.txt",
      "Territórios Goblin",
      600, 600 // Spawn padrão
    ));
    
    // Exemplo de novo mapa (vila)
    maps.put("village", new MapData(
      "maps/example.txt", // Usar mapa de exemplo por enquanto
      "Vila",
      400, 400
    ));
    
    // Exemplo de mapa menor
    maps.put("cave", new MapData(
      "maps/new_map_15x15.txt",
      "Caverna",
      200, 200
    ));
    
    currentMapId = "goblin_territories";
    System.out.println("🗺️ MapManager inicializado com " + maps.size() + " mapas");
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
  
  public String getCurrentMapId() {
    return currentMapId;
  }
  
  /**
   * Classe interna para armazenar dados de um mapa
   */
  public static class MapData {
    private String filePath;
    private String name;
    private int defaultSpawnX;
    private int defaultSpawnY;
    
    public MapData(String filePath, String name, int spawnX, int spawnY) {
      this.filePath = filePath;
      this.name = name;
      this.defaultSpawnX = spawnX;
      this.defaultSpawnY = spawnY;
    }
    
    public String getFilePath() {
      return filePath;
    }
    
    public String getName() {
      return name;
    }
    
    public int getDefaultSpawnX() {
      return defaultSpawnX;
    }
    
    public int getDefaultSpawnY() {
      return defaultSpawnY;
    }
  }
}
