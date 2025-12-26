package com.rpggame.enemies.Golem;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.core.GamePanel;

/**
 * Projétil de pedra lançado pelo Golem
 * Causa dano em área 2x2 tiles
 */
public class GolemStone {

  private double x, y;
  private double targetX, targetY;
  private double startX, startY;
  private double speed = 3.0;
  private int damage;
  private boolean active = true;
  private Player player;

  // Sprites da pedra girando
  private BufferedImage[] stoneSprites = new BufferedImage[4];
  private int currentFrame = 0;
  private int animationTimer = 0;
  private static final int ANIMATION_SPEED = 5; // Frames entre cada sprite

  // Efeito de arco parabólico
  private double travelProgress = 0.0; // 0.0 a 1.0
  private static final double MAX_HEIGHT = 100.0; // Altura máxima do arco

  // Área de impacto
  private boolean hasImpacted = false;
  private int impactTimer = 0;
  private static final int IMPACT_DURATION = 30; // 0.5 segundos de efeito visual
  private static final int STUN_DURATION = 60; // 1 segundo de atordoamento

  // Tamanho da pedra
  private static final int STONE_SIZE = 32;

  /**
   * Construtor do GolemStone
   */
  public GolemStone(double startX, double startY, double targetX, double targetY, int damage, Player player) {
    this.startX = startX;
    this.startY = startY;
    this.x = startX;
    this.y = startY;
    this.targetX = targetX;
    this.targetY = targetY;
    this.damage = damage;
    this.player = player;
    loadStoneSprites();
  }

  /**
   * Carrega os sprites da pedra girando
   */
  private void loadStoneSprites() {
    int loadedCount = 0;
    for (int i = 0; i < 4; i++) {
      String path = "sprites/GOLEMStone" + (i + 1) + ".png";
      stoneSprites[i] = loadSpriteFile(path);
      if (stoneSprites[i] != null) {
        loadedCount++;
      }
    }

    if (loadedCount == 4) {
      System.out.println("✅ Todos os sprites da pedra do Golem carregados (" + loadedCount + "/4)");
    } else {
      System.err.println("⚠️ Apenas " + loadedCount + "/4 sprites da pedra foram carregados");
    }
  }

  /**
   * Carrega um arquivo de sprite (igual ao método da classe Enemy)
   */
  private BufferedImage loadSpriteFile(String path) {
    try {
      // Tentar carregar como recurso do classpath
      InputStream is = getClass().getClassLoader().getResourceAsStream(path);
      if (is != null) {
        BufferedImage img = ImageIO.read(is);
        is.close();
        if (img != null) {
          return img;
        }
      }

      // Fallback: tentar carregar como arquivo externo
      String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(path);
      java.io.File file = new java.io.File(resolvedPath);
      if (file.exists()) {
        BufferedImage img = ImageIO.read(file);
        if (img != null) {
          return img;
        }
      }
    } catch (IOException e) {
      System.err.println("❌ Erro ao carregar " + path + ": " + e.getMessage());
    }

    return null;
  }

  /**
   * Atualiza a posição e estado da pedra
   */
  public void update() {
    if (!active)
      return;

    if (hasImpacted) {
      impactTimer--;
      if (impactTimer <= 0) {
        active = false;
      }
      return;
    }

    // Calcular progresso da viagem
    double deltaX = targetX - startX;
    double deltaY = targetY - startY;
    double totalDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    // Mover em direção ao alvo
    double moveX = (targetX - x);
    double moveY = (targetY - y);
    double currentDistance = Math.sqrt(moveX * moveX + moveY * moveY);

    if (currentDistance > speed) {
      double angle = Math.atan2(moveY, moveX);
      x += Math.cos(angle) * speed;
      y += Math.sin(angle) * speed;

      // Atualizar progresso
      travelProgress = 1.0 - (currentDistance / totalDistance);
    } else {
      // Chegou no alvo
      x = targetX;
      y = targetY;
      impact();
    }

    // Atualizar animação
    animationTimer++;
    if (animationTimer >= ANIMATION_SPEED) {
      animationTimer = 0;
      currentFrame = (currentFrame + 1) % 4;
    }
  }

  /**
   * Executa o impacto da pedra
   */
  private void impact() {
    hasImpacted = true;
    impactTimer = IMPACT_DURATION;

    // Verificar se o player está na área de impacto (2x2 tiles)
    int tileSize = GamePanel.TILE_SIZE;
    double areaRadius = tileSize; // Raio de 1 tile (área 2x2)

    double distanceToPlayer = Math.sqrt(
        Math.pow(player.getX() - x, 2) +
            Math.pow(player.getY() - y, 2));

    if (distanceToPlayer <= areaRadius) {
      // Player foi atingido!
      player.takeDamage(damage);

      // Aplicar atordoamento
      boolean wasStunned = player.applyStun(STUN_DURATION);
      if (wasStunned) {
        System.out.println("💫 Player foi atordoado pelo ataque do Golem!");
      }
    }

    System.out.println("💥 Pedra do Golem impactou o solo!");
  }

