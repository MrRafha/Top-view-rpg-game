package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.util.ArrayList;
import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;
import com.rpggame.core.GamePanel;

/**
 * Habilidade Ultimate: Meteoro Arcano
 * Invoca meteoro do céu que causa devastação em área
 */
public class ArcaneMeteorSkill extends Skill {

  private enum MeteorPhase {
    NONE,
    TARGETING, // Mostrando onde vai cair (1 segundo)
    FALLING, // Meteoro caindo (1 segundo)
    IMPACT // Explosão (animação rápida)
  }

  private class BurnEffect {
    Enemy enemy;
    int duration;
    int tickTimer;

    BurnEffect(Enemy enemy) {
      this.enemy = enemy;
      this.duration = 180; // 3 segundos a 60 FPS
      this.tickTimer = 0;
    }

    void update() {
      duration--;
      tickTimer++;

      // Causar dano de queimadura a cada 60 frames (1 segundo)
      if (tickTimer >= 60) {
        if (enemy.isAlive()) {
          enemy.takeDamage(5);
          System.out.println("🔥 Dano de queimadura: 5");
        }
        tickTimer = 0;
      }
    }

    boolean isActive() {
      return duration > 0 && enemy.isAlive();
    }
  }

  private MeteorPhase currentPhase;
  private int phaseTimer;
  private double targetX, targetY; // Posição do alvo no mundo
  private double meteorY; // Posição Y do meteoro durante queda
  private int explosionRadius;
  private int explosionTimer;
  private ArrayList<BurnEffect> burnEffects;
  private Player currentPlayer;

  private static final int TARGETING_DURATION = 60; // 1 segundo
  private static final int FALLING_DURATION = 60; // 1 segundo
  private static final int EXPLOSION_DURATION = 30; // 0.5 segundo
  private static final int AREA_SIZE = 4; // 4x4 tiles
  private static final int MAX_RANGE = 5; // 5 tiles

  public ArcaneMeteorSkill() {
    super("Meteoro Arcano",
        "Invoca meteoro devastador: 80+(INT×5) no centro, queima área",
        50, // 50 segundos de cooldown
        "Mage",
        70); // 70 de mana

    this.currentPhase = MeteorPhase.NONE;
    this.phaseTimer = 0;
    this.explosionRadius = 0;
    this.burnEffects = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    this.currentPlayer = player;

    // Calcular posição do alvo baseado na direção do player
    double facing = player.getFacingDirection();
    double distance = MAX_RANGE * GamePanel.TILE_SIZE;

    targetX = player.getX() + player.getWidth() / 2 + Math.cos(facing) * distance;
    targetY = player.getY() + player.getHeight() / 2 + Math.sin(facing) * distance;

    // Iniciar fase de marcação
    currentPhase = MeteorPhase.TARGETING;
    phaseTimer = TARGETING_DURATION;

    System.out.println("🔮 METEORO ARCANO INVOCADO!");
    System.out.println("📍 Alvo: (" + (int) targetX + ", " + (int) targetY + ")");
  }

  @Override
  public void update() {
    super.update();

    if (currentPhase == MeteorPhase.NONE) {
      return;
    }

    phaseTimer--;

    switch (currentPhase) {
      case TARGETING:
        if (phaseTimer <= 0) {
          // Transição para fase de queda
          currentPhase = MeteorPhase.FALLING;
          phaseTimer = FALLING_DURATION;
          meteorY = -200; // Começa acima da tela
          System.out.println("☄️ Meteoro caindo!");
        }
        break;

      case FALLING:
        // Meteoro caindo em direção ao alvo
        meteorY += 8; // Velocidade de queda

        if (phaseTimer <= 0) {
          // IMPACTO!
          currentPhase = MeteorPhase.IMPACT;
          phaseTimer = EXPLOSION_DURATION;
          explosionRadius = 0;

          // Causar dano e aplicar efeitos
          applyMeteorDamage();

          System.out.println("💥 IMPACTO DO METEORO!");
        }
        break;

      case IMPACT:
        // Explosão expandindo
        explosionRadius += 8;

        if (phaseTimer <= 0) {
          // Fim da animação
          currentPhase = MeteorPhase.NONE;
        }
        break;
    }

    // Atualizar efeitos de queimadura
    burnEffects.removeIf(effect -> {
      effect.update();
      return !effect.isActive();
    });
  }

