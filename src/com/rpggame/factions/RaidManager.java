package com.rpggame.factions;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia raids entre territórios.
 *
 * Regra principal: uma facção só pode iniciar raid em território inimigo
 * se existir território vizinho controlado por ela (adjacência válida).
 *
 * Resultados são entregues via RaidResultListener (implementado por FactionWarManager).
 */
public class RaidManager {

    private static final int RAID_COOLDOWN_TICKS   = 3600; // ~60s a 60fps
    private static final int RAID_DURATION_TICKS   = 1800; // ~30s
    private static final int RAID_PRESSURE_PER_TICK = 1;

    private final List<ActiveRaid> activeRaids  = new ArrayList<>();
    private final List<ActiveRaid> pendingRaids = new ArrayList<>();

    private RaidResultListener resultListener;

    public void setResultListener(RaidResultListener listener) {
        this.resultListener = listener;
    }

    // ---------------------------------------------------------------
    // Agendamento

    /**
     * Tenta agendar uma raid para a facção.
     * Escolhe o alvo de maior valor estratégico disponível.
     */
    public void tryScheduleRaid(FactionType attacker, TerritoryTracker tracker) {
        // Bloqueia se já existe raid ativa ou em cooldown desta facção
        for (ActiveRaid r : activeRaids) {
            if (r.attacker == attacker) return;
        }
        for (ActiveRaid r : pendingRaids) {
            if (r.attacker == attacker) return;
        }

        List<TerritoryNode> targets = tracker.getInvadableTargets(attacker);
        if (targets.isEmpty()) return;

        // Escolhe alvo com maior valor estratégico
        TerritoryNode best = targets.get(0);
        for (TerritoryNode t : targets) {
            if (t.getStrategicValue() > best.getStrategicValue()) best = t;
        }

        ActiveRaid raid = new ActiveRaid(attacker, best.getTerritoryId());
        raid.state = RaidState.PREPARING;
        raid.ticksRemaining = RAID_COOLDOWN_TICKS / 6; // ~10s de preparação
        pendingRaids.add(raid);
        System.out.println("[Raid] " + attacker + " preparando raid em " + best.getTerritoryId());
    }

    // ---------------------------------------------------------------
    // Tick (chamar do game loop)

    public void tick(TerritoryTracker tracker) {
        // Preparação → ativa
        List<ActiveRaid> toActivate = new ArrayList<>();
        for (ActiveRaid r : pendingRaids) {
            if (r.state != RaidState.PREPARING) continue;
            r.ticksRemaining--;
            if (r.ticksRemaining <= 0) {
                r.state = RaidState.ACTIVE;
                r.ticksRemaining = RAID_DURATION_TICKS;
                toActivate.add(r);
                System.out.println("[Raid] Raid de " + r.attacker + " INICIADA em " + r.targetMapId);
            }
        }
        pendingRaids.removeAll(toActivate);
        activeRaids.addAll(toActivate);

        // Raids ativas — aplica pressão e verifica encerramento
        List<ActiveRaid> toRemove = new ArrayList<>();
        for (ActiveRaid r : activeRaids) {
            TerritoryNode target = tracker.getNode(r.targetMapId);
            if (target == null) { toRemove.add(r); continue; }

            r.ticksRemaining--;
            target.applyRaidPressure(RAID_PRESSURE_PER_TICK);

            boolean defenseFallen = target.getDefenseStrength() <= 0;
            boolean timeUp        = r.ticksRemaining <= 0;

            if (defenseFallen) {
                r.state = RaidState.SUCCEEDED;
                if (resultListener != null) resultListener.onRaidSucceeded(r.attacker, r.targetMapId);
                scheduleRaidCooldown(r.attacker, r.targetMapId);
                toRemove.add(r);
            } else if (timeUp) {
                r.state = RaidState.FAILED;
                if (resultListener != null) resultListener.onRaidFailed(r.attacker, r.targetMapId);
                scheduleRaidCooldown(r.attacker, r.targetMapId);
                toRemove.add(r);
            }
        }
        activeRaids.removeAll(toRemove);

        // Cooldowns
        List<ActiveRaid> cooldownDone = new ArrayList<>();
        for (ActiveRaid r : pendingRaids) {
            if (r.state != RaidState.COOLDOWN) continue;
            r.ticksRemaining--;
            if (r.ticksRemaining <= 0) cooldownDone.add(r);
        }
        pendingRaids.removeAll(cooldownDone);
    }

    private void scheduleRaidCooldown(FactionType attacker, String targetMapId) {
        ActiveRaid cooldown = new ActiveRaid(attacker, targetMapId);
        cooldown.state = RaidState.COOLDOWN;
        cooldown.ticksRemaining = RAID_COOLDOWN_TICKS;
        pendingRaids.add(cooldown);
    }

    // ---------------------------------------------------------------
    // Consulta

    public boolean isRaidActive(FactionType attacker) {
        for (ActiveRaid r : activeRaids) {
            if (r.attacker == attacker && r.state == RaidState.ACTIVE) return true;
        }
        return false;
    }

    public boolean isMapUnderRaid(String mapId) {
        for (ActiveRaid r : activeRaids) {
            if (r.targetMapId.equals(mapId) && r.state == RaidState.ACTIVE) return true;
        }
        return false;
    }

    public RaidState getRaidState(FactionType attacker) {
        for (ActiveRaid r : activeRaids) {
            if (r.attacker == attacker) return r.state;
        }
        for (ActiveRaid r : pendingRaids) {
            if (r.attacker == attacker) return r.state;
        }
        return RaidState.NONE;
    }

    // ---------------------------------------------------------------
    // Classe interna de dados

    private static class ActiveRaid {
        final FactionType attacker;
        final String      targetMapId;
        RaidState         state;
        int               ticksRemaining;

        ActiveRaid(FactionType attacker, String targetMapId) {
            this.attacker       = attacker;
            this.targetMapId    = targetMapId;
            this.state          = RaidState.NONE;
            this.ticksRemaining = 0;
        }
    }
}
