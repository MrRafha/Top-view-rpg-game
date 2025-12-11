package com.rpggame.npcs;

import com.rpggame.entities.Player;

/**
 * NPC Sábio - Oferece conhecimento e dicas sobre o jogo, ensina habilidades
 */
public class WiseManNPC extends NPC {

  private boolean hasLearnedSkill = false;
  private Player lastPlayerInteracted = null;

  public WiseManNPC(double x, double y) {
    super(x, y, "Sábio", "sprites/Sabio.png");
  }

  @Override
  protected String[] initializeDialogues() {
    return new String[] {
        "Ah, jovem aventureiro... Bem-vindo.",
        "Vejo que você ainda não domina suas habilidades.",
        "Cada classe possui uma habilidade única especial:",
        "Guerreiros podem executar um ataque em área.",
        "Magos podem conjurar bolas de fogo explosivas.",
        "Arqueiros e Caçadores podem disparar flechas perfurantes.",
        "Gostaria de aprender sua primeira habilidade?",
        "Use as teclas 1, 2, 3 e 4 para ativar suas habilidades.",
        "Lembre-se: cada habilidade tem um tempo de recarga.",
        "Use suas habilidades sabiamente contra os goblins!"
    };
  }

  @Override
  public boolean nextDialog() {
    // Se chegou ao diálogo de aprender habilidade (índice 6)
    if (currentDialogIndex == 6 && lastPlayerInteracted != null && !hasLearnedSkill) {
      // Ensinar a primeira habilidade baseada na classe
      if (lastPlayerInteracted.getSkillManager() != null) {
        lastPlayerInteracted.getSkillManager().learnSkill(1);
        hasLearnedSkill = true;

        // Modificar o diálogo para confirmação
        String className = lastPlayerInteracted.getPlayerClass();
        String skillName = getSkillNameByClass(className);
        dialogLines[6] = "Excelente! Você aprendeu: " + skillName + "!";
      }
    }

    currentDialogIndex++;
    if (currentDialogIndex >= dialogLines.length) {
      currentDialogIndex = 0;
      return false;
    }
    return true;
  }

  @Override
  public void update(Player player) {
    super.update(player);
    lastPlayerInteracted = player;
  }

  @Override
  public void resetDialog() {
    super.resetDialog();

    // Reset para permitir nova interação se necessário
    if (lastPlayerInteracted != null && lastPlayerInteracted.getSkillManager() != null) {
      hasLearnedSkill = lastPlayerInteracted.getSkillManager().hasLearnedSkill(1);

      // Modificar diálogos baseado no status de aprendizado
      if (hasLearnedSkill) {
        dialogLines[1] = "Vejo que você já domina suas habilidades básicas.";
        dialogLines[6] = "Continue praticando suas habilidades, jovem " +
            lastPlayerInteracted.getPlayerClass() + "!";
      } else {
        dialogLines[1] = "Vejo que você ainda não domina suas habilidades.";
        dialogLines[6] = "Gostaria de aprender sua primeira habilidade?";
      }
    }
  }

  private String getSkillNameByClass(String className) {
    switch (className.toLowerCase()) {
      case "warrior":
        return "Golpe Horizontal";
      case "mage":
        return "Bola de Fogo";
      case "archer":
        return "Flecha Perfurante";
      case "hunter":
        return "Flecha Perfurante";
      default:
        return "Habilidade Especial";
    }
  }
}