  private void applyMeteorDamage() {
    System.out.println("🔥 Aplicando dano do meteoro...");

    if (currentPlayer == null) {
      System.out.println("❌ currentPlayer é null!");
      return;
    }

    EnemyManager enemyManager = currentPlayer.getEnemyManager();
    if (enemyManager == null) {
      System.out.println("❌ enemyManager é null!");
      return;
    }

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    System.out.println("🎯 Inimigos disponíveis: " + enemies.size());

    int intelligence = currentPlayer.getStats().getIntelligence();
    System.out.println("🧠 Inteligência do player: " + intelligence);

    // Calcular área de efeito
    double areaRadius = (AREA_SIZE * GamePanel.TILE_SIZE) / 2.0;
    double centerRadius = areaRadius * 0.5; // Centro = 50% do raio total

    System.out.println("📍 Área de efeito - Raio: " + areaRadius + ", Centro: " + centerRadius);
    System.out.println("📍 Alvo: (" + (int) targetX + ", " + (int) targetY + ")");

    int enemiesHit = 0;
    for (Enemy enemy : enemies) {
      if (!enemy.isAlive())
        continue;

      double enemyX = enemy.getX() + enemy.getWidth() / 2;
      double enemyY = enemy.getY() + enemy.getHeight() / 2;

      double dx = enemyX - targetX;
      double dy = enemyY - targetY;
      double distance = Math.sqrt(dx * dx + dy * dy);

      System.out.println("👹 Inimigo em (" + (int) enemyX + ", " + (int) enemyY + ") - Distância: " + (int) distance);

      if (distance <= areaRadius) {
        enemiesHit++;
        int damage;

        if (distance <= centerRadius) {
          // Dano do centro
          damage = 80 + (intelligence * 5);
          System.out.println("💥 DANO CENTRAL: " + damage);
        } else {
          // Dano da área externa
          damage = 50 + (intelligence * 3);
          System.out.println("💥 Dano da área: " + damage);
        }

        enemy.takeDamage(damage);

        // Aplicar queimadura
        burnEffects.add(new BurnEffect(enemy));
        System.out.println("🔥 Queimadura aplicada!");

        // Aplicar knockback (empurrar inimigo para fora)
        if (distance > 0) {
          double knockbackForce = 150;
          double knockbackX = (dx / distance) * knockbackForce;
          double knockbackY = (dy / distance) * knockbackForce;

          // Aplicar knockback diretamente nas coordenadas do inimigo
          try {
            java.lang.reflect.Field xField = enemy.getClass().getSuperclass().getDeclaredField("x");
            java.lang.reflect.Field yField = enemy.getClass().getSuperclass().getDeclaredField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);

            double newX = (double) xField.get(enemy) + knockbackX;
            double newY = (double) yField.get(enemy) + knockbackY;

            xField.set(enemy, newX);
            yField.set(enemy, newY);
          } catch (Exception e) {
            // Knockback falhou, mas o dano foi aplicado
          }
        }
      }
    }

    System.out.println("✅ Total de inimigos atingidos: " + enemiesHit);
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (currentPhase == MeteorPhase.NONE) {
      return;
    }

    java.awt.Composite originalComposite = g.getComposite();
    java.awt.Stroke originalStroke = g.getStroke();

    int screenX = (int) (targetX - camera.getX());
    int screenY = (int) (targetY - camera.getY());
    int areaSize = AREA_SIZE * GamePanel.TILE_SIZE;
    int halfArea = areaSize / 2;

