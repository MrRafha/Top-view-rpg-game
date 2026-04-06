package com.rpggame.client;

import com.rpggame.server.InProcessTransport;
import com.rpggame.server.JsonUtil;
import com.rpggame.server.TcpFraming;
import com.rpggame.shared.InputPacket;
import com.rpggame.shared.WorldSnapshot;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.ArrayList;

/**
 * Camada de rede do cliente: conecta ao GameServer via TCP e troca dados.
 *
 * O contrato com o resto do cliente (GameClient, ClientInput) nao muda —
 * ambos continuam usando InProcessTransport normalmente.
 * ClientNetwork e a "ponte" que alimenta/drena esse transporte pela rede:
 *
 *   Servidor --[TCP]--> snapshotReaderLoop --> transport.publishSnapshot()
 *   transport.drainInputs() --> inputWriterLoop --[TCP]--> Servidor
 *
 * Uso:
 *   ClientNetwork net = new ClientNetwork(transport, "player-1", "Warrior");
 *   net.connect("127.0.0.1", 7777);
 *   // ... jogo rodando ...
 *   net.disconnect();
 */
public class ClientNetwork {

  private static final int INPUT_DRAIN_INTERVAL_MS = 5; // ~200 Hz de tentativa

  private final InProcessTransport transport;
  private final String playerId;
  private final String playerClass;

  private volatile Socket socket;
  private volatile boolean running = false;
  private Thread readerThread;
  private Thread writerThread;

  /** Listener opcional para notificar o GamePanel sobre conexao/desconexao. */
  private ConnectionListener connectionListener;

  public ClientNetwork(InProcessTransport transport, String playerId, String playerClass) {
    this.transport = transport;
    this.playerId = playerId;
    this.playerClass = playerClass;
  }

  public void setConnectionListener(ConnectionListener listener) {
    this.connectionListener = listener;
  }

  /**
   * Conecta ao servidor, faz handshake e lanca as threads de I/O.
   * Nao bloqueia — retorna apos lancar as threads.
   *
   * @throws IOException se nao conseguir conectar ou o handshake falhar
   */
  public void connect(String host, int port) throws IOException {
    if (running) return;

    socket = new Socket(host, port);
    socket.setTcpNoDelay(true);
    running = true;

    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();

    // Handshake sincrono antes de lancar as threads
    doHandshake(in, out);

    readerThread = new Thread(() -> snapshotReaderLoop(in), "client-snapshot-reader");
    writerThread = new Thread(() -> inputWriterLoop(out), "client-input-writer");
    readerThread.setDaemon(true);
    writerThread.setDaemon(true);
    readerThread.start();
    writerThread.start();

    System.out.println("[ClientNetwork] Conectado a " + host + ":" + port + " como " + playerId);
  }

  /**
   * Encerra a conexao: para as threads e fecha o socket.
   * Desbloqueia read() bloqueados fechando o socket.
   */
  public void disconnect() {
    running = false;
    try {
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException ignored) {}
    if (readerThread != null) readerThread.interrupt();
    if (writerThread != null) writerThread.interrupt();
    System.out.println("[ClientNetwork] Desconectado.");
  }

  public boolean isConnected() {
    return running && socket != null && socket.isConnected() && !socket.isClosed();
  }

  // ---- Handshake ----

  private void doHandshake(InputStream in, OutputStream out) throws IOException {
    String reqJson = JsonUtil.handshakeRequestJson(playerId, playerClass);
    TcpFraming.writeMessage(out, reqJson);

    String respJson = TcpFraming.readMessage(in);
    if (!JsonUtil.parseHandshakeOk(respJson)) {
      running = false;
      throw new IOException("[ClientNetwork] Handshake recusado pelo servidor: " + respJson);
    }
    String assigned = JsonUtil.parseAssignedPlayerId(respJson);
    System.out.println("[ClientNetwork] Handshake OK. playerId atribuido: " + assigned);
  }

  // ---- Loop de leitura de snapshots (servidor -> cliente) ----

  private void snapshotReaderLoop(InputStream in) {
    try {
      while (running && !Thread.currentThread().isInterrupted()) {
        String json = TcpFraming.readMessage(in);
        WorldSnapshot snapshot = JsonUtil.snapshotFromJson(json);
        if (snapshot != null) {
          transport.publishSnapshot(snapshot);
        }
      }
    } catch (EOFException e) {
      // Servidor encerrou a conexao normalmente
      System.out.println("[ClientNetwork] Servidor encerrou a conexao.");
    } catch (SocketException e) {
      if (running) {
        System.err.println("[ClientNetwork] SocketException no reader: " + e.getMessage());
      }
    } catch (IOException e) {
      if (running) {
        System.err.println("[ClientNetwork] Erro no snapshotReader: " + e.getMessage());
      }
    } finally {
      running = false;
      notifyDisconnected();
    }
  }

  // ---- Loop de escrita de inputs (cliente -> servidor) ----

  private void inputWriterLoop(OutputStream out) {
    List<InputPacket> buffer = new ArrayList<>(8);
    try {
      while (running && !Thread.currentThread().isInterrupted()) {
        buffer.clear();
        transport.drainInputs(buffer);
        for (InputPacket packet : buffer) {
          TcpFraming.writeMessage(out, JsonUtil.toJson(packet));
        }
        if (buffer.isEmpty()) {
          Thread.sleep(INPUT_DRAIN_INTERVAL_MS);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (SocketException e) {
      if (running) {
        System.err.println("[ClientNetwork] SocketException no writer: " + e.getMessage());
      }
    } catch (IOException e) {
      if (running) {
        System.err.println("[ClientNetwork] Erro no inputWriter: " + e.getMessage());
      }
    }
  }

  private void notifyDisconnected() {
    if (connectionListener != null) {
      // Notificar na EDT para nao cruzar threads Swing
      javax.swing.SwingUtilities.invokeLater(() -> connectionListener.onDisconnected());
    }
  }

  // ---- Interface de callback ----

  public interface ConnectionListener {
    void onDisconnected();
  }
}
