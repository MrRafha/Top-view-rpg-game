package com.rpggame.npcs;

/**
 * NPC Guarda Real - Protege a área e oferece informações sobre segurança
 */
public class GuardNPC extends NPC {
  
  public GuardNPC(double x, double y) {
    super(x, y, "Guarda Real", "sprites/goblinLeader.png");
  }
  
  @Override
  protected String[] initializeDialogues() {
    return new String[] {
      "Alto lá! Esta área está sob minha proteção.",
      "Cuidado com os goblins que vagam por estas terras.",
      "Eles ficaram mais organizados recentemente...",
      "Ouvi dizer que formaram famílias e até realizam reuniões!",
      "Mantenha-se vigilante, guerreiro."
    };
  }
}
