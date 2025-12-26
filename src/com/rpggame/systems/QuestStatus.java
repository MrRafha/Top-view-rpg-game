package com.rpggame.systems;

/**
 * Status de uma quest
 */
public enum QuestStatus {
  AVAILABLE,    // Quest disponível mas não aceita ainda
  IN_PROGRESS,  // Quest aceita e em progresso
  COMPLETED,    // Quest completa mas não entregue
  FINISHED      // Quest entregue e recompensa recebida
}
