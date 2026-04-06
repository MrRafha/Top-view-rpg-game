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
 *  - um ClientHandler em thread propria
 *  - um PlayerSimulation registrado no ServerLoop
 *  - um InProcessTransport dedicado (retornado por registerPlayer)
 *
 * Fluxo por cliente:
 *   1. Aceita Socket
 *   2. Le handshake { "playerId": "...", "playerClass": "..." }
 *   3. Registra PlayerSimulation no ServerLoop -> obtem InProcessTransport
 *   4. Confirma handshake ao cliente
 *   5. Lanca threads inputReader e snapshotWriter em paralelo
 *   6. Ao desconectar, chama ServerLoop.unregisterPlayer
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
    if (running) return;
    serverSocket = new ServerSocket(port);
    running = true;
    acceptThread = new Thread(this::acceptLoop, "server-accept");
    acceptThread.setDaemon(true);
    acceptThread.start();
    System.out.println("[ServerNetwork] Escutando na porta " + port);
  }

  /**
   * Para o servidor: fecha o ServerSocket (desbloqueia o accept) e aguarda a thread.
   */
  public void stop() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException ignored) {}
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
      try (Socket s = socket) {
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        if (!doHandshake(in, out)) return;

        Thread inputReader = new Thread(
            () -> inputReaderLoop(in), "client-input-reader-" + clientId);
        Thread snapshotWriter = new Thread(
            () -> snapshotWriterLoop(out), "client-snapshot-writer-" + clientId);
        inputReader.setDaemon(true);
        snapshotWriter.setDaemon(true);
        inputReader.start();
        snapshotWriter.start();

        inputReader.join();
        snapshotWriter.interrupt();
        snapshotWriter.join(1000);

      } catch (IOException | InterruptedException e) {
        if (running) {
          System.err.println("[ServerNetwork] Erro no handler do cliente "
              + clientId + ": " + e.getMessage());
        }
      } finally {
        cleanup();
      }
    }

    private boolean doHandshake(InputStream in, OutputStream out) {
      try {
        String requestJson = TcpFraming.readMessage(in);
        String pid = JsonUtil.parseHandshakePlayerId(requestJson);
        if (pid == null || pid.isEmpty()) {
          TcpFraming.writeMessage(out,
              JsonUtil.handshakeResponseJson(false, null, "playerId ausente"));
          return false;
        }

        this.playerId = pid;
        PlayerSimulation sim = serverLoop.getOrCreateSimulationForPlayer(playerId);
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

    private void inputReaderLoop(InputStream in) {
      try {
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
          System.err.println("[ServerNetwork] SocketException no inputReader do cliente " + clientId);
        }
      } catch (IOException e) {
        System.err.println("[ServerNetwork] Erro no inputReader do cliente "
            + clientId + ": " + e.getMessage());
      }
    }

    private void snapshotWriterLoop(OutputStream out) {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          WorldSnapshot snapshot = transport.pollSnapshot();
          if (snapshot != null) {
            TcpFraming.writeMessage(out, JsonUtil.toJson(snapshot));
          } else {
            Thread.sleep(SNAPSHOT_POLL_INTERVAL_MS);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (SocketException e) {
        if (!socket.isClosed()) {
          System.err.println("[ServerNetwork] SocketException no snapshotWriter do cliente " + clientId);
        }
      } catch (IOException e) {
        System.err.println("[ServerNetwork] Erro no snapshotWriter do cliente "
            + clientId + ": " + e.getMessage());
      }
    }

    private void cleanup() {
      if (playerId != null) {
        serverLoop.unregisterPlayer(playerId);
        System.out.println("[ServerNetwork] Cliente " + clientId
            + " (" + playerId + ") desconectado e desregistrado.");
      }
      try {
        if (!socket.isClosed()) socket.close();
      } catch (IOException ignored) {}
    }
  }
}
