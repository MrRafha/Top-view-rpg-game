package com.rpggame.npcs;

/**
 * NPC Aldeão - Pessoa comum que fornece informações gerais
 */
public class VillagerNPC extends NPC {
  
  public VillagerNPC(double x, double y) {
    super(x, y, "Aldeão", "sprites/TinyGoblin.png");
  }
  
  @Override
  protected String[] initializeDialogues() {
    return new String[] {
      "Bem-vindo à nossa vila!",
      "A vida aqui é tranquila, quando os goblins não atacam...",
      "Se precisar de ajuda, fale com o guarda ou o mercador."
    };
  }
}
