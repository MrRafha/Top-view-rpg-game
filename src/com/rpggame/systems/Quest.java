package com.rpggame.systems;

import com.rpggame.npcs.NPC;

/**
 * Classe que representa uma quest
 */
public class Quest {
  private String id;
  private String name;
  private String description;
  private QuestType type;
  private QuestStatus status;
  private NPC questGiver;

  // Para quests de tipo KILL
  private String targetEnemyType; // Tipo de inimigo a matar (ex: "Goblin")
  private int targetAmount; // Quantidade necessária
  private int currentAmount; // Quantidade atual

  // Recompensas
  private int goldReward;
  private int expReward;

  public Quest(String id, String name, String description, QuestType type, NPC questGiver) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.questGiver = questGiver;
    this.status = QuestStatus.AVAILABLE;
    this.currentAmount = 0;
  }

  /**
   * Configura os parâmetros para uma quest de tipo KILL
   */
  public void setKillQuestParams(String targetEnemyType, int targetAmount) {
    this.targetEnemyType = targetEnemyType;
    this.targetAmount = targetAmount;
  }

  /**
   * Configura as recompensas da quest
   */
  public void setRewards(int gold, int exp) {
    this.goldReward = gold;
    this.expReward = exp;
  }

  /**
   * Incrementa o progresso da quest
   */
  public void incrementProgress() {
    if (status == QuestStatus.IN_PROGRESS) {
      currentAmount++;
      if (currentAmount >= targetAmount) {
        status = QuestStatus.COMPLETED;
        System.out.println("✅ Quest completada: " + name);
      }
    }
  }

  /**
   * Incrementa o progresso da quest por uma quantidade específica
   */
  public void incrementProgress(int amount) {
    if (status == QuestStatus.IN_PROGRESS) {
      currentAmount += amount;
      if (currentAmount >= targetAmount) {
        currentAmount = targetAmount;
        status = QuestStatus.COMPLETED;
        System.out.println("✅ Quest completada: " + name);
      }
    }
  }

  /**
   * Aceita a quest
   */
  public void accept() {
    if (status == QuestStatus.AVAILABLE) {
      status = QuestStatus.IN_PROGRESS;
      System.out.println("📜 Quest aceita: " + name);
    }
  }

  /**
   * Finaliza a quest (entrega recompensa)
   */
  public void finish() {
    if (status == QuestStatus.COMPLETED) {
      status = QuestStatus.FINISHED;
      System.out.println("🎁 Quest finalizada: " + name);
    }
  }

  /**
   * Verifica se a quest está completa
   */
  public boolean isCompleted() {
    return status == QuestStatus.COMPLETED;
  }

  /**
   * Verifica se a quest está em progresso
   */
  public boolean isInProgress() {
    return status == QuestStatus.IN_PROGRESS;
  }

  /**
   * Verifica se a quest está ativa (alias para isInProgress)
   */
  public boolean isActive() {
    return isInProgress();
  }

  /**
   * Verifica se a quest está disponível
   */
  public boolean isAvailable() {
    return status == QuestStatus.AVAILABLE;
  }

  /**
   * Verifica se a quest já foi finalizada
   */
  public boolean isFinished() {
    return status == QuestStatus.FINISHED;
  }

  /**
   * Retorna o progresso como string (ex: "5/10")
   */
  public String getProgressString() {
    if (type == QuestType.KILL) {
      return currentAmount + "/" + targetAmount;
    }
    return "";
  }

  // Getters
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public QuestType getType() {
    return type;
  }

  public QuestStatus getStatus() {
    return status;
  }

  public NPC getQuestGiver() {
    return questGiver;
  }

  public String getTargetEnemyType() {
    return targetEnemyType;
  }

  public int getTargetAmount() {
    return targetAmount;
  }

  public int getCurrentAmount() {
    return currentAmount;
  }

  public int getGoldReward() {
    return goldReward;
  }

  public int getExpReward() {
    return expReward;
  }
}
