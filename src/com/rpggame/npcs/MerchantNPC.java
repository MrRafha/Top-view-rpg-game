package com.rpggame.npcs;

import com.rpggame.entities.Player;
import com.rpggame.systems.Quest;
import com.rpggame.systems.QuestType;
import com.rpggame.items.*;

/**
 * NPC Mercador
 */
public class MerchantNPC extends NPC {
  private Quest goblinQuest;
  private boolean questGiven = false;
  private boolean questOffered = false; // Para controlar se a quest já foi oferecida
  private boolean shopUnlocked = false; // Loja desbloqueada após completar quest
  private Inventory shopInventory; // Inventário da loja

  public MerchantNPC(double x, double y) {
    super(x, y, "Mercador", "sprites/CommonGoblin.png");
    initializeShop();
  }

  /**
   * Inicializa o inventário da loja com os itens à venda
   */
  private void initializeShop() {
    shopInventory = new Inventory();
    // Adicionar 1 de cada arma ao estoque
    shopInventory.addItem(new OldSword(), 1);
    shopInventory.addItem(new OldStaff(), 1);
    shopInventory.addItem(new OldBow(), 1);
  }

  /**
   * Retorna o inventário da loja
   */
  public Inventory getShopInventory() {
    return shopInventory;
  }

  /**
   * Retorna se a loja está desbloqueada
   */
  public boolean isShopUnlocked() {
    return shopUnlocked;
  }

  @Override
  protected String[] initializeDialogues() {
    return new String[] {
        "Olá, viajante! Bem-vindo à minha loja!",
        "Tenho itens raros para vender, mas estou sem estoque no momento.",
        "Os goblins têm atacado minhas caravanas ultimamente...",
        "Volte mais tarde quando eu tiver mais mercadorias!"
    };
  }

  /**
   * Atualiza o estado do MerchantNPC, incluindo indicadores de quest
   */
  @Override
  public void update(Player player) {
    // Chamar o update da classe pai (verifica proximidade)
    super.update(player);

    if (player == null) {
      return;
    }

    // Calcular distância do player
    double distance = Math.sqrt(
        Math.pow(player.getX() - x, 2) +
            Math.pow(player.getY() - y, 2));

    // Só mostrar indicadores se o player estiver próximo
    boolean isPlayerNearby = distance <= INTERACTION_RANGE;

    if (!isPlayerNearby) {
      // Player longe - esconder todos os indicadores
      setHasQuestAvailable(false);
      setHasQuestCompleted(false);
      return;
    }

    // Player está próximo - verificar status da quest
    Quest quest = player.getQuestManager().getQuestById("merchant_goblin_hunt");

    if (quest != null) {
      // Quest existe - verificar status
      if (quest.isCompleted() && !quest.isFinished()) {
        // Quest completada mas não finalizada - mostrar indicador dourado
        setHasQuestCompleted(true);
        setHasQuestAvailable(false);
      } else if (quest.isAvailable() && !questGiven) {
        // Quest disponível mas não aceita - mostrar indicador amarelo
        setHasQuestAvailable(true);
        setHasQuestCompleted(false);
      } else if (quest.isActive()) {
        // Quest está ativa (em progresso) - sem indicador
        setHasQuestAvailable(false);
        setHasQuestCompleted(false);
      }
    } else {
      // Quest não existe - verificar se deve criar
      if (!questGiven && !questOffered) {
        // Tem quest para oferecer - mostrar indicador amarelo
        setHasQuestAvailable(true);
        setHasQuestCompleted(false);
      } else {
        // Sem quest
        setHasQuestAvailable(false);
        setHasQuestCompleted(false);
      }
    }
  }

  /**
   * Cria a quest de matar goblins
   */
  public void createGoblinQuest(Player player) {
    if (goblinQuest == null && !questGiven) {
      goblinQuest = new Quest(
          "merchant_goblin_hunt",
          "Caça aos Goblins",
          "O mercador precisa de ajuda para proteger suas caravanas. Mate 10 goblins e volte aqui para receber sua recompensa.",
          QuestType.KILL,
          this);

      // Configurar parâmetros da quest
      goblinQuest.setKillQuestParams("Goblin", 10);
      goblinQuest.setRewards(10, 50);

      // Registrar a quest no QuestManager do player
      player.getQuestManager().registerQuest(goblinQuest);

      // Marcar que temos quest disponível
      setHasQuestAvailable(true);

      System.out.println("✅ Quest 'Caça aos Goblins' criada pelo Mercador");
    }
  }

