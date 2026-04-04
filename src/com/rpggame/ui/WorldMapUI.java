package com.rpggame.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.rpggame.world.MapLoader;
import com.rpggame.world.MapManager;
import com.rpggame.world.TileType;
import com.rpggame.world.WorldLayout;

/**
 * Overlay de mapa mundi para consulta de regiões já conhecidas.
 * Não realiza fast travel nesta versão.
 */
public class WorldMapUI {
  private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 165);
  private static final Color PANEL_COLOR = new Color(20, 24, 34, 235);
  private static final Color PANEL_BORDER = new Color(180, 150, 90);
  private static final Color TITLE_COLOR = new Color(255, 225, 160);
  private static final Color TEXT_COLOR = new Color(230, 230, 230);
  private static final Color SUBTEXT_COLOR = new Color(170, 180, 200);
  private static final Color CURRENT_BORDER = new Color(255, 215, 0);
  private static final Color CONNECTION_COLOR = new Color(110, 140, 190);
  private static final Color DOT_COLOR = new Color(220, 220, 220);

  private static final int PANEL_WIDTH = 1120;
  private static final int PANEL_HEIGHT = 620;
  private static final int THUMBNAIL_SCALE = 4;
  private static final int THUMBNAIL_WIDTH = 100;
  private static final int THUMBNAIL_HEIGHT = 100;

  private final MapManager mapManager;
  private final Map<String, BufferedImage> thumbnailCache = new HashMap<>();

  private boolean visible;

  public WorldMapUI(MapManager mapManager) {
    this.mapManager = mapManager;
    this.visible = false;
    buildThumbnailCache();
  }

  public void toggle() {
    visible = !visible;
  }

  public void show() {
    visible = true;
  }

  public void hide() {
    visible = false;
  }

  public boolean isVisible() {
    return visible;
  }

  private void buildThumbnailCache() {
    for (Map.Entry<String, MapManager.MapData> entry : mapManager.getAllMaps().entrySet()) {
      TileType[][] map = MapLoader.loadMapFromFile(entry.getValue().getFilePath());
      if (map != null) {
        thumbnailCache.put(entry.getKey(), renderThumbnail(map));
      }
    }
  }

  private BufferedImage renderThumbnail(TileType[][] map) {
    int height = map.length;
    int width = map[0].length;
    BufferedImage image = new BufferedImage(width * THUMBNAIL_SCALE, height * THUMBNAIL_SCALE,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        g2d.setColor(getTileColor(map[y][x]));
        g2d.fillRect(x * THUMBNAIL_SCALE, y * THUMBNAIL_SCALE, THUMBNAIL_SCALE, THUMBNAIL_SCALE);
      }
    }

    g2d.dispose();
    return image;
  }

  private Color getTileColor(TileType tileType) {
    switch (tileType) {
      case WALL:
        return new Color(44, 44, 54);
      case STONE:
        return new Color(95, 95, 100);
      case WATER:
        return new Color(30, 90, 170);
      case WALKABLE_WATER:
        return new Color(60, 160, 170);
      case SAND:
        return new Color(210, 185, 130);
      case DIRT:
        return new Color(125, 90, 60);
      case GRASS_PATH:
        return new Color(85, 130, 65);
      case PORTAL:
        return new Color(200, 100, 220);
      case GRASS:
      default:
        return new Color(45, 115, 60);
    }
  }

  public void render(Graphics2D g, int screenWidth, int screenHeight) {
    if (!visible) {
      return;
    }

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    g.setColor(OVERLAY_COLOR);
    g.fillRect(0, 0, screenWidth, screenHeight);

    int panelX = (screenWidth - PANEL_WIDTH) / 2;
    int panelY = (screenHeight - PANEL_HEIGHT) / 2;

    g.setColor(PANEL_COLOR);
    g.fillRoundRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 24, 24);
    g.setColor(PANEL_BORDER);
    g.setStroke(new BasicStroke(3f));
    g.drawRoundRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 24, 24);

    g.setFont(new Font("Arial", Font.BOLD, 30));
    g.setColor(TITLE_COLOR);
    g.drawString("MAPA MUNDI", panelX + 28, panelY + 42);

    g.setFont(new Font("Arial", Font.PLAIN, 13));
    g.setColor(SUBTEXT_COLOR);
    g.drawString("Visualização do mundo atual. Pressione M ou ESC para fechar.", panelX + 28, panelY + 66);

    String currentMapId = mapManager.getCurrentMapId();
    Map<String, MapManager.MapData> maps = mapManager.getAllMaps();
    WorldLayout layout = mapManager.getWorldLayout();
    // Exibimos apenas o que já foi explorado, mantendo a sala atual sempre visível.
    Set<String> visibleMapIds = new LinkedHashSet<>(mapManager.getDiscoveredMapIds());
    visibleMapIds.add(currentMapId);

    Map<String, Point> nodePixels = projectNodes(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, layout, visibleMapIds);
    drawLayoutConnections(g, layout, nodePixels, visibleMapIds);

    for (Map.Entry<String, Point> nodeEntry : nodePixels.entrySet()) {
      Point p = nodeEntry.getValue();
      renderNode(g, maps, nodeEntry.getKey(), p.x, p.y, currentMapId);
    }

    drawLegend(g, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, visibleMapIds.size(), maps.size());
  }

  private Map<String, Point> projectNodes(int panelX, int panelY, int panelWidth, int panelHeight, WorldLayout layout,
      Set<String> visibleMapIds) {
    Map<String, Point> pixels = new HashMap<>();
    if (layout == null) {
      return pixels;
    }

    // Filtra posições do layout completo para desenhar só os nós descobertos.
    Map<String, Point> gridPositions = new HashMap<>();
    for (Map.Entry<String, Point> entry : layout.getAllNodePositions().entrySet()) {
      if (visibleMapIds.contains(entry.getKey())) {
        gridPositions.put(entry.getKey(), entry.getValue());
      }
    }

    if (gridPositions.isEmpty()) {
      return pixels;
    }

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (Point p : gridPositions.values()) {
      minX = Math.min(minX, p.x);
      minY = Math.min(minY, p.y);
      maxX = Math.max(maxX, p.x);
      maxY = Math.max(maxY, p.y);
    }

    int cols = Math.max(1, maxX - minX + 1);
    int rows = Math.max(1, maxY - minY + 1);

    // Reserva margens para título/legenda e distribui os nós no espaço restante.
    int drawableWidth = panelWidth - 180;
    int drawableHeight = panelHeight - 180;
    int stepX = Math.max(170, drawableWidth / cols);
    int stepY = Math.max(140, drawableHeight / rows);
    int startX = panelX + 70;
    int startY = panelY + 100;

    for (Map.Entry<String, Point> entry : gridPositions.entrySet()) {
      int normalizedX = entry.getValue().x - minX;
      int normalizedY = entry.getValue().y - minY;
      pixels.put(entry.getKey(), new Point(startX + normalizedX * stepX, startY + normalizedY * stepY));
    }

    return pixels;
  }

  private void drawLayoutConnections(Graphics2D g, WorldLayout layout, Map<String, Point> nodePixels,
      Set<String> visibleMapIds) {
    if (layout == null || nodePixels.isEmpty()) {
      return;
    }

    // Evita desenhar a mesma aresta duas vezes (A->B e B->A).
    Set<String> drawn = new HashSet<>();
    for (Map.Entry<String, Map<com.rpggame.world.Direction, String>> entry : layout.getAllConnections().entrySet()) {
      String fromMapId = entry.getKey();
      if (!visibleMapIds.contains(fromMapId)) {
        continue;
      }
      Point from = nodePixels.get(fromMapId);
      if (from == null) {
        continue;
      }

      for (String toMapId : entry.getValue().values()) {
        if (!visibleMapIds.contains(toMapId)) {
          continue;
        }
        Point to = nodePixels.get(toMapId);
        if (to == null) {
          continue;
        }

        String edgeKeyA = fromMapId + "->" + toMapId;
        String edgeKeyB = toMapId + "->" + fromMapId;
        if (drawn.contains(edgeKeyA) || drawn.contains(edgeKeyB)) {
          continue;
        }

        drawConnection(g, from, to);
        drawn.add(edgeKeyA);
      }
    }
  }

  private void drawConnection(Graphics2D g, Point start, Point end) {
    g.setColor(CONNECTION_COLOR);
    g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.drawLine(start.x + THUMBNAIL_WIDTH / 2, start.y + THUMBNAIL_HEIGHT / 2,
        end.x + THUMBNAIL_WIDTH / 2, end.y + THUMBNAIL_HEIGHT / 2);
  }

  private void renderNode(Graphics2D g, Map<String, MapManager.MapData> maps, String mapId,
      int x, int y, String currentMapId) {
    MapManager.MapData data = maps.get(mapId);
    if (data == null) {
      return;
    }

    BufferedImage thumbnail = thumbnailCache.get(mapId);
    boolean isCurrent = mapId.equals(currentMapId);

    if (thumbnail != null) {
      g.drawImage(thumbnail, x, y, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, null);
    } else {
      g.setColor(new Color(80, 80, 90));
      g.fillRoundRect(x, y, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, 12, 12);
    }

    g.setColor(isCurrent ? CURRENT_BORDER : PANEL_BORDER);
    g.setStroke(new BasicStroke(isCurrent ? 4f : 2f));
    g.drawRoundRect(x, y, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, 12, 12);

    g.setColor(isCurrent ? CURRENT_BORDER : TEXT_COLOR);
    g.fillOval(x + THUMBNAIL_WIDTH / 2 - 5, y - 12, 10, 10);

    g.setFont(new Font("Arial", Font.BOLD, 15));
    String label = data.getName();
    FontMetrics fm = g.getFontMetrics();
    int labelWidth = fm.stringWidth(label);
    g.drawString(label, x + (THUMBNAIL_WIDTH - labelWidth) / 2, y + THUMBNAIL_HEIGHT + 20);

    g.setFont(new Font("Arial", Font.PLAIN, 11));
    g.setColor(SUBTEXT_COLOR);
    String fileLabel = mapId;
    int fileWidth = g.getFontMetrics().stringWidth(fileLabel);
    g.drawString(fileLabel, x + (THUMBNAIL_WIDTH - fileWidth) / 2, y + THUMBNAIL_HEIGHT + 36);
  }

  private void drawLegend(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight,
      int discoveredCount, int totalCount) {
    int legendX = panelX + panelWidth - 300;
    int legendY = panelY + 26;

    g.setFont(new Font("Arial", Font.BOLD, 13));
    g.setColor(TITLE_COLOR);
    g.drawString("Legendas", legendX, legendY);

    g.setFont(new Font("Arial", Font.PLAIN, 12));
    g.setColor(TEXT_COLOR);
    g.drawString("• Amarelo: mapa atual", legendX, legendY + 22);
    g.drawString("• Linhas: conexões descobertas", legendX, legendY + 42);
    g.drawString("• Descobertos: " + discoveredCount + "/" + totalCount, legendX, legendY + 62);

    g.setColor(DOT_COLOR);
    g.fillOval(legendX - 14, legendY + 12, 8, 8);
    g.setColor(CONNECTION_COLOR);
    g.fillRect(legendX - 16, legendY + 32, 12, 4);
  }
}