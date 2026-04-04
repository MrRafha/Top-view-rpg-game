package com.rpggame.factions;

/**
 * Estado de uma raid entre territórios
 */
public enum RaidState {
    NONE,       // Sem raid ativa
    PREPARING,  // Raid sendo preparada (cooldown/condições)
    ACTIVE,     // Raid em andamento
    SUCCEEDED,  // Raid vencida — território contestado
    FAILED,     // Raid falhou — defensores seguraram
    COOLDOWN    // Em recarga antes da próxima raid
}