    switch (currentPhase) {
      case TARGETING:
        // Círculo de alvo vermelho pulsante
        float pulse = 0.3f + 0.3f * (float) Math.sin(phaseTimer * 0.2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));

        // Área de impacto
        g.setColor(new Color(255, 0, 0, 100));
        g.fillOval(screenX - halfArea, screenY - halfArea, areaSize, areaSize);

        // Bordas pulsantes
        g.setStroke(new BasicStroke(3));
        g.setColor(Color.RED);
        g.drawOval(screenX - halfArea, screenY - halfArea, areaSize, areaSize);

        // Cruz no centro
        g.drawLine(screenX - 20, screenY, screenX + 20, screenY);
        g.drawLine(screenX, screenY - 20, screenX, screenY + 20);
        break;

      case FALLING:
        // Meteoro caindo
        int meteorScreenY = (int) (meteorY - camera.getY());
        int meteorSize = 40;

        // Sombra no chão
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g.setColor(new Color(50, 0, 0));
        g.fillOval(screenX - halfArea, screenY - halfArea, areaSize, areaSize);

        // Meteoro com gradiente (simulado com círculos)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

        // Aura externa laranja
        g.setColor(new Color(255, 100, 0));
        g.fillOval(screenX - meteorSize, meteorScreenY - meteorSize, meteorSize * 2, meteorSize * 2);

        // Núcleo vermelho
        g.setColor(new Color(255, 0, 0));
        g.fillOval(screenX - meteorSize / 2, meteorScreenY - meteorSize / 2, meteorSize, meteorSize);

        // Centro brilhante
        g.setColor(new Color(255, 200, 100));
        g.fillOval(screenX - meteorSize / 4, meteorScreenY - meteorSize / 4, meteorSize / 2, meteorSize / 2);

        // Trail de partículas
        for (int i = 0; i < 5; i++) {
          int trailY = meteorScreenY - (i * 20);
          float trailAlpha = 0.5f - (i * 0.1f);
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trailAlpha));
          g.setColor(new Color(255, 150, 0));
          g.fillOval(screenX - 10, trailY - 10, 20, 20);
        }
        break;

      case IMPACT:
        // Explosão com ondas de choque
        g.setComposite(
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - (float) phaseTimer / EXPLOSION_DURATION));

        // Múltiplas ondas de choque
        for (int i = 0; i < 3; i++) {
          int waveRadius = explosionRadius + (i * 30);
          g.setStroke(new BasicStroke(5 - i));
          g.setColor(new Color(255, 100, 0, 200 - (i * 50)));
          g.drawOval(screenX - waveRadius, screenY - waveRadius, waveRadius * 2, waveRadius * 2);
        }

        // Flash central
        if (phaseTimer > EXPLOSION_DURATION - 10) {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
          g.setColor(Color.WHITE);
          g.fillOval(screenX - areaSize / 2, screenY - areaSize / 2, areaSize, areaSize);
        }
        break;
    }

    // Renderizar efeitos de queimadura nos inimigos
    for (BurnEffect effect : burnEffects) {
      if (effect.enemy.isAlive()) {
        int enemyScreenX = (int) (effect.enemy.getX() - camera.getX());
        int enemyScreenY = (int) (effect.enemy.getY() - camera.getY());

        // Chamas acima do inimigo
        float burnAlpha = 0.3f + 0.2f * (float) Math.sin(effect.duration * 0.15);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, burnAlpha));
        g.setColor(new Color(255, 100, 0));
        g.fillOval(enemyScreenX + 10, enemyScreenY - 5, 8, 8);
        g.setColor(new Color(255, 200, 0));
        g.fillOval(enemyScreenX + 12, enemyScreenY - 3, 4, 4);
      }
    }

    g.setComposite(originalComposite);
    g.setStroke(originalStroke);
  }
}
