package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;
import com.rpggame.core.GamePanel;

/**
 * Habilidade Ultimate: Chuva de Flechas
 * O caçador dispara múltiplas flechas no ar que caem sobre uma área
 */
public class ArrowRainSkill extends Skill {

  private enum RainPhase {
    NONE,
    PREPARING, // Preparação (0.5 segundo)
    RAINING // Chuva de flechas (4 segundos)
  }

  private class FallingArrow {
    double x, y; // Posição no chão onde vai cair
    double currentY; // Posição atual durante queda
    double velocity;
    boolean hasHit;
    int shadowAlpha;

    FallingArrow(double x, double y) {
      this.x = x;
      this.y = y;
      this.currentY = -100; // Começa acima da tela
      this.velocity = 8 + Math.random() * 4;
      this.hasHit = false;
      this.shadowAlpha = 0;
    }

    void update() {
      if (!hasHit) {
        currentY += velocity;

        // Sombra fica mais escura conforme se aproxima
        double progress = Math.min(1.0, (currentY + 100) / (y + 100));
        shadowAlpha = (int) (progress * 150);

        if (currentY >= y) {
          hasHit = true;
          // Aplicar dano aos inimigos próximos
          hitEnemies();
        }
      }
    }

    private void hitEnemies() {
      if (currentPlayer == null) {
        System.out.println("❌ Arrow: currentPlayer é null!");
        return;
      }

      EnemyManager enemyManager = currentPlayer.getEnemyManager();
      if (enemyManager == null) {
        System.out.println("❌ Arrow: enemyManager é null!");
        return;
      }

      ArrayList<Enemy> enemies = enemyManager.getEnemies();
      int dexterity = currentPlayer.getStats().getDexterity();
      int damage = 15 + (int) (dexterity * 1.5);

      System.out.println("🎯 Flecha caiu em (" + (int) x + ", " + (int) y + ") - Dano: " + damage);

      for (Enemy enemy : enemies) {
        if (!enemy.isAlive())
          continue;

        double enemyX = enemy.getX() + enemy.getWidth() / 2;
        double enemyY = enemy.getY() + enemy.getHeight() / 2;

        double dx = enemyX - x;
        double dy = enemyY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Raio de hit da flecha
        if (distance < 25) {
          System.out.println("✅ Flecha atingiu inimigo! Dano: " + damage);
          enemy.takeDamage(damage);

          // 30% de chance de sangramento
          if (Math.random() < 0.3) {
            bleedEffects.add(new BleedEffect(enemy));
            System.out.println("🩸 Sangramento aplicado!");
          }

          break; // Cada flecha atinge apenas um inimigo
        }
      }
    }
  }

  private class BleedEffect {
    Enemy enemy;
    int duration;
    int tickTimer;

    BleedEffect(Enemy enemy) {
      this.enemy = enemy;
      this.duration = 300; // 5 segundos a 60 FPS
      this.tickTimer = 0;
    }

    void update() {
      duration--;
      tickTimer++;

      // Causar dano de sangramento a cada 60 frames (1 segundo)
      if (tickTimer >= 60) {
        if (enemy.isAlive()) {
          enemy.takeDamage(3);
          System.out.println("🩸 Dano de sangramento: 3");
        }
        tickTimer = 0;
      }
    }

    boolean isActive() {
      return duration > 0 && enemy.isAlive();
    }
  }

  private class SlowEffect {
    Enemy enemy;
    double originalSpeed;

    SlowEffect(Enemy enemy) {
      this.enemy = enemy;
      // Reduzir velocidade em 40%
      try {
        java.lang.reflect.Field speedField = enemy.getClass().getSuperclass().getDeclaredField("speed");
        speedField.setAccessible(true);
        this.originalSpeed = (double) speedField.get(enemy);
        speedField.set(enemy, originalSpeed * 0.6); // 60% da velocidade
      } catch (Exception e) {
        this.originalSpeed = -1;
      }
    }

    void restore() {
      if (originalSpeed > 0 && enemy.isAlive()) {
        try {
          java.lang.reflect.Field speedField = enemy.getClass().getSuperclass().getDeclaredField("speed");
          speedField.setAccessible(true);
          speedField.set(enemy, originalSpeed);
        } catch (Exception e) {
          // Falha ao restaurar
        }
      }
    }
  }

  private RainPhase currentPhase;
  private int phaseTimer;
  private double targetX, targetY; // Centro da área
  private ArrayList<FallingArrow> arrows;
  private ArrayList<BleedEffect> bleedEffects;
  private ArrayList<SlowEffect> slowEffects;
  private int arrowSpawnTimer;
  private int arrowsSpawned;
  private Player currentPlayer;

