package com.rpggame.world;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.rpggame.core.GamePanel;

/**
 * Classe para carregar e gerenciar mapas personalizados
 */
public class MapLoader {

  /**
   * Carrega um mapa de um arquivo .txt
   * Formato: cada linha representa uma linha do mapa, cada char um tile
   */
  public static TileType[][] loadMapFromFile(String filePath) {
    try {
      List<String> lines = new ArrayList<>();

      // Tentar carregar como recurso primeiro (funciona dentro do JAR)
      InputStream resourceStream = MapLoader.class.getClassLoader().getResourceAsStream(filePath);
      if (resourceStream != null) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream))) {
          String line;
          while ((line = reader.readLine()) != null) {
            lines.add(line);
          }
          System.out.println("Mapa carregado como recurso: " + filePath);
        }
      } else {
        // Fallback: tentar carregar como arquivo do sistema (desenvolvimento)
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
          String line;
          while ((line = reader.readLine()) != null) {
            lines.add(line);
          }
          System.out.println("Mapa carregado como arquivo: " + filePath);
        }
      }
      if (lines.isEmpty()) {
        return generateDefaultMap();
      }

      // Encontrar dimensões do mapa
      int height = lines.size();
      int width = 0;
      for (String line : lines) {
        width = Math.max(width, line.length());
      }

      // Criar array de tiles
      TileType[][] map = new TileType[height][width];

      for (int y = 0; y < height; y++) {
        String line = lines.get(y);
        for (int x = 0; x < width; x++) {
          char tileChar = (x < line.length()) ? line.charAt(x) : '.';
          map[y][x] = TileType.fromChar(tileChar);
        }
      }

      System.out.println("Mapa carregado com sucesso: " + filePath);
      System.out.println("Dimensões: " + height + "x" + width);
      return map;

    } catch (IOException e) {
      System.err.println("Erro ao carregar mapa: " + filePath);
      System.err.println("Motivo: " + e.getMessage());
      System.err.println("Gerando mapa padrão...");
      return generateDefaultMap();
    }
  }

  /**
   * Salva um mapa em arquivo .txt
   */
  public static void saveMapToFile(TileType[][] map, String filePath) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
      for (int y = 0; y < map.length; y++) {
        StringBuilder line = new StringBuilder();
        for (int x = 0; x < map[y].length; x++) {
          line.append(map[y][x].toChar());
        }
        writer.println(line.toString());
      }
      System.out.println("Mapa salvo em: " + filePath);
    } catch (IOException e) {
      System.err.println("Erro ao salvar mapa: " + e.getMessage());
    }
  }

  /**
   * Gera um mapa padrão procedural com diferentes tiles
   */
  public static TileType[][] generateDefaultMap() {
    int width = GamePanel.MAP_WIDTH;
    int height = GamePanel.MAP_HEIGHT;
    TileType[][] map = new TileType[height][width];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Bordas são sempre paredes
        if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
          map[y][x] = TileType.WALL;
        }
        // Algumas pedras aleatórias
        else if (Math.random() < 0.1) {
          map[y][x] = TileType.STONE;
        }
        // Algumas áreas de terra
        else if (Math.random() < 0.15) {
          map[y][x] = TileType.DIRT;
        }
        // Resto é grama
        else {
          map[y][x] = TileType.GRASS;
        }
      }
    }

    // Garantir algumas áreas abertas para movimento
    for (int i = 0; i < 5; i++) {
      int centerX = 2 + (int) (Math.random() * (width - 4));
      int centerY = 2 + (int) (Math.random() * (height - 4));

      // Criar área 3x3 de grama
      for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
          int nx = centerX + dx;
          int ny = centerY + dy;
          if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1) {
            map[ny][nx] = TileType.GRASS;
          }
        }
      }
    }

    return map;
  }

  /**
   * Cria um mapa de exemplo e salva em arquivo
   */
  public static void createExampleMap() {
    String exampleMap = "WWWWWWWWWWWWWWWWWWWWW\n" +
        "W...................W\n" +
        "W.##.......##.......W\n" +
        "W....~~~~~..........W\n" +
        "W....~~~~~..##......W\n" +
        "W................#..W\n" +
        "W.......dddddd......W\n" +
        "W.......dddddd......W\n" +
        "W...................W\n" +
        "W..##......ssss.....W\n" +
        "W..##......ssss.....W\n" +
        "W...................W\n" +
        "W.....###...........W\n" +
        "W.....###...........W\n" +
        "W.....###...........W\n" +
        "W...................W\n" +
        "WWWWWWWWWWWWWWWWWWWWW";

    try (PrintWriter writer = new PrintWriter(new FileWriter("example_map.txt"))) {
      writer.print(exampleMap);
      System.out.println("Mapa de exemplo criado: example_map.txt");
    } catch (IOException e) {
      System.err.println("Erro ao criar mapa de exemplo: " + e.getMessage());
    }
  }
}