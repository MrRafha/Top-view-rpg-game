package com.rpggame.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

/**
 * Minigame de lockpicking para abrir baús.
 * Player precisa apertar F quando o marcador estiver na área verde
 */
public class LockpickingMinigame {
  private boolean active = false;
  private boolean success = false;
  private boolean finished = false;

  // Círculo principal
  private int centerX = 512; // Centro da tela (1024/2)
  private int centerY = 400; // Centro da tela (800/2)
  private int radius = 100;

  // Área verde (sucesso)
  private double greenStartAngle = 45; // Graus
  private double greenArcAngle = 30; // Tamanho da área verde

  // Marcador rotativo
  private double markerAngle = 0;
  private double markerSpeed = 3.0; // Velocidade de rotação

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
    markerAngle = 0;

    // Randomizar posição da zona verde
    greenStartAngle = Math.random() * 360;

    System.out.println("🔓 Minigame de lockpicking iniciado!");
  }

  /**
   * Atualiza o minigame
   */
  public void update() {
    if (!active || finished)
      return;

    // Rotacionar marcador
    markerAngle += markerSpeed;
    if (markerAngle >= 360) {
      markerAngle -= 360;
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
      // Ambos agora usam o sistema visual (0°=Norte/topo)
      // Comparar markerAngle diretamente com greenStartAngle
      double normalizedMarker = markerAngle % 360;
      double greenStart = greenStartAngle % 360;
      double greenEnd = (greenStartAngle + greenArcAngle) % 360;

      boolean inGreenZone = false;

      // Se a zona verde não cruza 0°
      if (greenEnd > greenStart) {
        inGreenZone = normalizedMarker >= greenStart && normalizedMarker <= greenEnd;
      } else {
        // Zona verde cruza 0° (ex: 350° - 10°)
        inGreenZone = normalizedMarker >= greenStart || normalizedMarker <= greenEnd;
      }

      success = inGreenZone;
      finished = true;

      System.out.println("🎯 Tentativa de lockpicking:");
      System.out.println("  Marcador: " + String.format("%.1f", normalizedMarker) + "°");
      System.out.println("  Zona verde: " + String.format("%.1f", greenStart) + "° - "
          + String.format("%.1f", greenEnd) + "°");
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

    // Círculo principal (preto)
    g.setColor(circleColor);
    g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

    // Zona verde (área de sucesso) - renderizar no sistema visual (0°=Norte)
    // fillArc usa 0°=Leste, então subtraímos 90° para alinhar com o topo
    double greenStartVisual = greenStartAngle - 90;
    g.setColor(greenZoneColor);
    g.fillArc(
        centerX - radius,
        centerY - radius,
        radius * 2,
        radius * 2,
        (int) greenStartVisual,
        (int) greenArcAngle);

    // Borda do círculo
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(3));
    g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

    // Marcador (linha do centro para a borda) - também renderizar no sistema visual
    double markerRadians = Math.toRadians(markerAngle - 90); // -90 para começar no topo
    int markerEndX = centerX + (int) (Math.cos(markerRadians) * radius);
    int markerEndY = centerY + (int) (Math.sin(markerRadians) * radius);

    g.setColor(markerColor);
    g.setStroke(new BasicStroke(4));
    g.drawLine(centerX, centerY, markerEndX, markerEndY);

    // Ponto no final do marcador
    g.fillOval(markerEndX - 6, markerEndY - 6, 12, 12);

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
