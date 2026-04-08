package com.rpggame.server;

import com.rpggame.shared.InputPacket;
import com.rpggame.shared.WorldSnapshot;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Camada de rede do servidor: aceita conexoes TCP e gerencia cada cliente.
 *
 * Cada cliente conectado ganha:
 * - um ClientHandler em thread propria
 * - um PlayerSimulation registrado no ServerLoop
 * - um InProcessTransport dedicado (retornado por registerPlayer)
 *
 * Fluxo por cliente:
 * 1. Aceita Socket
 * 2. Le handshake { "playerId": "...", "playerClass": "..." }
 * 3. Registra PlayerSimulation no ServerLoop -> obtem InProcessTransport
 * 4. Confirma handshake ao cliente
 * 5. Lanca threads inputReader e snapshotWriter em paralelo
 * 6. Ao desconectar, chama ServerLoop.unregisterPlayer
 */
public class ServerNetwork {

  private static final int SNAPSHOT_POLL_INTERVAL_MS = 5;

  private final ServerLoop serverLoop;
  private final AtomicInteger clientCounter = new AtomicInteger(0);

  private volatile ServerSocket serverSocket;
  private volatile boolean running = false;
  private Thread acceptThread;

  public ServerNetwork(ServerLoop serverLoop) {
    this.serverLoop = serverLoop;
  }

  /**
   * Abre o ServerSocket e inicia o loop de accept em thread dedicada.
   * Nao bloqueia — retorna imediatamente apos iniciar a thread.
   */
  public void start(int port) throws IOException {
    if (running)
      return;
    serverSocket = new ServerSocket(port);
    running = true;
    acceptThread = new Thread(this::acceptLoop, "server-accept");
    acceptThread.setDaemon(true);
    acceptThread.start();
    System.out.println("[ServerNetwork] Escutando na porta " + port);
  }

