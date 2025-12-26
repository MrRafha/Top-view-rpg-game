package com.rpggame.ui;

import com.rpggame.items.Inventory;
import com.rpggame.items.ItemStack;
import com.rpggame.items.EquippableItem;
import com.rpggame.entities.Player;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Interface da loja do mercador
 * Similar ao inventário mas para comprar itens
 */
public class ShopUI {
  private Inventory shopInventory;
  private Player player;
  private boolean visible;

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

  // Slot selecionado
  private int selectedSlot = 0;

  // Cores
  private static final Color BG_COLOR = new Color(30, 40, 30, 230);
  private static final Color SLOT_COLOR = new Color(50, 60, 50);
  private static final Color SLOT_SELECTED_COLOR = new Color(80, 140, 80);
  private static final Color BORDER_COLOR = new Color(100, 140, 100);
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color GOLD_COLOR = new Color(255, 215, 0);
  private static final Color PRICE_COLOR = new Color(150, 255, 150);
  private static final Color CANT_AFFORD_COLOR = new Color(255, 100, 100);

  public ShopUI(Inventory shopInventory, Player player) {
    this.shopInventory = shopInventory;
    this.player = player;
    this.visible = false;
  }

  public void show() {
    visible = true;
    selectedSlot = 0;
  }

  public void hide() {
    visible = false;
  }

  public boolean isVisible() {
    return visible;
  }

  public void updatePosition(int screenWidth, int screenHeight) {
    gridWidth = COLS * (SLOT_SIZE + SLOT_PADDING) + (2 * GRID_PADDING);
    gridHeight = ROWS * (SLOT_SIZE + SLOT_PADDING) + (2 * GRID_PADDING) + 100;

    gridX = (screenWidth - gridWidth) / 2;
    gridY = (screenHeight - gridHeight) / 2;
  }

