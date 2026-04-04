package com.rpggame.server;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro global e persistente de todos os mapas do mundo.
 *
 * Cada mapa possui exatamente uma {@link MapSimulation} que é criada na
 * primeira vez que o mapa é necessário e nunca destruída durante a sessão.
 * Isso garante que inimigos, famílias e timers sobrevivam a trocas de mapa
 * do jogador.
 *
 * Uso típico em GamePanel:
 * 
 * <pre>
 * // Ao iniciar o jogo
 * worldState = new WorldState();
 *
 * // Ao entrar em um mapa
 * MapSimulation sim = worldState.getOrCreate(mapId);
 * enemyManager.loadFromSimulation(sim);
 * sim.setHasActivePlayers(true);
 *
 * // Ao sair do mapa
 * enemyManager.saveToSimulation(sim);
 * sim.setHasActivePlayers(false);
 * </pre>
 */
public class WorldState {

  // LinkedHashMap preserva a ordem de inserção — útil para debug/logs
  private final Map<String, MapSimulation> simulations = new LinkedHashMap<>();

  /**
   * Retorna a simulação existente para o mapa, ou cria uma nova se ainda não
   * existir.
   */
  public synchronized MapSimulation getOrCreate(String mapId) {
    return simulations.computeIfAbsent(mapId, MapSimulation::new);
  }

  /**
   * Retorna a simulação de um mapa sem criá-la caso não exista.
   * Retorna null se o mapa ainda não foi visitado/registrado.
   */
  public synchronized MapSimulation get(String mapId) {
    return simulations.get(mapId);
  }

  /**
   * Verifica se já existe um estado persistido para o mapa (ou seja, o mapa
   * já foi visitado pelo menos uma vez nesta sessão).
   */
  public synchronized boolean hasSimulation(String mapId) {
    return simulations.containsKey(mapId);
  }

  /**
   * Retorna todas as simulações registradas (somente leitura).
   * Usado pelo ServerLoop para iterar e fazer tick de todos os mapas.
   */
  public synchronized Collection<MapSimulation> getAllSimulations() {
    return Collections.unmodifiableCollection(new ArrayList<>(simulations.values()));
  }

  /**
   * Marca todos os mapas como sem jogadores ativos.
   * Chamado antes de marcar o mapa atual como ativo, para garantir que
   * apenas um mapa por jogador esteja em modo ativo por vez.
   */
  public synchronized void deactivateAll() {
    for (MapSimulation sim : simulations.values()) {
      sim.setHasActivePlayers(false);
    }
  }

  /**
   * Número de mapas já registrados/visitados nesta sessão.
   */
  public synchronized int size() {
    return simulations.size();
  }

  @Override
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder("WorldState{\n");
    for (MapSimulation sim : simulations.values()) {
      sb.append("  ").append(sim).append("\n");
    }
    sb.append("}");
    return sb.toString();
  }
}
