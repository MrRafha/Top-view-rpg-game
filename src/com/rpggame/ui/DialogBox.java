package com.rpggame.ui;

import java.awt.*;

/**
 * Caixa de diálogo estilo Pokemon Fire Red
 * Fundo preto com bordas brancas, mostra nome do NPC e texto
 */
public class DialogBox {
  // Dimensões e posição da caixa
  private static final int BOX_WIDTH = 700;
  private static final int BOX_HEIGHT = 120;
  private static final int PADDING = 15;
  private static final int LINE_HEIGHT = 22;
  
  // Cores
  private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 230); // Preto semi-transparente
  private static final Color BORDER_COLOR = Color.WHITE;
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color NAME_BG_COLOR = new Color(40, 40, 40, 230);
  
  // Fontes
  private static final Font NAME_FONT = new Font("Arial", Font.BOLD, 16);
  private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 18);
  
  // Animação de texto (efeito de digitação)
  private String fullText;
  private String displayedText;
  private int charIndex = 0;
  private int animationTimer = 0;
  private static final int CHARS_PER_FRAME = 1; // Velocidade da animação
  private boolean textComplete = false;
  
  // Indicador de continuar
  private int arrowBlink = 0;
  private static final int ARROW_BLINK_SPEED = 30;
  
  /**
   * Define o texto a ser exibido
   */
  public void setText(String text) {
    this.fullText = text;
    this.displayedText = "";
    this.charIndex = 0;
    this.textComplete = false;
  }
  
  /**
   * Atualiza a animação do texto
   */
  public void update() {
    if (!textComplete) {
      animationTimer++;
      
      if (animationTimer >= 2) { // A cada 2 frames
        animationTimer = 0;
        
        if (charIndex < fullText.length()) {
          charIndex += CHARS_PER_FRAME;
          if (charIndex > fullText.length()) {
            charIndex = fullText.length();
          }
          displayedText = fullText.substring(0, charIndex);
        } else {
          textComplete = true;
        }
      }
    }
    
    // Animar seta de continuar
    if (textComplete) {
      arrowBlink++;
    }
  }
  
  /**
   * Pula a animação e mostra todo o texto
   */
  public void skipAnimation() {
    displayedText = fullText;
    charIndex = fullText.length();
    textComplete = true;
  }
  
  /**
   * Renderiza a caixa de diálogo
   */
  public void render(Graphics2D g, String npcName, int screenWidth, int screenHeight) {
    // Ativar anti-aliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    // Posição da caixa (parte inferior da tela)
    int boxX = (screenWidth - BOX_WIDTH) / 2;
    int boxY = screenHeight - BOX_HEIGHT - 20;
    
    // Desenhar sombra
    g.setColor(new Color(0, 0, 0, 100));
    g.fillRoundRect(boxX + 4, boxY + 4, BOX_WIDTH, BOX_HEIGHT, 10, 10);
    
    // Desenhar fundo da caixa
    g.setColor(BACKGROUND_COLOR);
    g.fillRoundRect(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 10, 10);
    
    // Desenhar borda branca
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(3));
    g.drawRoundRect(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 10, 10);
    g.setStroke(new BasicStroke(1));
    
    // Desenhar placa com nome do NPC
    int nameBoxWidth = 150;
    int nameBoxHeight = 30;
    int nameBoxX = boxX + 20;
    int nameBoxY = boxY - nameBoxHeight / 2;
    
    // Fundo da placa de nome
    g.setColor(NAME_BG_COLOR);
    g.fillRoundRect(nameBoxX, nameBoxY, nameBoxWidth, nameBoxHeight, 8, 8);
    
    // Borda da placa de nome
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(nameBoxX, nameBoxY, nameBoxWidth, nameBoxHeight, 8, 8);
    g.setStroke(new BasicStroke(1));
    
    // Nome do NPC
    g.setFont(NAME_FONT);
    g.setColor(TEXT_COLOR);
    FontMetrics nameFm = g.getFontMetrics();
    int nameX = nameBoxX + (nameBoxWidth - nameFm.stringWidth(npcName)) / 2;
    int nameY = nameBoxY + ((nameBoxHeight - nameFm.getHeight()) / 2) + nameFm.getAscent();
    g.drawString(npcName, nameX, nameY);
    
    // Desenhar texto do diálogo com quebra de linha
    g.setFont(TEXT_FONT);
    drawWrappedText(g, displayedText, boxX + PADDING, boxY + PADDING + 16, 
                    BOX_WIDTH - (PADDING * 2));
    
    // Desenhar seta de continuar (se texto completo)
    if (textComplete) {
      drawContinueArrow(g, boxX + BOX_WIDTH - 30, boxY + BOX_HEIGHT - 25);
    }
  }
  
  /**
   * Desenha texto com quebra de linha automática
   */
  private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth) {
    FontMetrics fm = g.getFontMetrics();
    
    // Renderizar o texto com anti-aliasing para melhor qualidade
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    String[] words = text.split(" ");
    StringBuilder line = new StringBuilder();
    int currentY = y;
    int lineCount = 0;
    int maxLines = 3; // Máximo de 3 linhas
    
    for (String word : words) {
      String testLine = line.length() == 0 ? word : line + " " + word;
      int lineWidth = fm.stringWidth(testLine);
      
      if (lineWidth > maxWidth && line.length() > 0) {
        // Desenhar linha atual
        if (lineCount < maxLines) {
          g.drawString(line.toString(), x, currentY);
          currentY += LINE_HEIGHT;
          lineCount++;
        }
        line = new StringBuilder(word);
      } else {
        line = new StringBuilder(testLine);
      }
    }
    
    // Desenhar última linha
    if (line.length() > 0 && lineCount < maxLines) {
      g.drawString(line.toString(), x, currentY);
    }
  }
  
  /**
   * Desenha a seta de continuar (piscando)
   */
  private void drawContinueArrow(Graphics2D g, int x, int y) {
    if ((arrowBlink / ARROW_BLINK_SPEED) % 2 == 0) {
      g.setColor(TEXT_COLOR);
      
      // Desenhar seta para baixo (triângulo)
      int[] xPoints = {x, x + 10, x + 5};
      int[] yPoints = {y, y, y + 8};
      g.fillPolygon(xPoints, yPoints, 3);
    }
  }
  
  /**
   * Verifica se a animação do texto terminou
   */
  public boolean isTextComplete() {
    return textComplete;
  }
  
  /**
   * Reseta o estado da caixa
   */
  public void reset() {
    fullText = "";
    displayedText = "";
    charIndex = 0;
    textComplete = false;
    arrowBlink = 0;
  }
}