  public void render(Graphics2D g) {
    if (!visible) {
      return;
    }

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Desenha fundo
    g.setColor(BG_COLOR);
    g.fillRoundRect(gridX, gridY, gridWidth, gridHeight, 20, 20);

    // Desenha borda
    g.setColor(BORDER_COLOR);
    g.setStroke(new BasicStroke(3));
    g.drawRoundRect(gridX, gridY, gridWidth, gridHeight, 20, 20);

    // Desenha título
    g.setColor(GOLD_COLOR);
    g.setFont(new Font("Arial", Font.BOLD, 24));
    String title = "LOJA DO MERCADOR";
    int titleWidth = g.getFontMetrics().stringWidth(title);
    g.drawString(title, gridX + (gridWidth - titleWidth) / 2, gridY + 40);

    // Desenha gold do player
    g.setColor(TEXT_COLOR);
    g.setFont(new Font("Arial", Font.PLAIN, 14));
    String goldText = "Seu Gold: " + player.getGold();
    g.drawString(goldText, gridX + 20, gridY + 65);

    // Desenha instruções
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    String instructions = "WASD: navegar | ENTER: comprar | ESC: fechar";
    int instructionsWidth = g.getFontMetrics().stringWidth(instructions);
    g.drawString(instructions, gridX + (gridWidth - instructionsWidth) / 2, gridY + 80);

    // Desenha slots
    int startY = gridY + 100;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        int slotIndex = row * COLS + col;
        int slotX = gridX + GRID_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
        int slotY = startY + GRID_PADDING + row * (SLOT_SIZE + SLOT_PADDING);

        drawSlot(g, slotIndex, slotX, slotY);
      }
    }

    // Desenha seta no slot selecionado
    drawSelectionArrow(g);

    // Desenha descrição do item selecionado
    if (selectedSlot >= 0 && selectedSlot < shopInventory.getMaxSlots()) {
      ItemStack stack = shopInventory.getSlot(selectedSlot);
      if (stack != null) {
        drawItemDescription(g, stack);
      }
    }
  }

  private void drawSlot(Graphics2D g, int slotIndex, int x, int y) {
    // Cor do slot
    if (slotIndex == selectedSlot) {
      g.setColor(SLOT_SELECTED_COLOR);
    } else {
      g.setColor(SLOT_COLOR);
    }

    // Desenha fundo
    g.fillRoundRect(x, y, SLOT_SIZE, SLOT_SIZE, 8, 8);

    // Desenha borda
    if (slotIndex == selectedSlot) {
      g.setColor(GOLD_COLOR);
      g.setStroke(new BasicStroke(3));
    } else {
      g.setColor(BORDER_COLOR);
      g.setStroke(new BasicStroke(1));
    }
    g.drawRoundRect(x, y, SLOT_SIZE, SLOT_SIZE, 8, 8);

    // Desenha item se existir
    ItemStack stack = shopInventory.getSlot(slotIndex);
    if (stack != null) {
      // Desenha nome do item ao invés do sprite
      String itemName = stack.getItem().getName();
      g.setColor(TEXT_COLOR);
      g.setFont(new Font("Arial", Font.BOLD, 12));

      // Calcular posição centralizada do texto
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(itemName);
      int textX = x + (SLOT_SIZE - textWidth) / 2;
      int textY = y + SLOT_SIZE / 2 - 10;

      g.drawString(itemName, textX, textY);

      // Desenha preço
      if (stack.getItem() instanceof EquippableItem) {
        EquippableItem equippable = (EquippableItem) stack.getItem();
        int price = equippable.getGoldCost();
        boolean canAfford = player.getGold() >= price;

        g.setColor(canAfford ? PRICE_COLOR : CANT_AFFORD_COLOR);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String priceText = price + "G";
        textWidth = g.getFontMetrics().stringWidth(priceText);
        g.drawString(priceText, x + (SLOT_SIZE - textWidth) / 2, y + SLOT_SIZE - 4);
      }
    }
  }

  private void drawSelectionArrow(Graphics2D g) {
    if (selectedSlot < 0 || selectedSlot >= shopInventory.getMaxSlots()) {
      return;
    }

    int row = selectedSlot / COLS;
    int col = selectedSlot % COLS;
    int startY = gridY + 100;
    int slotX = gridX + GRID_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
    int slotY = startY + GRID_PADDING + row * (SLOT_SIZE + SLOT_PADDING);

    int arrowX = slotX - 20;
    int arrowY = slotY + SLOT_SIZE / 2;

    g.setColor(GOLD_COLOR);
    int[] xPoints = { arrowX, arrowX + 10, arrowX + 10 };
    int[] yPoints = { arrowY, arrowY - 8, arrowY + 8 };
    g.fillPolygon(xPoints, yPoints, 3);
  }

  private void drawItemDescription(Graphics2D g, ItemStack stack) {
    String itemName = stack.getItem().getName();
    String itemDesc = stack.getItem().getDescription();

    // Calcular largura do balão
    g.setFont(new Font("Arial", Font.BOLD, 14));
    int nameWidth = g.getFontMetrics().stringWidth(itemName);

    String[] descLines = itemDesc.split("\\n");
    int maxDescWidth = 0;
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    for (String line : descLines) {
      int lineWidth = g.getFontMetrics().stringWidth(line);
      if (lineWidth > maxDescWidth) {
        maxDescWidth = lineWidth;
      }
    }

    int balloonWidth = Math.max(nameWidth, maxDescWidth) + 30;
    int balloonHeight = 40 + (descLines.length * 16);

    // Verificar se pode equipar
    boolean canEquip = false;
    String reqText = "";
    if (stack.getItem() instanceof EquippableItem) {
      EquippableItem equippable = (EquippableItem) stack.getItem();
      canEquip = equippable.canEquip(player);
      if (!canEquip) {
        reqText = "Falta: " + equippable.getMissingRequirements(player);
        balloonHeight += 20;
      }
    }

    // Posicionar à direita da grid
    int balloonX = gridX + gridWidth + 20;
    int balloonY = gridY + 100;

    // Fundo
    g.setColor(new Color(20, 20, 30, 240));
    g.fillRoundRect(balloonX, balloonY, balloonWidth, balloonHeight, 10, 10);

    // Borda
    g.setColor(canEquip ? PRICE_COLOR : CANT_AFFORD_COLOR);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(balloonX, balloonY, balloonWidth, balloonHeight, 10, 10);

    // Nome
    g.setColor(TEXT_COLOR);
    g.setFont(new Font("Arial", Font.BOLD, 14));
    g.drawString(itemName, balloonX + 15, balloonY + 20);

    // Descrição
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    int textY = balloonY + 40;
    for (String line : descLines) {
      g.drawString(line, balloonX + 15, textY);
      textY += 16;
    }

    // Requisitos não atendidos
    if (!canEquip && !reqText.isEmpty()) {
      g.setColor(CANT_AFFORD_COLOR);
      g.drawString(reqText, balloonX + 15, textY);
    }
  }

  public void keyPressed(KeyEvent e) {
    if (!visible) {
      return;
    }

    int keyCode = e.getKeyCode();

    // Navegação WASD
    if (keyCode == KeyEvent.VK_W) {
      moveSelection(0, -1);
    } else if (keyCode == KeyEvent.VK_S) {
      moveSelection(0, 1);
    } else if (keyCode == KeyEvent.VK_A) {
      moveSelection(-1, 0);
    } else if (keyCode == KeyEvent.VK_D) {
      moveSelection(1, 0);
    }
    // Enter para comprar
    else if (keyCode == KeyEvent.VK_ENTER) {
      buyItem();
    }
  }

  private void moveSelection(int dx, int dy) {
    int col = selectedSlot % COLS;
    int row = selectedSlot / COLS;

    col += dx;
    row += dy;

    // Wrap around
    if (col < 0)
      col = COLS - 1;
    if (col >= COLS)
      col = 0;
    if (row < 0)
      row = ROWS - 1;
    if (row >= ROWS)
      row = 0;

    selectedSlot = row * COLS + col;
  }

  private void buyItem() {
    ItemStack stack = shopInventory.getSlot(selectedSlot);
    if (stack == null) {
      return;
    }

    if (stack.getItem() instanceof EquippableItem) {
      EquippableItem item = (EquippableItem) stack.getItem();
      int price = item.getGoldCost();

      // Verificar se tem gold suficiente
      if (player.getGold() < price) {
        System.out.println("❌ Gold insuficiente! Preço: " + price);
        return;
      }

      // Comprar item
      player.addGold(-price);
      player.getInventory().addItem(item, 1);

      // Remove do inventário da loja usando o nome do item
      shopInventory.removeItem(item.getName(), 1);

      System.out.println("✅ Comprou " + item.getName() + " por " + price + " gold!");
    }
  }
}
