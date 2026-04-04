package com.rpggame.factions;

import java.util.EnumMap;
import java.util.Map;

/**
 * Gerencia a reputação do jogador com cada facção.
 *
 * Reputação numérica:
 *   <= -75  → HATED
 *   -74/-40 → HOSTILE
 *   -39/-10 → UNWELCOME
 *    -9/  9 → NEUTRAL
 *    10/ 39 → TOLERATED
 *    40/ 74 → FRIENDLY
 *    >= 75  → ALLIED
 */
public class FactionRelationManager {

    private static final int MIN_REP = -100;
    private static final int MAX_REP =  100;

    private final Map<FactionType, Integer> reputation = new EnumMap<>(FactionType.class);

    public FactionRelationManager() {
        // Jogador começa neutro com todas as facções
        for (FactionType faction : FactionType.values()) {
            reputation.put(faction, 0);
        }
    }

    // ---------------------------------------------------------------
    // Leitura

    public int getReputation(FactionType faction) {
        return reputation.getOrDefault(faction, 0);
    }

    public FactionStanding getStanding(FactionType faction) {
        return FactionStanding.fromReputation(getReputation(faction));
    }

    public boolean isHostile(FactionType faction) {
        FactionStanding s = getStanding(faction);
        return s == FactionStanding.HOSTILE || s == FactionStanding.HATED;
    }

    public boolean isFriendly(FactionType faction) {
        FactionStanding s = getStanding(faction);
        return s == FactionStanding.FRIENDLY || s == FactionStanding.ALLIED;
    }

    public boolean isNeutral(FactionType faction) {
        return getStanding(faction) == FactionStanding.NEUTRAL;
    }

    public boolean isTolerated(FactionType faction) {
        FactionStanding s = getStanding(faction);
        return s == FactionStanding.NEUTRAL || s == FactionStanding.TOLERATED
                || s == FactionStanding.FRIENDLY || s == FactionStanding.ALLIED;
    }

    // ---------------------------------------------------------------
    // Modificação

    /** Adiciona reputação positiva com uma facção */
    public void addReputation(FactionType faction, int amount) {
        if (amount <= 0) return;
        int current = getReputation(faction);
        setReputation(faction, current + amount);
        System.out.println("[Facção] +" + amount + " reputação com " + faction
                + " → " + getReputation(faction) + " (" + getStanding(faction) + ")");
    }

    /** Remove reputação (piora relação) com uma facção */
    public void removeReputation(FactionType faction, int amount) {
        if (amount <= 0) return;
        int current = getReputation(faction);
        setReputation(faction, current - amount);
        System.out.println("[Facção] -" + amount + " reputação com " + faction
                + " → " + getReputation(faction) + " (" + getStanding(faction) + ")");
    }

    /** Define valor direto (usado por serialização / cheats) */
    public void setReputation(FactionType faction, int value) {
        reputation.put(faction, Math.max(MIN_REP, Math.min(MAX_REP, value)));
    }

    // ---------------------------------------------------------------
    // Ações comuns de reputação

    /** Jogador completou uma quest para a facção */
    public void onQuestCompleted(FactionType faction, int gain) {
        addReputation(faction, gain);
    }

    /** Jogador matou um membro da facção */
    public void onKilledMember(FactionType faction, int loss) {
        removeReputation(faction, loss);
    }

    /** Jogador ajudou a defender território da facção */
    public void onDefendedTerritory(FactionType faction, int gain) {
        addReputation(faction, gain);
    }

    /** Jogador invadiu área sensível da facção */
    public void onInvadedSensitiveArea(FactionType faction, int loss) {
        removeReputation(faction, loss);
    }

    // ---------------------------------------------------------------
    // Debug

    public String getSummary() {
        StringBuilder sb = new StringBuilder("=== Reputação ===\n");
        for (FactionType f : FactionType.values()) {
            sb.append(f).append(": ").append(getReputation(f))
              .append(" (").append(getStanding(f)).append(")\n");
        }
        return sb.toString();
    }
}
