package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;

/**
 * Habilidade: Armadilha Mortal
 * Deixa uma armadilha no chão que causa dano leve e atordoa por 3 segundos
 */
public class DeadlyTrapSkill extends Skill {

  private class Trap {
    double x;
    double y;
    int lifetime; // Em frames
    boolean active;
    static final int TRAP_SIZE = 48;
    static final int MAX_LIFETIME = 900; // 15 segundos a 60 FPS
    static final int STUN_DURATION = 180; // 3 segundos a 60 FPS
    static final int TRAP_DAMAGE = 15;

    BufferedImage sprite;

    Trap(double x, double y) {
      this.x = x;
      this.y = y;
      this.lifetime = MAX_LIFETIME;
      this.active = true;
      loadSprite();
    }

    private void loadSprite() {
      try {
        // Tentar carregar como recurso do classpath (funciona no JAR)
        InputStream is = getClass().getClassLoader().getResourceAsStream("sprites/HunterTrap.png");
        if (is != null) {
          sprite = ImageIO.read(is);
          is.close();
          System.out.println("✅ Sprite da armadilha carregado do JAR!");
        } else {
          // Fallback: tentar carregar como arquivo externo (desenvolvimento)
          String path = com.rpggame.world.ResourceResolver.getResourcePath("sprites/HunterTrap.png");
          File spriteFile = new File(path);
          if (spriteFile.exists()) {
            sprite = ImageIO.read(spriteFile);
            System.out.println("✅ Sprite da armadilha carregado do arquivo!");
          } else {
            System.err.println("⚠️ Sprite HunterTrap.png não encontrado");
            createDefaultSprite();
          }
        }
      } catch (Exception e) {
        System.err.println("❌ Erro ao carregar sprite da armadilha: " + e.getMessage());
        createDefaultSprite();
      }
    }

    private void createDefaultSprite() {
      sprite = new BufferedImage(TRAP_SIZE, TRAP_SIZE, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = sprite.createGraphics();

      // Fundo transparente
      g.setComposite(AlphaComposite.Clear);
      g.fillRect(0, 0, TRAP_SIZE, TRAP_SIZE);
      g.setComposite(AlphaComposite.SrcOver);

      // Desenhar armadilha simples (círculo com dentes)
      g.setColor(new Color(100, 100, 100));
      g.fillOval(8, 8, 32, 32);

      g.setColor(new Color(150, 150, 150));
      g.fillOval(12, 12, 24, 24);

      // Dentes da armadilha
      g.setColor(new Color(80, 80, 80));
      for (int i = 0; i < 8; i++) {
        double angle = i * Math.PI / 4;
        int x1 = 24 + (int) (Math.cos(angle) * 12);
        int y1 = 24 + (int) (Math.sin(angle) * 12);
        int x2 = 24 + (int) (Math.cos(angle) * 18);
        int y2 = 24 + (int) (Math.sin(angle) * 18);
        g.drawLine(x1, y1, x2, y2);
      }

      g.dispose();
    }

    void update() {
      lifetime--;
      if (lifetime <= 0) {
        active = false;
        System.out.println("🪤 Armadilha expirou!");
      }
    }

    boolean checkCollision(Enemy enemy) {
      Rectangle trapBounds = new Rectangle((int) x, (int) y, TRAP_SIZE, TRAP_SIZE);
      Rectangle enemyBounds = enemy.getBounds();
      return trapBounds.intersects(enemyBounds);
    }

    void trigger(Enemy enemy) {
      enemy.takeDamage(TRAP_DAMAGE);
      enemy.applyStun(STUN_DURATION);
      active = false;
      System.out.println("🪤 ARMADILHA ATIVADA! Causou " + TRAP_DAMAGE + " de dano e atordoou o inimigo!");
    }

    void render(Graphics2D g, Camera camera) {
      if (!active)
        return;

      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());

      if (sprite != null) {
        // Desenhar sprite
        g.drawImage(sprite, screenX, screenY, null);

        // Piscar nos últimos 3 segundos
        if (lifetime < 180 && (lifetime / 10) % 2 == 0) {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
          g.setColor(Color.RED);
          g.fillRect(screenX, screenY, TRAP_SIZE, TRAP_SIZE);
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
      }
    }
  }

  private ArrayList<Trap> traps;
  private Player currentPlayer;

  public DeadlyTrapSkill() {
    super("Armadilha Mortal",
        "Deixa armadilha que atordoa inimigos por 3s",
        12, // 12 segundos de cooldown
        "Hunter",
        10); // 10 de mana
    traps = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    this.currentPlayer = player;

    // Colocar armadilha na posição atual do jogador (um pouco atrás)
    double facing = player.getFacingDirection();
    double offsetX = -Math.cos(facing) * 20; // Atrás do jogador
    double offsetY = -Math.sin(facing) * 20;

    double trapX = player.getX() + offsetX;
    double trapY = player.getY() + offsetY;

    Trap trap = new Trap(trapX, trapY);
    traps.add(trap);

    System.out.println("🪤 ARMADILHA COLOCADA! Posição: (" + (int) trapX + ", " + (int) trapY + ")");
  }

  @Override
  public void update() {
    super.update();

    if (currentPlayer == null)
      return;

    EnemyManager enemyManager = currentPlayer.getEnemyManager();
    if (enemyManager == null)
      return;

    ArrayList<Enemy> enemies = enemyManager.getEnemies();

    Iterator<Trap> iterator = traps.iterator();
    while (iterator.hasNext()) {
      Trap trap = iterator.next();

      if (!trap.active) {
        iterator.remove();
        continue;
      }

      trap.update();

      // Verificar colisão com inimigos
      for (Enemy enemy : enemies) {
        if (!enemy.isAlive())
          continue;

        if (trap.checkCollision(enemy)) {
          trap.trigger(enemy);
          break;
        }
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    // Renderizar todas as armadilhas ativas
    for (Trap trap : traps) {
      if (trap.active) {
        trap.render(g, camera);
      }
    }
  }
}
