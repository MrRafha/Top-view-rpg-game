package com.rpggame.factions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um nó territorial no mundo.
 * Cada mapa/zona tem um TerritoryNode que guarda owner, estado,
 * força de defesa, IDs vizinhos e se pode ser invadido.
 */
public class TerritoryNode {

    private final String territoryId;
    private FactionType ownerFaction;
    private TerritoryType type;
    private TerritoryState state;

    /** 0-100: quanto a facção dona controla o local */
    private int controlStrength;
    /** 0-100: capacidade de defesa atual */
    private int defenseStrength;
    /** Valor estratégico (influencia prioridade de raid) */
    private int strategicValue;

    private final List<String> neighborIds = new ArrayList<>();

    public TerritoryNode(String territoryId, FactionType owner, TerritoryType type) {
        this.territoryId    = territoryId;
        this.ownerFaction   = owner;
        this.type           = type;
        this.state          = TerritoryState.STABLE;
        this.controlStrength = 100;
        this.defenseStrength = 50;
        this.strategicValue  = 10;
    }

    // ---------------------------------------------------------------
    // Vizinhança

    public void addNeighbor(String neighborId) {
        if (!neighborIds.contains(neighborId)) neighborIds.add(neighborId);
    }

    public List<String> getNeighborIds() {
        return Collections.unmodifiableList(neighborIds);
    }

    /** Verifica se uma facção tem território vizinho (pré-condição para raid) */
    public boolean canRaidFrom(FactionType attacker, TerritoryTracker tracker) {
        for (String nid : neighborIds) {
            TerritoryNode neighbor = tracker.getNode(nid);
            if (neighbor != null && neighbor.getOwnerFaction() == attacker) return true;
        }
        return false;
    }

    /** Verifica se pode ser invadido por uma facção */
    public boolean canBeInvadedBy(FactionType attacker, TerritoryTracker tracker) {
        return ownerFaction != attacker && canRaidFrom(attacker, tracker);
    }

    // ---------------------------------------------------------------
    // Controle territorial

    /** Aplica dano de raid na defesa */
    public void applyRaidPressure(int damage) {
        defenseStrength = Math.max(0, defenseStrength - damage);
        controlStrength = Math.max(0, controlStrength - damage / 2);
        updateState();
    }

    /** Restaura defesa ao longo do tempo */
    public void recover(int amount) {
        defenseStrength = Math.min(100, defenseStrength + amount);
        controlStrength = Math.min(100, controlStrength + amount / 2);
        updateState();
    }

    /** Captura o território para outra facção */
    public void capture(FactionType newOwner) {
        this.ownerFaction   = newOwner;
        this.controlStrength = 30;   // Controle inicial fraco
        this.defenseStrength = 20;
        this.state          = TerritoryState.CAPTURED_RECENTLY;

        // Ajusta tipo de território ao novo dono
        if (newOwner == FactionType.GOBLINS) {
            if (type == TerritoryType.HUMAN_SETTLEMENT) type = TerritoryType.CONTESTED_ZONE;
            else if (type == TerritoryType.HUMAN_FRONTIER) type = TerritoryType.GOBLIN_FRONTIER;
            else if (type == TerritoryType.NEUTRAL_WILDS) type = TerritoryType.GOBLIN_FRONTIER;
        } else if (newOwner == FactionType.HUMANS) {
            if (type == TerritoryType.GOBLIN_TERRITORY) type = TerritoryType.CONTESTED_ZONE;
            else if (type == TerritoryType.GOBLIN_FRONTIER) type = TerritoryType.HUMAN_FRONTIER;
            else if (type == TerritoryType.NEUTRAL_WILDS) type = TerritoryType.HUMAN_FRONTIER;
        }

        System.out.println("[Território] " + territoryId + " capturado por " + newOwner);
    }

    private void updateState() {
        if (state == TerritoryState.CAPTURED_RECENTLY) {
            // Sai do estado CAPTURED_RECENTLY quando estabilizar
            if (controlStrength >= 60) state = TerritoryState.STABLE;
            return;
        }
        if (defenseStrength == 0 || controlStrength < 20) {
            state = TerritoryState.CONTESTED;
        } else if (controlStrength < 50) {
            state = TerritoryState.UNDER_PRESSURE;
        } else {
            state = TerritoryState.STABLE;
        }
    }

    // ---------------------------------------------------------------
    // Getters / Setters

    public String getTerritoryId()             { return territoryId; }
    public FactionType getOwnerFaction()       { return ownerFaction; }
    public TerritoryType getType()             { return type; }
    public TerritoryState getState()           { return state; }
    public void setState(TerritoryState state) { this.state = state; }
    public int getControlStrength()            { return controlStrength; }
    public int getDefenseStrength()            { return defenseStrength; }
    public int getStrategicValue()             { return strategicValue; }
    public void setStrategicValue(int v)       { this.strategicValue = v; }

    public boolean isContested()  { return state == TerritoryState.CONTESTED; }
    public boolean isStable()     { return state == TerritoryState.STABLE; }
    public boolean isFortified()  { return state == TerritoryState.FORTIFIED; }

    public void fortify(int amount) {
        defenseStrength = Math.min(100, defenseStrength + amount);
        state = TerritoryState.FORTIFIED;
    }
}
