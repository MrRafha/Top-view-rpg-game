package com.rpggame.render;

import com.rpggame.core.Game;
import com.rpggame.core.GamePanel;
import com.rpggame.entities.Structure;
import com.rpggame.shared.WorldSnapshot;
import com.rpggame.world.Camera;
import com.rpggame.world.FogOfWar;
import com.rpggame.world.ResourceResolver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Renderiza o mundo baseado em WorldSnapshot sem acessar estado interno da
 * simulacao.
 */
public final class SnapshotRenderSystem {
  private static final Map<String, BufferedImage> SPRITE_CACHE = new HashMap<>();
  private static final long SNAPSHOT_INTERVAL_NANOS = 1_000_000_000L / 20L;

  private SnapshotRenderSystem() {
  }

  public static void renderEnemies(
      Graphics2D g,
      Camera camera,
      WorldSnapshot previous,
      WorldSnapshot current,
      double alpha,
      FogOfWar fogOfWar) {
    if (current == null) {
      return;
    }

    for (WorldSnapshot.SnapshotEnemy enemy : current.getEnemies()) {
      WorldSnapshot.SnapshotEnemy prevEnemy = findEnemy(previous, enemy.getId());
      double x = lerp(prevEnemy == null ? enemy.getX() : prevEnemy.getX(), enemy.getX(), alpha);
      double y = lerp(prevEnemy == null ? enemy.getY() : prevEnemy.getY(), enemy.getY(), alpha);

      int tileX = (int) (x / GamePanel.TILE_SIZE);
      int tileY = (int) (y / GamePanel.TILE_SIZE);
      if (fogOfWar != null && !fogOfWar.isVisible(tileX, tileY)) {
        continue;
      }

      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());
      if (screenX < -48 || screenY < -48 || screenX > Game.SCREEN_WIDTH + 48 || screenY > Game.SCREEN_HEIGHT + 48) {
        continue;
      }

      BufferedImage sprite = loadSprite(enemy.getSpritePath());
      int renderWidth = enemy.getWidth() > 0 ? enemy.getWidth() : 32;
      int renderHeight = enemy.getHeight() > 0 ? enemy.getHeight() : 32;
      if (sprite != null) {
        g.drawImage(sprite, screenX, screenY, renderWidth, renderHeight, null);
      } else {
        g.setColor(colorForEnemyType(enemy.getType()));
        g.fillOval(screenX, screenY, renderWidth, renderHeight);
        g.setColor(Color.BLACK);
        g.drawOval(screenX, screenY, renderWidth, renderHeight);
      }

      if (enemy.getMaxHp() > 0) {
        int barWidth = renderWidth;
        int hpWidth = (int) Math.max(0, Math.min(barWidth, ((double) enemy.getHp() / enemy.getMaxHp()) * barWidth));
        g.setColor(Color.RED);
        g.fillRect(screenX, screenY - 8, barWidth, 4);
        g.setColor(Color.GREEN);
        g.fillRect(screenX, screenY - 8, hpWidth, 4);
      }
    }
  }

  public static void renderPlayer(
      Graphics2D g,
      Camera camera,
      WorldSnapshot previous,
      WorldSnapshot current,
      double alpha) {
    if (current == null || current.getPlayers().isEmpty()) {
      return;
    }

    // Fase 5: renderiza todos os players do snapshot
    for (WorldSnapshot.SnapshotPlayer player : current.getPlayers()) {
      WorldSnapshot.SnapshotPlayer prevPlayer = findPlayerById(previous, player.getId());

      double x = lerp(prevPlayer == null ? player.getX() : prevPlayer.getX(), player.getX(), alpha);
      double y = lerp(prevPlayer == null ? player.getY() : prevPlayer.getY(), player.getY(), alpha);

      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());

      BufferedImage sprite = resolvePlayerSprite(player, current.getTick());
      if (sprite != null) {
        g.drawImage(sprite, screenX, screenY, 33, 48, null);
      } else {
        // P1 azul, P2+ verde para distinguir visualmente
        boolean isP1 = "player-1".equals(player.getId());
        g.setColor(isP1 ? new Color(40, 130, 255) : new Color(50, 200, 80));
        g.fillRect(screenX, screenY, 33, 48);
        g.setColor(Color.WHITE);
        g.drawRect(screenX, screenY, 33, 48);
        // Label com ID do player
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 9));
        g.drawString(player.getId(), screenX + 2, screenY + 10);
      }
    }
  }

  private static WorldSnapshot.SnapshotPlayer findPlayerById(WorldSnapshot snapshot, String id) {
    if (snapshot == null || id == null) return null;
    for (WorldSnapshot.SnapshotPlayer p : snapshot.getPlayers()) {
      if (id.equals(p.getId())) return p;
    }
    return null;
  }

  public static void renderProjectiles(
      Graphics2D g,
      Camera camera,
      WorldSnapshot previous,
      WorldSnapshot current,
      double alpha) {
    if (current == null) {
      return;
    }

    g.setColor(new Color(80, 200, 255));
    for (WorldSnapshot.SnapshotProjectile projectile : current.getProjectiles()) {
      WorldSnapshot.SnapshotProjectile prevProjectile = findProjectile(previous, projectile.getId());
      double x = lerp(prevProjectile == null ? projectile.getX() : prevProjectile.getX(), projectile.getX(), alpha);
      double y = lerp(prevProjectile == null ? projectile.getY() : prevProjectile.getY(), projectile.getY(), alpha);
      int screenX = (int) (x - camera.getX());
      int screenY = (int) (y - camera.getY());
      g.fillOval(screenX - 3, screenY - 3, 6, 6);
    }
  }

  public static void renderNpcs(Graphics2D g, Camera camera, WorldSnapshot snapshot) {
    if (snapshot == null) {
      return;
    }

    for (WorldSnapshot.SnapshotNpc npc : snapshot.getNpcs()) {
      int screenX = (int) (npc.getX() - camera.getX());
      int screenY = (int) (npc.getY() - camera.getY());

      int width = npc.getWidth() > 0 ? npc.getWidth() : 48;
      int height = npc.getHeight() > 0 ? npc.getHeight() : 48;
      BufferedImage sprite = loadSprite(npc.getSpritePath());
      if (sprite != null) {
        int scaledWidth = (int) (width * 1.5);
        int scaledHeight = (int) (height * 1.5);
        g.drawImage(sprite, screenX, screenY, scaledWidth, scaledHeight, null);
      } else {
        g.setColor(new Color(180, 180, 220));
        g.fillRect(screenX, screenY, 28, 40);
        g.setColor(Color.BLACK);
        g.drawRect(screenX, screenY, 28, 40);
      }

      if (npc.hasQuest()) {
        g.setColor(Color.YELLOW);
        g.fillOval(screenX - 8, screenY + 10, 10, 10);
      }
    }
  }

  public static void renderChests(Graphics2D g, Camera camera, WorldSnapshot snapshot, FogOfWar fogOfWar) {
    if (snapshot == null) {
      return;
    }

    for (WorldSnapshot.SnapshotChest chest : snapshot.getChests()) {
      int tileX = (int) (chest.getX() / GamePanel.TILE_SIZE);
      int tileY = (int) (chest.getY() / GamePanel.TILE_SIZE);
      if (fogOfWar != null && !fogOfWar.isVisible(tileX, tileY)) {
        continue;
      }

      int screenX = (int) (chest.getX() - camera.getX());
      int screenY = (int) (chest.getY() - camera.getY());

      BufferedImage sprite = chest.isOpen()
          ? loadSprite("sprites/OpenedChest.png")
          : loadSprite("sprites/ClosedChest.png");
      if (sprite != null) {
        g.drawImage(sprite, screenX, screenY, 48, 48, null);
      } else {
        g.setColor(chest.isOpen() ? new Color(120, 120, 120) : new Color(139, 69, 19));
        g.fillRect(screenX, screenY, 48, 48);
        g.setColor(Color.BLACK);
        g.drawRect(screenX, screenY, 48, 48);
      }
    }
  }

  public static void renderStructures(Graphics2D g, Camera camera, List<Structure> structures) {
    if (structures == null) {
      return;
    }

    for (Structure structure : structures) {
      int screenX = (int) (structure.getX() - camera.getX());
      int screenY = (int) (structure.getY() - camera.getY());

      if (structure.isDestroyed()) {
        g.setColor(new Color(64, 64, 64));
        g.fillRect(screenX, screenY, structure.getWidth(), structure.getHeight());
        g.setColor(Color.RED);
        g.drawRect(screenX, screenY, structure.getWidth() - 1, structure.getHeight() - 1);
        continue;
      }

      if (structure.getSprite() != null) {
        g.drawImage(structure.getSprite(), screenX, screenY, structure.getWidth(), structure.getHeight(), null);
      } else {
        g.setColor(new Color(139, 69, 19));
        g.fillRect(screenX, screenY, structure.getWidth(), structure.getHeight());
        g.setColor(Color.BLACK);
        g.drawRect(screenX, screenY, structure.getWidth() - 1, structure.getHeight() - 1);
      }

      if (structure.isVulnerable()) {
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));
        g.drawRect(screenX - 1, screenY - 1, structure.getWidth() + 1, structure.getHeight() + 1);
        g.setStroke(new BasicStroke(1));
      }

      if (structure.isVulnerable() && structure.getCurrentHealth() < structure.getMaxHealth()) {
        int barWidth = structure.getWidth();
        int healthWidth = (int) ((double) structure.getCurrentHealth() / structure.getMaxHealth() * barWidth);
        g.setColor(Color.RED);
        g.fillRect(screenX, screenY - 10, barWidth, 6);
        g.setColor(Color.GREEN);
        g.fillRect(screenX, screenY - 10, Math.max(0, healthWidth), 6);
        g.setColor(Color.BLACK);
        g.drawRect(screenX, screenY - 10, barWidth, 6);
      }
    }
  }

  private static Color colorForEnemyType(String type) {
    if (type == null) {
      return new Color(200, 90, 90);
    }
    String lower = type.toLowerCase();
    if (lower.contains("goblin")) {
      return new Color(80, 170, 80);
    }
    if (lower.contains("golem")) {
      return new Color(130, 130, 130);
    }
    if (lower.contains("mimic")) {
      return new Color(180, 130, 70);
    }
    return new Color(200, 90, 90);
  }

  public static double computeInterpolationAlpha(long lastSnapshotNanos) {
    if (lastSnapshotNanos <= 0) {
      return 1.0;
    }
    long elapsed = System.nanoTime() - lastSnapshotNanos;
    double alpha = (double) elapsed / SNAPSHOT_INTERVAL_NANOS;
    return Math.max(0.0, Math.min(1.0, alpha));
  }

  private static double lerp(double from, double to, double alpha) {
    return from + (to - from) * alpha;
  }

  private static WorldSnapshot.SnapshotEnemy findEnemy(WorldSnapshot snapshot, String id) {
    if (snapshot == null || id == null) {
      return null;
    }
    for (WorldSnapshot.SnapshotEnemy enemy : snapshot.getEnemies()) {
      if (id.equals(enemy.getId())) {
        return enemy;
      }
    }
    return null;
  }

  private static WorldSnapshot.SnapshotProjectile findProjectile(WorldSnapshot snapshot, String id) {
    if (snapshot == null || id == null) {
      return null;
    }
    for (WorldSnapshot.SnapshotProjectile projectile : snapshot.getProjectiles()) {
      if (id.equals(projectile.getId())) {
        return projectile;
      }
    }
    return null;
  }

  private static BufferedImage resolvePlayerSprite(WorldSnapshot.SnapshotPlayer player, long tick) {
    String playerClass = player.getPlayerClass() == null ? "warrior" : player.getPlayerClass().toLowerCase();
    String classPrefix;
    switch (playerClass) {
      case "mage":
        classPrefix = "MagePlayer";
        break;
      case "hunter":
      case "archer":
        classPrefix = "HunterPlayer";
        break;
      default:
        classPrefix = "WarriorPlayer";
        break;
    }

    boolean movingFrameTwo = "MOVING".equalsIgnoreCase(player.getAnimState()) && (tick % 2 == 0);
    String suffix = player.isFacingLeft() ? "Left" : "";
    String frameSuffix = movingFrameTwo ? "2" : "";
    String spriteName = "sprites/" + classPrefix + suffix + frameSuffix + ".png";
    return loadSprite(spriteName);
  }

  private static BufferedImage loadSprite(String spritePath) {
    if (spritePath == null || spritePath.isEmpty()) {
      return null;
    }

    BufferedImage cached = SPRITE_CACHE.get(spritePath);
    if (cached != null) {
      return cached;
    }

    try {
      InputStream is = SnapshotRenderSystem.class.getClassLoader().getResourceAsStream(spritePath);
      if (is != null) {
        BufferedImage image = ImageIO.read(is);
        is.close();
        if (image != null) {
          SPRITE_CACHE.put(spritePath, image);
          return image;
        }
      }

      String resolvedPath = ResourceResolver.getResourcePath(spritePath);
      File file = new File(resolvedPath);
      if (file.exists()) {
        BufferedImage image = ImageIO.read(file);
        if (image != null) {
          SPRITE_CACHE.put(spritePath, image);
          return image;
        }
      }
    } catch (Exception ignored) {
      // Fallback para render por forma.
    }

    return null;
  }
}
