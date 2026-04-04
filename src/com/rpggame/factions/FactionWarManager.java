package com.rpggame.factions;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia o progresso de guerra de cada facção e decide quando
 * a fronteira avança/recua.
 *
 * Implementa RaidResultListener para receber callbacks do RaidManager
 * sem criar dependência circular.
 */
public class FactionWarManager implements RaidResultListener {

    private static final int MAX_WAR_PROGRESS = 100;
    private static final int MIN_WAR_PROGRESS = 0;

    private final Map<FactionType, Integer> warProgress = new EnumMap<>(FactionType.class);

    private final TerritoryTracker territoryTracker;
    private RaidManager raidManager;
    private GoblinLeaderBrain leaderBrain;

    public FactionWarManager(TerritoryTracker territoryTracker) {
        this.territoryTracker = territoryTracker;
        for (FactionType f : FactionType.values()) {
            warProgress.put(f, 0);
        }
    }

    /** Liga o RaidManager e registra este objeto como listener de resultado */
    public void setRaidManager(RaidManager raidManager) {
        this.raidManager = raidManager;
        raidManager.setResultListener(this);
    }

    public void setLeaderBrain(GoblinLeaderBrain brain) {
        this.leaderBrain = brain;
    }

    // ---------------------------------------------------------------
    // Progresso de guerra

    public int getWarProgress(FactionType faction) {
        return warProgress.getOrDefault(faction, 0);
    }

    public void addWarProgress(FactionType faction, int amount) {
        if (amount <= 0) return;
        int next = Math.min(MAX_WAR_PROGRESS, getWarProgress(faction) + amount);
        warProgress.put(faction, next);
        System.out.println("[Guerra] +" + amount + " progresso de guerra para "
                + faction + " → " + next);
        propagateToLeader(faction);
        checkExpansionThreshold(faction);
    }

    public void removeWarProgress(FactionType faction, int amount) {
        if (amount <= 0) return;
        warProgress.put(faction, Math.max(MIN_WAR_PROGRESS, getWarProgress(faction) - amount));
    }

    /** Chamado ao completar uma quest FACTION_PROGRESSION */
    public void onFactionQuestCompleted(FactionType faction, int warGain,
                                         boolean unlockRaid, boolean reinforceDefense,
                                         String territoryTarget) {
        addWarProgress(faction, warGain);

        if (reinforceDefense && territoryTarget != null) {
            TerritoryNode node = territoryTracker.getNode(territoryTarget);
            if (node != null && node.getOwnerFaction() == faction) {
                node.fortify(20);
                System.out.println("[Guerra] " + territoryTarget + " reforçado por " + faction);
            }
        }

        if (unlockRaid && raidManager != null) {
            raidManager.tryScheduleRaid(faction, territoryTracker);
        }
    }

    // ---------------------------------------------------------------
    // RaidResultListener

    @Override
    public void onRaidSucceeded(FactionType attacker, String targetMapId) {
        TerritoryNode target = territoryTracker.getNode(targetMapId);
        if (target == null) return;

        if (target.getState() == TerritoryState.CONTESTED
                && target.canBeInvadedBy(attacker, territoryTracker)) {
            target.capture(attacker);
            removeWarProgress(attacker, 20);
        }

        System.out.println("[Guerra] Raid de " + attacker + " em " + targetMapId
                + " → " + target.getState());
    }

    @Override
    public void onRaidFailed(FactionType attacker, String targetMapId) {
        removeWarProgress(attacker, 10);
        TerritoryNode target = territoryTracker.getNode(targetMapId);
        if (target != null) target.recover(15);
        System.out.println("[Guerra] Raid de " + attacker + " em " + targetMapId + " falhou.");
    }

    // ---------------------------------------------------------------
    // Internos

    private void checkExpansionThreshold(FactionType faction) {
        if (getWarProgress(faction) >= 40 && raidManager != null) {
            List<TerritoryNode> targets = territoryTracker.getInvadableTargets(faction);
            if (!targets.isEmpty()) {
                raidManager.tryScheduleRaid(faction, territoryTracker);
            }
        }
    }

    private void propagateToLeader(FactionType faction) {
        if (leaderBrain == null || faction != FactionType.GOBLINS) return;
        leaderBrain.setWarProgress(getWarProgress(faction));
        List<TerritoryNode> frontier = territoryTracker.getFrontierNodes(faction);
        List<TerritoryNode> owned    = territoryTracker.getNodesOwnedBy(faction);
        int pressure = owned.isEmpty() ? 0 : (frontier.size() * 100 / owned.size());
        leaderBrain.setBorderPressure(pressure);
    }
}
