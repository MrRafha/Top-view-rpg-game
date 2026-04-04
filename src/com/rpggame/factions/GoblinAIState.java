package com.rpggame.factions;

/**
 * Estados da IA local de um goblin no novo sistema social/territorial
 */
public enum GoblinAIState {
    PATROL,           // Estado padrão no território
    OBSERVE,          // Jogador neutro entrou na área
    WARN,             // Suspeita subiu / jogador muito próximo
    ESCORT_OUT,       // Empurrando jogador para fora do território
    ATTACK,           // Standing hostil ou suspeita ultrapassou limite
    FLEE,             // Personalidade tímida
    RETURN_HOME,      // Perdeu alvo ou tensão diminuiu
    RAID_ATTACK,      // Participando de investida em território vizinho
    DEFEND_TERRITORY  // Território goblin sob ataque
}
