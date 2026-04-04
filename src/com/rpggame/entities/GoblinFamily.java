package com.rpggame.entities;

import com.rpggame.factions.FactionRelationManager;
import com.rpggame.factions.GoblinLeaderBrain;
import com.rpggame.factions.LeaderDecision;
import com.rpggame.factions.PlayerSuspicionTracker;
import com.rpggame.factions.TerritoryTracker;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa a família goblin principal do mundo.
 *
 * NOTE: Sistema de múltiplas famílias e guerra entre famílias estão
 * desativados por enquanto. Apenas 1 instância desta classe deve existir.
 * O controle social agora é feito pelo GoblinLeaderBrain + FactionRelationManager.
 */
public class GoblinFamily {
    private static int nextFamilyId = 1;

    private int familyId;
    private String familyName;
    private ArrayList<Goblin> members;
    private Goblin leader;
    private Rectangle territory;
    private Point hutPosition;

    // Estados da família — guerra entre famílias DESATIVADA
    /** @deprecated Múltiplas famílias desativadas. Use FactionWarManager para guerra territorial. */
    @Deprecated private boolean atWar = false;
    /** @deprecated Múltiplas famílias desativadas. */
    @Deprecated private GoblinFamily enemyFamily = null;

    // Novo sistema social
    private GoblinLeaderBrain leaderBrain;

    // Configurações de território
    private static final int TERRITORY_SIZE = 300;
    private static final int MAX_FAMILY_SIZE = 6;
    
    /**
     * Construtor da família goblin principal.
     */
    public GoblinFamily(Point hutPosition, String name) {
        this.familyId   = nextFamilyId++;
        this.familyName = name != null ? name : "Família Goblin";
        this.hutPosition = new Point(hutPosition);
        this.members = new ArrayList<>();

        this.territory = new Rectangle(
            hutPosition.x - TERRITORY_SIZE / 2,
            hutPosition.y - TERRITORY_SIZE / 2,
            TERRITORY_SIZE,
            TERRITORY_SIZE
        );
    }

    /**
     * Inicializa o cérebro do líder (chamar após criar os gerenciadores de facções).
     */
    public void initLeaderBrain(FactionRelationManager relations,
                                PlayerSuspicionTracker suspicion,
                                TerritoryTracker territory) {
        this.leaderBrain = new GoblinLeaderBrain(relations, suspicion, territory);
    }
    
    /**
     * Adiciona um goblin à família
     */
    public void addMember(Goblin goblin) {
        if (members.size() < MAX_FAMILY_SIZE) {
            goblin.setFamily(this);
            members.add(goblin);
            
            // O primeiro goblin adicionado se torna o líder
            if (leader == null && goblin.getPersonality() == GoblinPersonality.LEADER) {
                leader = goblin;
            }
        }
    }
    
    /**
     * Remove um goblin da família (quando morre)
     */
    public boolean removeMember(Goblin goblin) {
        members.remove(goblin);
        if (goblin == leader) {
            // Escolher novo líder
            electNewLeader();
        }
        
        // Retorna true se a família foi completamente derrotada
        return members.isEmpty();
    }
    
    /**
     * Elege um novo líder da família
     */
    private void electNewLeader() {
        leader = null;
        // Procurar por um líder nato
        for (Goblin goblin : members) {
            if (goblin.getPersonality() == GoblinPersonality.LEADER) {
                leader = goblin;
                return;
            }
        }
        // Se não houver líder nato, escolher o mais agressivo
        for (Goblin goblin : members) {
            if (goblin.getPersonality() == GoblinPersonality.AGGRESSIVE) {
                leader = goblin;
                return;
            }
        }
        // Último recurso: qualquer goblin
        if (!members.isEmpty()) {
            leader = members.get(0);
        }
    }
    
    /**
     * Verifica se uma posição está dentro do território da família
     */
    public boolean isInTerritory(double x, double y) {
        return territory.contains(x, y);
    }
    
    /**
     * Verifica se o player está no território
     */
    public boolean isPlayerInTerritory(Player player) {
        return isInTerritory(player.getX(), player.getY());
    }
    
    /**
     * Consulta o líder e retorna a decisão para agir sobre o jogador.
     * Requer que initLeaderBrain() tenha sido chamado.
     */
    public LeaderDecision queryLeaderDecision(Player player, String playerMapId) {
        if (leaderBrain == null) return LeaderDecision.IGNORE;

        boolean inTerritory = isPlayerInTerritory(player);
        boolean nearLeader  = leader != null && distanceTo(player.getX(), player.getY(),
                                  leader.getX(), leader.getY()) < 100;

        return leaderBrain.decide(inTerritory, nearLeader, false, playerMapId);
    }

    /**
     * Compatibilidade com código legado do Goblin.java.
     * Delega ao novo sistema de liderança quando disponível;
     * caso contrário usa heurística simples de território.
     */
    public boolean shouldPursuePlayer(Player player) {
        if (leaderBrain != null) {
            LeaderDecision decision = queryLeaderDecision(player, "");
            return decision == LeaderDecision.ATTACK || decision == LeaderDecision.EXPEL;
        }
        // Fallback: perseguir apenas dentro do território
        return isPlayerInTerritory(player);
    }

    private double distanceTo(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Guerra entre famílias — mantido para compatibilidade, mas desativado no novo sistema
    public void declareWarAgainst(GoblinFamily enemy) {
        this.atWar = true;
        this.enemyFamily = enemy;
        enemy.atWar = true;
        enemy.enemyFamily = this;
    }

    public void endWar() {
        if (this.atWar && this.enemyFamily != null) {
            this.enemyFamily.atWar = false;
            this.enemyFamily.enemyFamily = null;
        }
        this.atWar = false;
        this.enemyFamily = null;
    }

    public boolean isEnemyOf(GoblinFamily otherFamily) {
        return this.atWar && this.enemyFamily == otherFamily;
    }

    // Getters e Setters
    public int getFamilyId()                    { return familyId; }
    public String getFamilyName()               { return familyName; }
    public void setFamilyName(String name)      { this.familyName = name; }
    public List<Goblin> getMembers()            { return new ArrayList<>(members); }
    public Goblin getLeader()                   { return leader; }
    public Rectangle getTerritory()             { return new Rectangle(territory); }
    public Point getHutPosition()               { return new Point(hutPosition); }
    public boolean isAtWar()                    { return atWar; }
    public GoblinFamily getEnemyFamily()        { return enemyFamily; }
    public int getMemberCount()                 { return members.size(); }
    
    /**
     * Verifica se a família foi derrotada (sem membros vivos)
     */
    public boolean isDefeated() {
        return members.isEmpty();
    }
}