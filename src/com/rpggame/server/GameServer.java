package com.rpggame.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Entry point headless do servidor de jogo.
 *
 * Nao importa nada de Swing ou AWT. Roda como processo independente.
 *
 * Uso:
 * java -cp bin com.rpggame.server.GameServer
 * java -cp bin com.rpggame.server.GameServer 7777
 *
 * O servidor aceita conexoes TCP na porta informada (padrao 7777).
 * Clientes conectam via GamePanel com USE_NETWORK=true ou via ClientNetwork.
 */
public class GameServer {

  public static final int DEFAULT_PORT = 7777;
  private static final int PORT_SCAN_ATTEMPTS = 20;

  public static void main(String[] args) throws Exception {
    int requestedPort = DEFAULT_PORT;
    if (args.length > 0) {
      try {
        requestedPort = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Porta invalida '" + args[0] + "', usando " + DEFAULT_PORT);
      }
    }

    int port = findAvailablePort(requestedPort);
    if (port != requestedPort) {
      System.out.println("[GameServer] Porta " + requestedPort + " ocupada. Usando porta " + port + ".");
    }

    WorldState worldState = new WorldState();
    // Não marcar "village" como ativo aqui: ServerLoop.registerPlayer() faz isso
    // quando o primeiro cliente TCP conectar, evitando que tickAllMaps() pule o mapa.
    worldState.getOrCreate("village"); // garante que a simulação exista
    ServerLoop serverLoop = new ServerLoop(worldState);
    ServerNetwork serverNetwork = new ServerNetwork(serverLoop);

    // Contexto inicial para snapshots no modo headless
    serverLoop.updateSnapshotContext(
        "village",
        null,
        null,
        java.util.Collections.emptyList(),
        java.util.Collections.emptyList());

    // Shutdown hook para encerrar limpo com Ctrl+C
    final ServerLoop loopRef = serverLoop;
    final ServerNetwork netRef = serverNetwork;
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\n[GameServer] Encerrando...");
      netRef.stop();
      loopRef.stop();
      System.out.println("[GameServer] Encerrado.");
    }, "shutdown-hook"));

    serverLoop.start();
    serverNetwork.start(port);

    System.out.println("[GameServer] Servidor iniciado na porta " + port + ". Pressione Ctrl+C para encerrar.");
    // Exibe instrucao clara de conexao — util quando a porta pedida estava ocupada
    // e o servidor subiu em porta diferente da configurada no cliente.
    System.out.println("[GameServer] Para conectar: use o host desta maquina e a porta " + port);
    if (port != requestedPort) {
      System.out.println("[GameServer] ATENCAO: porta diferente da solicitada (" + requestedPort
          + "). Atualize o cliente para usar a porta " + port + ".");
    }

    // Manter a thread principal viva ate o shutdown hook ser acionado
    Thread.currentThread().join();
  }

  private static int findAvailablePort(int startPort) {
    int port = startPort;
    for (int i = 0; i < PORT_SCAN_ATTEMPTS; i++) {
      if (isPortAvailable(port)) {
        return port;
      }
      port++;
    }

    throw new IllegalStateException(
        "Nenhuma porta livre encontrada no intervalo " + startPort + "-" + (startPort + PORT_SCAN_ATTEMPTS - 1));
  }

  private static boolean isPortAvailable(int port) {
    try (ServerSocket ignored = new ServerSocket(port)) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
