package com.rpggame.systems;

import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.systems.skills.*;

/**
 * Gerenciador de habilidades do jogador
 */
public class SkillManager {
  private Map<Integer, Skill> skills; // Mapa slot -> habilidade
  private Player player;

  public SkillManager(Player player) {
    this.player = player;
    this.skills = new HashMap<>();
    initializeSkills();
  }

  /**
   * Inicializa as habilidades baseadas na classe do jogador.
   */
  private void initializeSkills() {
    String playerClass = player.getPlayerClass();

    if (playerClass == null) {
      return; // Aguardar classe ser definida
    }

    switch (playerClass.toLowerCase()) {
      case "warrior":
        skills.put(1, new HorizontalSlashSkill());
        break;
      case "mage":
        skills.put(1, new FireballSkill());
        break;
      case "archer":
        skills.put(1, new PiercingArrowSkill());
        break;
      case "hunter":
        skills.put(1, new PiercingArrowSkill());
        break;
    }
  }

  /**
   * Reinicializa as habilidades (chamado quando a classe do jogador muda).
   */
  public void reinitializeSkills() {
    skills.clear();
    initializeSkills();
  }

  /**
   * Executa uma habilidade pelo número do slot.
   */
  public boolean useSkill(int slot) {
    Skill skill = skills.get(slot);
    if (skill != null) {
      System.out.println("🎯 Tentando usar habilidade " + skill.getName() + " (slot " + slot + ")");
      if (!skill.isLearned()) {
        System.out.println("❌ Habilidade '" + skill.getName() + "' ainda não foi aprendida!");
        return false;
      }
      return skill.execute(player);
    } else {
      System.out.println("❌ Nenhuma habilidade no slot " + slot + " para classe " + player.getPlayerClass());
    }
    return false;
  }

  /**
   * Atualiza todas as habilidades (cooldowns, efeitos, etc.)
   */
  public void update() {
    for (Skill skill : skills.values()) {
      if (skill != null) {
        skill.update();
      }
    }
  }

  /**
   * Renderiza efeitos visuais de todas as habilidades
   */
  public void render(Graphics2D g, Camera camera) {
    for (Skill skill : skills.values()) {
      if (skill != null) {
        skill.render(g, camera);
      }
    }
  }

  /**
   * Ensina uma habilidade para o jogador
   */
  public void learnSkill(int slot) {
    Skill skill = skills.get(slot);
    if (skill != null && !skill.isLearned()) {
      skill.setLearned(true);
      System.out.println("📚 " + player.getPlayerClass() + " aprendeu: " + skill.getName() + "!");
      System.out.println("💡 " + skill.getDescription());
    }
  }

  /**
   * Verifica se uma habilidade está aprendida
   */
  public boolean isSkillLearned(int slot) {
    Skill skill = skills.get(slot);
    return skill != null && skill.isLearned();
  }

  /**
   * Verifica se uma habilidade foi aprendida (método alternativo para
   * compatibilidade)
   */
  public boolean hasLearnedSkill(int slot) {
    return isSkillLearned(slot);
  }

  /**
   * Obtém uma habilidade por slot
   */
  public Skill getSkill(int slot) {
    return skills.get(slot);
  }

  /**
   * Obtém informações sobre todas as habilidades para exibição na UI
   */
  public String getSkillInfo(int slot) {
    Skill skill = skills.get(slot);
    if (skill == null) {
      return "Slot " + slot + ": Vazio";
    }

    if (!skill.isLearned()) {
      return "Slot " + slot + ": ??? (Não aprendida)";
    }

    String status = skill.isOnCooldown() ? " (Cooldown: " + skill.getCooldownInSeconds() + "s)" : " (Pronta)";

    return "Slot " + slot + ": " + skill.getName() + status;
  }
}