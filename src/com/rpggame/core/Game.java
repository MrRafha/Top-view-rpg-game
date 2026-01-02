package com.rpggame.core;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import com.rpggame.ui.MainMenuScreen;

/**
 * Classe principal do jogo RPG 2.5D.
 */
public class Game {
  public static final int SCREEN_WIDTH = 1024;
  public static final int SCREEN_HEIGHT = 800;
  public static final String GAME_TITLE = "Echoes of Forgotten Quests";

  private JFrame frame;
  // private GamePanel gamePanel; // TODO: Implementar se necessário

  /**
   * Construtor da classe Game.
   * Inicializa o jogo chamando o método initializeGame.
   */
  public Game() {
    initializeGame();
  }

  private void initializeGame() {
    // Configuração da janela principal
    frame = new JFrame(GAME_TITLE);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);
    frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);

    // Iniciar com o menu principal
    MainMenuScreen mainMenu = new MainMenuScreen(frame);
    frame.add(mainMenu);

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    // Focar no painel para receber input
    mainMenu.requestFocusInWindow();
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