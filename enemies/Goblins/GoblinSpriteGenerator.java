import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Gerador de sprite para Goblin
 */
public class GoblinSpriteGenerator {
  public static void main(String[] args) {
    createGoblinSprite();
  }
  
  public static void createGoblinSprite() {
    int width = 32;
    int height = 32;
    
    BufferedImage sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = sprite.createGraphics();
    
    // Fundo transparente
    g.setComposite(AlphaComposite.Clear);
    g.fillRect(0, 0, width, height);
    g.setComposite(AlphaComposite.SrcOver);
    
    // Corpo do Goblin (verde escuro)
    g.setColor(new Color(34, 139, 34));
    g.fillOval(8, 12, 16, 18);
    
    // Cabeça (verde mais claro)
    g.setColor(new Color(50, 205, 50));
    g.fillOval(10, 4, 12, 12);
    
    // Olhos (vermelho)
    g.setColor(Color.RED);
    g.fillOval(12, 7, 2, 2);
    g.fillOval(18, 7, 2, 2);
    
    // Orelhas pontudas
    g.setColor(new Color(34, 139, 34));
    int[] xPoints = {8, 10, 12};
    int[] yPoints = {8, 4, 8};
    g.fillPolygon(xPoints, yPoints, 3);
    
    int[] xPoints2 = {20, 22, 24};
    int[] yPoints2 = {8, 4, 8};
    g.fillPolygon(xPoints2, yPoints2, 3);
    
    // Braços
    g.setColor(new Color(34, 139, 34));
    g.fillOval(4, 14, 6, 8);
    g.fillOval(22, 14, 6, 8);
    
    // Pernas
    g.fillOval(10, 26, 4, 6);
    g.fillOval(18, 26, 4, 6);
    
    // Arma (bastão)
    g.setColor(new Color(139, 69, 19));
    g.fillRect(28, 8, 2, 16);
    
    // Borda para definição
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(1));
    g.drawOval(10, 4, 12, 12); // Cabeça
    g.drawOval(8, 12, 16, 18); // Corpo
    
    g.dispose();
    
    // Salvar o sprite
    try {
      File outputFile = new File("enemies/Goblins/goblin_sprite.png");
      outputFile.getParentFile().mkdirs();
      ImageIO.write(sprite, "PNG", outputFile);
      System.out.println("Sprite do Goblin criado com sucesso!");
    } catch (IOException e) {
      System.err.println("Erro ao salvar sprite do Goblin: " + e.getMessage());
    }
  }
}