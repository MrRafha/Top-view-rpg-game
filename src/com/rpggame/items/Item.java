package com.rpggame.items;

import java.awt.image.BufferedImage;

/**
 * Classe base para todos os itens do inventário.
 */
public abstract class Item {
  private String name;
  private String description;
  private BufferedImage sprite;
  private boolean stackable;
  private int maxStackSize;
  private ItemType type;

  /**
   * Construtor do item.
   */
  public Item(String name, String description, BufferedImage sprite, ItemType type, boolean stackable,
      int maxStackSize) {
    this.name = name;
    this.description = description;
    this.sprite = sprite;
    this.type = type;
    this.stackable = stackable;
    this.maxStackSize = stackable ? maxStackSize : 1;
  }

  /**
   * Usa o item (a ser implementado pelas subclasses).
   */
  public abstract void use();

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BufferedImage getSprite() {
    return sprite;
  }

  public boolean isStackable() {
    return stackable;
  }

  public int getMaxStackSize() {
    return maxStackSize;
  }

  public ItemType getType() {
    return type;
  }

  @Override
  public String toString() {
    return name;
  }
}
