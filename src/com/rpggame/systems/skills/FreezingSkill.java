package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.Iterator;
import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.world.Camera;
import com.rpggame.world.TileMap;
import com.rpggame.world.TileType;
import com.rpggame.systems.Skill;
import com.rpggame.systems.EnemyManager;
import com.rpggame.core.GamePanel;

/**
 * Habilidade: Congelamento
 * Congela 2 tiles à frente por 5 segundos, congelando inimigos e tiles de
 * água/lava
 */
public class FreezingSkill extends Skill {

  // Classe para representar um tile congelado
  private class FrozenTile {
    int tileX, tileY;
    int duration;
    TileType originalType; // Armazena o tipo original do tile

    FrozenTile(int tileX, int tileY, int duration, TileType originalType) {
      this.tileX = tileX;
      this.tileY = tileY;
      this.duration = duration;
      this.originalType = originalType;
    }
  }

  private ArrayList<FrozenTile> frozenTiles = new ArrayList<>();
  private static final int FREEZE_DURATION = 300; // 5 segundos a 60 FPS

  public FreezingSkill() {
    super("Congelamento",
        "Congela 2 tiles à frente, congelando inimigos e permitindo andar em água/lava",
        5, // 5 segundos de cooldown
        "Mage",
        30); // 30 de mana
  }

  @Override
  protected void performSkill(Player player) {
    // Calcular direção que o player está olhando
    double facing = player.getFacingDirection();

    // Converter direção para movimento de tiles
    int tileDirX = 0;
    int tileDirY = 0;

    // Determinar direção baseada no facing (0 = direita, PI/2 = baixo, PI =
    // esquerda, 3*PI/2 = cima)
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

    // Posição do player em tiles
    int playerTileX = (int) (player.getX() / GamePanel.TILE_SIZE);
    int playerTileY = (int) (player.getY() / GamePanel.TILE_SIZE);

    // Pegar TileMap do player
    TileMap tileMap = player.getTileMap();
    if (tileMap == null) {
      System.out.println("⚠️ TileMap não encontrado!");
      return;
    }

    // Pegar inimigos do EnemyManager
    EnemyManager enemyManager = player.getEnemyManager();
    if (enemyManager == null) {
      System.out.println("⚠️ EnemyManager não encontrado!");
      return;
    }

    ArrayList<Enemy> enemies = enemyManager.getEnemies();
    int frozenEnemyCount = 0;
    int frozenTileCount = 0;

    // Congelar 2 tiles à frente
    for (int i = 1; i <= 2; i++) {
      int targetTileX = playerTileX + (tileDirX * i);
      int targetTileY = playerTileY + (tileDirY * i);

      // Pegar tipo do tile
      TileType tileType = tileMap.getTileAt(targetTileX, targetTileY);

      // Adicionar tile congelado
      frozenTiles.add(new FrozenTile(targetTileX, targetTileY, FREEZE_DURATION, tileType));
      frozenTileCount++;

      // Congelar inimigos neste tile
      for (Enemy enemy : enemies) {
        if (!enemy.isAlive())
          continue;

        int enemyTileX = (int) (enemy.getX() / GamePanel.TILE_SIZE);
        int enemyTileY = (int) (enemy.getY() / GamePanel.TILE_SIZE);

        if (enemyTileX == targetTileX && enemyTileY == targetTileY) {
          freezeEnemy(enemy, FREEZE_DURATION);
          frozenEnemyCount++;
        }
      }
    }

    System.out
        .println("❄️ CONGELAMENTO! " + frozenTileCount + " tiles e " + frozenEnemyCount + " inimigos congelados!");
  }

  /**
   * Congela um inimigo usando reflection
   */
  private void freezeEnemy(Enemy enemy, int duration) {
    try {
      java.lang.reflect.Field frozenField = enemy.getClass().getSuperclass().getDeclaredField("frozen");
      java.lang.reflect.Field freezeTimerField = enemy.getClass().getSuperclass().getDeclaredField("freezeTimer");

      frozenField.setAccessible(true);
      freezeTimerField.setAccessible(true);

      frozenField.set(enemy, true);
      freezeTimerField.set(enemy, duration);

      System.out.println("❄️ Inimigo congelado!");
    } catch (Exception e) {
      System.err.println("❌ Erro ao congelar inimigo: " + e.getMessage());
    }
  }

  @Override
  public void update() {
    super.update();

    // Atualizar duração dos tiles congelados
    Iterator<FrozenTile> iterator = frozenTiles.iterator();
    while (iterator.hasNext()) {
      FrozenTile tile = iterator.next();
      tile.duration--;
      if (tile.duration <= 0) {
        iterator.remove();
      }
    }
  }

  /**
   * Verifica se um tile está congelado (para permitir passagem em água/lava)
   */
  public boolean isTileFrozen(int tileX, int tileY) {
    for (FrozenTile tile : frozenTiles) {
      if (tile.tileX == tileX && tile.tileY == tileY) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retorna o tipo original do tile congelado (para renderização)
   */
  public TileType getOriginalTileType(int tileX, int tileY) {
    for (FrozenTile tile : frozenTiles) {
      if (tile.tileX == tileX && tile.tileY == tileY) {
        return tile.originalType;
      }
    }
    return null;
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    // Renderizar tiles congelados
    for (FrozenTile tile : frozenTiles) {
      int screenX = (tile.tileX * GamePanel.TILE_SIZE) - (int) camera.getX();
      int screenY = (tile.tileY * GamePanel.TILE_SIZE) - (int) camera.getY();

      // Calcular transparência baseada na duração restante
      float alpha = Math.min(1.0f, tile.duration / 60.0f);

      // Desenhar quadrado azul semi-transparente
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.6f));
      g.setColor(new Color(100, 200, 255));
      g.fillRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);

      // Desenhar borda mais clara
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.8f));
      g.setColor(new Color(200, 240, 255));
      g.setStroke(new java.awt.BasicStroke(2));
      g.drawRect(screenX, screenY, GamePanel.TILE_SIZE, GamePanel.TILE_SIZE);

      // Desenhar padrão de gelo (linhas diagonais)
      g.setColor(new Color(220, 245, 255));
      for (int i = 0; i < 3; i++) {
        int offset = i * 16;
        g.drawLine(screenX + offset, screenY, screenX, screenY + offset);
        g.drawLine(screenX + GamePanel.TILE_SIZE, screenY + offset,
            screenX + offset, screenY + GamePanel.TILE_SIZE);
      }
    }

    // Resetar composite e stroke
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    g.setStroke(new java.awt.BasicStroke(1));
  }
}
