import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Tela de criação de personagem onde o jogador escolhe sua classe
 */
public class CharacterCreationScreen extends JPanel implements ActionListener {
  private JFrame parentFrame;
  private GamePanel gamePanel;

  // Componentes da interface
  private JLabel titleLabel;
  private JLabel instructionLabel;
  private JButton warriorButton;
  private JButton mageButton;
  private JButton hunterButton;
  private JButton startGameButton;

  // Sprites das classes
  private BufferedImage warriorSprite;
  private BufferedImage mageSprite;
  private BufferedImage hunterSprite;

  // Classe selecionada
  private String selectedClass = "";
  private BufferedImage selectedSprite;

  // Cores do tema
  private final Color BACKGROUND_COLOR = new Color(20, 30, 40);
  private final Color PANEL_COLOR = new Color(40, 50, 60);
  private final Color TEXT_COLOR = new Color(220, 220, 220);
  private final Color ACCENT_COLOR = new Color(100, 150, 200);

  public CharacterCreationScreen(JFrame parentFrame) {
    this.parentFrame = parentFrame;
    loadSprites();
    initializeUI();
  }

  private void loadSprites() {
    try {
      warriorSprite = ImageIO.read(new File("sprites/WarriorPlayer.png"));
      mageSprite = ImageIO.read(new File("sprites/MagePlayer.png"));
      hunterSprite = ImageIO.read(new File("sprites/HunterPlayer.png"));
    } catch (IOException e) {
      System.err.println("Erro ao carregar sprites das classes");
      e.printStackTrace();
    }
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    setBackground(BACKGROUND_COLOR);

    // Título
    JPanel titlePanel = new JPanel();
    titlePanel.setBackground(BACKGROUND_COLOR);
    titleLabel = new JLabel("CRIAÇÃO DE PERSONAGEM");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
    titleLabel.setForeground(TEXT_COLOR);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    titlePanel.add(titleLabel);

    // Instruções
    instructionLabel = new JLabel("Escolha sua classe:");
    instructionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
    instructionLabel.setForeground(TEXT_COLOR);
    instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);

    JPanel instructionPanel = new JPanel();
    instructionPanel.setBackground(BACKGROUND_COLOR);
    instructionPanel.add(instructionLabel);

    // Painel das classes
    JPanel classesPanel = createClassesPanel();

    // Botão de continuar
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(BACKGROUND_COLOR);
    startGameButton = new JButton("CUSTOMIZAR ATRIBUTOS");
    startGameButton.setFont(new Font("Arial", Font.BOLD, 20));
    startGameButton.setPreferredSize(new Dimension(250, 50));
    startGameButton.addActionListener(this);
    startGameButton.setEnabled(false);
    startGameButton.setBackground(ACCENT_COLOR);
    startGameButton.setForeground(Color.WHITE);
    buttonPanel.add(startGameButton);

