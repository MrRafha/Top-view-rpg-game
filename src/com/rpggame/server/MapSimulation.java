package com.rpggame.server;

import com.rpggame.entities.Enemy;
import com.rpggame.entities.GoblinFamily;
import com.rpggame.entities.Structure;

import java.util.ArrayList;

/**
 * Mantém o estado persistente de simulação de um único mapa.
 *
 * Esta classe é criada uma vez por mapId e nunca destruída enquanto o mundo
 * existir — inimigos, famílias e estruturas sobrevivem a trocas de mapa do
 * jogador. Quando o jogador retorna, o EnemyManager carrega este snapshot de
 * volta ao invés de reinicializar do zero.
 *
 * A simulação de background (ticks reduzidos quando sem jogadores) é
 * coordenada pelo ServerLoop via SimulationClock.
 */
public class MapSimulation {

  private final String mapId;

  // Estado completo dos inimigos deste mapa
  private final ArrayList<Enemy> enemies;
  private final ArrayList<GoblinFamily> goblinFamilies;
  private final ArrayList<Structure> structures;

  // Flag de controle para saber se as famílias já foram inicializadas
  // (inicialização ocorre uma única vez por mapa por sessão de jogo)
  private boolean familiesInitialized = false;

  // Contadores de respawn persistidos para continuar onde pararam
  private int respawnTimer = 0;
  private int familyRespawnTimer = 0;

  // Clock de simulação: define se roda a 20 TPS ou 4 TPS (background)
  private final SimulationClock clock;

  public MapSimulation(String mapId) {
    this.mapId = mapId;
    this.enemies = new ArrayList<>();
    this.goblinFamilies = new ArrayList<>();
    this.structures = new ArrayList<>();
    this.clock = new SimulationClock();
  }

  // ---- Accessors ----

  public String getMapId() {
    return mapId;
  }

  public ArrayList<Enemy> getEnemies() {
    return enemies;
  }

  public ArrayList<GoblinFamily> getGoblinFamilies() {
    return goblinFamilies;
  }

  public ArrayList<Structure> getStructures() {
    return structures;
  }

  public boolean isFamiliesInitialized() {
    return familiesInitialized;
  }

  public void setFamiliesInitialized(boolean familiesInitialized) {
    this.familiesInitialized = familiesInitialized;
  }

  public int getRespawnTimer() {
    return respawnTimer;
  }

  public void setRespawnTimer(int respawnTimer) {
    this.respawnTimer = respawnTimer;
  }

  public int getFamilyRespawnTimer() {
    return familyRespawnTimer;
  }

  public void setFamilyRespawnTimer(int familyRespawnTimer) {
    this.familyRespawnTimer = familyRespawnTimer;
  }

  public SimulationClock getClock() {
    return clock;
  }

  /**
   * Marca se o jogador está neste mapa agora — afeta o tick rate via SimulationClock.
   */
  public void setHasActivePlayers(boolean active) {
    clock.setHasActivePlayers(active);
  }

  public boolean hasActivePlayers() {
    return clock.hasActivePlayers();
  }

  /**
   * Snapshot rápido para diagnóstico e logs.
   */
  @Override
  public String toString() {
    return "MapSimulation{mapId='" + mapId + "'"
        + ", enemies=" + enemies.size()
        + ", families=" + goblinFamilies.size()
        + ", initialized=" + familiesInitialized
        + ", activePlayers=" + hasActivePlayers()
        + ", tps=" + clock.getCurrentTps()
        + "}";
  }
}