  /**
   * Para o servidor: fecha o ServerSocket (desbloqueia o accept) e aguarda a
   * thread.
   */
  public void stop() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException ignored) {
    }
    if (acceptThread != null) {
      acceptThread.interrupt();
    }
    System.out.println("[ServerNetwork] Parado.");
  }

  private void acceptLoop() {
    while (running) {
      try {
        Socket socket = serverSocket.accept();
        int id = clientCounter.incrementAndGet();
        socket.setTcpNoDelay(true);
        ClientHandler handler = new ClientHandler(socket, id);
        Thread t = new Thread(handler, "client-handler-" + id);
        t.setDaemon(true);
        t.start();
      } catch (SocketException e) {
        if (running) {
          System.err.println("[ServerNetwork] Erro no accept: " + e.getMessage());
        }
      } catch (IOException e) {
        if (running) {
          System.err.println("[ServerNetwork] IOException no accept: " + e.getMessage());
        }
      }
    }
  }

  // ---- ClientHandler ----

  private class ClientHandler implements Runnable {

    private final Socket socket;
    private final int clientId;
    private String playerId;
    private InProcessTransport transport;

    ClientHandler(Socket socket, int clientId) {
      this.socket = socket;
      this.clientId = clientId;
    }

    @Override
    public void run() {
      System.out.println("[ServerNetwork] Cliente " + clientId
          + " conectado: " + socket.getRemoteSocketAddress());
      try {
        if (!doHandshake(socket.getInputStream(), socket.getOutputStream()))
          return;

        // Threads obtem streams diretamente do socket — nao capturam referencias
        // locais que ficam invalidas quando o socket e fechado por cleanup().
        Thread inputReader = new Thread(
            this::inputReaderLoop, "client-input-reader-" + clientId);
        Thread snapshotWriter = new Thread(
            this::snapshotWriterLoop, "client-snapshot-writer-" + clientId);
        inputReader.setDaemon(true);
        snapshotWriter.setDaemon(true);
        inputReader.start();
        snapshotWriter.start();

        // Aguarda o inputReader terminar (indica desconexao do cliente).
        // Entao interrompe o snapshotWriter — ele nao precisa continuar sozinho.
        inputReader.join();
        snapshotWriter.interrupt();
        snapshotWriter.join(2000);

      } catch (IOException | InterruptedException e) {
        if (running) {
          System.err.println("[ServerNetwork] Erro no handler do cliente "
              + clientId + ": " + e.getMessage());
        }
      } finally {
        // cleanup() fecha o socket DEPOIS que as threads terminaram,
        // nao durante — evita SocketException prematura no inputReader.
        cleanup();
      }
    }

    private boolean doHandshake(InputStream in, OutputStream out) {
      try {
        String requestJson = TcpFraming.readMessage(in);
        String requestedPid = JsonUtil.parseHandshakePlayerId(requestJson);
        String pid = requestedPid;
        if (pid == null || pid.isEmpty()) {
          TcpFraming.writeMessage(out,
              JsonUtil.handshakeResponseJson(false, null, "playerId ausente"));
          return false;
        }

        if (serverLoop.hasPlayerSimulation(pid)) {
          pid = requestedPid + "-" + clientId;
        }

        this.playerId = pid;
        String playerClass = JsonUtil.parseHandshakePlayerClass(requestJson);
        PlayerSimulation sim = serverLoop.getOrCreateSimulationForPlayer(playerId, playerClass);
        this.transport = serverLoop.registerPlayer(sim);

        TcpFraming.writeMessage(out,
            JsonUtil.handshakeResponseJson(true, playerId, null));
        System.out.println("[ServerNetwork] Handshake OK para " + playerId);
        return true;

      } catch (IOException e) {
        System.err.println("[ServerNetwork] Falha no handshake do cliente "
            + clientId + ": " + e.getMessage());
        return false;
      }
    }

    private void inputReaderLoop() {
      try {
        InputStream in = socket.getInputStream();
        while (!Thread.currentThread().isInterrupted()) {
          String json = TcpFraming.readMessage(in);
          InputPacket packet = JsonUtil.inputPacketFromJson(json);
          if (packet != null) {
            transport.publishInput(packet);
          }
        }
      } catch (EOFException e) {
        // Conexao encerrada normalmente pelo cliente
      } catch (SocketException e) {
        if (!socket.isClosed()) {
          System.err.println("[ServerNetwork] SocketException no inputReader do cliente "
              + clientId + ": " + e.getMessage());
        }
      } catch (IOException e) {
        System.err.println("[ServerNetwork] Erro no inputReader do cliente "
            + clientId + ": " + e.getMessage());
      }
      // Ao sair, a thread principal (run) detecta via join() e chama cleanup()
    }

    private void snapshotWriterLoop() {
      try {
        OutputStream out = socket.getOutputStream();
        long lastSentMs = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
          WorldSnapshot snapshot = transport.pollSnapshot();
          long nowMs = System.currentTimeMillis();
          if (snapshot != null) {
            TcpFraming.writeMessage(out, JsonUtil.toJson(snapshot));
            lastSentMs = nowMs;
          } else if (nowMs - lastSentMs > 2000) {
            // Heartbeat: mantém o socket TCP vivo quando não há snapshots novos.
            TcpFraming.writeMessage(out, "{\"ping\":true}");
            lastSentMs = nowMs;
          } else {
            Thread.sleep(SNAPSHOT_POLL_INTERVAL_MS);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (SocketException e) {
        if (!socket.isClosed()) {
          System.err.println("[ServerNetwork] SocketException no snapshotWriter do cliente "
              + clientId + ": " + e.getMessage());
        }
        // Fechar socket desbloqueia o inputReader que esta em join() no run()
        try { socket.close(); } catch (IOException ignored) {}
      } catch (IOException e) {
        System.err.println("[ServerNetwork] Erro no snapshotWriter do cliente "
            + clientId + ": " + e.getMessage());
        try { socket.close(); } catch (IOException ignored) {}
      } catch (RuntimeException e) {
        System.err.println("[ServerNetwork] Erro inesperado no snapshotWriter do cliente "
            + clientId + ": " + e);
        try { socket.close(); } catch (IOException ignored) {}
      }
    }

    private void cleanup() {
      if (playerId != null) {
        serverLoop.unregisterPlayer(playerId);
        System.out.println("[ServerNetwork] Cliente " + clientId
            + " (" + playerId + ") desconectado e desregistrado.");
      }
      try {
        if (!socket.isClosed())
          socket.close();
      } catch (IOException ignored) {
      }
    }
  }
}
