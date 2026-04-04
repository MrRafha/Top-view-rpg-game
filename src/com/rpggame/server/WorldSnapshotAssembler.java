package com.rpggame.server;

import com.rpggame.entities.Enemy;
import com.rpggame.entities.Chest;
import com.rpggame.entities.Player;
import com.rpggame.entities.Projectile;
import com.rpggame.factions.FactionSystem;
import com.rpggame.factions.FactionType;
import com.rpggame.npcs.NPC;
import com.rpggame.shared.WorldSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monta WorldSnapshot imutavel com base no estado atual da simulacao.
 */
public class WorldSnapshotAssembler {

  public WorldSnapshot assemble(
      long tick,
      String activeMapId,
      WorldState worldState,
      Player activePlayer,
      FactionSystem factionSystem,
      List<NPC> activeNpcs,
      List<Chest> activeChests) {

    if (activeMapId == null || worldState == null) {
      return new WorldSnapshot(
          tick,
          System.nanoTime(),
          "unknown",
          Collections.emptyList(),
          Collections.emptyList(),
          Collections.emptyList(),
          Collections.emptyList(),
          Collections.emptyList(),
          null,
          Collections.emptyList(),
          Collections.emptyList());
    }

    MapSimulation activeSimulation = worldState.get(activeMapId);

    List<WorldSnapshot.SnapshotPlayer> players = buildPlayers(activePlayer);
    List<WorldSnapshot.SnapshotEnemy> enemies = buildEnemies(activeSimulation, activeMapId);
    List<WorldSnapshot.SnapshotProjectile> projectiles = buildProjectiles(activePlayer, activeMapId);
    List<WorldSnapshot.SnapshotNpc> npcs = buildNpcs(activeNpcs, activeMapId);
    List<WorldSnapshot.SnapshotChest> chests = buildChests(activeChests, activeMapId);
    WorldSnapshot.SnapshotFactionSummary factionSummary = buildFactionSummary(factionSystem, activeMapId);
    List<WorldSnapshot.SnapshotMapBackground> background = buildBackgroundMaps(worldState, factionSystem, activeMapId);

    return new WorldSnapshot(
        tick,
        System.nanoTime(),
        activeMapId,
        players,
        enemies,
        projectiles,
        npcs,
        chests,
        factionSummary,
        background,
        Collections.emptyList());
  }

  private List<WorldSnapshot.SnapshotPlayer> buildPlayers(Player activePlayer) {
    if (activePlayer == null) {
      return Collections.emptyList();
    }

    List<WorldSnapshot.SnapshotPlayer> players = new ArrayList<>(1);
    players.add(new WorldSnapshot.SnapshotPlayer(
        "player-1",
        activePlayer.getX(),
        activePlayer.getY(),
        activePlayer.getCurrentHealth(),
        activePlayer.getMaxHealth(),
        activePlayer.getCurrentMana(),
        activePlayer.getMaxMana(),
        activePlayer.isMoving() ? "MOVING" : "IDLE",
        activePlayer.isFacingLeft(),
        activePlayer.getPlayerClass()));
    return players;
  }

  private List<WorldSnapshot.SnapshotEnemy> buildEnemies(MapSimulation simulation, String mapId) {
    if (simulation == null) {
      return Collections.emptyList();
    }

    ArrayList<Enemy> copy;
    synchronized (simulation) {
      copy = new ArrayList<>(simulation.getEnemies());
    }

    List<WorldSnapshot.SnapshotEnemy> enemies = new ArrayList<>(copy.size());
    for (int i = 0; i < copy.size(); i++) {
      Enemy enemy = copy.get(i);
      if (!enemy.isAlive()) {
        continue;
      }

      enemies.add(new WorldSnapshot.SnapshotEnemy(
          mapId + "-enemy-" + i,
          enemy.getEnemyTypeName(),
          enemy.getX(),
          enemy.getY(),
          enemy.getCurrentHealth(),
          enemy.getMaxHealth(),
          enemy.getAiStateName(),
          enemy.getSpritePath(),
          enemy.getWidth(),
          enemy.getHeight()));
    }

    return enemies;
  }

