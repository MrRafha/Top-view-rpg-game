package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.Iterator;
import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;

/**
 * Habilidade: Encantamento
 * Lança um projétil roxo que faz o inimigo atacar seus aliados por 5 segundos
 */
public class EnchantmentSkill extends Skill {

  private class EnchantmentProjectile {
    double posX;
    double posY;
    double velocityX;
    double velocityY;
    boolean active;
    static final double SPEED = 6.0;

    EnchantmentProjectile(double x, double y, double dirX, double dirY) {
      this.posX = x;
      this.posY = y;
      this.velocityX = dirX * SPEED;
      this.velocityY = dirY * SPEED;
      this.active = true;
    }

    void update() {
      posX += velocityX;
      posY += velocityY;
    }
  }

  private ArrayList<EnchantmentProjectile> projectiles;
  private static final int CHARM_DURATION = 300; // 5 segundos a 60 FPS
  private Player currentPlayer; // Armazenar referência ao player

  public EnchantmentSkill() {
    super("Encantamento",
        "Faz um inimigo atacar seus aliados por 5 segundos",
        10, // 10 segundos de cooldown
        "Mage",
        40); // 40 de mana
    projectiles = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    this.currentPlayer = player;
    // Calcular direção do projétil
    double facing = player.getFacingDirection();
    double dirX = Math.cos(facing);
    double dirY = Math.sin(facing);

    // Posição inicial (centro do player)
    double startX = player.getX() + player.getWidth() / 2;
    double startY = player.getY() + player.getHeight() / 2;

    // Criar projétil
    projectiles.add(new EnchantmentProjectile(startX, startY, dirX, dirY));

    System.out.println("💜 ENCANTAMENTO! Projétil lançado!");
  }

  @Override
  public void update() {
    super.update();

    // Debug: mostrar quando há projéteis ativos
    if (projectiles.size() > 0 && Math.random() < 0.05) { // 5% de chance de printar
      System.out.println("💜 EnchantmentSkill.update() - Projéteis ativos: " + projectiles.size());
    }

    // Verificar colisões com inimigos
    if (currentPlayer == null) {
      if (projectiles.size() > 0) {
        System.out.println("⚠️ currentPlayer é null! Não pode verificar colisões.");
      }
      return;
    }

    EnemyManager enemyManager = currentPlayer.getEnemyManager();
    if (enemyManager == null) {
      if (projectiles.size() > 0) {
        System.out.println("⚠️ enemyManager é null! Não pode verificar colisões.");
      }
      return;
    }

    ArrayList<Enemy> enemies = enemyManager.getEnemies();

    Iterator<EnchantmentProjectile> iterator = projectiles.iterator();
    while (iterator.hasNext()) {
      EnchantmentProjectile proj = iterator.next();

      if (!proj.active) {
        iterator.remove();
        continue;
      }

      proj.update();

      // Verificar colisão com inimigos
      for (Enemy enemy : enemies) {
        if (!enemy.isAlive())
          continue;

        double dx = enemy.getX() - proj.posX;
        double dy = enemy.getY() - proj.posY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 30) { // Raio de colisão
          // Encantar o inimigo
          System.out.println("💜 COLISÃO DETECTADA! Distância: " + distance);
          System.out.println("💜 Inimigo pos: (" + enemy.getX() + ", " + enemy.getY() + ")");
          System.out.println("💜 Projétil pos: (" + proj.posX + ", " + proj.posY + ")");
          charmEnemy(enemy);
          proj.active = false;
          System.out.println("💜 Inimigo encantado! Atacará seus aliados!");
          break;
        }
      }

      // Remover se saiu da tela (distância > 800)
      if (Math.abs(proj.posX) > 2000 || Math.abs(proj.posY) > 2000) {
        iterator.remove();
      }
    }
  }

  private void charmEnemy(Enemy enemy) {
    // Chamar o método público diretamente
    System.out.println("💜 charmEnemy() iniciado...");
    System.out.println("💜 Enemy antes: charmed = " + enemy.isCharmed());
    enemy.applyCharm(CHARM_DURATION);
    System.out.println("💜 applyCharm() chamado diretamente!");
    System.out.println("💜 Enemy depois: charmed = " + enemy.isCharmed());
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    // Renderizar projéteis
    for (EnchantmentProjectile proj : projectiles) {
      if (!proj.active)
        continue;

      int screenX = (int) (proj.posX - camera.getX());
      int screenY = (int) (proj.posY - camera.getY());

      // Desenhar orbe roxa brilhante
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));

      // Aura externa
      g.setColor(new Color(200, 100, 255, 100));
      g.fillOval(screenX - 20, screenY - 20, 40, 40);

      // Orbe central
      g.setColor(new Color(150, 50, 255));
      g.fillOval(screenX - 8, screenY - 8, 16, 16);

      // Brilho interno
      g.setColor(new Color(220, 150, 255));
      g.fillOval(screenX - 4, screenY - 4, 8, 8);

      // Partículas ao redor
      g.setColor(new Color(180, 100, 255));
      for (int i = 0; i < 6; i++) {
        double angle = (System.currentTimeMillis() * 0.005 + i * Math.PI / 3);
        int px = screenX + (int) (Math.cos(angle) * 12);
        int py = screenY + (int) (Math.sin(angle) * 12);
        g.fillOval(px - 2, py - 2, 4, 4);
      }
    }

    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
  }
}
