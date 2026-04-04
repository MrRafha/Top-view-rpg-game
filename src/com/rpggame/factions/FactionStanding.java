package com.rpggame.factions;

/**
 * Nível de relacionamento do jogador com uma facção.
 * Determinado pelo valor numérico de reputação.
 */
public enum FactionStanding {
    HATED,      // <= -75
    HOSTILE,    // -74 a -40
    UNWELCOME,  // -39 a -10
    NEUTRAL,    // -9 a 9
    TOLERATED,  // 10 a 39
    FRIENDLY,   // 40 a 74
    ALLIED;     // >= 75

    /** Converte valor numérico de reputação para FactionStanding */
    public static FactionStanding fromReputation(int rep) {
        if (rep <= -75) return HATED;
        if (rep <= -40) return HOSTILE;
        if (rep <= -10) return UNWELCOME;
        if (rep <=   9) return NEUTRAL;
        if (rep <=  39) return TOLERATED;
        if (rep <=  74) return FRIENDLY;
        return ALLIED;
    }
}
