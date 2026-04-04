package com.rpggame.factions;

/**
 * Tipo de território no mundo
 */
public enum TerritoryType {
    HUMAN_SETTLEMENT,  // Centro habitado por humanos
    HUMAN_FRONTIER,    // Borda do território humano
    GOBLIN_TERRITORY,  // Área controlada por goblins
    GOBLIN_FRONTIER,   // Borda do território goblin
    NEUTRAL_WILDS,     // Zona neutra sem controle
    CONTESTED_ZONE,    // Disputada entre facções
    SPECIAL_AREA       // Área especial (dungeon, etc.)
}
