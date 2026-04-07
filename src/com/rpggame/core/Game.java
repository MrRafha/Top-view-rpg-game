package com.rpggame.core;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.rpggame.server.ServerLoop;
import com.rpggame.server.ServerNetwork;
import com.rpggame.server.WorldState;
import com.rpggame.ui.GameLauncher;
import com.rpggame.ui.MainMenuScreen;

/**
 * Classe principal do jogo RPG 2.5D.
 */
public class Game {
  public static final int SCREEN_WIDTH = 1024;
  public static final int SCREEN_HEIGHT = 800;
  public static final String GAME_TITLE = "Echoes of Forgotten Quests";
  private static final int DEFAULT_SERVER_PORT = 7777;

  private JFrame frame;
  private ServerLoop hostedServerLoop;
  private ServerNetwork hostedServerNetwork;

  /**
   * Construtor da classe Game.
   * Inicializa o jogo chamando o método initializeGame.
   */
  public Game() {
    initializeGame();
  }

  private void initializeGame() {
    frame = new JFrame(GAME_TITLE);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(true);
    frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        stopHostedServer();
      }
    });

    GameLauncher.showLauncher(frame, new GameLauncher.OnLaunchListener() {
      @Override
      public void onSoloPlay() {
        startMenuFlow(false, "127.0.0.1", DEFAULT_SERVER_PORT);
      }

      @Override
      public void onCreateServer() {
        int port = askServerPort(DEFAULT_SERVER_PORT);
        if (port < 1) {
          return;
        }

        if (startHostedServer(port)) {
          JOptionPane.showMessageDialog(frame,
              "Servidor criado na porta " + port + ".\nVoce sera conectado como host.",
              "Servidor iniciado",
              JOptionPane.INFORMATION_MESSAGE);
          startMenuFlow(true, "127.0.0.1", port);
        }
      }

      @Override
      public void onJoinServer() {
        String host = JOptionPane.showInputDialog(frame,
            "Digite o IP/host do servidor:", "127.0.0.1");
        if (host == null || host.trim().isEmpty()) {
          return;
        }

        int port = askServerPort(DEFAULT_SERVER_PORT);
        if (port < 1) {
          return;
        }

        startMenuFlow(true, host.trim(), port);
      }
    });

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void startMenuFlow(boolean useNetwork, String host, int port) {
    MainMenuScreen mainMenu = new MainMenuScreen(frame, useNetwork, host, port);
    frame.setContentPane(mainMenu);
    frame.revalidate();
    frame.repaint();
    mainMenu.requestFocusInWindow();
  }

  private int askServerPort(int defaultPort) {
    String portText = JOptionPane.showInputDialog(frame,
        "Digite a porta do servidor:", String.valueOf(defaultPort));
    if (portText == null) {
      return -1;
    }

    try {
      int port = Integer.parseInt(portText.trim());
      if (port < 1 || port > 65535) {
        throw new NumberFormatException("fora da faixa");
      }
      return port;
    } catch (NumberFormatException e) {
      JOptionPane.showMessageDialog(frame,
          "Porta invalida. Use um numero entre 1 e 65535.",
          "Erro de porta",
          JOptionPane.ERROR_MESSAGE);
      return -1;
    }
  }

  private boolean startHostedServer(int port) {
    if (hostedServerNetwork != null) {
      JOptionPane.showMessageDialog(frame,
          "Ja existe um servidor local em execucao.",
          "Servidor local",
          JOptionPane.INFORMATION_MESSAGE);
      return true;
    }

    try {
      WorldState worldState = new WorldState();
      hostedServerLoop = new ServerLoop(worldState);
      hostedServerNetwork = new ServerNetwork(hostedServerLoop);
      hostedServerLoop.start();
      hostedServerNetwork.start(port);
      System.out.println("[Game] Servidor local iniciado na porta " + port);
      return true;
    } catch (Exception e) {
      stopHostedServer();
      JOptionPane.showMessageDialog(frame,
          "Nao foi possivel iniciar o servidor local:\n" + e.getMessage(),
          "Erro ao iniciar servidor",
          JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  private void stopHostedServer() {
    if (hostedServerNetwork != null) {
      hostedServerNetwork.stop();
      hostedServerNetwork = null;
    }
    if (hostedServerLoop != null) {
      hostedServerLoop.stop();
      hostedServerLoop = null;
    }
  }

  /**
   * Método principal para iniciar a aplicação.
   *
   * @param args argumentos da linha de comando
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        // Usar o look and feel padrão do sistema
      } catch (Exception e) {
        e.printStackTrace();
      }
      new Game();
    });
  }
}
