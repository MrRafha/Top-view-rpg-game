package com.rpggame.factions;

/**
 * Estado de suspeita de uma facção em relação ao jogador.
 * Baseado em valor numérico acumulado (0-100+).
 */
public enum SuspicionState {
    IGNORED,   // 0-19:  sem interesse
    OBSERVED,  // 20-39: sendo observado
    ALERTED,   // 40-59: guardas em alerta
    EXPELLING, // 60-79: sendo forçado a sair
    HOSTILE;   // 80+:   ataque autorizado

    /** Converte valor numérico de suspeita para SuspicionState */
    public static SuspicionState fromValue(int value) {
        if (value < 20) return IGNORED;
        if (value < 40) return OBSERVED;
        if (value < 60) return ALERTED;
        if (value < 80) return EXPELLING;
        return HOSTILE;
    }
}
