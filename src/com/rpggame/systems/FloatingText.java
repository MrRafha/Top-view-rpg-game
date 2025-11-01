import java.awt.*;

/**
 * Classe para representar texto flutuante temporário (evasão, dano, etc.)
 */
public class FloatingText {
  private double x, y;
  private String text;
  private Color color;
  private int lifetime;
  private int currentLife;
  private boolean active;
  private double velocityY;

  public FloatingText(double x, double y, String text, Color color) {
    this.x = x;
    this.y = y;
    this.text = text;
    this.color = color;
    this.lifetime = 120; // 2 segundos a 60 FPS
    this.currentLife = 0;
    this.active = true;
    this.velocityY = -1.0; // Flutua para cima
  }

  public void update() {
    if (!active)
      return;

    // Mover para cima
    y += velocityY;

    // Incrementar tempo de vida
    currentLife++;

    // Verificar se deve ser removido
    if (currentLife >= lifetime) {
      active = false;
    }
  }

  public void render(Graphics2D g, Camera camera) {
    if (!active)
      return;

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    // Só renderizar se estiver na tela
    if (screenX > -100 && screenX < Game.SCREEN_WIDTH + 100 &&
        screenY > -50 && screenY < Game.SCREEN_HEIGHT + 50) {

      // Calcular transparência baseada no tempo de vida
      float alpha = 1.0f - ((float) currentLife / lifetime);
      Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
          (int) (alpha * 255));

      // Configurar fonte e cor
      g.setColor(fadeColor);
      g.setFont(new Font("Arial", Font.BOLD, 14));
      FontMetrics fm = g.getFontMetrics();

      // Centralizar o texto
      int textWidth = fm.stringWidth(text);
      g.drawString(text, screenX - textWidth / 2, screenY);

      // Adicionar contorno para melhor visibilidade
      g.setColor(new Color(0, 0, 0, (int) (alpha * 180)));
      g.drawString(text, screenX - textWidth / 2 + 1, screenY + 1);
    }
  }

  // Getters
  public boolean isActive() {
    return active;
  }
}