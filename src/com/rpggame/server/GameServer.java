package com.rpggame.server;

/**
 * Entry point headless do servidor de jogo.
 *
 * Nao importa nada de Swing ou AWT. Roda como processo independente.
 *
 * Uso:
 *   java -cp bin com.rpggame.server.GameServer
 *   java -cp bin com.rpggame.server.GameServer 7777
 *
 * O servidor aceita conexoes TCP na porta informada (padrao 7777).
 * Clientes conectam via GamePanel com USE_NETWORK=true ou via ClientNetwork.
 */
public class GameServer {

  public static final int DEFAULT_PORT = 7777;

  public static void main(String[] args) throws Exception {
    int port = DEFAULT_PORT;
    if (args.length > 0) {
      try { port = Integer.parseInt(args[0]); }
      catch (NumberFormatException e) {
        System.err.println("Porta invalida '" + args[0] + "', usando " + DEFAULT_PORT);
      }
    }

    WorldState worldState = new WorldState();
    ServerLoop serverLoop = new ServerLoop(worldState);
    ServerNetwork serverNetwork = new ServerNetwork(serverLoop);

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

    // Manter a thread principal viva ate o shutdown hook ser acionado
    Thread.currentThread().join();
  }
}
