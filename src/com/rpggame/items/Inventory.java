package com.rpggame.items;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia o inventário do jogador com 20 slots fixos.
 */
public class Inventory {
  private static final int MAX_SLOTS = 20;
  private ItemStack[] slots;

  /**
   * Construtor do inventário.
   */
  public Inventory() {
    this.slots = new ItemStack[MAX_SLOTS];
  }

  /**
   * Adiciona um item ao inventário.
   * 
   * @return true se o item foi adicionado com sucesso
   */
  public boolean addItem(Item item, int quantity) {
    if (item == null || quantity <= 0) {
      return false;
    }

    int remainingQuantity = quantity;

    // Se o item é empilhável, tenta empilhar em slots existentes
    if (item.isStackable()) {
      for (int i = 0; i < MAX_SLOTS && remainingQuantity > 0; i++) {
        if (slots[i] != null && slots[i].getItem().getName().equals(item.getName())) {
          if (!slots[i].isFull()) {
            remainingQuantity = slots[i].addQuantity(remainingQuantity);
          }
        }
      }
    }

    // Se ainda sobrou quantidade, cria novos slots
    while (remainingQuantity > 0) {
      int emptySlot = findEmptySlot();
      if (emptySlot == -1) {
        return false; // Inventário cheio
      }

      int quantityToAdd = Math.min(remainingQuantity, item.getMaxStackSize());
      slots[emptySlot] = new ItemStack(item, quantityToAdd);
      remainingQuantity -= quantityToAdd;
    }

    return true;
  }

  /**
   * Remove uma quantidade específica de um item.
   */
  public boolean removeItem(String itemName, int quantity) {
    int remainingQuantity = quantity;

    for (int i = 0; i < MAX_SLOTS && remainingQuantity > 0; i++) {
      if (slots[i] != null && slots[i].getItem().getName().equals(itemName)) {
        int currentQuantity = slots[i].getQuantity();

        if (currentQuantity <= remainingQuantity) {
          remainingQuantity -= currentQuantity;
          slots[i] = null;
        } else {
          slots[i].removeQuantity(remainingQuantity);
          remainingQuantity = 0;
        }
      }
    }

    return remainingQuantity == 0;
  }

  /**
   * Remove um item de um slot específico.
   */
  public ItemStack removeItemFromSlot(int slot) {
    if (slot < 0 || slot >= MAX_SLOTS) {
      return null;
    }

    ItemStack removed = slots[slot];
    slots[slot] = null;
    return removed;
  }

  /**
   * Usa um item de um slot específico.
   */
  public boolean useItem(int slot) {
    if (slot < 0 || slot >= MAX_SLOTS || slots[slot] == null) {
      return false;
    }

    slots[slot].getItem().use();

    // Remove 1 unidade do item
    if (slots[slot].removeQuantity(1)) {
      if (slots[slot].isEmpty()) {
        slots[slot] = null;
      }
      return true;
    }

    return false;
  }

  /**
   * Verifica se o inventário tem um item específico.
   */
  public boolean hasItem(String itemName) {
    return countItem(itemName) > 0;
  }

  /**
   * Conta quantos itens de um tipo específico existem no inventário.
   */
  public int countItem(String itemName) {
    int count = 0;
    for (ItemStack stack : slots) {
      if (stack != null && stack.getItem().getName().equals(itemName)) {
        count += stack.getQuantity();
      }
    }
    return count;
  }

  /**
   * Encontra o primeiro slot vazio.
   */
  private int findEmptySlot() {
    for (int i = 0; i < MAX_SLOTS; i++) {
      if (slots[i] == null) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Verifica se o inventário está cheio.
   */
  public boolean isFull() {
    return findEmptySlot() == -1;
  }

  /**
   * Limpa o inventário.
   */
  public void clear() {
    for (int i = 0; i < MAX_SLOTS; i++) {
      slots[i] = null;
    }
  }

  /**
   * Move um item de um slot para outro.
   */
  public void swapSlots(int slot1, int slot2) {
    if (slot1 < 0 || slot1 >= MAX_SLOTS || slot2 < 0 || slot2 >= MAX_SLOTS) {
      return;
    }

    ItemStack temp = slots[slot1];
    slots[slot1] = slots[slot2];
    slots[slot2] = temp;
  }

  public ItemStack getSlot(int index) {
    if (index < 0 || index >= MAX_SLOTS) {
      return null;
    }
    return slots[index];
  }

  public int getMaxSlots() {
    return MAX_SLOTS;
  }

  public ItemStack[] getSlots() {
    return slots;
  }

  /**
   * Retorna lista de todos os itens não vazios.
   */
  public List<ItemStack> getAllItems() {
    List<ItemStack> items = new ArrayList<>();
    for (ItemStack stack : slots) {
      if (stack != null) {
        items.add(stack);
      }
    }
    return items;
  }
}
