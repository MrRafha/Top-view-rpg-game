package com.rpggame.npcs;

/**
 * NPC Sábio - Oferece conhecimento e dicas sobre o jogo
 */
public class WiseManNPC extends NPC {
  
  public WiseManNPC(double x, double y) {
    super(x, y, "Sábio", "sprites/AgresiveGoblin.png");
  }
  
  @Override
  protected String[] initializeDialogues() {
    return new String[] {
      "Ah, jovem aventureiro... Bem-vindo.",
      "Os goblins possuem quatro personalidades distintas:",
      "Líder, Agressivo, Comum e Tímido.",
      "Cada um se comporta de forma diferente em batalha.",
      "Use suas habilidades sabiamente, e a vitória será sua."
    };
  }
}
