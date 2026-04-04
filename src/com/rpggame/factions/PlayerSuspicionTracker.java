package com.rpggame.factions;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rastreia a suspeita que cada facção tem sobre o jogador.
 *
 * Fontes de aumento:
 *   +5  entrar em território restrito
 *   +10 aproximar-se de área sensível (cabana/líder)
 *   +15 abrir baú/objeto local
 *   +20 destruir estrutura
 *   +25 sacar arma perto de líder
 *   +30 atacar membro da facção
 *   +20 interferir em patrulha/raid
 *
 * Fontes de redução:
 *   -2/tick  sair do território (decay passivo)
 *   -10      ficar parado em área permitida
 *   -20      completar quest da facção
 *   -15      obedecer ordem de retirada
 *   -5       ter reputação positiva (bonus passivo por tick)
 */
public class PlayerSuspicionTracker {

    private static final int MAX_SUSPICION = 100;
    private static final int MIN_SUSPICION = 0;

    /** Suspeita acumulada por facção */
    private final Map<FactionType, Integer> suspicion = new EnumMap<>(FactionType.class);

    /** Ticks de decay por facção (conta quanto tempo o jogador está fora) */
    private final Map<FactionType, Integer> decayTicks = new EnumMap<>(FactionType.class);

    public PlayerSuspicionTracker() {
        for (FactionType f : FactionType.values()) {
            suspicion.put(f, 0);
            decayTicks.put(f, 0);
        }
    }

    // ---------------------------------------------------------------
    // Aumento de suspeita

    public void addSuspicion(FactionType faction, int amount) {
        if (amount <= 0) return;
        int current = suspicion.getOrDefault(faction, 0);
        int next = Math.min(MAX_SUSPICION, current + amount);
        suspicion.put(faction, next);
        decayTicks.put(faction, 0); // Reinicia decay
        System.out.println("[Suspeita] +" + amount + " com " + faction
                + " → " + next + " (" + getState(faction) + ")");
    }

    // ---------------------------------------------------------------
    // Redução manual

    public void reduceSuspicion(FactionType faction, int amount) {
        if (amount <= 0) return;
        int current = suspicion.getOrDefault(faction, 0);
        suspicion.put(faction, Math.max(MIN_SUSPICION, current - amount));
    }

    public void clearSuspicion(FactionType faction) {
        suspicion.put(faction, 0);
    }

    // ---------------------------------------------------------------
    // Leitura

    public int getValue(FactionType faction) {
        return suspicion.getOrDefault(faction, 0);
    }

    public SuspicionState getState(FactionType faction) {
        return SuspicionState.fromValue(getValue(faction));
    }

    public boolean isHostile(FactionType faction) {
        return getState(faction) == SuspicionState.HOSTILE;
    }

    // ---------------------------------------------------------------
    // Eventos de domínio

    public void onEnteredRestrictedArea(FactionType faction) {
        addSuspicion(faction, 5);
    }

    public void onApproachedSensitiveArea(FactionType faction) {
        addSuspicion(faction, 10);
    }

    public void onOpenedLocalChest(FactionType faction) {
        addSuspicion(faction, 15);
    }

    public void onDestroyedStructure(FactionType faction) {
        addSuspicion(faction, 20);
    }

    public void onDrewWeaponNearLeader(FactionType faction) {
        addSuspicion(faction, 25);
    }

    public void onAttackedMember(FactionType faction) {
        addSuspicion(faction, 30);
    }

    public void onInterferredWithPatrol(FactionType faction) {
        addSuspicion(faction, 20);
    }

    public void onQuestCompleted(FactionType faction) {
        reduceSuspicion(faction, 20);
    }

    public void onObeyedExpulsion(FactionType faction) {
        reduceSuspicion(faction, 15);
    }

    // ---------------------------------------------------------------
    // Tick passivo (chamar a cada frame ou segundo)

    /**
     * Aplica decay passivo de suspeita.
     *
     * @param inTerritory  true se o jogador ainda está no território da facção
     * @param reputation   reputação numérica atual do jogador com a facção
     */
    public void tick(FactionType faction, boolean inTerritory, int reputation) {
        int current = getValue(faction);
        if (current == 0) return;

        if (!inTerritory) {
            // Decay mais rápido fora do território
            int ticks = decayTicks.getOrDefault(faction, 0) + 1;
            decayTicks.put(faction, ticks);
            if (ticks % 30 == 0) { // A cada ~0.5s (60fps)
                reduceSuspicion(faction, 2);
                // Reputação positiva acelera o decay
                if (reputation >= 10) reduceSuspicion(faction, 1);
            }
        }
    }
}
