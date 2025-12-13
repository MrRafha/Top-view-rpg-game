package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;
import com.rpggame.world.TileMap;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;
import com.rpggame.core.GamePanel;

/**
 * Habilidade: Dash do Touro
 * O guerreiro se lança 3 tiles à frente, causando dano em todos os inimigos no
 * trajeto
 */
public class BullRushSkill extends Skill {

  // Efeito visual do dash
  private boolean isDashing = false;
  private int dashTimer = 0;
  private static final int DASH_DURATION = 15; // ~0.25 segundos
  private double startX, startY;
  private double endX, endY;
  private ArrayList<DashTrail> trails;

  // Classe para rastro visual
  private class DashTrail {
    double x, y;
    int duration;

    DashTrail(double x, double y, int duration) {
      this.x = x;
      this.y = y;
      this.duration = duration;
    }
  }

  public BullRushSkill() {
    super("Dash do Touro",
        "Lança-se 3 tiles à frente, causando dano massivo aos inimigos no trajeto",
        8, // 8 segundos de cooldown
        "Warrior",
        40); // 40 de mana
    trails = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    // Calcular direção
    double facing = player.getFacingDirection();
    int tileDirX = 0;
    int tileDirY = 0;

    double angleInDegrees = Math.toDegrees(facing);
    if (angleInDegrees < 0)
      angleInDegrees += 360;

    if (angleInDegrees >= 315 || angleInDegrees < 45) {
      tileDirX = 1; // Direita
    } else if (angleInDegrees >= 45 && angleInDegrees < 135) {
      tileDirY = 1; // Baixo
    } else if (angleInDegrees >= 135 && angleInDegrees < 225) {
      tileDirX = -1; // Esquerda
    } else {
      tileDirY = -1; // Cima
    }

    // Posição atual do player em tiles
    int currentTileX = (int) (player.getX() / GamePanel.TILE_SIZE);
    int currentTileY = (int) (player.getY() / GamePanel.TILE_SIZE);

    // Pegar TileMap
    TileMap tileMap = player.getTileMap();
    if (tileMap == null) {
      System.out.println("⚠️ TileMap não encontrado!");
      return;
    }

    // Calcular distância máxima (verificar colisões)
    int maxDistance = 3; // 3 tiles
    int actualDistance = 0;

    for (int i = 1; i <= maxDistance; i++) {
      int checkTileX = currentTileX + (tileDirX * i);
      int checkTileY = currentTileY + (tileDirY * i);

      // Verificar se o tile é caminhável
      if (tileMap.isWalkable(checkTileX, checkTileY)) {
        actualDistance = i;
      } else {
        // Parou em uma parede
        break;
      }
    }

    if (actualDistance == 0) {
      System.out.println("❌ Não é possível realizar Dash do Touro - caminho bloqueado!");
      return;
    }

    // Posição inicial e final
    startX = player.getX();
    startY = player.getY();
    endX = startX + (tileDirX * actualDistance * GamePanel.TILE_SIZE);
    endY = startY + (tileDirY * actualDistance * GamePanel.TILE_SIZE);

    // Mover o player instantaneamente
    player.setPosition(endX, endY);

    // Ativar efeito visual
    isDashing = true;
    dashTimer = DASH_DURATION;

    // Causar dano nos inimigos no trajeto
    EnemyManager enemyManager = player.getEnemyManager();
    if (enemyManager == null) {
      System.out.println("⚠️ EnemyManager não encontrado!");
      return;
    }

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    int damagedCount = 0;
    int damage = 25 + player.getStats().getStrength() * 2; // Dano base + bônus de força

    // Verificar todos os tiles no trajeto
    for (int i = 1; i <= actualDistance; i++) {
      int checkTileX = currentTileX + (tileDirX * i);
      int checkTileY = currentTileY + (tileDirY * i);

      // Adicionar rastro visual
      double trailX = startX + (tileDirX * i * GamePanel.TILE_SIZE);
      double trailY = startY + (tileDirY * i * GamePanel.TILE_SIZE);
      trails.add(new DashTrail(trailX, trailY, 30));

      // Verificar inimigos neste tile
      for (Enemy enemy : enemies) {
        if (!enemy.isAlive())
          continue;

        int enemyTileX = (int) (enemy.getX() / GamePanel.TILE_SIZE);
        int enemyTileY = (int) (enemy.getY() / GamePanel.TILE_SIZE);

        if (enemyTileX == checkTileX && enemyTileY == checkTileY) {
          enemy.takeDamage(damage);
          damagedCount++;
          System.out.println("🐂 BULL RUSH! Causou " + damage + " de dano!");
        }
      }
    }

    System.out
        .println("🐂 DASH DO TOURO! Avançou " + actualDistance + " tiles e atingiu " + damagedCount + " inimigos!");
  }

  @Override
  public void update() {
    super.update();

    // Atualizar timer do dash
    if (isDashing && dashTimer > 0) {
      dashTimer--;
      if (dashTimer <= 0) {
        isDashing = false;
      }
    }

    // Atualizar rastros
    trails.removeIf(trail -> {
      trail.duration--;
      return trail.duration <= 0;
    });
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    // Renderizar rastros
    for (DashTrail trail : trails) {
      int screenX = (int) (trail.x - camera.getX());
      int screenY = (int) (trail.y - camera.getY());

      // Calcular transparência baseada na duração
      float alpha = Math.min(1.0f, trail.duration / 15.0f);

      // Desenhar rastro vermelho
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.6f));
      g.setColor(new Color(200, 50, 50));
      g.fillRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);

      // Linhas de movimento
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.8f));
      g.setColor(new Color(255, 100, 100));
      g.setStroke(new java.awt.BasicStroke(3));
      for (int i = 0; i < 3; i++) {
        int offset = i * 12;
        g.drawLine(screenX + offset, screenY, screenX + offset, screenY + GamePanel.TILE_SIZE);
      }
    }

    // Resetar composite e stroke
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    g.setStroke(new java.awt.BasicStroke(1));
  }
}
