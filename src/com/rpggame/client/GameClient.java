package com.rpggame.client;

import com.rpggame.server.InProcessTransport;
import com.rpggame.shared.WorldSnapshot;

/**
 * Cliente in-process que consome {@link WorldSnapshot} da fila de transporte.
 *
 * Na Fase 4, GameClient roda na mesma JVM que o servidor (in-process).
 * Ele é a camada de abstração que permite ao GamePanel obter snapshots
 * sem acesso direto ao ServerLoop — o mesmo contrato que um cliente TCP
 * usaria na Fase 6.
 *
 * O GamePanel deve substituir:
 *   serverLoop.getLatestSnapshot()
 * por:
 *   gameClient.getLatestSnapshot()
 *
 * Assim, quando TCP for introduzido, apenas GameClient muda — GamePanel
 * continua igual.
 */
public class GameClient {

  private final InProcessTransport transport;

  private WorldSnapshot latestSnapshot;
  private WorldSnapshot previousSnapshot;
  private long latestSnapshotNanos = 0L;

  public GameClient(InProcessTransport transport) {
    this.transport = transport;
  }

  /**
   * Drena a fila de snapshots e retém o mais recente.
   * Deve ser chamado uma vez por frame no game loop do cliente (GamePanel.update).
   */
  public void pollSnapshot() {
    WorldSnapshot next = transport.pollSnapshot();
    if (next != null) {
      previousSnapshot = latestSnapshot;
      latestSnapshot   = next;
      latestSnapshotNanos = System.nanoTime();
    }
  }

  public WorldSnapshot getLatestSnapshot() {
    return latestSnapshot;
  }

  public WorldSnapshot getPreviousSnapshot() {
    return previousSnapshot;
  }

  public long getLatestSnapshotNanos() {
    return latestSnapshotNanos;
  }
}
