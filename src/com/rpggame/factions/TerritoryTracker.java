package com.rpggame.factions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro global de todos os nós territoriais.
 * Inicializado com os IDs de mapa vindos de WorldLayout/WorldGenerator.
 */
public class TerritoryTracker {

    private final Map<String, TerritoryNode> nodes = new LinkedHashMap<>();

    // ---------------------------------------------------------------
    // Registro

    public void registerNode(TerritoryNode node) {
        nodes.put(node.getTerritoryId(), node);
    }

    /** Registra vizinhança bidirecional entre dois mapas */
    public void connectNeighbors(String mapA, String mapB) {
        TerritoryNode a = nodes.get(mapA);
        TerritoryNode b = nodes.get(mapB);
        if (a != null) a.addNeighbor(mapB);
        if (b != null) b.addNeighbor(mapA);
    }

    // ---------------------------------------------------------------
    // Consulta

    public TerritoryNode getNode(String mapId) {
        return nodes.get(mapId);
    }

    public FactionType getOwner(String mapId) {
        TerritoryNode n = nodes.get(mapId);
        return n != null ? n.getOwnerFaction() : FactionType.NEUTRAL;
    }

    public TerritoryType getType(String mapId) {
        TerritoryNode n = nodes.get(mapId);
        return n != null ? n.getType() : TerritoryType.NEUTRAL_WILDS;
    }

    public boolean isGoblinTerritory(String mapId) {
        FactionType owner = getOwner(mapId);
        return owner == FactionType.GOBLINS;
    }

    public boolean isHumanTerritory(String mapId) {
        FactionType owner = getOwner(mapId);
        return owner == FactionType.HUMANS;
    }

    public boolean isContested(String mapId) {
        TerritoryNode n = nodes.get(mapId);
        return n != null && n.isContested();
    }

    /** Retorna todos os nós controlados por uma facção */
    public List<TerritoryNode> getNodesOwnedBy(FactionType faction) {
        List<TerritoryNode> result = new ArrayList<>();
        for (TerritoryNode n : nodes.values()) {
            if (n.getOwnerFaction() == faction) result.add(n);
        }
        return result;
    }

    /** Retorna territórios de uma facção que fazem borda com inimigos (candidatos a raid) */
    public List<TerritoryNode> getFrontierNodes(FactionType faction) {
        List<TerritoryNode> result = new ArrayList<>();
        for (TerritoryNode n : nodes.values()) {
            if (n.getOwnerFaction() != faction) continue;
            for (String nid : n.getNeighborIds()) {
                TerritoryNode neighbor = nodes.get(nid);
                if (neighbor != null && neighbor.getOwnerFaction() != faction) {
                    result.add(n);
                    break;
                }
            }
        }
        return result;
    }

    /** Retorna territórios inimigos adjacentes a territórios da facção */
    public List<TerritoryNode> getInvadableTargets(FactionType attacker) {
        List<TerritoryNode> result = new ArrayList<>();
        for (TerritoryNode n : nodes.values()) {
            if (n.getOwnerFaction() == attacker) continue;
            if (n.getOwnerFaction() == FactionType.NEUTRAL) continue;
            if (n.canBeInvadedBy(attacker, this)) result.add(n);
        }
        return result;
    }

    public Collection<TerritoryNode> getAllNodes() {
        return nodes.values();
    }

    // ---------------------------------------------------------------
    // Tick de recuperação passiva (chamar do game loop)

    public void tick() {
        for (TerritoryNode n : nodes.values()) {
            if (n.getState() == TerritoryState.UNDER_PRESSURE
                    || n.getState() == TerritoryState.CAPTURED_RECENTLY) {
                n.recover(1); // Recupera lentamente
            }
        }
    }
}
