package com.rpggame.shared;

import java.util.Collections;
import java.util.List;

/**
 * Snapshot imutavel do estado do mundo em um tick especifico.
 */
public final class WorldSnapshot {
  private final long tick;
  private final long timestampNanos;
  private final String mapId;
  private final List<SnapshotPlayer> players;
  private final List<SnapshotEnemy> enemies;
  private final List<SnapshotProjectile> projectiles;
  private final List<SnapshotNpc> npcs;
  private final List<SnapshotChest> chests;
  private final SnapshotFactionSummary factionStatus;
  private final List<SnapshotMapBackground> backgroundMaps;
  private final List<SnapshotEvent> events;

  public WorldSnapshot(
      long tick,
      long timestampNanos,
      String mapId,
      List<SnapshotPlayer> players,
      List<SnapshotEnemy> enemies,
      List<SnapshotProjectile> projectiles,
      List<SnapshotNpc> npcs,
      List<SnapshotChest> chests,
      SnapshotFactionSummary factionStatus,
      List<SnapshotMapBackground> backgroundMaps,
      List<SnapshotEvent> events) {
    this.tick = tick;
    this.timestampNanos = timestampNanos;
    this.mapId = mapId;
    this.players = players == null ? Collections.emptyList() : Collections.unmodifiableList(players);
    this.enemies = enemies == null ? Collections.emptyList() : Collections.unmodifiableList(enemies);
    this.projectiles = projectiles == null ? Collections.emptyList() : Collections.unmodifiableList(projectiles);
    this.npcs = npcs == null ? Collections.emptyList() : Collections.unmodifiableList(npcs);
    this.chests = chests == null ? Collections.emptyList() : Collections.unmodifiableList(chests);
    this.factionStatus = factionStatus;
    this.backgroundMaps = backgroundMaps == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(backgroundMaps);
    this.events = events == null ? Collections.emptyList() : Collections.unmodifiableList(events);
  }

  public long getTick() {
    return tick;
  }

  public long getTimestampNanos() {
    return timestampNanos;
  }

  public String getMapId() {
    return mapId;
  }

  public List<SnapshotPlayer> getPlayers() {
    return players;
  }

  public List<SnapshotEnemy> getEnemies() {
    return enemies;
  }

  public List<SnapshotProjectile> getProjectiles() {
    return projectiles;
  }

  public List<SnapshotNpc> getNpcs() {
    return npcs;
  }

  public List<SnapshotChest> getChests() {
    return chests;
  }

  public SnapshotFactionSummary getFactionStatus() {
    return factionStatus;
  }

  public List<SnapshotMapBackground> getBackgroundMaps() {
    return backgroundMaps;
  }

  public List<SnapshotEvent> getEvents() {
    return events;
  }

  public static final class SnapshotPlayer {
    private final String id;
    private final double x;
    private final double y;
    private final int hp;
    private final int maxHp;
    private final int mana;
    private final int maxMana;
    private final String animState;
    private final boolean facingLeft;
    private final String playerClass;

    public SnapshotPlayer(String id, double x, double y, int hp, int maxHp, int mana, int maxMana,
        String animState, boolean facingLeft, String playerClass) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.hp = hp;
      this.maxHp = maxHp;
      this.mana = mana;
      this.maxMana = maxMana;
      this.animState = animState;
      this.facingLeft = facingLeft;
      this.playerClass = playerClass;
    }

    public String getId() {
      return id;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    public int getHp() {
      return hp;
    }

    public int getMaxHp() {
      return maxHp;
    }

    public int getMana() {
      return mana;
    }

    public int getMaxMana() {
      return maxMana;
    }

    public String getAnimState() {
      return animState;
    }

    public boolean isFacingLeft() {
      return facingLeft;
    }

    public String getPlayerClass() {
      return playerClass;
    }
  }

  public static final class SnapshotEnemy {
    private final String id;
    private final String type;
    private final double x;
    private final double y;
    private final int hp;
    private final int maxHp;
    private final String aiState;
    private final String spritePath;
    private final int width;
    private final int height;

