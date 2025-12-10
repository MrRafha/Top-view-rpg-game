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
      12, 3 // Spawn em tile (12, 3) - na frente dos portais (que estão em y=0)
    ));
    
    // Vila com praia à esquerda
    maps.put("village", new MapData(
      "maps/village.txt",
      "Vila da Praia",
      12, 22 // Spawn em tile (12, 22) - logo acima dos portais
    ));
    
    // Exemplo de mapa menor
    maps.put("cave", new MapData(
      "maps/new_map_15x15.txt",
      "Caverna",
      7, 7 // Centro do mapa 15x15
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
    private int spawnTileX;
    private int spawnTileY;
    
    public MapData(String filePath, String name, int spawnTileX, int spawnTileY) {
      this.filePath = filePath;
      this.name = name;
      this.spawnTileX = spawnTileX;
      this.spawnTileY = spawnTileY;
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
    
    // Para compatibilidade, retorna pixels
    public int getDefaultSpawnX() {
      return spawnTileX * 48; // TILE_SIZE = 48
    }
    
    public int getDefaultSpawnY() {
      return spawnTileY * 48;
    }
  }
}
