package com.rpggame.world;

import com.rpggame.entities.Player;
import com.rpggame.core.Game;
import com.rpggame.core.GamePanel;

/**
 * Sistema de câmera para seguir o jogador
 */
public class Camera {
  private double x, y;

  public Camera(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public void centerOnPlayer(Player player) {
    // Centralizar a câmera no jogador
    x = player.getX() + player.getWidth() / 2.0 - Game.SCREEN_WIDTH / 2.0;
    y = player.getY() + player.getHeight() / 2.0 - Game.SCREEN_HEIGHT / 2.0;

    // Limitar a câmera aos bounds do mapa
    double mapWidth = GamePanel.MAP_WIDTH * GamePanel.TILE_SIZE;
    double mapHeight = GamePanel.MAP_HEIGHT * GamePanel.TILE_SIZE;

    x = Math.max(0, Math.min(x, mapWidth - Game.SCREEN_WIDTH));
    y = Math.max(0, Math.min(y, mapHeight - Game.SCREEN_HEIGHT));
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }
}