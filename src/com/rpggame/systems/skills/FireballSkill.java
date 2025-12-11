package com.rpggame.systems.skills;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import com.rpggame.entities.Enemy;
import com.rpggame.entities.Player;
import com.rpggame.entities.Projectile;
import com.rpggame.systems.EnemyManager;
import com.rpggame.systems.Skill;
import com.rpggame.world.Camera;

/**
 * Habilidade do Mago: Bola de Fogo
 * Dispara uma bola de fogo que explode ao atingir um inimigo, causando dano em
 * área
 */
public class FireballSkill extends Skill {
  private ArrayList<FireballProjectile> activeFireballs;
  private ArrayList<FireballExplosion> activeExplosions;

  private static final double FIREBALL_SPEED = 8.0;
  private static final double FIREBALL_RANGE = 400.0;
  private static final double EXPLOSION_RADIUS = 100.0;

  public FireballSkill() {
    super("Bola de Fogo",
        "Dispara uma bola de fogo mágica que explode ao atingir inimigos, causando grande dano em área",
        30, // 30 segundos de cooldown
        "Mage");

    this.activeFireballs = new ArrayList<>();
    this.activeExplosions = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    // Criar nova bola de fogo
    double startX = player.getX() + player.getWidth() / 2;
    double startY = player.getY() + player.getHeight() / 2;
    double direction = player.getFacingDirection();

    FireballProjectile fireball = new FireballProjectile(
        startX, startY, direction, FIREBALL_SPEED, FIREBALL_RANGE, player);

    activeFireballs.add(fireball);
    System.out.println("🔥 Bola de Fogo disparada!");
  }