  private static final int PREPARING_DURATION = 30; // 0.5 segundo
  private static final int RAINING_DURATION = 240; // 4 segundos
  private static final int TOTAL_ARROWS = 20;
  private static final int AREA_SIZE = 5; // 5x5 tiles
  private static final int MAX_RANGE = 6; // 6 tiles

  public ArrowRainSkill() {
    super("Chuva de Flechas",
        "20 flechas caem na área: 15+(DEX×1.5) cada, slow 40%",
        40, // 40 segundos de cooldown
        "Hunter",
        65); // 65 de mana

    this.currentPhase = RainPhase.NONE;
    this.phaseTimer = 0;
    this.arrows = new ArrayList<>();
    this.bleedEffects = new ArrayList<>();
    this.slowEffects = new ArrayList<>();
    this.arrowsSpawned = 0;
  }

  @Override
  protected void performSkill(Player player) {
    this.currentPlayer = player;

    // Calcular posição do alvo baseado na direção do player
    double facing = player.getFacingDirection();
    double distance = MAX_RANGE * GamePanel.TILE_SIZE;

    targetX = player.getX() + player.getWidth() / 2 + Math.cos(facing) * distance;
    targetY = player.getY() + player.getHeight() / 2 + Math.sin(facing) * distance;

    // Iniciar fase de preparação
    currentPhase = RainPhase.PREPARING;
    phaseTimer = PREPARING_DURATION;
    arrows.clear();
    arrowsSpawned = 0;
    arrowSpawnTimer = 0;

    System.out.println("🏹 CHUVA DE FLECHAS ATIVADA!");
    System.out.println("📍 Área: (" + (int) targetX + ", " + (int) targetY + ")");
  }

  @Override
  public void update() {
    super.update();

    if (currentPhase == RainPhase.NONE) {
      return;
    }

    phaseTimer--;

    switch (currentPhase) {
      case PREPARING:
        if (phaseTimer <= 0) {
          // Transição para chuva
          currentPhase = RainPhase.RAINING;
          phaseTimer = RAINING_DURATION;

          // Aplicar slow em todos os inimigos na área
          applySlowEffect();

          System.out.println("☔ Chuva de flechas começou!");
        }
        break;

      case RAINING:
        // Spawnar flechas aleatoriamente
        arrowSpawnTimer++;
        int spawnInterval = RAINING_DURATION / TOTAL_ARROWS;

        if (arrowSpawnTimer >= spawnInterval && arrowsSpawned < TOTAL_ARROWS) {
          spawnArrow();
          arrowSpawnTimer = 0;
          arrowsSpawned++;
        }

        // Atualizar flechas
        Iterator<FallingArrow> arrowIterator = arrows.iterator();
        while (arrowIterator.hasNext()) {
          FallingArrow arrow = arrowIterator.next();
          arrow.update();

          // Remover flechas que já caíram há algum tempo
          if (arrow.hasHit && arrow.currentY > arrow.y + 50) {
            arrowIterator.remove();
          }
        }

        if (phaseTimer <= 0) {
          // Fim da chuva
          currentPhase = RainPhase.NONE;

          // Restaurar velocidade dos inimigos
          for (SlowEffect slow : slowEffects) {
            slow.restore();
          }
          slowEffects.clear();

          System.out.println("🏹 Chuva de flechas terminou!");
        }
        break;
    }

    // Atualizar efeitos de sangramento
    bleedEffects.removeIf(effect -> {
      effect.update();
      return !effect.isActive();
    });
  }

  private void spawnArrow() {
    // Posição aleatória dentro da área circular
    double areaRadius = (AREA_SIZE * GamePanel.TILE_SIZE) / 2.0;
    double angle = Math.random() * Math.PI * 2;
    double distance = Math.random() * areaRadius;

    double arrowX = targetX + Math.cos(angle) * distance;
    double arrowY = targetY + Math.sin(angle) * distance;

    arrows.add(new FallingArrow(arrowX, arrowY));
  }

