package com.rpggame.systems.skills;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import com.rpggame.entities.Enemy;
import com.rpggame.entities.Player;
import com.rpggame.systems.EnemyManager;
import com.rpggame.systems.Skill;
import com.rpggame.world.Camera;

/**
 * Habilidade do Archer: Flecha Perfurante
 * Dispara uma flecha que atravessa todos os inimigos em linha reta
 */
public class PiercingArrowSkill extends Skill {
  private ArrayList<PiercingArrow> activeArrows;

  private static final double ARROW_SPEED = 12.0;
  private static final double ARROW_RANGE = 500.0;
  private static final int ARROW_WIDTH = 8;
  private static final int ARROW_HEIGHT = 30;

  public PiercingArrowSkill() {
    super("Flecha Perfurante",
        "Dispara uma flecha mágica que atravessa todos os inimigos em seu caminho",
        20, // 20 segundos de cooldown
        "Archer");

    this.activeArrows = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    // Criar nova flecha perfurante
    double startX = player.getX() + player.getWidth() / 2;
    double startY = player.getY() + player.getHeight() / 2;
    double direction = player.getFacingDirection();

    PiercingArrow arrow = new PiercingArrow(
        startX, startY, direction, ARROW_SPEED, ARROW_RANGE, player);

    activeArrows.add(arrow);
    System.out.println("🏹 Flecha Perfurante disparada!");
  }

