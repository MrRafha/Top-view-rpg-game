package com.rpggame.items;

import java.awt.image.BufferedImage;
import com.rpggame.entities.Player;

/**
 * Classe base para itens equipáveis (armas, armaduras, etc)
 */
public abstract class EquippableItem extends Item {
  protected int damageBonus;
  protected int requiredStrength;
  protected int requiredDexterity;
  protected int requiredIntelligence;
  protected int goldCost;

  public EquippableItem(String name, String description, BufferedImage sprite,
      int damageBonus, int requiredStrength, int requiredDexterity, int requiredIntelligence, int goldCost) {
    super(name, description, sprite, ItemType.WEAPON, false, 1);
    this.damageBonus = damageBonus;
    this.requiredStrength = requiredStrength;
    this.requiredDexterity = requiredDexterity;
    this.requiredIntelligence = requiredIntelligence;
    this.goldCost = goldCost;
  }

  @Override
  public void use() {
    // Equipar/desequipar será feito através do inventário
  }

  /**
   * Verifica se o player pode equipar este item
   */
  public boolean canEquip(Player player) {
    return player.getStats().getStrength() >= requiredStrength &&
        player.getStats().getDexterity() >= requiredDexterity &&
        player.getStats().getIntelligence() >= requiredIntelligence;
  }

  /**
   * Retorna string com requisitos não atendidos
   */
  public String getMissingRequirements(Player player) {
    StringBuilder missing = new StringBuilder();
    if (player.getStats().getStrength() < requiredStrength) {
      missing.append("Força: ").append(requiredStrength).append(" ");
    }
    if (player.getStats().getDexterity() < requiredDexterity) {
      missing.append("Destreza: ").append(requiredDexterity).append(" ");
    }
    if (player.getStats().getIntelligence() < requiredIntelligence) {
      missing.append("Inteligência: ").append(requiredIntelligence);
    }
    return missing.toString().trim();
  }

  public int getDamageBonus() {
    return damageBonus;
  }

  public int getGoldCost() {
    return goldCost;
  }

  public int getRequiredStrength() {
    return requiredStrength;
  }

  public int getRequiredDexterity() {
    return requiredDexterity;
  }

  public int getRequiredIntelligence() {
    return requiredIntelligence;
  }
}
