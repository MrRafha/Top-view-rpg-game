package com.rpggame.systems;

/**
 * Categoria de impacto da quest no mundo.
 *
 * PLAYER_PROGRESSION  → XP, ouro, itens, reputação pessoal.
 *                       Não altera fronteiras nem guerra territorial.
 *
 * FACTION_PROGRESSION → Altera war progress, pode mover fronteira,
 *                       destrava raids, reforça defesa territorial.
 */
public enum QuestCategory {
    PLAYER_PROGRESSION,
    FACTION_PROGRESSION
}
