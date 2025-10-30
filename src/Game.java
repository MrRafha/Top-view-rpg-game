import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Classe principal do jogo RPG 2.5D.
 */
public class Game {
  public static final int SCREEN_WIDTH = 1024;
  public static final int SCREEN_HEIGHT = 800;
  public static final String GAME_TITLE = "RPG 2.5D - Open World";

  private JFrame frame;
  private GamePanel gamePanel;

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

    // Iniciar com a tela combinada de criação de personagem
    CombinedCharacterScreen characterScreen = new CombinedCharacterScreen(frame);
    frame.add(characterScreen);

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    // Focar no painel para receber input do teclado
    characterScreen.requestFocusInWindow();
  }

  /**
   * Método principal para iniciar a aplicação.
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