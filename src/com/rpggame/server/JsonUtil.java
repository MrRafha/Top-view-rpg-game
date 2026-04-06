package com.rpggame.server;

import com.rpggame.shared.InputPacket;
import com.rpggame.shared.WorldSnapshot;

import java.util.List;

/**
 * Serializacao/desserializacao JSON minimalista sem dependencias externas.
 *
 * Cobre apenas os dois tipos trocados pela rede: WorldSnapshot (servidor->cliente)
 * e InputPacket (cliente->servidor). Nao e um parser JSON generico.
 *
 * Formato gerado e compativel com o parser interno — os dois lados devem
 * usar esta classe para garantir consistencia.
 */
public final class JsonUtil {

  private JsonUtil() {}

  // ============================================================
  // WorldSnapshot -> JSON
  // ============================================================

  public static String toJson(WorldSnapshot s) {
    if (s == null) return "null";
    StringBuilder b = new StringBuilder(512);
    b.append("{");
    b.append("\"tick\":").append(s.getTick()).append(',');
    b.append("\"timestampNanos\":").append(s.getTimestampNanos()).append(',');
    b.append("\"mapId\":").append(str(s.getMapId())).append(',');
    b.append("\"players\":").append(playersJson(s.getPlayers())).append(',');
    b.append("\"enemies\":").append(enemiesJson(s.getEnemies())).append(',');
    b.append("\"projectiles\":").append(projectilesJson(s.getProjectiles())).append(',');
    b.append("\"npcs\":").append(npcsJson(s.getNpcs())).append(',');
    b.append("\"chests\":").append(chestsJson(s.getChests())).append(',');
    b.append("\"factionStatus\":").append(factionJson(s.getFactionStatus())).append(',');
    b.append("\"backgroundMaps\":").append(bgMapsJson(s.getBackgroundMaps())).append(',');
    b.append("\"events\":").append(eventsJson(s.getEvents()));
    b.append("}");
    return b.toString();
  }