    // Adicionar componentes
    add(titlePanel, BorderLayout.NORTH);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(BACKGROUND_COLOR);
    centerPanel.add(instructionPanel, BorderLayout.NORTH);
    centerPanel.add(classesPanel, BorderLayout.CENTER);
    centerPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(centerPanel, BorderLayout.CENTER);
  }

  private JPanel createClassesPanel() {
    JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.setBackground(BACKGROUND_COLOR);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(20, 20, 20, 20);
    gbc.anchor = GridBagConstraints.CENTER;

    // Warrior
    gbc.gridx = 0;
    gbc.gridy = 0;
    JPanel warriorPanel = createClassPanel("GUERREIRO", warriorSprite,
        "Forte e resistente, especialista em combate corpo a corpo");
    mainPanel.add(warriorPanel, gbc);

    // Mage
    gbc.gridx = 1;
    gbc.gridy = 0;
    JPanel magePanel = createClassPanel("MAGO", mageSprite,
        "Mestre das artes arcanas, usa magia para derrotar inimigos");
    mainPanel.add(magePanel, gbc);

    // Hunter
    gbc.gridx = 2;
    gbc.gridy = 0;
    JPanel hunterPanel = createClassPanel("CAÇADOR", hunterSprite,
        "Ágil e preciso, especialista em ataques à distância");
    mainPanel.add(hunterPanel, gbc);

    return mainPanel;
  }

  private JPanel createClassPanel(String className, BufferedImage sprite, String description) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(PANEL_COLOR);
    panel.setBorder(BorderFactory.createRaisedBevelBorder());
    panel.setPreferredSize(new Dimension(250, 350));

    // Nome da classe
    JLabel nameLabel = new JLabel(className);
    nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
    nameLabel.setForeground(TEXT_COLOR);
    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Sprite (ampliado)
    JLabel spriteLabel = new JLabel();
    if (sprite != null) {
      ImageIcon icon = new ImageIcon(sprite.getScaledInstance(88, 128, Image.SCALE_FAST));
      spriteLabel.setIcon(icon);
    }
    spriteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Descrição
    JTextArea descArea = new JTextArea(description);
    descArea.setFont(new Font("Arial", Font.PLAIN, 12));
    descArea.setForeground(TEXT_COLOR);
    descArea.setBackground(PANEL_COLOR);
    descArea.setLineWrap(true);
    descArea.setWrapStyleWord(true);
    descArea.setEditable(false);
    descArea.setPreferredSize(new Dimension(230, 60));
    descArea.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Botão de seleção
    JButton selectButton = new JButton("Escolher");
    selectButton.setActionCommand(className);
    selectButton.addActionListener(this);
    selectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    selectButton.setBackground(ACCENT_COLOR);
    selectButton.setForeground(Color.WHITE);

    // Armazenar referência aos botões
    if (className.equals("GUERREIRO")) {
      warriorButton = selectButton;
    } else if (className.equals("MAGO")) {
      mageButton = selectButton;
    } else if (className.equals("CAÇADOR")) {
      hunterButton = selectButton;
    }

    // Adicionar componentes
    panel.add(Box.createVerticalGlue());
    panel.add(nameLabel);
    panel.add(Box.createRigidArea(new Dimension(0, 10)));
    panel.add(spriteLabel);
    panel.add(Box.createRigidArea(new Dimension(0, 10)));
    panel.add(descArea);
    panel.add(Box.createRigidArea(new Dimension(0, 15)));
    panel.add(selectButton);
    panel.add(Box.createVerticalGlue());

    return panel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();

    if (command.equals("GUERREIRO")) {
      selectedClass = "Warrior";
      selectedSprite = warriorSprite;
      updateButtonStates();
    } else if (command.equals("MAGO")) {
      selectedClass = "Mage";
      selectedSprite = mageSprite;
      updateButtonStates();
    } else if (command.equals("CAÇADOR")) {
      selectedClass = "Hunter";
      selectedSprite = hunterSprite;
      updateButtonStates();
    } else if (command.equals("CUSTOMIZAR ATRIBUTOS")) {
      startGame();
    }
  }

  private void updateButtonStates() {
    // Resetar todos os botões
    warriorButton.setText("Escolher");
    mageButton.setText("Escolher");
    hunterButton.setText("Escolher");

    // Marcar o selecionado
    if (selectedClass.equals("Warrior")) {
      warriorButton.setText("SELECIONADO");
    } else if (selectedClass.equals("Mage")) {
      mageButton.setText("SELECIONADO");
    } else if (selectedClass.equals("Hunter")) {
      hunterButton.setText("SELECIONADO");
    }

    // Habilitar botão de iniciar
    startGameButton.setEnabled(true);

    // Atualizar instrução
    instructionLabel.setText("Classe " + selectedClass + " selecionada!");
  }

  private void startGame() {
    // Criar o caminho do sprite da classe selecionada
    String spritePath = "sprites/" + selectedClass + "Player.png";

    // Remover a tela de criação
    parentFrame.getContentPane().removeAll();

    // Ir para a tela de customização de atributos
    AttributeCustomizationScreen attributeScreen = new AttributeCustomizationScreen(parentFrame, selectedClass,
        spritePath);
    parentFrame.add(attributeScreen);
    parentFrame.revalidate();
    parentFrame.repaint();

    // Focar na tela de atributos
    attributeScreen.requestFocusInWindow();
  }
}