  @Override
  public void update() {
    super.update();

    // Atualizar flechas
    for (int i = activeArrows.size() - 1; i >= 0; i--) {
      PiercingArrow arrow = activeArrows.get(i);
      arrow.update();

      if (arrow.shouldRemove()) {
        activeArrows.remove(i);
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    // Renderizar flechas
    for (PiercingArrow arrow : activeArrows) {
      arrow.render(g, camera);
    }
  }

  /**
   * Classe interna para a flecha perfurante
   */
  private class PiercingArrow {
    private double x, y;
    private double dx, dy;
    private double speed;
    private double maxRange;
    private double travelDistance;
    private Player owner;
    private double direction;
    private ArrayList<Enemy> hitEnemies; // Para evitar múltiplos hits no mesmo inimigo
    private int trailTimer = 0;

    public PiercingArrow(double startX, double startY, double direction,
        double speed, double range, Player owner) {
      this.x = startX;
      this.y = startY;
      this.speed = speed;
      this.maxRange = range;
      this.owner = owner;
      this.direction = direction;
      this.travelDistance = 0;
      this.hitEnemies = new ArrayList<>();

      this.dx = Math.cos(direction) * speed;
      this.dy = Math.sin(direction) * speed;
    }

    public void update() {
      // Mover flecha
      x += dx;
      y += dy;
      travelDistance += speed;
      trailTimer++;

      // Verificar colisão com inimigos (perfurante - não para ao atingir)
      checkEnemyCollisions();
    }

    private void checkEnemyCollisions() {
      EnemyManager enemyManager = owner.getEnemyManager();
      if (enemyManager == null)
        return;

      for (Enemy enemy : enemyManager.getEnemies()) {
        if (enemy.isAlive() && !hitEnemies.contains(enemy)) {
          // Verificar colisão em linha com a flecha
          if (isEnemyInArrowPath(enemy)) {
            hitEnemies.add(enemy);

            // Dano baseado na Destreza (DEX) - atributo principal do Arqueiro/Hunter
            int damage = (int) (owner.getStats().getDexterity() * 2.5 + 20);
            enemy.takeDamage(damage);

            System.out.println("🎯 Flecha Perfurante atingiu inimigo! (" +
                hitEnemies.size() + " inimigos perfurados)");

            // TODO: Adicionar knockback quando o método for implementado na classe Enemy
          }
        }
      }
    }

    /**
     * Verifica se um inimigo está no caminho da flecha
     */
    private boolean isEnemyInArrowPath(Enemy enemy) {
      // Verificar se o inimigo está próximo da linha da flecha
      double enemyCenterX = enemy.getX() + enemy.getWidth() / 2;
      double enemyCenterY = enemy.getY() + enemy.getHeight() / 2;

      // Calcular distância do ponto (inimigo) à linha (flecha)
      double A = -dy; // -sin
      double B = dx; // cos
      double C = -(A * x + B * y);

      double distance = Math.abs(A * enemyCenterX + B * enemyCenterY + C) /
          Math.sqrt(A * A + B * B);

      // Se a distância é menor que o raio de colisão
      if (distance <= 20) {
        // Verificar se o inimigo está à frente da flecha
        double dotProduct = (enemyCenterX - x) * dx + (enemyCenterY - y) * dy;
        return dotProduct > 0; // Inimigo está à frente
      }

      return false;
    }

    public boolean shouldRemove() {
      return travelDistance >= maxRange;
    }

    public void render(Graphics2D g, Camera camera) {
      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());

      // Rastro da flecha (efeito de movimento)
      renderTrail(g, camera);

      // Corpo da flecha
      renderArrow(g, screenX, screenY);

      // Efeito mágico (brilho azul/verde)
      renderMagicEffect(g, screenX, screenY);
    }

    private void renderTrail(Graphics2D g, Camera camera) {
      // Desenhar rastro atrás da flecha
      for (int i = 1; i <= 5; i++) {
        double trailX = x - dx * i * 0.8;
        double trailY = y - dy * i * 0.8;

        int trailScreenX = (int) (trailX - camera.getX());
        int trailScreenY = (int) (trailY - camera.getY());

        float alpha = 0.3f - (i * 0.05f);
        g.setColor(new Color(0, 200, 255, (int) (alpha * 255)));
        g.fillOval(trailScreenX - 2, trailScreenY - 2, 4, 4);
      }
    }

    private void renderArrow(Graphics2D g, int screenX, int screenY) {
      g.setColor(new Color(139, 69, 19)); // Marrom para o cabo

      // Calcular posições da flecha baseado na direção
      double cos = Math.cos(direction);
      double sin = Math.sin(direction);

      // Ponta da flecha
      int[] xPoints = {
          screenX + (int) (cos * 15),
          screenX + (int) (cos * 5 - sin * 3),
          screenX + (int) (cos * 5 + sin * 3)
      };
      int[] yPoints = {
          screenY + (int) (sin * 15),
          screenY + (int) (sin * 5 + cos * 3),
          screenY + (int) (sin * 5 - cos * 3)
      };

      // Desenhar ponta (metal)
      g.setColor(Color.LIGHT_GRAY);
      g.fillPolygon(xPoints, yPoints, 3);

      // Cabo da flecha
      g.setColor(new Color(139, 69, 19));
      g.drawLine(
          screenX, screenY,
          screenX - (int) (cos * 12), screenY - (int) (sin * 12));

      // Penas
      g.setColor(new Color(255, 255, 255, 180));
      for (int i = -1; i <= 1; i += 2) {
        g.drawLine(
            screenX - (int) (cos * 10), screenY - (int) (sin * 10),
            screenX - (int) (cos * 8 + sin * i * 2), screenY - (int) (sin * 8 - cos * i * 2));
      }
    }

    private void renderMagicEffect(Graphics2D g, int screenX, int screenY) {
      // Efeito mágico pulsante
      float pulse = (float) (0.5 + 0.3 * Math.sin(trailTimer * 0.2));

      // Aura azul
      g.setColor(new Color(0, 150, 255, (int) (pulse * 80)));
      g.fillOval(screenX - 8, screenY - 8, 16, 16);

      // Brilho central
      g.setColor(new Color(255, 255, 255, (int) (pulse * 120)));
      g.fillOval(screenX - 3, screenY - 3, 6, 6);
    }
  }
}