  private static String playersJson(List<WorldSnapshot.SnapshotPlayer> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotPlayer p = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"id\":").append(str(p.getId())).append(',');
      b.append("\"x\":").append(p.getX()).append(',');
      b.append("\"y\":").append(p.getY()).append(',');
      b.append("\"hp\":").append(p.getHp()).append(',');
      b.append("\"maxHp\":").append(p.getMaxHp()).append(',');
      b.append("\"mana\":").append(p.getMana()).append(',');
      b.append("\"maxMana\":").append(p.getMaxMana()).append(',');
      b.append("\"animState\":").append(str(p.getAnimState())).append(',');
      b.append("\"facingLeft\":").append(p.isFacingLeft()).append(',');
      b.append("\"playerClass\":").append(str(p.getPlayerClass())).append('}');
    }
    return b.append(']').toString();
  }

  private static String enemiesJson(List<WorldSnapshot.SnapshotEnemy> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotEnemy e = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"id\":").append(str(e.getId())).append(',');
      b.append("\"type\":").append(str(e.getType())).append(',');
      b.append("\"x\":").append(e.getX()).append(',');
      b.append("\"y\":").append(e.getY()).append(',');
      b.append("\"hp\":").append(e.getHp()).append(',');
      b.append("\"maxHp\":").append(e.getMaxHp()).append(',');
      b.append("\"aiState\":").append(str(e.getAiState())).append(',');
      b.append("\"spritePath\":").append(str(e.getSpritePath())).append(',');
      b.append("\"width\":").append(e.getWidth()).append(',');
      b.append("\"height\":").append(e.getHeight()).append('}');
    }
    return b.append(']').toString();
  }

  private static String projectilesJson(List<WorldSnapshot.SnapshotProjectile> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotProjectile p = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"id\":").append(str(p.getId())).append(',');
      b.append("\"x\":").append(p.getX()).append(',');
      b.append("\"y\":").append(p.getY()).append(',');
      b.append("\"direction\":").append(p.getDirection()).append('}');
    }
    return b.append(']').toString();
  }

  private static String npcsJson(List<WorldSnapshot.SnapshotNpc> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotNpc n = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"id\":").append(str(n.getId())).append(',');
      b.append("\"x\":").append(n.getX()).append(',');
      b.append("\"y\":").append(n.getY()).append(',');
      b.append("\"hasQuest\":").append(n.hasQuest()).append(',');
      b.append("\"spritePath\":").append(str(n.getSpritePath())).append(',');
      b.append("\"width\":").append(n.getWidth()).append(',');
      b.append("\"height\":").append(n.getHeight()).append('}');
    }
    return b.append(']').toString();
  }

  private static String chestsJson(List<WorldSnapshot.SnapshotChest> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotChest c = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"id\":").append(str(c.getId())).append(',');
      b.append("\"x\":").append(c.getX()).append(',');
      b.append("\"y\":").append(c.getY()).append(',');
      b.append("\"open\":").append(c.isOpen()).append('}');
    }
    return b.append(']').toString();
  }

  private static String factionJson(WorldSnapshot.SnapshotFactionSummary f) {
    if (f == null) return "null";
    return "{\"goblinReputation\":" + f.getGoblinReputation()
        + ",\"humanReputation\":" + f.getHumanReputation()
        + ",\"goblinSuspicion\":" + f.getGoblinSuspicion()
        + ",\"currentMapOwner\":" + str(f.getCurrentMapOwner())
        + ",\"mapContested\":" + f.isMapContested()
        + ",\"raidActive\":" + f.isRaidActive() + "}";
  }

  private static String bgMapsJson(List<WorldSnapshot.SnapshotMapBackground> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotMapBackground m = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"mapId\":").append(str(m.getMapId())).append(',');
      b.append("\"aliveEnemies\":").append(m.getAliveEnemies()).append(',');
      b.append("\"contested\":").append(m.isContested()).append(',');
      b.append("\"owner\":").append(str(m.getOwner())).append(',');
      b.append("\"raidActive\":").append(m.isRaidActive()).append('}');
    }
    return b.append(']').toString();
  }

  private static String eventsJson(List<WorldSnapshot.SnapshotEvent> list) {
    if (list == null || list.isEmpty()) return "[]";
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      WorldSnapshot.SnapshotEvent e = list.get(i);
      if (i > 0) b.append(',');
      b.append("{\"type\":").append(str(e.getType())).append(',');
      b.append("\"data\":").append(str(e.getData())).append('}');
    }
    return b.append(']').toString();
  }

  // ============================================================
  // JSON -> WorldSnapshot
  // ============================================================

  public static WorldSnapshot snapshotFromJson(String json) {
    if (json == null || json.equals("null")) return null;
    SimpleJsonReader r = new SimpleJsonReader(json);
    long tick = r.readLong("tick");
    long tsNanos = r.readLong("timestampNanos");
    String mapId = r.readString("mapId");

    List<WorldSnapshot.SnapshotPlayer> players = r.readPlayerList("players");
    List<WorldSnapshot.SnapshotEnemy> enemies = r.readEnemyList("enemies");
    List<WorldSnapshot.SnapshotProjectile> projectiles = r.readProjectileList("projectiles");
    List<WorldSnapshot.SnapshotNpc> npcs = r.readNpcList("npcs");
    List<WorldSnapshot.SnapshotChest> chests = r.readChestList("chests");
    WorldSnapshot.SnapshotFactionSummary faction = r.readFaction("factionStatus");
    List<WorldSnapshot.SnapshotMapBackground> bgMaps = r.readBgMapList("backgroundMaps");
    List<WorldSnapshot.SnapshotEvent> events = r.readEventList("events");

    return new WorldSnapshot(tick, tsNanos, mapId, players, enemies,
        projectiles, npcs, chests, faction, bgMaps, events);
  }

  // ============================================================
  // InputPacket -> JSON
  // ============================================================

  public static String toJson(InputPacket p) {
    if (p == null) return "null";
    return "{\"playerId\":" + str(p.getPlayerId())
        + ",\"clientFrame\":" + p.getClientFrame()
        + ",\"up\":" + p.isUp()
        + ",\"down\":" + p.isDown()
        + ",\"left\":" + p.isLeft()
        + ",\"right\":" + p.isRight()
        + ",\"attack\":" + p.isAttack()
        + ",\"skill1\":" + p.isSkill1()
        + ",\"skill2\":" + p.isSkill2()
        + ",\"skill3\":" + p.isSkill3()
        + ",\"skill4\":" + p.isSkill4()
        + ",\"interact\":" + p.isInteract()
        + ",\"mouseX\":" + p.getMouseX()
        + ",\"mouseY\":" + p.getMouseY() + "}";
  }

  // ============================================================
  // JSON -> InputPacket
  // ============================================================

  public static InputPacket inputPacketFromJson(String json) {
    if (json == null || json.equals("null")) return null;
    SimpleJsonReader r = new SimpleJsonReader(json);
    return InputPacket.builder(r.readString("playerId"), r.readLong("clientFrame"))
        .up(r.readBool("up"))
        .down(r.readBool("down"))
        .left(r.readBool("left"))
        .right(r.readBool("right"))
        .attack(r.readBool("attack"))
        .skill1(r.readBool("skill1"))
        .skill2(r.readBool("skill2"))
        .skill3(r.readBool("skill3"))
        .skill4(r.readBool("skill4"))
        .interact(r.readBool("interact"))
        .mouse(r.readFloat("mouseX"), r.readFloat("mouseY"))
        .build();
  }

  // ============================================================
  // Handshake DTOs (simples, apenas para ServerNetwork/ClientNetwork)
  // ============================================================

  public static String handshakeRequestJson(String playerId, String playerClass) {
    return "{\"playerId\":" + str(playerId) + ",\"playerClass\":" + str(playerClass) + "}";
  }

  /** Retorna { "ok": true/false, "assignedPlayerId": "...", "error": "..." } */
  public static String handshakeResponseJson(boolean ok, String assignedPlayerId, String error) {
    return "{\"ok\":" + ok
        + ",\"assignedPlayerId\":" + str(assignedPlayerId)
        + ",\"error\":" + str(error) + "}";
  }

  /** Extrai campo "ok" de uma resposta de handshake. */
  public static boolean parseHandshakeOk(String json) {
    return json != null && json.contains("\"ok\":true");
  }

  /** Extrai campo "assignedPlayerId" de uma resposta de handshake. */
  public static String parseAssignedPlayerId(String json) {
    return new SimpleJsonReader(json).readString("assignedPlayerId");
  }

  /** Extrai campo "playerId" de uma requisicao de handshake. */
  public static String parseHandshakePlayerId(String json) {
    return new SimpleJsonReader(json).readString("playerId");
  }

  /** Extrai campo "playerClass" de uma requisicao de handshake. */
  public static String parseHandshakePlayerClass(String json) {
    return new SimpleJsonReader(json).readString("playerClass");
  }

  // ============================================================
  // Helpers internos
  // ============================================================

  /** Envolve uma String em aspas JSON, ou retorna "null" se nula. */
  static String str(String s) {
    if (s == null) return "null";
    // Escape basico — suficiente para caminhos de sprite e ids sem caracteres especiais
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  // ============================================================
  // Parser JSON minimalista
  // ============================================================

  /**
   * Leitor JSON minimalista baseado em busca de chaves por nome.
   * Suporta apenas o subconjunto necessario para WorldSnapshot e InputPacket.
   * Nao e um parser completo — nao lida com JSON aninhado arbitrario via cursor.
   */
  static final class SimpleJsonReader {
    private final String json;

    SimpleJsonReader(String json) {
      this.json = json;
    }

    String readString(String key) {
      String k = "\"" + key + "\":";
      int idx = json.indexOf(k);
      if (idx < 0) return null;
      int start = idx + k.length();
      if (start >= json.length()) return null;
      char first = json.charAt(start);
      if (first == 'n') return null; // null
      if (first != '"') return null;
      int end = json.indexOf('"', start + 1);
      // Handle escaped quotes
      while (end > 0 && json.charAt(end - 1) == '\\') {
        end = json.indexOf('"', end + 1);
      }
      if (end < 0) return null;
      return json.substring(start + 1, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    long readLong(String key) {
      String raw = readRaw(key);
      if (raw == null) return 0L;
      try { return Long.parseLong(raw.trim()); } catch (NumberFormatException e) { return 0L; }
    }

    int readInt(String key) {
      String raw = readRaw(key);
      if (raw == null) return 0;
      try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 0; }
    }

    double readDouble(String key) {
      String raw = readRaw(key);
      if (raw == null) return 0.0;
      try { return Double.parseDouble(raw.trim()); } catch (NumberFormatException e) { return 0.0; }
    }

    float readFloat(String key) {
      return (float) readDouble(key);
    }

    boolean readBool(String key) {
      String raw = readRaw(key);
      return "true".equals(raw != null ? raw.trim() : null);
    }

    private String readRaw(String key) {
      String k = "\"" + key + "\":";
      int idx = json.indexOf(k);
      if (idx < 0) return null;
      int start = idx + k.length();
      if (start >= json.length()) return null;
      // Encontrar fim do valor (proximo , } ou ])
      int end = start;
      while (end < json.length()) {
        char c = json.charAt(end);
        if (c == ',' || c == '}' || c == ']') break;
        end++;
      }
      return json.substring(start, end);
    }

    // --- Leitores de listas ---

    java.util.List<WorldSnapshot.SnapshotPlayer> readPlayerList(String key) {
      java.util.List<WorldSnapshot.SnapshotPlayer> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotPlayer(
            r.readString("id"), r.readDouble("x"), r.readDouble("y"),
            r.readInt("hp"), r.readInt("maxHp"), r.readInt("mana"), r.readInt("maxMana"),
            r.readString("animState"), r.readBool("facingLeft"), r.readString("playerClass")));
      }
      return list;
    }

    java.util.List<WorldSnapshot.SnapshotEnemy> readEnemyList(String key) {
      java.util.List<WorldSnapshot.SnapshotEnemy> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotEnemy(
            r.readString("id"), r.readString("type"),
            r.readDouble("x"), r.readDouble("y"),
            r.readInt("hp"), r.readInt("maxHp"),
            r.readString("aiState"), r.readString("spritePath"),
            r.readInt("width"), r.readInt("height")));
      }
      return list;
    }

    java.util.List<WorldSnapshot.SnapshotProjectile> readProjectileList(String key) {
      java.util.List<WorldSnapshot.SnapshotProjectile> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotProjectile(
            r.readString("id"), r.readDouble("x"), r.readDouble("y"), r.readDouble("direction")));
      }
      return list;
    }

    java.util.List<WorldSnapshot.SnapshotNpc> readNpcList(String key) {
      java.util.List<WorldSnapshot.SnapshotNpc> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotNpc(
            r.readString("id"), r.readDouble("x"), r.readDouble("y"),
            r.readBool("hasQuest"), r.readString("spritePath"),
            r.readInt("width"), r.readInt("height")));
      }
      return list;
    }

    java.util.List<WorldSnapshot.SnapshotChest> readChestList(String key) {
      java.util.List<WorldSnapshot.SnapshotChest> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotChest(
            r.readString("id"), r.readDouble("x"), r.readDouble("y"), r.readBool("open")));
      }
      return list;
    }

    WorldSnapshot.SnapshotFactionSummary readFaction(String key) {
      String k = "\"" + key + "\":";
      int idx = json.indexOf(k);
      if (idx < 0) return null;
      int start = json.indexOf('{', idx + k.length());
      if (start < 0) return null;
      int end = findMatchingBrace(json, start);
      if (end < 0) return null;
      SimpleJsonReader r = new SimpleJsonReader(json.substring(start, end + 1));
      return new WorldSnapshot.SnapshotFactionSummary(
          r.readInt("goblinReputation"), r.readInt("humanReputation"),
          r.readInt("goblinSuspicion"), r.readString("currentMapOwner"),
          r.readBool("mapContested"), r.readBool("raidActive"));
    }

    java.util.List<WorldSnapshot.SnapshotMapBackground> readBgMapList(String key) {
      java.util.List<WorldSnapshot.SnapshotMapBackground> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotMapBackground(
            r.readString("mapId"), r.readInt("aliveEnemies"),
            r.readBool("contested"), r.readString("owner"), r.readBool("raidActive")));
      }
      return list;
    }

    java.util.List<WorldSnapshot.SnapshotEvent> readEventList(String key) {
      java.util.List<WorldSnapshot.SnapshotEvent> list = new java.util.ArrayList<>();
      for (String obj : extractArrayObjects(key)) {
        SimpleJsonReader r = new SimpleJsonReader(obj);
        list.add(new WorldSnapshot.SnapshotEvent(r.readString("type"), r.readString("data")));
      }
      return list;
    }

    /** Extrai objetos JSON {...} dentro do array identificado pela chave. */
    private java.util.List<String> extractArrayObjects(String key) {
      java.util.List<String> result = new java.util.ArrayList<>();
      String k = "\"" + key + "\":";
      int idx = json.indexOf(k);
      if (idx < 0) return result;
      int arrStart = json.indexOf('[', idx + k.length());
      if (arrStart < 0) return result;
      int arrEnd = findMatchingBracket(json, arrStart);
      if (arrEnd < 0) return result;
      String arr = json.substring(arrStart + 1, arrEnd);
      int pos = 0;
      while (pos < arr.length()) {
        int objStart = arr.indexOf('{', pos);
        if (objStart < 0) break;
        int objEnd = findMatchingBrace(arr, objStart);
        if (objEnd < 0) break;
        result.add(arr.substring(objStart, objEnd + 1));
        pos = objEnd + 1;
      }
      return result;
    }

    private static int findMatchingBrace(String s, int open) {
      int depth = 0;
      for (int i = open; i < s.length(); i++) {
        if (s.charAt(i) == '{') depth++;
        else if (s.charAt(i) == '}') { depth--; if (depth == 0) return i; }
      }
      return -1;
    }

    private static int findMatchingBracket(String s, int open) {
      int depth = 0;
      for (int i = open; i < s.length(); i++) {
        if (s.charAt(i) == '[') depth++;
        else if (s.charAt(i) == ']') { depth--; if (depth == 0) return i; }
      }
      return -1;
    }
  }
}
