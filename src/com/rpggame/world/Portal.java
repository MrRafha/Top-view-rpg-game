package com.rpggame.world;

/**
 * Representa um portal no mapa que leva a outro mapa
 */
public class Portal {
  private int x, y; // Posição do portal no mapa (em tiles)
  private String targetMapId; // ID do mapa de destino
  private int targetX, targetY; // Posição de spawn no mapa de destino (em pixels)
  private String name; // Nome do portal (opcional)
  
  public Portal(int x, int y, String targetMapId, int targetX, int targetY) {
    this(x, y, targetMapId, targetX, targetY, "Portal");
  }
  
  public Portal(int x, int y, String targetMapId, int targetX, int targetY, String name) {
    this.x = x;
    this.y = y;
    this.targetMapId = targetMapId;
    this.targetX = targetX;
    this.targetY = targetY;
    this.name = name;
  }
  
  /**
   * Verifica se o jogador está sobre este portal
   */
  public boolean isPlayerOn(int playerTileX, int playerTileY) {
    return playerTileX == x && playerTileY == y;
  }
  
  // Getters
  public int getX() {
    return x;
  }
  
  public int getY() {
    return y;
  }
  
  public String getTargetMapId() {
    return targetMapId;
  }
  
  public int getTargetX() {
    return targetX;
  }
  
  public int getTargetY() {
    return targetY;
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public String toString() {
    return String.format("Portal '%s' (%d,%d) -> %s (%d,%d)", 
      name, x, y, targetMapId, targetX, targetY);
  }
}
