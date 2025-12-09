package com.rpggame.world;

import java.awt.*;
import com.rpggame.entities.Player;
import com.rpggame.core.GamePanel;
import com.rpggame.core.Game;
import com.rpggame.systems.CharacterStats;

/**
 * Sistema de Fog of War - controla a visibilidade dos tiles baseado na posição
 * do jogador
 */
public class FogOfWar {
  private boolean[][] explored; // Tiles que já foram explorados
  private boolean[][] visible; // Tiles atualmente visíveis
  private int mapWidth, mapHeight;
  private float visionRange;

  public FogOfWar(int mapWidth, int mapHeight) {
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    this.explored = new boolean[mapHeight][mapWidth];
    this.visible = new boolean[mapHeight][mapWidth];
    this.visionRange = 3.0f; // Range base de visão
  }

  /**
   * Atualiza a visibilidade baseada na posição do jogador
   */
  public void updateVisibility(Player player, TileType[][] map) {
    // Limpar visibilidade atual
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        visible[y][x] = false;
      }
    }

    // Calcular posição do jogador em tiles
    int playerTileX = (int) (player.getX() / GamePanel.TILE_SIZE);
    int playerTileY = (int) (player.getY() / GamePanel.TILE_SIZE);

    // Aplicar multiplicador de visão baseado na sabedoria
    float actualVisionRange = visionRange;
    if (player.getStats() != null) {
      actualVisionRange = visionRange * getVisionMultiplier(player.getStats().getWisdom());
    }

    // Usar algoritmo de ray-casting para determinar visibilidade
    int visionRadius = (int) Math.ceil(actualVisionRange);

    for (int dy = -visionRadius; dy <= visionRadius; dy++) {
      for (int dx = -visionRadius; dx <= visionRadius; dx++) {
        int targetX = playerTileX + dx;
        int targetY = playerTileY + dy;

        // Verificar se está dentro dos limites do mapa
        if (targetX >= 0 && targetX < mapWidth && targetY >= 0 && targetY < mapHeight) {
          // Calcular distância
          double distance = Math.sqrt(dx * dx + dy * dy);

          if (distance <= actualVisionRange) {
            // Verificar se há linha de visão clara
            if (hasLineOfSight(playerTileX, playerTileY, targetX, targetY, map)) {
              visible[targetY][targetX] = true;
              explored[targetY][targetX] = true;
            }
          }
        }
      }
    }
  }

  /**
   * Verifica se há linha de visão entre dois pontos usando algoritmo de Bresenham
   */
  private boolean hasLineOfSight(int x0, int y0, int x1, int y1, TileType[][] map) {
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);

    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;

    int err = dx - dy;
    int x = x0;
    int y = y0;

    while (true) {
      // Se chegou ao destino, verificar se o tile de destino é uma parede
      if (x == x1 && y == y1) {
        // Não pode ver através de paredes, mesmo que seja o tile de destino
        if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
          return map[y][x].isWalkable();
        }
        return true;
      }
      
      // Se encontrou uma parede (exceto na posição inicial), bloqueia a visão
      if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
        if (!map[y][x].isWalkable() && !(x == x0 && y == y0)) {
          return false;
        }
      }

      int e2 = 2 * err;

      if (e2 > -dy) {
        err -= dy;
        x += sx;
      }

      if (e2 < dx) {
        err += dx;
        y += sy;
      }
    }
  }

  /**
   * Calcula multiplicador de visão baseado na sabedoria
   */
  private float getVisionMultiplier(int wisdom) {
    int wisdomBonus = wisdom - CharacterStats.BASE_ATTRIBUTE;
    return 1.0f + (wisdomBonus * 0.15f); // 15% por ponto de sabedoria
  }

  /**
   * Renderiza o fog of war
   */
  public void render(Graphics2D g, Camera camera, TileType[][] map) {
    int startTileX = Math.max(0, (int) (camera.getX() / GamePanel.TILE_SIZE));
    int endTileX = Math.min(mapWidth, (int) ((camera.getX() + Game.SCREEN_WIDTH) / GamePanel.TILE_SIZE) + 1);
    int startTileY = Math.max(0, (int) (camera.getY() / GamePanel.TILE_SIZE));
    int endTileY = Math.min(mapHeight, (int) ((camera.getY() + Game.SCREEN_HEIGHT) / GamePanel.TILE_SIZE) + 1);

    for (int tileY = startTileY; tileY < endTileY; tileY++) {
      for (int tileX = startTileX; tileX < endTileX; tileX++) {
        int screenX = (int) (tileX * GamePanel.TILE_SIZE - camera.getX());
        int screenY = (int) (tileY * GamePanel.TILE_SIZE - camera.getY());

        if (!explored[tileY][tileX]) {
          // Tile não explorado - fog completo
          g.setColor(new Color(0, 0, 0, 200));
          g.fillRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        } else if (!visible[tileY][tileX]) {
          // Tile explorado mas não visível - fog parcial
          g.setColor(new Color(0, 0, 0, 100));
          g.fillRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        }
        // Tiles visíveis não têm fog
      }
    }
  }

  /**
   * Verifica se um tile está visível
   */
  public boolean isVisible(int tileX, int tileY) {
    if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
      return false;
    }
    return visible[tileY][tileX];
  }

  /**
   * Verifica se um tile foi explorado
   */
  public boolean isExplored(int tileX, int tileY) {
    if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
      return false;
    }
    return explored[tileY][tileX];
  }

  /**
   * Revela todo o mapa (para debug)
   */
  public void revealAll() {
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        explored[y][x] = true;
        visible[y][x] = true;
      }
    }
  }
}