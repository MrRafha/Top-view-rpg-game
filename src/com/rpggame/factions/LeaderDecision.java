package com.rpggame.factions;

/**
 * Decisões que o líder goblin pode tomar em relação ao jogador/situação
 */
public enum LeaderDecision {
    IGNORE,           // Ignorar completamente
    OBSERVE,          // Monitorar discretamente
    WARN,             // Avisar para sair/parar
    EXPEL,            // Expulsar ativamente do território
    ATTACK,           // Autorizar ataque
    PREPARE_RAID,     // Preparar invasão de território vizinho
    REINFORCE_BORDER  // Reforçar fronteira ameaçada
}