  /**
   * Renderiza a pedra
   */
  public void render(Graphics2D g, Camera camera) {
    if (!active)
      return;

    int screenX = (int) (x - camera.getX());
    int screenY = (int) (y - camera.getY());

    if (hasImpacted) {
      // Renderizar efeito de impacto
      renderImpactEffect(g, screenX, screenY);
    } else {
      // Renderizar pedra voando com efeito de altura
      double height = calculateHeight();
      int heightOffset = (int) height;

      // Desenhar sombra no chão
      g.setColor(new Color(0, 0, 0, 80));
      g.fillOval(screenX - STONE_SIZE / 2, screenY - STONE_SIZE / 2, STONE_SIZE, STONE_SIZE);

      // Desenhar pedra com offset de altura
      if (stoneSprites[currentFrame] != null) {
        g.drawImage(
            stoneSprites[currentFrame],
            screenX - STONE_SIZE / 2,
            screenY - STONE_SIZE / 2 - heightOffset,
            STONE_SIZE,
            STONE_SIZE,
            null);
      } else {
        // Fallback
        g.setColor(Color.GRAY);
        g.fillOval(screenX - STONE_SIZE / 2, screenY - STONE_SIZE / 2 - heightOffset, STONE_SIZE, STONE_SIZE);
      }

      // Indicador de trajetória (linha pontilhada até o alvo)
      g.setColor(new Color(255, 0, 0, 100));
      g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 5 }, 0));
      int targetScreenX = (int) (targetX - camera.getX());
      int targetScreenY = (int) (targetY - camera.getY());
      g.drawLine(screenX, screenY, targetScreenX, targetScreenY);
    }
  }

  /**
   * Calcula a altura da pedra no arco parabólico
   */
  private double calculateHeight() {
    // Parábola: y = 4h * x * (1 - x) onde x é o progresso (0 a 1)
    return MAX_HEIGHT * 4 * travelProgress * (1 - travelProgress);
  }

  /**
   * Renderiza o efeito visual de impacto
   */
  private void renderImpactEffect(Graphics2D g, int screenX, int screenY) {
    float intensity = (float) impactTimer / IMPACT_DURATION;
    int tileSize = GamePanel.TILE_SIZE;
    int areaSize = tileSize * 2;

    // Ondas de choque expandindo
    for (int i = 0; i < 3; i++) {
      int waveRadius = (int) ((1.0 - intensity) * (areaSize / 2 + i * 20));
      int alpha = (int) (255 * intensity);

      g.setColor(new Color(255, 150, 0, Math.max(0, alpha - i * 50)));
      g.setStroke(new BasicStroke(4 - i));
      g.drawOval(screenX - waveRadius, screenY - waveRadius, waveRadius * 2, waveRadius * 2);
    }

    // Área de dano
    g.setColor(new Color(255, 0, 0, (int) (150 * intensity)));
    g.fillRect(screenX - areaSize / 2, screenY - areaSize / 2, areaSize, areaSize);

    // Partículas de destroços
    for (int i = 0; i < 8; i++) {
      double angle = (Math.PI * 2 / 8) * i;
      int particleDistance = (int) ((1.0 - intensity) * 40);
      int particleX = screenX + (int) (Math.cos(angle) * particleDistance);
      int particleY = screenY + (int) (Math.sin(angle) * particleDistance);

      g.setColor(new Color(100, 100, 100, (int) (255 * intensity)));
      g.fillRect(particleX - 3, particleY - 3, 6, 6);
    }

    // Efeito de tela tremendo (screen shake) - visual
    if (intensity > 0.7) {
      g.setColor(new Color(255, 255, 255, (int) (100 * intensity)));
      g.setStroke(new BasicStroke(2));

      // Linhas de impacto radiais
      for (int i = 0; i < 12; i++) {
        double angle = (Math.PI * 2 / 12) * i;
        int lineLength = (int) (60 * intensity);
        int endX = screenX + (int) (Math.cos(angle) * lineLength);
        int endY = screenY + (int) (Math.sin(angle) * lineLength);
        g.drawLine(screenX, screenY, endX, endY);
      }
    }
  }

  /**
   * Retorna se a pedra ainda está ativa
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Retorna os bounds da área de impacto para colisão
   */
  public Rectangle getImpactBounds() {
    int tileSize = GamePanel.TILE_SIZE;
    int areaSize = tileSize * 2;
    return new Rectangle((int) (x - areaSize / 2), (int) (y - areaSize / 2), areaSize, areaSize);
  }

  /**
   * Retorna se a pedra já impactou
   */
  public boolean hasImpacted() {
    return hasImpacted;
  }
}
