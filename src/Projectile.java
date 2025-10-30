import java.awt.*;

/**
 * Classe para representar projéteis no jogo
 */
public class Projectile {
  private double x, y;
  private double dx, dy;
  private double speed;
  private int damage;
  private String type;
  private Color color;
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
        this.color = new Color(100, 100, 255);
        this.size = 6;
        this.lifetime = 120; // 2 segundos a 60 FPS
        break;
      case ARROW:
        this.speed = 12.0;
        this.color = new Color(139, 69, 19);
        this.size = 4;
        this.lifetime = 180; // 3 segundos a 60 FPS
        break;
      case SWORD_SLASH:
        this.speed = 6.0;
        this.color = new Color(255, 215, 0);
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

  public void render(Graphics2D g, Camera camera) {
    if (!active)
      return;

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Só renderizar se estiver na tela
    if (screenX > -size && screenX < Game.SCREEN_WIDTH + size &&
        screenY > -size && screenY < Game.SCREEN_HEIGHT + size) {

      g.setColor(color);

      switch (type) {
        case MAGIC_BOLT:
          // Desenhar um orbe mágico
          g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
          g.setColor(new Color(200, 200, 255, 150));
          g.fillOval(screenX - size / 3, screenY - size / 3, size / 2, size / 2);
          break;

        case ARROW:
          // Desenhar uma flecha
          double angle = Math.atan2(dy, dx);
          int[] xPoints = {
              (int) (screenX + Math.cos(angle) * size),
              (int) (screenX - Math.cos(angle) * size / 2 + Math.sin(angle) * 2),
              (int) (screenX - Math.cos(angle) * size / 2 - Math.sin(angle) * 2)
          };
          int[] yPoints = {
              (int) (screenY + Math.sin(angle) * size),
              (int) (screenY - Math.sin(angle) * size / 2 - Math.cos(angle) * 2),
              (int) (screenY - Math.sin(angle) * size / 2 + Math.cos(angle) * 2)
          };
          g.fillPolygon(xPoints, yPoints, 3);
          break;

        case SWORD_SLASH:
          // Desenhar um efeito de corte
          float alpha = 1.0f - ((float) currentLife / lifetime);
          g.setColor(new Color(255, 215, 0, (int) (alpha * 255)));

          double slashAngle = Math.atan2(dy, dx);
          int slashLength = size;
          int x1 = (int) (screenX - Math.cos(slashAngle) * slashLength / 2);
          int y1 = (int) (screenY - Math.sin(slashAngle) * slashLength / 2);
          int x2 = (int) (screenX + Math.cos(slashAngle) * slashLength / 2);
          int y2 = (int) (screenY + Math.sin(slashAngle) * slashLength / 2);

          g.setStroke(new BasicStroke(3));
          g.drawLine(x1, y1, x2, y2);
          g.setStroke(new BasicStroke(1));
          break;
      }
    }
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