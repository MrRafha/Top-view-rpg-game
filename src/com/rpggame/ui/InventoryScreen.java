package com.rpggame.ui;

import com.rpggame.items.Inventory;
import com.rpggame.items.ItemStack;
import com.rpggame.items.EquippableItem;
import com.rpggame.entities.Player;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * Interface visual do inventário do jogador.
 * Grid 5x4 retangular com 20 slots fixos, navegação por seta.
 */
public class InventoryScreen extends JPanel implements KeyListener {
  private Inventory inventory;
  private Player player;
  private boolean isVisible;

  // Layout do grid (retangular 5x4)
  private static final int COLS = 5;
  private static final int ROWS = 4;
  private static final int SLOT_SIZE = 64;
  private static final int SLOT_PADDING = 8;
  private static final int GRID_PADDING = 20;

  // Posicionamento
  private int gridX;
  private int gridY;
  private int gridWidth;
  private int gridHeight;

  // Slot selecionado com seta
  private int selectedSlot = 0;
  private int hoveredSlot = -1;

  // Cores
  private static final Color BG_COLOR = new Color(30, 30, 40, 230);
  private static final Color SLOT_COLOR = new Color(50, 50, 60);
  private static final Color SLOT_HOVER_COLOR = new Color(70, 70, 90);
  private static final Color SLOT_SELECTED_COLOR = new Color(80, 120, 180);
  private static final Color BORDER_COLOR = new Color(100, 100, 120);
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color QUANTITY_COLOR = new Color(255, 255, 150);

