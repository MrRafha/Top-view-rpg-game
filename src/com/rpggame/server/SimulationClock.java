package com.rpggame.server;

/**
 * Controla o tick rate de simulação por mapa.
 *
 * Mapas com jogadores ativos rodam a ACTIVE_TPS (20).
 * Mapas sem jogadores rodam a BACKGROUND_TPS (4) para manter raids,
 * timers e progressão territorial sem desperdiçar ciclos de CPU.
 */
public class SimulationClock {

  public static final int ACTIVE_TPS = 20;
  public static final int BACKGROUND_TPS = 4;

  // Acumulador de tempo em nanosegundos para o mapa
  private long accumNanos = 0;
  private boolean hasActivePlayers = false;

  /**
   * Avança o acumulador de tempo e informa quantos ticks devem ser executados.
   *
   * @param deltaNanos tempo decorrido desde a última chamada, em nanosegundos
   * @return número de ticks de simulação que devem ocorrer neste intervalo
   */
  public int advance(long deltaNanos) {
    accumNanos += deltaNanos;

    int tps = hasActivePlayers ? ACTIVE_TPS : BACKGROUND_TPS;
    long nanosPerTick = 1_000_000_000L / tps;

    int ticks = 0;
    while (accumNanos >= nanosPerTick) {
      accumNanos -= nanosPerTick;
      ticks++;
    }
    return ticks;
  }

  /**
   * Marca se há jogadores ativos neste mapa.
   * Ao marcar true, o clock migra para ACTIVE_TPS no próximo advance().
   */
  public void setHasActivePlayers(boolean hasActivePlayers) {
    if (this.hasActivePlayers != hasActivePlayers) {
      // Zera acumulador ao mudar de modo para evitar burst de ticks represados
      accumNanos = 0;
    }
    this.hasActivePlayers = hasActivePlayers;
  }

  public boolean hasActivePlayers() {
    return hasActivePlayers;
  }

  public int getCurrentTps() {
    return hasActivePlayers ? ACTIVE_TPS : BACKGROUND_TPS;
  }
}
