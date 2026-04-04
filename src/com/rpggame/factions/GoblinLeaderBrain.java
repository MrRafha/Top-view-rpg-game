package com.rpggame.factions;

/**
 * Cérebro de decisão do líder goblin.
 *
 * Entradas:
 *   - Reputação do jogador com os goblins
 *   - Suspeita acumulada
 *   - Jogador está em zona sensível?
 *   - Jogador está armado/agindo de forma agressiva?
 *   - Jogador matou goblins antes? (rep < -10)
 *   - Pressão territorial (fronteira sob ameaça humana)
 *   - Progresso de guerra goblin
 *
 * Saídas: LeaderDecision
 */
public class GoblinLeaderBrain {

    private final FactionRelationManager relationManager;
    private final PlayerSuspicionTracker suspicionTracker;
    private final TerritoryTracker territoryTracker;

    /** Progresso de guerra goblin (0-100). Alimentado por FactionWarManager. */
    private int warProgress = 0;
    /** Nível de pressão territorial na fronteira (0-100). */
    private int borderPressure = 0;

    public GoblinLeaderBrain(
            FactionRelationManager relationManager,
            PlayerSuspicionTracker suspicionTracker,
            TerritoryTracker territoryTracker) {
        this.relationManager   = relationManager;
        this.suspicionTracker  = suspicionTracker;
        this.territoryTracker  = territoryTracker;
    }

    // ---------------------------------------------------------------
    // Decisão principal

    /**
     * Decide o que o líder ordena considerando o estado completo do mundo.
     *
     * @param playerInGoblinTerritory  jogador está fisicamente no território
     * @param playerNearLeader         jogador está próximo à cabana/líder
     * @param playerWeaponDrawn        jogador sacou arma
     * @param currentMapId             mapa onde o jogador está
     */
    public LeaderDecision decide(
            boolean playerInGoblinTerritory,
            boolean playerNearLeader,
            boolean playerWeaponDrawn,
            String currentMapId) {

        int rep       = relationManager.getReputation(FactionType.GOBLINS);
        FactionStanding standing = FactionStanding.fromReputation(rep);
        SuspicionState suspicion = suspicionTracker.getState(FactionType.GOBLINS);

        // ---- Regras de ataque imediato --------------------------------
        if (standing == FactionStanding.HATED) return LeaderDecision.ATTACK;
        if (standing == FactionStanding.HOSTILE && playerInGoblinTerritory) return LeaderDecision.ATTACK;
        if (suspicion == SuspicionState.HOSTILE) return LeaderDecision.ATTACK;

        // ---- Jogador não está no território goblin --------------------
        if (!playerInGoblinTerritory) {
            if (borderPressure >= 70 && warProgress >= 50) {
                return LeaderDecision.PREPARE_RAID;
            }
            if (borderPressure >= 50) {
                return LeaderDecision.REINFORCE_BORDER;
            }
            return LeaderDecision.IGNORE;
        }

        // ---- Jogador no território ------------------------------------
        // Reputação muito negativa — expulsar ou atacar
        if (standing == FactionStanding.UNWELCOME) {
            if (playerNearLeader || playerWeaponDrawn) return LeaderDecision.ATTACK;
            return LeaderDecision.EXPEL;
        }

        // Suspeita moderada-alta — escalar gradualmente
        switch (suspicion) {
            case EXPELLING:
                return LeaderDecision.EXPEL;
            case ALERTED:
                if (playerNearLeader || playerWeaponDrawn) return LeaderDecision.EXPEL;
                return LeaderDecision.WARN;
            case OBSERVED:
                return LeaderDecision.OBSERVE;
            case IGNORED:
            default:
                // Jogador é neutro ou amigo — tolerado
                if (standing == FactionStanding.FRIENDLY || standing == FactionStanding.ALLIED) {
                    return LeaderDecision.IGNORE;
                }
                return LeaderDecision.OBSERVE;
        }
    }

    // ---------------------------------------------------------------
    // Decisão de raid (independente do jogador)

    /**
     * Decide se é hora de lançar uma raid em território humano vizinho.
     * Chamado pelo FactionWarManager periodicamente.
     */
    public boolean shouldLaunchRaid(String goblinMapId) {
        if (warProgress < 40) return false;
        if (borderPressure < 30) return false;
        // Só raida se tiver algum território humano invadível adjacente
        return !territoryTracker.getInvadableTargets(FactionType.GOBLINS).isEmpty();
    }

    // ---------------------------------------------------------------
    // Setters para estado de guerra

    public void setWarProgress(int value)    { this.warProgress    = Math.max(0, Math.min(100, value)); }
    public void setBorderPressure(int value) { this.borderPressure = Math.max(0, Math.min(100, value)); }
    public int getWarProgress()              { return warProgress; }
    public int getBorderPressure()           { return borderPressure; }
}
