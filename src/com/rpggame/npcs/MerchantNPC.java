package com.rpggame.npcs;

/**
 * NPC Mercador - Vende itens e oferece informações sobre comércio
 */
public class MerchantNPC extends NPC {
  
  public MerchantNPC(double x, double y) {
    super(x, y, "Mercador", "sprites/CommonGoblin.png");
  }
  
  @Override
  protected String[] initializeDialogues() {
    return new String[] {
      "Olá, viajante! Bem-vindo à minha loja!",
      "Tenho itens raros para vender, mas estou sem estoque no momento.",
      "Volte mais tarde quando eu tiver mais mercadorias!",
      "Os goblins têm atacado minhas caravanas ultimamente..."
    };
  }
}
