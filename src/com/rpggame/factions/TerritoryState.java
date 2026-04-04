package com.rpggame.factions;

/**
 * Estado atual de controle de um território
 */
public enum TerritoryState {
    STABLE,             // Controle firme pela facção dona
    UNDER_PRESSURE,     // Sob pressão inimiga, mas ainda controlado
    CONTESTED,          // Disputado ativamente
    CAPTURED_RECENTLY,  // Acabou de mudar de dono
    FORTIFIED           // Defesa reforçada
}
