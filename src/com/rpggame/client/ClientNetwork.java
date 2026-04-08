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
 * Correcoes Bug#1:
 * - Socket armazenado no campo antes de lancar threads; threads obteem streams
 *   diretamente do socket em vez de capturar referencias externas.
 * - inputWriterLoop nao encerra a conexao ao terminar — continua tentando ate
 *   running=false ou excecao de rede.
 * - disconnect() fecha o socket antes de interromper threads, o que desbloqueia
 *   read() bloqueados.
 */
public class ClientNetwork {

  private static final int INPUT_DRAIN_INTERVAL_MS = 5;

  private final InProcessTransport transport;
  private final String playerId;
  private final String playerClass;

  private volatile Socket socket;
  private volatile boolean running = false;
  private Thread readerThread;
  private Thread writerThread;

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
   * Nao bloqueia apos lancar as threads.
   *
   * @throws IOException se nao conseguir conectar ou o handshake falhar
   */
  public void connect(String host, int port) throws IOException {
    if (running) return;

    Socket s = new Socket(host, port);
    s.setTcpNoDelay(true);
    this.socket = s;

    // Handshake sincrono — usa streams obtidos diretamente do socket
    try {
      doHandshake(s.getInputStream(), s.getOutputStream());
    } catch (IOException e) {
      running = false;
      closeSocket();
      throw e;
    }

    running = true;

    // Threads obtem streams do campo socket, nao de variaveis locais capturadas.
    // Isso garante que disconnect() consegue desbloquear os read() fechando o socket.
    readerThread = new Thread(this::snapshotReaderLoop, "client-snapshot-reader");
    writerThread = new Thread(this::inputWriterLoop, "client-input-writer");
    readerThread.setDaemon(true);
    writerThread.setDaemon(true);
    readerThread.start();
    writerThread.start();

    System.out.println("[ClientNetwork] Conectado a " + host + ":" + port + " como " + playerId);
  }

  /**
   * Encerra a conexao: fecha o socket primeiro (desbloqueia read() bloqueados),
   * depois interrompe as threads.
   */
  public void disconnect() {
    running = false;
    closeSocket();                           // desbloqueia read() nas threads
    if (readerThread != null) readerThread.interrupt();
    if (writerThread != null) writerThread.interrupt();
    System.out.println("[ClientNetwork] Desconectado.");
  }

  public boolean isConnected() {
    return running && socket != null && socket.isConnected() && !socket.isClosed();
  }

  // ---- Handshake ----

  private void doHandshake(InputStream in, OutputStream out) throws IOException {
    TcpFraming.writeMessage(out, JsonUtil.handshakeRequestJson(playerId, playerClass));
    String respJson = TcpFraming.readMessage(in);
    if (!JsonUtil.parseHandshakeOk(respJson)) {
      throw new IOException("[ClientNetwork] Handshake recusado: " + respJson);
    }
    String assigned = JsonUtil.parseAssignedPlayerId(respJson);
    System.out.println("[ClientNetwork] Handshake OK. playerId atribuido: " + assigned);
  }

  // ---- Loop de leitura de snapshots (servidor -> cliente) ----

  private void snapshotReaderLoop() {
    try {
      InputStream in = socket.getInputStream();
      while (running && !Thread.currentThread().isInterrupted()) {
        String json = TcpFraming.readMessage(in);
        // Ignorar heartbeat pings do servidor (não são snapshots).
        if (json != null && json.contains("\"ping\":true")) continue;
        try {
          WorldSnapshot snapshot = JsonUtil.snapshotFromJson(json);
          if (snapshot != null) {
            transport.publishSnapshot(snapshot);
            System.out.println("[SNAPSHOT_RCV] t=" + System.currentTimeMillis() + " tick=" + snapshot.getTick());
          }
        } catch (RuntimeException e) {
          // Snapshot malformado — loga e continua lendo o proximo.
          // Nao encerra a conexao por um JSON corrompido pontual.
          if (running) System.err.println("[ClientNetwork] Snapshot malformado ignorado: " + e.getMessage());
        }
      }
    } catch (EOFException e) {
      if (running) System.out.println("[ClientNetwork] Servidor encerrou a conexao.");
    } catch (SocketException e) {
      if (running) System.err.println("[ClientNetwork] SocketException no reader: " + e.getMessage());
    } catch (IOException e) {
      if (running) System.err.println("[ClientNetwork] Erro no snapshotReader: " + e.getMessage());
    } finally {
      running = false;
      closeSocket(); // garante que o servidor recebe FIN/RST ao sair
      notifyDisconnected();
    }
  }

  // ---- Loop de escrita de inputs (cliente -> servidor) ----

  private void inputWriterLoop() {
    List<InputPacket> buffer = new ArrayList<>(8);
    try {
      OutputStream out = socket.getOutputStream();
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
      if (running) System.err.println("[ClientNetwork] SocketException no writer: " + e.getMessage());
    } catch (IOException e) {
      if (running) System.err.println("[ClientNetwork] Erro no inputWriter: " + e.getMessage());
    }
    // Nao chama notifyDisconnected aqui — o snapshotReaderLoop e quem detecta
    // desconexao e notifica, evitando dupla notificacao.
  }

  private void closeSocket() {
    try {
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException ignored) {}
  }

  private void notifyDisconnected() {
    if (connectionListener != null) {
      javax.swing.SwingUtilities.invokeLater(() -> connectionListener.onDisconnected());
    }
  }

  public interface ConnectionListener {
    void onDisconnected();
  }
}
