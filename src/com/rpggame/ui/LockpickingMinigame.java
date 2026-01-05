package com.rpggame.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Minigame de lockpicking para abrir baús.
 * Player precisa apertar F quando o marcador estiver na área verde.
 * Sistema baseado em pontos discretos (0-360) para evitar bugs de ângulos.
 */
public class LockpickingMinigame {
  private boolean active = false;
  private boolean success = false;
  private boolean finished = false;

  // Círculo principal
  private int centerX = 512; // Centro da tela (1024/2)
  private int centerY = 400; // Centro da tela (800/2)
  private int radius = 100;

  // Sistema de pontos discretos
  private static final int TOTAL_POINTS = 360; // 0-359
  private static final int GREEN_ZONE_SIZE = 20; // 20 pontos verdes
  private Set<Integer> greenZoneIndices = new HashSet<>();

  // Marcador rotativo
  private int currentMarkerIndex = 0;
  private int markerSpeed = 2; // Velocidade em pontos por frame
  private int dotRadius = 3; // Raio de cada ponto no círculo

  // Cores
  private Color backgroundColor = new Color(0, 0, 0, 200);
  private Color circleColor = new Color(30, 30, 30);
  private Color greenZoneColor = new Color(50, 200, 50);
  private Color markerColor = new Color(255, 215, 0);
  private Color successColor = new Color(0, 255, 0);
  private Color failColor = new Color(255, 0, 0);

  /**
   * Inicia o minigame
   */
  public void start() {
    active = true;
    success = false;
    finished = false;
    currentMarkerIndex = 0;

    // Sortear 10 índices consecutivos para serem a zona verde
    Random random = new Random();
    int greenStart = random.nextInt(TOTAL_POINTS);

    greenZoneIndices.clear();
    for (int i = 0; i < GREEN_ZONE_SIZE; i++) {
      greenZoneIndices.add((greenStart + i) % TOTAL_POINTS);
    }

    System.out.println("🔓 Minigame de lockpicking iniciado!");
    System.out.println("  Zona verde começa no índice: " + greenStart);
  }

  /**
   * Atualiza o minigame
   */
  public void update() {
    if (!active || finished)
      return;

    // Avançar marcador pelos pontos
    currentMarkerIndex += markerSpeed;
    if (currentMarkerIndex >= TOTAL_POINTS) {
      currentMarkerIndex = currentMarkerIndex % TOTAL_POINTS;
    }
  }

  /**
   * Processa input do jogador.
   * 
   * @return true se bem-sucedido, false se falhou
   */
  public boolean handleInput(int keyCode) {
    if (!active || finished) {
      return false;
    }

    if (keyCode == KeyEvent.VK_F) {
      // Verificar se o índice atual está na zona verde
      success = greenZoneIndices.contains(currentMarkerIndex);
      finished = true;

      System.out.println("🎯 Tentativa de lockpicking:");
      System.out.println("  Marcador no índice: " + currentMarkerIndex);
      System.out.println("  Zona verde: " + greenZoneIndices);
      System.out.println("  Resultado: " + (success ? "✅ SUCESSO!" : "❌ FALHOU!"));

      return success;
    }
    return false;
  }

  /**
   * Reinicia o minigame.
   */
  public void reset() {
    start();
  }

  /**
   * Renderiza o minigame.
   */
  public void render(Graphics2D g, int screenWidth, int screenHeight) {
    if (!active) {
      return;
    }

    // Recalcular centro baseado na tela
    centerX = screenWidth / 2;
    centerY = screenHeight / 2;

    // Ativar anti-aliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Fundo escurecido
    g.setColor(backgroundColor);
    g.fillRect(0, 0, screenWidth, screenHeight);

    // Renderizar todos os 360 pontos ao redor do círculo
    for (int i = 0; i < TOTAL_POINTS; i++) {
      // Calcular posição de cada ponto (começando do topo, 0° = Norte)
      double angleRadians = Math.toRadians(i - 90); // -90 para começar no topo
      int pointX = centerX + (int) (Math.cos(angleRadians) * radius);
      int pointY = centerY + (int) (Math.sin(angleRadians) * radius);

      // Determinar cor do ponto
      Color pointColor;
      if (i == currentMarkerIndex) {
        // Marcador atual - amarelo/dourado
        pointColor = markerColor;
      } else if (greenZoneIndices.contains(i)) {
        // Zona verde
        pointColor = greenZoneColor;
      } else {
        // Ponto normal - branco
        pointColor = Color.WHITE;
      }

      g.setColor(pointColor);
      g.fillOval(pointX - dotRadius, pointY - dotRadius, dotRadius * 2, dotRadius * 2);
    }

    // Linha do marcador (do centro até o ponto atual)
    double markerRadians = Math.toRadians(currentMarkerIndex - 90);
    int markerEndX = centerX + (int) (Math.cos(markerRadians) * radius);
    int markerEndY = centerY + (int) (Math.sin(markerRadians) * radius);

    g.setColor(markerColor);
    g.setStroke(new BasicStroke(3));
    g.drawLine(centerX, centerY, markerEndX, markerEndY);

    // Círculo central
    g.setColor(circleColor);
    g.fillOval(centerX - 10, centerY - 10, 20, 20);

    // Instruções
    g.setColor(Color.WHITE);
    g.setFont(new Font("Arial", Font.BOLD, 20));
    String instruction = "Aperte [F] quando o marcador estiver no VERDE!";
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(instruction);
    g.drawString(instruction, (screenWidth - textWidth) / 2, centerY + radius + 50);

    // Resultado (se finalizado)
    if (finished) {
      g.setFont(new Font("Arial", Font.BOLD, 36));
      String result = success ? "SUCESSO!" : "FALHOU!";
      g.setColor(success ? successColor : failColor);
      textWidth = g.getFontMetrics().stringWidth(result);
      g.drawString(result, (screenWidth - textWidth) / 2, centerY - radius - 30);
    }
  }

  /**
   * Fecha o minigame
   */
  public void close() {
    active = false;
  }

  // Getters
  public boolean isActive() {
    return active;
  }

  public boolean isSuccess() {
    return success;
  }

  public boolean isFinished() {
    return finished;
  }
}