  private void applySlowEffect() {
    if (currentPlayer == null)
      return;

    EnemyManager enemyManager = currentPlayer.getEnemyManager();
    if (enemyManager == null)
      return;

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    double areaRadius = (AREA_SIZE * GamePanel.TILE_SIZE) / 2.0;

    slowEffects.clear();

    for (Enemy enemy : enemies) {
      if (!enemy.isAlive())
        continue;

      double enemyX = enemy.getX() + enemy.getWidth() / 2;
      double enemyY = enemy.getY() + enemy.getHeight() / 2;

      double dx = enemyX - targetX;
      double dy = enemyY - targetY;
      double distance = Math.sqrt(dx * dx + dy * dy);

      if (distance <= areaRadius) {
        slowEffects.add(new SlowEffect(enemy));
        System.out.println("🐌 Inimigo desacelerado!");
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (currentPhase == RainPhase.NONE) {
      return;
    }

    java.awt.Composite originalComposite = g.getComposite();
    java.awt.Stroke originalStroke = g.getStroke();

    int screenX = (int) (targetX - camera.getX());
    int screenY = (int) (targetY - camera.getY());
    int areaSize = AREA_SIZE * GamePanel.TILE_SIZE;
    int radius = areaSize / 2;

    // Área circular verde no chão
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
    g.setColor(new Color(0, 255, 0, 80));
    g.fillOval(screenX - radius, screenY - radius, areaSize, areaSize);

    // Borda verde brilhante
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
    g.setStroke(new BasicStroke(3));
    g.setColor(new Color(0, 255, 0));
    g.drawOval(screenX - radius, screenY - radius, areaSize, areaSize);

    // Partículas verdes flutuando (apenas durante a chuva)
    if (currentPhase == RainPhase.RAINING) {
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
      for (int i = 0; i < 10; i++) {
        double angle = (System.currentTimeMillis() * 0.001 + i) % (Math.PI * 2);
        double dist = radius * 0.7 + Math.sin(angle * 3) * 20;
        int px = screenX + (int) (Math.cos(angle) * dist);
        int py = screenY + (int) (Math.sin(angle) * dist);

        g.setColor(new Color(150, 255, 150));
        g.fillOval(px - 3, py - 3, 6, 6);
      }
    }

    // Renderizar flechas caindo
    for (FallingArrow arrow : arrows) {
      int arrowScreenX = (int) (arrow.x - camera.getX());
      int arrowScreenY = (int) (arrow.currentY - camera.getY());
      int arrowGroundY = (int) (arrow.y - camera.getY());

      if (!arrow.hasHit) {
        // Sombra no chão
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, arrow.shadowAlpha / 255.0f));
        g.setColor(new Color(0, 50, 0));
        g.fillOval(arrowScreenX - 8, arrowGroundY - 4, 16, 8);

        // Flecha caindo (linha com ponta)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(0, 200, 0));
        g.drawLine(arrowScreenX, arrowScreenY - 15, arrowScreenX, arrowScreenY);

        // Ponta da flecha (triângulo)
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(arrowScreenX, arrowScreenY);
        arrowHead.addPoint(arrowScreenX - 4, arrowScreenY - 8);
        arrowHead.addPoint(arrowScreenX + 4, arrowScreenY - 8);
        g.fillPolygon(arrowHead);

        // Trail luminoso
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.setColor(new Color(150, 255, 150));
        g.drawLine(arrowScreenX, arrowScreenY - 15, arrowScreenX, arrowScreenY - 25);
      } else {
        // Flecha no chão (efeito de impacto)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g.setColor(new Color(0, 150, 0));
        g.fillOval(arrowScreenX - 2, arrowGroundY - 2, 4, 4);

        // Ondas de impacto
        int impactRadius = (int) ((arrow.currentY - arrow.y) / 2);
        if (impactRadius < 20) {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
          g.drawOval(arrowScreenX - impactRadius, arrowGroundY - impactRadius,
              impactRadius * 2, impactRadius * 2);
        }
      }
    }

    // Renderizar efeitos de sangramento
    for (BleedEffect effect : bleedEffects) {
      if (effect.enemy.isAlive()) {
        int enemyScreenX = (int) (effect.enemy.getX() - camera.getX());
        int enemyScreenY = (int) (effect.enemy.getY() - camera.getY());

        // Gotas de sangue caindo
        float bleedAlpha = 0.4f + 0.2f * (float) Math.sin(effect.duration * 0.1);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bleedAlpha));
        g.setColor(new Color(200, 0, 0));

        for (int i = 0; i < 3; i++) {
          int dropX = enemyScreenX + 15 + (i * 10);
          int dropY = enemyScreenY + (effect.tickTimer % 20);
          g.fillOval(dropX, dropY, 4, 6);
        }
      }
    }

    g.setComposite(originalComposite);
    g.setStroke(originalStroke);
  }
}
