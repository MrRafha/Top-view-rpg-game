package com.rpggame.world;

import java.awt.*;
import com.rpggame.entities.Player;
import com.rpggame.core.GamePanel;
import com.rpggame.core.Game;
import com.rpggame.systems.CharacterStats;

/**
 * Sistema de Fog of War - controla a visibilidade dos tiles baseado na posição
 * do jogador.
 *
 * Correcao de race condition (tiles piscando pretos):
 * O cálculo de visibilidade é feito em um array de trabalho (visibleWork) e só
 * é publicado para o array de leitura (visibleRead) via troca atomica de
 * referencia ao final do calculo. A thread de render lê visibleRead e nunca
 * ve o estado intermediario "tudo false".
 *
 * explored[][] nao precisa de double-buffer porque só vai de false->true
 * (nunca regride) — ler um valor desatualizado por um frame nao causa piscar.
 */
public class FogOfWar {
  private static final Color UNEXPLORED_FOG_COLOR = new Color(0, 0, 0, 200);
  private static final Color EXPLORED_FOG_COLOR = new Color(0, 0, 0, 100);

  private boolean[][] explored;      // Tiles ja explorados (monotonicamente true)
  private volatile boolean[][] visibleRead;  // Lido pela thread de render (EDT)
  private boolean[][] visibleWork;           // Escrito pela thread de update

  private int mapWidth, mapHeight;
  private float visionRange;

  public FogOfWar(int mapWidth, int mapHeight) {
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    this.explored    = new boolean[mapHeight][mapWidth];
    this.visibleRead = new boolean[mapHeight][mapWidth];
    this.visibleWork = new boolean[mapHeight][mapWidth];
    this.visionRange = 3.0f;
  }

  /**
   * Atualiza a visibilidade baseada na posicao do jogador.
   * Deve ser chamado apenas pela thread de update (GamePanel.update).
   * Nunca bloqueia a thread de render.
   */
  public void updateVisibility(Player player, TileType[][] map) {
    // Limpar apenas o array de trabalho — visibleRead continua intacto
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        visibleWork[y][x] = false;
      }
    }

    int playerTileX = (int) (player.getX() / GamePanel.TILE_SIZE);
    int playerTileY = (int) (player.getY() / GamePanel.TILE_SIZE);

    float actualVisionRange = visionRange;
    if (player.getStats() != null) {
      actualVisionRange = visionRange * getVisionMultiplier(player.getStats().getWisdom());
    }

    int visionRadius = (int) Math.ceil(actualVisionRange);

    for (int dy = -visionRadius; dy <= visionRadius; dy++) {
      for (int dx = -visionRadius; dx <= visionRadius; dx++) {
        int targetX = playerTileX + dx;
        int targetY = playerTileY + dy;

        if (targetX >= 0 && targetX < mapWidth && targetY >= 0 && targetY < mapHeight) {
          double distance = Math.sqrt(dx * dx + dy * dy);
          if (distance <= actualVisionRange) {
            if (hasLineOfSight(playerTileX, playerTileY, targetX, targetY, map)) {
              visibleWork[targetY][targetX] = true;
              explored[targetY][targetX] = true;  // explored so vai true->true, seguro sem lock
            }
          }
        }
      }
    }

    // Publicar o novo estado de visibilidade atomicamente.
    // A troca de referencia e atomica na JVM (volatile garante visibilidade imediata).
    // A thread de render que estava lendo visibleRead antigo termina sem problema
    // (o array antigo se torna o novo visibleWork na proxima chamada).
    boolean[][] temp = visibleRead;
    visibleRead = visibleWork;
    visibleWork = temp;
  }

  private boolean hasLineOfSight(int x0, int y0, int x1, int y1, TileType[][] map) {
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int x = x0;
    int y = y0;

    while (true) {
      if (x == x1 && y == y1) {
        if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
          TileType tileType = map[y][x];
          return tileType != TileType.WALL && tileType != TileType.STONE;
        }
        return true;
      }

      if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
        if (!(x == x0 && y == y0)) {
          TileType tileType = map[y][x];
          if (tileType == TileType.WALL || tileType == TileType.STONE) {
            return false;
          }
        }
      }

      int e2 = 2 * err;
      if (e2 > -dy) { err -= dy; x += sx; }
      if (e2 < dx)  { err += dx; y += sy; }
    }
  }

  private float getVisionMultiplier(int wisdom) {
    int wisdomBonus = wisdom - CharacterStats.BASE_ATTRIBUTE;
    return 1.0f + (wisdomBonus * 0.15f);
  }

  /**
   * Renderiza o fog of war. Chamado pela EDT (paintComponent).
   * Le visibleRead — nunca ve estado intermediario zerado.
   */
  public void render(Graphics2D g, Camera camera, TileType[][] map) {
    // Captura referencia local para consistencia durante todo o frame de render
    boolean[][] vis = visibleRead;

    int startTileX = Math.max(0, (int) (camera.getX() / GamePanel.TILE_SIZE));
    int endTileX   = Math.min(mapWidth,  (int) ((camera.getX() + Game.SCREEN_WIDTH)  / GamePanel.TILE_SIZE) + 1);
    int startTileY = Math.max(0, (int) (camera.getY() / GamePanel.TILE_SIZE));
    int endTileY   = Math.min(mapHeight, (int) ((camera.getY() + Game.SCREEN_HEIGHT) / GamePanel.TILE_SIZE) + 1);

    for (int tileY = startTileY; tileY < endTileY; tileY++) {
      for (int tileX = startTileX; tileX < endTileX; tileX++) {
        int screenX = (int) (tileX * GamePanel.TILE_SIZE - camera.getX());
        int screenY = (int) (tileY * GamePanel.TILE_SIZE - camera.getY());

        if (!explored[tileY][tileX]) {
          g.setColor(UNEXPLORED_FOG_COLOR);
          g.fillRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        } else if (!vis[tileY][tileX]) {
          g.setColor(EXPLORED_FOG_COLOR);
          g.fillRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);
        }
      }
    }
  }

  public boolean isVisible(int tileX, int tileY) {
    if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) return false;
    return visibleRead[tileY][tileX];
  }

  public boolean isExplored(int tileX, int tileY) {
    if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) return false;
    return explored[tileY][tileX];
  }

  public void revealAll() {
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        explored[y][x]    = true;
        visibleRead[y][x] = true;
        visibleWork[y][x] = true;
      }
    }
  }

  public void resetFog() {
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        explored[y][x]    = false;
        visibleRead[y][x] = false;
        visibleWork[y][x] = false;
      }
    }
  }
}
