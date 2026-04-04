package com.rpggame.entities;

import java.awt.Rectangle;
import com.rpggame.core.GamePanel;

/**
 * Classe para representar projéteis no jogo
 */
public class Projectile {
  private double x, y;
  private double dx, dy;
  private double speed;
  private int damage;
  private String type;
  private int size;
  private int lifetime; // frames até desaparecer
  private int currentLife;
  private boolean active;

  // Tipos de projétil
  public static final String MAGIC_BOLT = "magic_bolt";
  public static final String ARROW = "arrow";
  public static final String SWORD_SLASH = "sword_slash";

  public Projectile(double x, double y, double angle, String type, int damage) {
    this.x = x;
    this.y = y;
    this.type = type;
    this.damage = damage;
    this.active = true;

    // Configurar propriedades baseado no tipo
    switch (type) {
      case MAGIC_BOLT:
        this.speed = 8.0;
        this.size = 6;
        this.lifetime = 120; // 2 segundos a 60 FPS
        break;
      case ARROW:
        this.speed = 12.0;
        this.size = 4;
        this.lifetime = 180; // 3 segundos a 60 FPS
        break;
      case SWORD_SLASH:
        this.speed = 6.0;
        this.size = 12;
        this.lifetime = 20; // 0.33 segundos - ataque rápido
        break;
    }

    // Calcular velocidade baseada no ângulo
    this.dx = Math.cos(angle) * speed;
    this.dy = Math.sin(angle) * speed;
    this.currentLife = 0;
  }

  public void update() {
    if (!active)
      return;

    // Mover o projétil
    x += dx;
    y += dy;

    // Incrementar tempo de vida
    currentLife++;

    // Verificar se deve ser removido
    if (currentLife >= lifetime) {
      active = false;
    }

    // Verificar limites do mundo
    if (x < 0 || x > GamePanel.MAP_WIDTH * GamePanel.TILE_SIZE ||
        y < 0 || y > GamePanel.MAP_HEIGHT * GamePanel.TILE_SIZE) {
      active = false;
    }
  }

  /**
   * Verifica se o projétil colidiu com uma parede
   */
  public boolean checkWallCollision(com.rpggame.world.TileMap tileMap) {
    if (!active)
      return false;

    int tileX = (int) (x / GamePanel.TILE_SIZE);
    int tileY = (int) (y / GamePanel.TILE_SIZE);

    if (!tileMap.isWalkable(tileX, tileY)) {
      active = false;
      return true;
    }
    return false;
  }

  // Verificar colisão com retângulo
  public boolean collidesWith(double targetX, double targetY, int targetWidth, int targetHeight) {
    if (!active)
      return false;

    return x >= targetX && x <= targetX + targetWidth &&
        y >= targetY && y <= targetY + targetHeight;
  }

  // Getters
  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public int getDamage() {
    return damage;
  }

  public String getType() {
    return type;
  }

  public boolean isActive() {
    return active;
  }

  public int getSize() {
    return size;
  }

  public double getDx() {
    return dx;
  }

  public double getDy() {
    return dy;
  }

  /**
   * Retorna o retângulo de colisão do projétil.
   */
  public Rectangle getBounds() {
    return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
  }

  // Setter
  public void setActive(boolean active) {
    this.active = active;
  }
}