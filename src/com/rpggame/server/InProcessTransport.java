package com.rpggame.server;

import com.rpggame.shared.InputPacket;
import com.rpggame.shared.WorldSnapshot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Transporte in-process entre servidor e cliente via BlockingQueue.
 *
 * Substitui o acesso direto a campos volatile entre ServerLoop e GamePanel.
 * Cada sentido de comunicação tem sua própria fila:
 *
 *   Servidor → Cliente : snapshotQueue  (WorldSnapshot por tick)
 *   Cliente  → Servidor: inputQueue     (InputPacket por frame)
 *
 * Capacidades limitadas para evitar acúmulo de snapshots/inputs antigos:
 * - snapshotQueue: 2 slots — o cliente sempre consome o mais recente
 * - inputQueue:    8 slots — o servidor drena tudo antes de cada tick
 *
 * Quando TCP for introduzido na Fase 6, apenas este arquivo muda —
 * ServerLoop e GameClient continuam operando com as mesmas interfaces.
 */
public class InProcessTransport {

  // Servidor → Cliente: apenas os 2 snapshots mais recentes importam
  private final BlockingQueue<WorldSnapshot> snapshotQueue;

  // Cliente → Servidor: fila pequena para inputs, drenada por tick
  private final BlockingQueue<InputPacket> inputQueue;

  public InProcessTransport() {
    this.snapshotQueue = new LinkedBlockingQueue<>(2);
    this.inputQueue    = new LinkedBlockingQueue<>(8);
  }

  // ---- Servidor: publica snapshot ----

  /**
   * Publica snapshot para o cliente. Se a fila estiver cheia (cliente lento),
   * descarta o mais antigo e insere o novo — o cliente sempre vê o estado atual.
   */
  public void publishSnapshot(WorldSnapshot snapshot) {
    if (!snapshotQueue.offer(snapshot)) {
      snapshotQueue.poll(); // descarta o mais antigo
      snapshotQueue.offer(snapshot);
    }
  }

  /**
   * Drena todos os inputs pendentes da fila para o array de destino.
   * Retorna o número de inputs drenados.
   */
  public int drainInputs(java.util.List<InputPacket> destination) {
    return inputQueue.drainTo(destination);
  }

  // ---- Cliente: publica input ----

  /**
   * Envia InputPacket ao servidor. Se a fila estiver cheia, descarta o
   * input mais antigo — o servidor prefere inputs recentes.
   */
  public void publishInput(InputPacket packet) {
    if (!inputQueue.offer(packet)) {
      inputQueue.poll();
      inputQueue.offer(packet);
    }
  }

  /**
   * Retorna o snapshot mais recente disponível, ou null se não houver nenhum.
   * Não bloqueia — o cliente continua renderizando o snapshot anterior se
   * o servidor ainda não produziu um novo.
   */
  public WorldSnapshot pollSnapshot() {
    // Drena a fila e retorna apenas o último
    WorldSnapshot latest = null;
    WorldSnapshot s;
    while ((s = snapshotQueue.poll()) != null) {
      latest = s;
    }
    return latest;
  }
}
