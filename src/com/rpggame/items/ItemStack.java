package com.rpggame.items;

/**
 * Representa uma pilha de itens no inventário.
 */
public class ItemStack {
  private Item item;
  private int quantity;

  /**
   * Construtor da pilha de itens.
   */
  public ItemStack(Item item, int quantity) {
    this.item = item;
    this.quantity = Math.min(quantity, item.getMaxStackSize());
  }

  /**
   * Adiciona quantidade à pilha.
   */
  public int addQuantity(int amount) {
    int maxAdd = item.getMaxStackSize() - quantity;
    int actualAdd = Math.min(amount, maxAdd);
    quantity += actualAdd;
    return amount - actualAdd; // Retorna o resto que não coube
  }

  /**
   * Remove quantidade da pilha.
   */
  public boolean removeQuantity(int amount) {
    if (amount > quantity) {
      return false;
    }
    quantity -= amount;
    return true;
  }

  public Item getItem() {
    return item;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = Math.min(quantity, item.getMaxStackSize());
  }

  public boolean isEmpty() {
    return quantity <= 0;
  }

  public boolean isFull() {
    return quantity >= item.getMaxStackSize();
  }

  @Override
  public String toString() {
    return item.getName() + " x" + quantity;
  }
}