  @Override
  public void update() {
    super.update();

    // Atualizar bolas de fogo
    for (int i = activeFireballs.size() - 1; i >= 0; i--) {
      FireballProjectile fireball = activeFireballs.get(i);
      fireball.update();

      if (fireball.shouldRemove()) {
        activeFireballs.remove(i);
      }
    }

    // Atualizar explosões
    for (int i = activeExplosions.size() - 1; i >= 0; i--) {
      FireballExplosion explosion = activeExplosions.get(i);
      explosion.update();

      if (explosion.isFinished()) {
        activeExplosions.remove(i);
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    // Renderizar bolas de fogo
    for (FireballProjectile fireball : activeFireballs) {
      fireball.render(g, camera);
    }

    // Renderizar explosões
    for (FireballExplosion explosion : activeExplosions) {
      explosion.render(g, camera);
    }
  }

  /**
   * Cria uma explosão na posição especificada
   */
  private void createExplosion(double x, double y, Player player) {
    FireballExplosion explosion = new FireballExplosion(x, y);
    activeExplosions.add(explosion);

    // Causar dano em área
    dealExplosionDamage(x, y, player);
  }

  /**
   * Causa dano a todos os inimigos na área da explosão
   */
  private void dealExplosionDamage(double explosionX, double explosionY, Player player) {
    EnemyManager enemyManager = player.getEnemyManager();
    if (enemyManager == null)
      return;

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    int enemiesHit = 0;

    for (Enemy enemy : enemies) {
      if (enemy.isAlive()) {
        double dx = enemy.getX() - explosionX;
        double dy = enemy.getY() - explosionY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= EXPLOSION_RADIUS) {
          // Dano maior no centro, menor na borda
          double damageMultiplier = 1.0 - (distance / EXPLOSION_RADIUS);
          // Dano baseado na Inteligência (INT) - atributo principal do Mago
          int damage = (int) ((player.getStats().getIntelligence() * 3.0 + 25) * damageMultiplier);

          enemy.takeDamage(damage);
          enemiesHit++;

          // TODO: Adicionar knockback quando o método for implementado na classe Enemy
        }
      }
    }

    if (enemiesHit > 0) {
      System.out.println("💥 Explosão da Bola de Fogo atingiu " + enemiesHit + " inimigo(s)!");
    }
  }

  /**
   * Classe interna para a bola de fogo
   */
  private class FireballProjectile {
    private double x, y;
    private double dx, dy;
    private double speed;
    private double maxRange;
    private double travelDistance;
    private Player owner;
    private int animationFrame;

    public FireballProjectile(double startX, double startY, double direction,
        double speed, double range, Player owner) {
      this.x = startX;
      this.y = startY;
      this.speed = speed;
      this.maxRange = range;
      this.owner = owner;
      this.travelDistance = 0;
      this.animationFrame = 0;

      this.dx = Math.cos(direction) * speed;
      this.dy = Math.sin(direction) * speed;
    }

    public void update() {
      // Mover projétil
      x += dx;
      y += dy;
      travelDistance += speed;
      animationFrame++;

      // Verificar colisão com inimigos
      if (checkEnemyCollision()) {
        createExplosion(x, y, owner);
        return;
      }

      // Verificar se atingiu alcance máximo
      if (travelDistance >= maxRange) {
        createExplosion(x, y, owner);
      }
    }

    private boolean checkEnemyCollision() {
      EnemyManager enemyManager = owner.getEnemyManager();
      if (enemyManager == null)
        return false;

      for (Enemy enemy : enemyManager.getEnemies()) {
        if (enemy.isAlive()) {
          double dx = enemy.getX() - x;
          double dy = enemy.getY() - y;
          double distance = Math.sqrt(dx * dx + dy * dy);

          if (distance <= 25) { // Raio de colisão da bola de fogo
            return true;
          }
        }
      }
      return false;
    }

    public boolean shouldRemove() {
      return travelDistance >= maxRange;
    }

    public void render(Graphics2D g, Camera camera) {
      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());

      // Efeito de chama animada
      int flameSize = 20 + (int) (Math.sin(animationFrame * 0.3) * 5);

      // Núcleo da bola de fogo (branco quente)
      g.setColor(new Color(255, 255, 200, 200));
      g.fillOval(screenX - flameSize / 3, screenY - flameSize / 3, flameSize * 2 / 3, flameSize * 2 / 3);

      // Chama externa (laranja/vermelho)
      g.setColor(new Color(255, 100, 0, 150));
      g.fillOval(screenX - flameSize / 2, screenY - flameSize / 2, flameSize, flameSize);

      // Partículas ao redor
      for (int i = 0; i < 4; i++) {
        double angle = (animationFrame + i * 90) * Math.PI / 180;
        int particleX = screenX + (int) (Math.cos(angle) * 15);
        int particleY = screenY + (int) (Math.sin(angle) * 15);

        g.setColor(new Color(255, 150, 0, 100));
        g.fillOval(particleX - 3, particleY - 3, 6, 6);
      }
    }
  }

  /**
   * Classe interna para a explosão da bola de fogo
   */
  private class FireballExplosion {
    private double x, y;
    private int timer;
    private static final int EXPLOSION_DURATION = 20; // ~0.33 segundos

    public FireballExplosion(double x, double y) {
      this.x = x;
      this.y = y;
      this.timer = EXPLOSION_DURATION;
    }

    public void update() {
      timer--;
    }

    public boolean isFinished() {
      return timer <= 0;
    }

    public void render(Graphics2D g, Camera camera) {
      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());

      float intensity = (float) timer / EXPLOSION_DURATION;
      int radius = (int) (EXPLOSION_RADIUS * (1.0f - intensity * 0.5f));

      // Explosão em camadas
      // Onda de choque externa
      g.setColor(new Color(255, 0, 0, (int) (intensity * 60)));
      g.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

      // Núcleo da explosão
      int coreRadius = radius / 2;
      g.setColor(new Color(255, 255, 0, (int) (intensity * 150)));
      g.fillOval(screenX - coreRadius, screenY - coreRadius, coreRadius * 2, coreRadius * 2);

      // Centro branco quente
      int hotRadius = radius / 4;
      g.setColor(new Color(255, 255, 255, (int) (intensity * 200)));
      g.fillOval(screenX - hotRadius, screenY - hotRadius, hotRadius * 2, hotRadius * 2);
    }
  }
}