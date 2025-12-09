package com.rpggame.world;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Gerencia a animação de transição entre mapas
 * Efeito de círculo expandindo/contraindo para fade in/out
 */
public class MapTransition {
  private boolean isTransitioning = false;
  private boolean isFadingOut = true; // true = escurecendo, false = clareando
  private float transitionProgress = 0.0f;
  private static final float TRANSITION_SPEED = 0.03f; // Velocidade da transição
  
  private String targetMapPath;
  private int playerSpawnX, playerSpawnY;
  
  /**
   * Inicia uma transição para um novo mapa
   */
  public void startTransition(String mapPath, int spawnX, int spawnY) {
    this.targetMapPath = mapPath;
    this.playerSpawnX = spawnX;
    this.playerSpawnY = spawnY;
    this.isTransitioning = true;
    this.isFadingOut = true;
    this.transitionProgress = 0.0f;
    System.out.println("🌀 Transição iniciada para: " + mapPath);
  }
  
  /**
   * Atualiza a animação de transição
   * @return true quando a transição está no meio (tela totalmente preta)
   */
  public boolean update() {
    if (!isTransitioning) return false;
    
    if (isFadingOut) {
      // Fase 1: Escurecendo
      transitionProgress += TRANSITION_SPEED;
      
      if (transitionProgress >= 1.0f) {
        transitionProgress = 1.0f;
        isFadingOut = false;
        // Momento de trocar o mapa (tela totalmente preta)
        return true;
      }
    } else {
      // Fase 2: Clareando
      transitionProgress -= TRANSITION_SPEED;
      
      if (transitionProgress <= 0.0f) {
        transitionProgress = 0.0f;
        isTransitioning = false;
        System.out.println("✅ Transição concluída!");
      }
    }
    
    return false;
  }
  
  /**
   * Renderiza o efeito de transição
   */
  public void render(Graphics2D g, int screenWidth, int screenHeight) {
    if (!isTransitioning) return;
    
    // Salvar configurações originais
    Color originalColor = g.getColor();
    Composite originalComposite = g.getComposite();
    
    // Calcular raio do círculo (começa grande, encolhe até cobrir tudo)
    int centerX = screenWidth / 2;
    int centerY = screenHeight / 2;
    
    // Raio máximo que cobre toda a tela (diagonal)
    double maxRadius = Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) / 2;
    
    // Raio atual (inverte durante fade out)
    double currentRadius = maxRadius * (1.0 - transitionProgress);
    
    // Criar máscara circular
    Shape originalClip = g.getClip();
    
    if (currentRadius > 0) {
      // Desenhar círculo vazado (área visível)
      Ellipse2D circle = new Ellipse2D.Double(
        centerX - currentRadius,
        centerY - currentRadius,
        currentRadius * 2,
        currentRadius * 2
      );
      
      // Inverter: desenhar tudo preto exceto o círculo
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, screenWidth, screenHeight);
      
      // Criar área de clipping invertida
      java.awt.geom.Area screenArea = new java.awt.geom.Area(
        new Rectangle(0, 0, screenWidth, screenHeight)
      );
      java.awt.geom.Area circleArea = new java.awt.geom.Area(circle);
      screenArea.subtract(circleArea);
      
      g.setClip(screenArea);
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, screenWidth, screenHeight);
    } else {
      // Tela totalmente preta
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, screenWidth, screenHeight);
    }
    
    // Restaurar configurações
    g.setClip(originalClip);
    g.setColor(originalColor);
    g.setComposite(originalComposite);
  }
  
  /**
   * Renderiza versão alternativa (fade simples se círculo não funcionar bem)
   */
  public void renderSimpleFade(Graphics2D g, int screenWidth, int screenHeight) {
    if (!isTransitioning) return;
    
    // Fade simples com alpha
    int alpha = (int)(transitionProgress * 255);
    g.setColor(new Color(0, 0, 0, alpha));
    g.fillRect(0, 0, screenWidth, screenHeight);
  }
  
  // Getters
  public boolean isTransitioning() {
    return isTransitioning;
  }
  
  public String getTargetMapPath() {
    return targetMapPath;
  }
  
  public int getPlayerSpawnX() {
    return playerSpawnX;
  }
  
  public int getPlayerSpawnY() {
    return playerSpawnY;
  }
  
  public boolean isFadingOut() {
    return isFadingOut;
  }
  
  public float getProgress() {
    return transitionProgress;
  }
}