  /**
   * Construtor do InventoryScreen.
   */
  public InventoryScreen(Inventory inventory, Player player) {
    this.inventory = inventory;
    this.player = player;
    this.isVisible = false;

    setOpaque(false);
    setFocusable(true);

    // Adiciona listener de mouse para interação com slots
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        handleMouseClick(e);
      }
    });

    addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        handleMouseMove(e);
      }
    });
  }

  /**
   * Atualiza o layout do inventário.
   */
  public void updateLayout(int screenWidth, int screenHeight) {
    gridWidth = COLS * (SLOT_SIZE + SLOT_PADDING) + (2 * GRID_PADDING);
    gridHeight = ROWS * (SLOT_SIZE + SLOT_PADDING) + (2 * GRID_PADDING) + 80; // +80 para título

    gridX = (screenWidth - gridWidth) / 2 - 80; // Deslocado 80px para a esquerda
    gridY = (screenHeight - gridHeight) / 2;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (!isVisible) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Desenha fundo
    g2d.setColor(BG_COLOR);
    g2d.fillRoundRect(gridX, gridY, gridWidth, gridHeight, 20, 20);

    // Desenha borda
    g2d.setColor(BORDER_COLOR);
    g2d.drawRoundRect(gridX, gridY, gridWidth, gridHeight, 20, 20);

    // Desenha título
    g2d.setColor(TEXT_COLOR);
    g2d.setFont(new Font("Arial", Font.BOLD, 24));
    String title = "INVENTÁRIO";
    int titleWidth = g2d.getFontMetrics().stringWidth(title);
    g2d.drawString(title, gridX + (gridWidth - titleWidth) / 2, gridY + 40);

    // Desenha instruções
    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
    String instructions = "WASD: navegar | ENTER: usar item | I: fechar";
    int instructionsWidth = g2d.getFontMetrics().stringWidth(instructions);
    g2d.drawString(instructions, gridX + (gridWidth - instructionsWidth) / 2, gridY + 60);

    // Desenha slots
    int startY = gridY + 80;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        int slotIndex = row * COLS + col;
        int slotX = gridX + GRID_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
        int slotY = startY + GRID_PADDING + row * (SLOT_SIZE + SLOT_PADDING);

        drawSlot(g2d, slotIndex, slotX, slotY);
      }
    }

    // Desenha seta no slot selecionado
    drawSelectionArrow(g2d);

    // Desenha balão de descrição do item selecionado
    if (selectedSlot >= 0 && selectedSlot < inventory.getMaxSlots()) {
      ItemStack stack = inventory.getSlot(selectedSlot);
      if (stack != null) {
        drawItemDescriptionBalloon(g2d, stack);
      }
    }
  }

  /**
   * Desenha a seta indicadora no slot selecionado.
   */
  private void drawSelectionArrow(Graphics2D g2d) {
    if (selectedSlot < 0 || selectedSlot >= inventory.getMaxSlots()) {
      return;
    }

    int row = selectedSlot / COLS;
    int col = selectedSlot % COLS;
    int startY = gridY + 80;
    int slotX = gridX + GRID_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
    int slotY = startY + GRID_PADDING + row * (SLOT_SIZE + SLOT_PADDING);

    // Desenha seta à esquerda do slot
    int arrowX = slotX - 20;
    int arrowY = slotY + SLOT_SIZE / 2;

    g2d.setColor(new Color(255, 215, 0)); // Dourado
    int[] xPoints = { arrowX, arrowX + 10, arrowX + 10 };
    int[] yPoints = { arrowY, arrowY - 8, arrowY + 8 };
    g2d.fillPolygon(xPoints, yPoints, 3);
  }

  /**
   * Desenha um slot individual.
   */
  private void drawSlot(Graphics2D g2d, int slotIndex, int x, int y) {
    // Cor do slot baseada no estado
    if (slotIndex == selectedSlot) {
      g2d.setColor(SLOT_SELECTED_COLOR);
    } else {
      g2d.setColor(SLOT_COLOR);
    }

    // Desenha fundo do slot
    g2d.fillRoundRect(x, y, SLOT_SIZE, SLOT_SIZE, 8, 8);

    // Desenha borda do slot
    if (slotIndex == selectedSlot) {
      g2d.setColor(new Color(255, 215, 0)); // Borda dourada no selecionado
      g2d.setStroke(new java.awt.BasicStroke(3));
    } else {
      g2d.setColor(BORDER_COLOR);
      g2d.setStroke(new java.awt.BasicStroke(1));
    }
    g2d.drawRoundRect(x, y, SLOT_SIZE, SLOT_SIZE, 8, 8);

    // Desenha item se existir
    ItemStack stack = inventory.getSlot(slotIndex);
    if (stack != null) {
      // Desenha sprite do item
      if (stack.getItem().getSprite() != null) {
        int spriteSize = 48;
        int spriteX = x + (SLOT_SIZE - spriteSize) / 2;
        int spriteY = y + (SLOT_SIZE - spriteSize) / 2;
        g2d.drawImage(stack.getItem().getSprite(), spriteX, spriteY, spriteSize, spriteSize, null);
      }

      // Desenha quantidade se empilhável
      if (stack.getItem().isStackable() && stack.getQuantity() > 1) {
        g2d.setColor(QUANTITY_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String quantityText = "x" + stack.getQuantity();
        int textWidth = g2d.getFontMetrics().stringWidth(quantityText);
        g2d.drawString(quantityText, x + SLOT_SIZE - textWidth - 4, y + SLOT_SIZE - 4);
      }

      // Desenha indicador "E" verde se item estiver equipado
      if (player != null && stack.getItem() instanceof EquippableItem) {
        EquippableItem equippable = (EquippableItem) stack.getItem();
        if (player.getEquippedWeapon() == equippable) {
          // Fundo verde semi-transparente
          g2d.setColor(new Color(0, 255, 0, 100));
          g2d.fillRoundRect(x + 2, y + 2, 18, 18, 4, 4);

          // Letra E verde
          g2d.setColor(new Color(0, 255, 0));
          g2d.setFont(new Font("Arial", Font.BOLD, 16));
          g2d.drawString("E", x + 6, y + 16);
        }
      }
    }
  }

  /**
   * Desenha balão de descrição ao lado da seta.
   */
  private void drawItemDescriptionBalloon(Graphics2D g2d, ItemStack stack) {
    int row = selectedSlot / COLS;
    int col = selectedSlot % COLS;
    int startY = gridY + 80;
    int slotX = gridX + GRID_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
    int slotY = startY + GRID_PADDING + row * (SLOT_SIZE + SLOT_PADDING);

    String itemName = stack.getItem().getName();
    String itemDesc = stack.getItem().getDescription();

    g2d.setFont(new Font("Arial", Font.BOLD, 14));
    int nameWidth = g2d.getFontMetrics().stringWidth(itemName);

    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
    int descWidth = g2d.getFontMetrics().stringWidth(itemDesc);

    int balloonWidth = Math.max(nameWidth, descWidth) + 20;
    int balloonHeight = 60;

    // Posicionar à esquerda da seta
    int balloonX = slotX - balloonWidth - 30;
    int balloonY = slotY;

    // Se não couber à esquerda, coloca à direita
    if (balloonX < 10) {
      balloonX = gridX + gridWidth + 20;
    }

    // Fundo do balão
    g2d.setColor(new Color(20, 20, 30, 240));
    g2d.fillRoundRect(balloonX, balloonY, balloonWidth, balloonHeight, 10, 10);

    // Borda do balão
    g2d.setColor(new Color(255, 215, 0));
    g2d.setStroke(new java.awt.BasicStroke(2));
    g2d.drawRoundRect(balloonX, balloonY, balloonWidth, balloonHeight, 10, 10);

    // Nome do item
    g2d.setColor(new Color(255, 215, 0));
    g2d.setFont(new Font("Arial", Font.BOLD, 14));
    g2d.drawString(itemName, balloonX + 10, balloonY + 25);

    // Descrição
    g2d.setColor(TEXT_COLOR);
    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
    g2d.drawString(itemDesc, balloonX + 10, balloonY + 45);
  }

  /**
   * Trata clique do mouse (desabilitado - usar WASD + Enter).
   */
  private void handleMouseClick(MouseEvent e) {
    // Sistema de navegação por teclado - mouse desabilitado
  }

  /**
   * Trata movimento do mouse (desabilitado).
   */
  private void handleMouseMove(MouseEvent e) {
    // Sistema de navegação por teclado - mouse desabilitado
  }

  /**
   * Move a seleção com WASD.
   */
  private void moveSelection(int deltaX, int deltaY) {
    int currentRow = selectedSlot / COLS;
    int currentCol = selectedSlot % COLS;

    int newRow = currentRow + deltaY;
    int newCol = currentCol + deltaX;

    // Limitar aos bounds
    if (newRow < 0)
      newRow = 0;
    if (newRow >= ROWS)
      newRow = ROWS - 1;
    if (newCol < 0)
      newCol = 0;
    if (newCol >= COLS)
      newCol = COLS - 1;

    int newSlot = newRow * COLS + newCol;
    if (newSlot >= 0 && newSlot < inventory.getMaxSlots()) {
      selectedSlot = newSlot;
      repaint();
    }
  }

  public void toggleVisibility() {
    isVisible = !isVisible;
    if (isVisible) {
      selectedSlot = 0; // Começa no primeiro slot
    }
    repaint();
  }

  public void setVisible(boolean visible) {
    this.isVisible = visible;
    if (visible) {
      selectedSlot = 0; // Começa no primeiro slot
    }
    repaint();
  }

  public boolean isInventoryVisible() {
    return isVisible;
  }

  /**
   * Método público para renderizar o inventário.
   */
  public void render(Graphics g) {
    paintComponent(g);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (!isVisible) {
      return;
    }

    int keyCode = e.getKeyCode();

    // Navegação WASD
    if (keyCode == KeyEvent.VK_W) {
      moveSelection(0, -1); // Cima
    } else if (keyCode == KeyEvent.VK_S) {
      moveSelection(0, 1); // Baixo
    } else if (keyCode == KeyEvent.VK_A) {
      moveSelection(-1, 0); // Esquerda
    } else if (keyCode == KeyEvent.VK_D) {
      moveSelection(1, 0); // Direita
    }
    // Enter para usar item ou equipar/desequipar
    else if (keyCode == KeyEvent.VK_ENTER) {
      ItemStack stack = inventory.getSlot(selectedSlot);
      if (stack != null && stack.getItem() instanceof EquippableItem && player != null) {
        EquippableItem equippable = (EquippableItem) stack.getItem();

        // Verificar se já está equipado
        if (player.getEquippedWeapon() == equippable) {
          // Desequipar
          player.unequipWeapon();
        } else {
          // Equipar
          player.equipWeapon(equippable);
        }
        repaint();
      } else if (inventory.useItem(selectedSlot)) {
        // Usar item consumível normalmente
        System.out.println("Item usado do slot " + selectedSlot);
        repaint();
      }
    }
    // I ou ESC para fechar
    else if (keyCode == KeyEvent.VK_I || keyCode == KeyEvent.VK_ESCAPE) {
      toggleVisibility();
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // Não usado
  }

  @Override
  public void keyTyped(KeyEvent e) {
    // Não usado
  }
}