  private List<WorldSnapshot.SnapshotProjectile> buildProjectiles(Player activePlayer, String mapId) {
    if (activePlayer == null) {
      return Collections.emptyList();
    }

    ArrayList<Projectile> projectilesCopy = new ArrayList<>(activePlayer.getProjectiles());
    List<WorldSnapshot.SnapshotProjectile> projectiles = new ArrayList<>(projectilesCopy.size());

    for (int i = 0; i < projectilesCopy.size(); i++) {
      Projectile projectile = projectilesCopy.get(i);
      if (!projectile.isActive()) {
        continue;
      }
      double direction = Math.atan2(projectile.getDy(), projectile.getDx());
      projectiles.add(new WorldSnapshot.SnapshotProjectile(
          mapId + "-projectile-" + i,
          projectile.getX(),
          projectile.getY(),
          direction));
    }

    return projectiles;
  }

  private List<WorldSnapshot.SnapshotNpc> buildNpcs(List<NPC> activeNpcs, String mapId) {
    if (activeNpcs == null || activeNpcs.isEmpty()) {
      return Collections.emptyList();
    }

    ArrayList<NPC> npcsCopy = new ArrayList<>(activeNpcs);
    List<WorldSnapshot.SnapshotNpc> npcs = new ArrayList<>(npcsCopy.size());

    for (int i = 0; i < npcsCopy.size(); i++) {
      NPC npc = npcsCopy.get(i);
      npcs.add(new WorldSnapshot.SnapshotNpc(
          mapId + "-npc-" + i,
          npc.getX(),
          npc.getY(),
          npc.hasQuestIndicator(),
          npc.getSpritePath(),
          npc.getWidth(),
          npc.getHeight()));
    }

    return npcs;
  }

  private List<WorldSnapshot.SnapshotChest> buildChests(List<Chest> activeChests, String mapId) {
    if (activeChests == null || activeChests.isEmpty()) {
      return Collections.emptyList();
    }

    ArrayList<Chest> chestsCopy = new ArrayList<>(activeChests);
    List<WorldSnapshot.SnapshotChest> chests = new ArrayList<>(chestsCopy.size());

    for (int i = 0; i < chestsCopy.size(); i++) {
      Chest chest = chestsCopy.get(i);
      chests.add(new WorldSnapshot.SnapshotChest(
          mapId + "-chest-" + i,
          chest.getX(),
          chest.getY(),
          chest.isOpened()));
    }

    return chests;
  }

  private WorldSnapshot.SnapshotFactionSummary buildFactionSummary(FactionSystem factionSystem, String mapId) {
    if (factionSystem == null || mapId == null) {
      return null;
    }

    int goblinRep = factionSystem.getRelations().getReputation(FactionType.GOBLINS);
    int humanRep = factionSystem.getRelations().getReputation(FactionType.HUMANS);
    int goblinSuspicion = factionSystem.getSuspicion().getValue(FactionType.GOBLINS);
    String owner = factionSystem.getTerritories().getOwner(mapId).name();
    boolean contested = factionSystem.getTerritories().isContested(mapId);
    boolean raidActive = factionSystem.getRaids().isMapUnderRaid(mapId);

    return new WorldSnapshot.SnapshotFactionSummary(
        goblinRep,
        humanRep,
        goblinSuspicion,
        owner,
        contested,
        raidActive);
  }

  private List<WorldSnapshot.SnapshotMapBackground> buildBackgroundMaps(
      WorldState worldState,
      FactionSystem factionSystem,
      String activeMapId) {
    List<WorldSnapshot.SnapshotMapBackground> result = new ArrayList<>();

    for (MapSimulation sim : worldState.getAllSimulations()) {
      if (sim.getMapId().equals(activeMapId)) {
        continue;
      }

      ArrayList<Enemy> enemiesCopy;
      synchronized (sim) {
        enemiesCopy = new ArrayList<>(sim.getEnemies());
      }

      int aliveEnemies = 0;
      for (Enemy enemy : enemiesCopy) {
        if (enemy.isAlive()) {
          aliveEnemies++;
        }
      }

      String owner = "NEUTRAL";
      boolean contested = false;
      boolean raidActive = false;
      if (factionSystem != null) {
        owner = factionSystem.getTerritories().getOwner(sim.getMapId()).name();
        contested = factionSystem.getTerritories().isContested(sim.getMapId());
        raidActive = factionSystem.getRaids().isMapUnderRaid(sim.getMapId());
      }

      result.add(new WorldSnapshot.SnapshotMapBackground(
          sim.getMapId(),
          aliveEnemies,
          contested,
          owner,
          raidActive));
    }

    return result;
  }
}