    public SnapshotEnemy(String id, String type, double x, double y, int hp, int maxHp, String aiState,
        String spritePath, int width, int height) {
      this.id = id;
      this.type = type;
      this.x = x;
      this.y = y;
      this.hp = hp;
      this.maxHp = maxHp;
      this.aiState = aiState;
      this.spritePath = spritePath;
      this.width = width;
      this.height = height;
    }

    public String getId() {
      return id;
    }

    public String getType() {
      return type;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    public int getHp() {
      return hp;
    }

    public int getMaxHp() {
      return maxHp;
    }

    public String getAiState() {
      return aiState;
    }

    public String getSpritePath() {
      return spritePath;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }
  }

  public static final class SnapshotProjectile {
    private final String id;
    private final double x;
    private final double y;
    private final double direction;

    public SnapshotProjectile(String id, double x, double y, double direction) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.direction = direction;
    }

    public String getId() {
      return id;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    public double getDirection() {
      return direction;
    }
  }

  public static final class SnapshotNpc {
    private final String id;
    private final double x;
    private final double y;
    private final boolean hasQuest;
    private final String spritePath;
    private final int width;
    private final int height;

    public SnapshotNpc(String id, double x, double y, boolean hasQuest, String spritePath, int width, int height) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.hasQuest = hasQuest;
      this.spritePath = spritePath;
      this.width = width;
      this.height = height;
    }

    public String getId() {
      return id;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    public boolean hasQuest() {
      return hasQuest;
    }

    public String getSpritePath() {
      return spritePath;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }
  }

  public static final class SnapshotChest {
    private final String id;
    private final double x;
    private final double y;
    private final boolean open;

    public SnapshotChest(String id, double x, double y, boolean open) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.open = open;
    }

    public String getId() {
      return id;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }

    public boolean isOpen() {
      return open;
    }
  }

  public static final class SnapshotFactionSummary {
    private final int goblinReputation;
    private final int humanReputation;
    private final int goblinSuspicion;
    private final String currentMapOwner;
    private final boolean mapContested;
    private final boolean raidActive;

    public SnapshotFactionSummary(int goblinReputation, int humanReputation, int goblinSuspicion,
        String currentMapOwner, boolean mapContested, boolean raidActive) {
      this.goblinReputation = goblinReputation;
      this.humanReputation = humanReputation;
      this.goblinSuspicion = goblinSuspicion;
      this.currentMapOwner = currentMapOwner;
      this.mapContested = mapContested;
      this.raidActive = raidActive;
    }

    public int getGoblinReputation() {
      return goblinReputation;
    }

    public int getHumanReputation() {
      return humanReputation;
    }

    public int getGoblinSuspicion() {
      return goblinSuspicion;
    }

    public String getCurrentMapOwner() {
      return currentMapOwner;
    }

    public boolean isMapContested() {
      return mapContested;
    }

    public boolean isRaidActive() {
      return raidActive;
    }
  }

  public static final class SnapshotMapBackground {
    private final String mapId;
    private final int aliveEnemies;
    private final boolean contested;
    private final String owner;
    private final boolean raidActive;

    public SnapshotMapBackground(String mapId, int aliveEnemies, boolean contested, String owner, boolean raidActive) {
      this.mapId = mapId;
      this.aliveEnemies = aliveEnemies;
      this.contested = contested;
      this.owner = owner;
      this.raidActive = raidActive;
    }

    public String getMapId() {
      return mapId;
    }

    public int getAliveEnemies() {
      return aliveEnemies;
    }

    public boolean isContested() {
      return contested;
    }

    public String getOwner() {
      return owner;
    }

    public boolean isRaidActive() {
      return raidActive;
    }
  }

  public static final class SnapshotEvent {
    private final String type;
    private final String data;

    public SnapshotEvent(String type, String data) {
      this.type = type;
      this.data = data;
    }

    public String getType() {
      return type;
    }

    public String getData() {
      return data;
    }
  }
}
