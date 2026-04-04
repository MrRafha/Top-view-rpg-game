package com.rpggame.factions;

/**
 * Ponto central de acesso a todo o sistema de facções.
 *
 * Cria e conecta todos os subsistemas:
 *   FactionRelationManager ← reputação
 *   PlayerSuspicionTracker ← suspeita
 *   TerritoryTracker       ← ownership dos mapas
 *   RaidManager            ← raids por adjacência
 *   FactionWarManager      ← progresso de guerra + captura
 *   GoblinLeaderBrain      ← decisões do líder goblin
 *
 * Uso típico em GamePanel / LibGDXGameAdapter:
 *   FactionSystem fs = new FactionSystem();
 *   fs.registerMap("goblin_village", FactionType.GOBLINS, TerritoryType.GOBLIN_TERRITORY);
 *   fs.registerMap("neutral_wilds",  FactionType.NEUTRAL, TerritoryType.NEUTRAL_WILDS);
 *   fs.registerMap("human_village",  FactionType.HUMANS,  TerritoryType.HUMAN_SETTLEMENT);
 *   fs.connect("goblin_village", "neutral_wilds");
 *   fs.connect("neutral_wilds",  "human_village");
 *   fs.buildLeaderBrain();
 *   // A cada tick:
 *   fs.tick(currentMapId, playerX, playerY);
 */
public class FactionSystem {

    private final FactionRelationManager relations;
    private final PlayerSuspicionTracker suspicion;
    private final TerritoryTracker       territories;
    private final RaidManager            raids;
    private final FactionWarManager      war;
    private GoblinLeaderBrain            leaderBrain;

    public FactionSystem() {
        relations   = new FactionRelationManager();
        suspicion   = new PlayerSuspicionTracker();
        territories = new TerritoryTracker();
        raids       = new RaidManager();
        war         = new FactionWarManager(territories);
        war.setRaidManager(raids);
    }

    // ---------------------------------------------------------------
    // Setup de mapa

    public void registerMap(String mapId, FactionType owner, TerritoryType type) {
        territories.registerNode(new TerritoryNode(mapId, owner, type));
    }

    public void connect(String mapA, String mapB) {
        territories.connectNeighbors(mapA, mapB);
    }

    public void setStrategicValue(String mapId, int value) {
        TerritoryNode n = territories.getNode(mapId);
        if (n != null) n.setStrategicValue(value);
    }

    /** Deve ser chamado após registrar todos os mapas */
    public void buildLeaderBrain() {
        leaderBrain = new GoblinLeaderBrain(relations, suspicion, territories);
        war.setLeaderBrain(leaderBrain);
    }

    // ---------------------------------------------------------------
    // Tick principal (chamar 1× por frame ou a cada segundo)

    /**
     * @param currentMapId  ID do mapa onde o jogador está agora
     * @param playerX       posição X do jogador no mundo
     * @param playerY       posição Y do jogador no mundo
     */
    public void tick(String currentMapId, double playerX, double playerY) {
        territories.tick();
        raids.tick(territories);

        // Decay de suspeita por facção
        for (FactionType f : FactionType.values()) {
            boolean inTerritory = territories.getOwner(currentMapId) == f;
            suspicion.tick(f, inTerritory, relations.getReputation(f));
        }
    }

    // ---------------------------------------------------------------
    // Decisão do líder goblin

    /**
     * Consulta o líder goblin e retorna a decisão para agir.
     *
     * @param playerInGoblinMap  jogador está num mapa goblin agora
     * @param playerNearLeader   distância < 100px do líder/cabana
     * @param playerWeaponDrawn  jogador sacou arma
     * @param currentMapId       mapa atual do jogador
     */
    public LeaderDecision queryLeaderDecision(boolean playerInGoblinMap,
                                               boolean playerNearLeader,
                                               boolean playerWeaponDrawn,
                                               String currentMapId) {
        if (leaderBrain == null) return LeaderDecision.IGNORE;
        return leaderBrain.decide(playerInGoblinMap, playerNearLeader,
                                   playerWeaponDrawn, currentMapId);
    }

    // ---------------------------------------------------------------
    // Eventos de reputação

    public void onKilledGoblin()         { relations.onKilledMember(FactionType.GOBLINS, 15);
                                           suspicion.onAttackedMember(FactionType.GOBLINS); }
    public void onKilledHuman()          { relations.onKilledMember(FactionType.HUMANS, 15);
                                           suspicion.onAttackedMember(FactionType.HUMANS); }
    public void onEnteredGoblinArea(String mapId) {
        if (territories.isGoblinTerritory(mapId))
            suspicion.onEnteredRestrictedArea(FactionType.GOBLINS);
    }
    public void onApproachedGoblinLeader() { suspicion.onApproachedSensitiveArea(FactionType.GOBLINS); }
    public void onDestroyedGoblinStructure() { suspicion.onDestroyedStructure(FactionType.GOBLINS); }
    public void onObeyedExpulsion(FactionType f) { suspicion.onObeyedExpulsion(f); }

    // ---------------------------------------------------------------
    // Eventos de quest

    /** Chamado pelo QuestManager ao finalizar qualquer quest */
    public void onQuestCompleted(com.rpggame.systems.Quest quest) {
        FactionType faction = quest.getFactionTarget();
        if (faction == null) return;

        relations.onQuestCompleted(faction, quest.getReputationGain() > 0
                ? quest.getReputationGain() : 10);
        suspicion.onQuestCompleted(faction);

        if (quest.isFactionQuest()) {
            war.onFactionQuestCompleted(
                faction,
                quest.getWarProgressGain(),
                quest.isUnlockRaid(),
                quest.isReinforceDefense(),
                quest.getTerritoryTarget()
            );
        }
    }

    // ---------------------------------------------------------------
    // Feedback para UI

    /** Mensagem a exibir ao jogador com base no estado atual */
    public String getContextMessage(String currentMapId) {
        FactionType owner = territories.getOwner(currentMapId);
        if (owner == FactionType.NEUTRAL) return null;

        SuspicionState sus = suspicion.getState(owner);
        FactionStanding standing = relations.getStanding(owner);
        boolean underRaid = raids.isMapUnderRaid(currentMapId);

        if (underRaid) return "Território sob ataque!";
        if (territories.isContested(currentMapId)) return "Território contestado.";

        switch (sus) {
            case OBSERVED:  return owner == FactionType.GOBLINS
                    ? "Os goblins estão observando você."
                    : "Os guardas estão de olho em você.";
            case ALERTED:   return "Sua presença está se tornando indesejada.";
            case EXPELLING: return owner == FactionType.GOBLINS
                    ? "O líder goblin ordenou sua retirada."
                    : "Os guardas estão expulsando você.";
            case HOSTILE:   return "Você é tratado como inimigo aqui.";
            default: break;
        }

        if (standing == FactionStanding.HOSTILE || standing == FactionStanding.HATED) {
            return "Você não é bem-vindo neste território.";
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Getters de subsistemas (para leitura externa)

    public FactionRelationManager getRelations()   { return relations; }
    public PlayerSuspicionTracker getSuspicion()    { return suspicion; }
    public TerritoryTracker       getTerritories()  { return territories; }
    public RaidManager            getRaids()        { return raids; }
    public FactionWarManager      getWar()          { return war; }
    public GoblinLeaderBrain      getLeaderBrain()  { return leaderBrain; }
}
