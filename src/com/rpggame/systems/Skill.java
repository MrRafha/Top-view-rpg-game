package com.rpggame.systems;

import java.awt.Graphics2D;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;

/**
 * Classe base para todas as habilidades do jogo
 */
public abstract class Skill {
  protected String name;
  protected String description;
  protected int cooldownTime; // Em frames (60 fps)
  protected int currentCooldown;
  protected boolean isLearned;
  protected String requiredClass;

  public Skill(String name, String description, int cooldownSeconds, String requiredClass) {
    this.name = name;
    this.description = description;
    this.cooldownTime = cooldownSeconds * 60; // Converter para frames
    this.currentCooldown = 0;
    this.isLearned = false;
    this.requiredClass = requiredClass;
  }

  /**
   * Executa a habilidade se estiver fora de cooldown
   */
  public boolean execute(Player player) {
    if (!isLearned) {
      System.out.println("❌ Habilidade '" + name + "' ainda não foi aprendida!");
      return false;
    }

    if (currentCooldown > 0) {
      int secondsLeft = (currentCooldown / 60) + 1;
      System.out.println("⏱️ " + name + " em cooldown! " + secondsLeft + "s restantes");
      return false;
    }

    System.out.println("🔥 " + player.getPlayerClass() + " usou: " + name + "!");
    currentCooldown = cooldownTime;
    performSkill(player);
    return true;
  }

  /**
   * Implementação específica da habilidade
   */
  protected abstract void performSkill(Player player);

  /**
   * Atualiza o cooldown da habilidade
   */
  public void update() {
    if (currentCooldown > 0) {
      currentCooldown--;
    }
  }

  /**
   * Renderiza efeitos visuais da habilidade
   */
  public abstract void render(Graphics2D g, Camera camera);

  // Getters e Setters
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isOnCooldown() {
    return currentCooldown > 0;
  }

  public int getCooldownRemaining() {
    return currentCooldown;
  }

  public boolean isLearned() {
    return isLearned;
  }

  public void setLearned(boolean learned) {
    this.isLearned = learned;
  }

  public String getRequiredClass() {
    return requiredClass;
  }

  /**
   * Verifica se o player pode usar esta habilidade
   */
  public boolean canUse(Player player) {
    return isLearned && !isOnCooldown() &&
        player.getPlayerClass().equalsIgnoreCase(requiredClass);
  }

  /**
   * Obtém o tempo de cooldown em segundos para exibição
   */
  public int getCooldownInSeconds() {
    return (currentCooldown / 60) + (currentCooldown % 60 > 0 ? 1 : 0);
  }

  /**
   * Obtém o tempo total de cooldown em frames.
   */
  public int getTotalCooldownTime() {
    return cooldownTime;
  }
}