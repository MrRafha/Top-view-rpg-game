package com.rpggame.systems;

import java.util.ArrayList;
import com.rpggame.npcs.NPC;

/**
 * Gerenciador de quests do jogador
 */
public class QuestManager {
  private ArrayList<Quest> allQuests;
  private ArrayList<Quest> activeQuests;

  public QuestManager() {
    this.allQuests = new ArrayList<>();
    this.activeQuests = new ArrayList<>();
  }

  /**
   * Registra uma nova quest no sistema
   */
  public void registerQuest(Quest quest) {
    allQuests.add(quest);
  }

  /**
   * Aceita uma quest
   */
  public void acceptQuest(String questId) {
    for (Quest quest : allQuests) {
      if (quest.getId().equals(questId) && quest.isAvailable()) {
        quest.accept();
        activeQuests.add(quest);
        break;
      }
    }
  }

  /**
   * Finaliza uma quest e remove das ativas
   */
  public void finishQuest(String questId) {
    for (Quest quest : activeQuests) {
      if (quest.getId().equals(questId) && quest.isCompleted()) {
        quest.finish();
        activeQuests.remove(quest);
        break;
      }
    }
  }

  /**
   * Notifica o sistema quando um inimigo é morto
   */
  public void onEnemyKilled(String enemyType) {
    for (Quest quest : activeQuests) {
      if (quest.getType() == QuestType.KILL &&
          quest.getTargetEnemyType().equalsIgnoreCase(enemyType)) {
        quest.incrementProgress();
      }
    }
  }

  /**
   * Retorna todas as quests ativas
   */
  public ArrayList<Quest> getActiveQuests() {
    return new ArrayList<>(activeQuests);
  }

  /**
   * Retorna uma quest por ID
   */
  public Quest getQuest(String questId) {
    for (Quest quest : allQuests) {
      if (quest.getId().equals(questId)) {
        return quest;
      }
    }
    return null;
  }

  /**
   * Retorna uma quest por ID (alias para getQuest)
   */
  public Quest getQuestById(String questId) {
    return getQuest(questId);
  }

  /**
   * Verifica se um NPC tem quests disponíveis
   */
  public boolean hasAvailableQuest(NPC npc) {
    for (Quest quest : allQuests) {
      if (quest.getQuestGiver() == npc && quest.isAvailable()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Verifica se um NPC tem quests completas para entregar
   */
  public boolean hasCompletedQuest(NPC npc) {
    for (Quest quest : activeQuests) {
      if (quest.getQuestGiver() == npc && quest.isCompleted()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retorna a quest disponível de um NPC
   */
  public Quest getAvailableQuestFrom(NPC npc) {
    for (Quest quest : allQuests) {
      if (quest.getQuestGiver() == npc && quest.isAvailable()) {
        return quest;
      }
    }
    return null;
  }

  /**
   * Retorna a quest completa de um NPC
   */
  public Quest getCompletedQuestFrom(NPC npc) {
    for (Quest quest : activeQuests) {
      if (quest.getQuestGiver() == npc && quest.isCompleted()) {
        return quest;
      }
    }
    return null;
  }

  /**
   * Verifica se há quests ativas
   */
  public boolean hasActiveQuests() {
    return !activeQuests.isEmpty();
  }

  /**
   * Remove uma quest do sistema (quando recusada)
   */
  public void removeQuest(String questId) {
    allQuests.removeIf(quest -> quest.getId().equals(questId));
    activeQuests.removeIf(quest -> quest.getId().equals(questId));
  }
}