  /**
   * Retorna se a quest foi oferecida ao player
   */
  public boolean isQuestOffered() {
    return questOffered;
  }

  /**
   * Marca que a quest foi oferecida ao player
   */
  public void setQuestOffered(boolean offered) {
    this.questOffered = offered;
  }

  /**
   * Retorna se o player já aceitou a quest
   */
  public boolean isQuestGiven() {
    return questGiven;
  }

  /**
   * Retorna o diálogo de oferecimento da quest
   */
  public String[] getQuestOfferDialogues() {
    return new String[] {
        "Os goblins têm atacado minhas caravanas!",
        "Preciso de alguém corajoso para eliminar 10 deles.",
        "Pagarei 10 moedas de ouro e darei boa experiência.",
        "Você aceita esta missão? (S/N)"
    };
  }

  /**
   * Recusa a quest
   */
  public void declineQuest(Player player) {
    // Remover a quest do QuestManager
    player.getQuestManager().removeQuest("merchant_goblin_hunt");
    goblinQuest = null;
    setHasQuestAvailable(false);
    questOffered = false;

    // Voltar aos diálogos normais
    updateDialogues(new String[] {
        "Tudo bem, entendo sua decisão.",
        "Se mudar de ideia, estarei aqui."
    });

    System.out.println("❌ Player recusou a quest 'Caça aos Goblins'");
  }

  /**
   * Aceita a quest quando o player interage
   */
  public void acceptQuest(Player player) {
    if (goblinQuest != null && !questGiven) {
      player.getQuestManager().acceptQuest("merchant_goblin_hunt");
      questGiven = true;
      setHasQuestAvailable(false);

      // Atualizar diálogos para refletir a quest ativa
      updateDialogues(new String[] {
          "Ótimo! Mate 10 goblins e volte aqui.",
          "Os goblins estão ao norte, nos territórios selvagens.",
          "Boa sorte, aventureiro!"
      });

      System.out.println("📜 Player aceitou a quest 'Caça aos Goblins'");
    }
  }

  /**
   * Completa a quest e dá a recompensa
   */
  public boolean completeQuest(Player player) {
    Quest quest = player.getQuestManager().getQuestById("merchant_goblin_hunt");

    if (quest != null && quest.isCompleted() && !quest.isFinished()) {
      // Dar recompensa
      player.addGold(quest.getGoldReward());
      player.gainExperience(quest.getExpReward());

      // Finalizar a quest
      player.getQuestManager().finishQuest("merchant_goblin_hunt");

      // Atualizar diálogos
      updateDialogues(new String[] {
          "Excelente trabalho! Aqui está sua recompensa.",
          "Com os goblins afastados, posso reabrir minhas rotas de comércio.",
          "Agora tenho armas disponíveis na minha loja!",
          "Pressione L para abrir a loja e ver o que tenho para vender."
      });

      // Desbloquear loja
      shopUnlocked = true;

      // Marcar indicador de quest completa
      setHasQuestCompleted(false);

      System.out.println("✅ Quest 'Caça aos Goblins' completada!");
      System.out.println("🏪 Loja do Mercador desbloqueada! Pressione L para abrir.");
      return true;
    }

    return false;
  }

  /**
   * Verifica o status da quest quando o player interage
   */
  public void checkQuestStatus(Player player) {
    Quest quest = player.getQuestManager().getQuestById("merchant_goblin_hunt");

    if (quest != null && quest.isActive()) {
      // Atualizar indicador visual se a quest está completa
      if (quest.isCompleted()) {
        setHasQuestCompleted(true);
      }
    }
  }

  /**
   * Atualiza os diálogos do NPC
   */
  public void updateDialogues(String[] newDialogues) {
    this.dialogLines = newDialogues;
    this.currentDialogIndex = 0;
  }
}
