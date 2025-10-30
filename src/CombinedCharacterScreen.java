import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Tela combinada de criação de personagem e customização de atributos
 */
public class CombinedCharacterScreen extends JPanel implements ActionListener {
  private JFrame parentFrame;

  // Sprites das classes
  private BufferedImage warriorSprite;
  private BufferedImage mageSprite;
  private BufferedImage hunterSprite;

  // Classe selecionada
  private String selectedClass = "";
  private CharacterStats stats;

  // Componentes da interface
  private JLabel titleLabel;
  private JLabel classLabel;
  private JLabel pointsLabel;
  private JButton warriorButton;
  private JButton mageButton;
  private JButton hunterButton;
  private JButton confirmButton;
  private JButton resetButton;

  // Componentes dos atributos
  private JLabel[] attributeLabels = new JLabel[6];
  private JLabel[] valueLabels = new JLabel[6];
  private JButton[] minusButtons = new JButton[6];
  private JButton[] plusButtons = new JButton[6];
  private JLabel[] bonusLabels = new JLabel[6];

  // Painel de atributos
  private JPanel attributesPanel;

  // Nomes dos atributos
  private final String[] ATTRIBUTE_NAMES = {
      "Força", "Destreza", "Inteligência", "Sabedoria", "Carisma", "Constituição"
  };

  private final String[] ATTRIBUTE_DESCRIPTIONS = {
      "(Dano + Slots de inventário)",
      "(Dano + Chance de evasão)",
      "(Dano + Projéteis mágicos)",
      "(XP +5% a cada 2 pontos)",
      "(Sistema de intimidação futuro)",
      "(Vida +10HP e -1% dano recebido a cada 2 pts)"
  };

  // Cores do tema
  private final Color BACKGROUND_COLOR = new Color(20, 30, 40);
  private final Color PANEL_COLOR = new Color(40, 50, 60);
  private final Color TEXT_COLOR = new Color(220, 220, 220);
  private final Color ACCENT_COLOR = new Color(100, 150, 200);
  private final Color PRIMARY_COLOR = new Color(255, 215, 0);
  private final Color WARNING_COLOR = new Color(255, 100, 100);

  public CombinedCharacterScreen(JFrame parentFrame) {
    this.parentFrame = parentFrame;
    this.stats = new CharacterStats("Warrior");
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

    // Painel superior com título
    JPanel topPanel = createTopPanel();
    add(topPanel, BorderLayout.NORTH);

    // Painel central principal
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(BACKGROUND_COLOR);

    // Painel esquerdo - seleção de classes
    JPanel leftPanel = createClassSelectionPanel();
    centerPanel.add(leftPanel, BorderLayout.WEST);

    // Painel direito - atributos
    attributesPanel = createAttributesPanel();
    attributesPanel.setVisible(false); // Inicialmente escondido
    centerPanel.add(attributesPanel, BorderLayout.CENTER);

    add(centerPanel, BorderLayout.CENTER);

    // Painel inferior com botões
    JPanel bottomPanel = createBottomPanel();
    add(bottomPanel, BorderLayout.SOUTH);
  }

  private JPanel createTopPanel() {
    JPanel panel = new JPanel();
    panel.setBackground(BACKGROUND_COLOR);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

    titleLabel = new JLabel("CRIAÇÃO DE PERSONAGEM", SwingConstants.CENTER);
    titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
    titleLabel.setForeground(TEXT_COLOR);

    classLabel = new JLabel("Escolha sua classe:", SwingConstants.CENTER);
    classLabel.setFont(new Font("Arial", Font.PLAIN, 16));
    classLabel.setForeground(TEXT_COLOR);

    pointsLabel = new JLabel("", SwingConstants.CENTER);
    pointsLabel.setFont(new Font("Arial", Font.BOLD, 14));
    pointsLabel.setVisible(false);

    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(titleLabel);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(classLabel);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(pointsLabel);

    return panel;
  }

  private JPanel createClassSelectionPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(BACKGROUND_COLOR);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 5, 10, 5);

    // Warrior
    gbc.gridx = 0;
    gbc.gridy = 0;
    JPanel warriorPanel = createClassPanel("GUERREIRO", warriorSprite,
        "Forte e resistente, especialista em combate corpo a corpo");
    panel.add(warriorPanel, gbc);

    // Mage
    gbc.gridx = 0;
    gbc.gridy = 1;
    JPanel magePanel = createClassPanel("MAGO", mageSprite,
        "Mestre das artes arcanas, usa magia para derrotar inimigos");
    panel.add(magePanel, gbc);

    // Hunter
    gbc.gridx = 0;
    gbc.gridy = 2;
    JPanel hunterPanel = createClassPanel("CAÇADOR", hunterSprite,
        "Ágil e preciso, especialista em ataques à distância");
    panel.add(hunterPanel, gbc);

    return panel;
  }

  private JPanel createClassPanel(String className, BufferedImage sprite, String description) {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.setBackground(PANEL_COLOR);
    panel.setBorder(BorderFactory.createRaisedBevelBorder());
    panel.setPreferredSize(new Dimension(280, 120));

    // Painel esquerdo com sprite
    JPanel spritePanel = new JPanel();
    spritePanel.setBackground(PANEL_COLOR);
    spritePanel.setPreferredSize(new Dimension(80, 120));

    if (sprite != null) {
      JLabel spriteLabel = new JLabel();
      ImageIcon icon = new ImageIcon(sprite.getScaledInstance(44, 64, Image.SCALE_FAST));
      spriteLabel.setIcon(icon);
      spritePanel.add(spriteLabel);
    }

    // Painel central com informações
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
    infoPanel.setBackground(PANEL_COLOR);
    infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

    JLabel nameLabel = new JLabel(className);
    nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
    nameLabel.setForeground(TEXT_COLOR);
    nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JTextArea descArea = new JTextArea(description);
    descArea.setFont(new Font("Arial", Font.PLAIN, 11));
    descArea.setForeground(new Color(180, 180, 180));
    descArea.setBackground(PANEL_COLOR);
    descArea.setLineWrap(true);
    descArea.setWrapStyleWord(true);
    descArea.setEditable(false);
    descArea.setAlignmentX(Component.LEFT_ALIGNMENT);

    infoPanel.add(nameLabel);
    infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    infoPanel.add(descArea);
    infoPanel.add(Box.createVerticalGlue());

    // Botão de seleção
    JButton selectButton = new JButton("Escolher");
    selectButton.setActionCommand(className);
    selectButton.addActionListener(this);
    selectButton.setBackground(ACCENT_COLOR);
    selectButton.setForeground(Color.WHITE);
    selectButton.setPreferredSize(new Dimension(80, 30));

    // Armazenar referência aos botões
    if (className.equals("GUERREIRO")) {
      warriorButton = selectButton;
    } else if (className.equals("MAGO")) {
      mageButton = selectButton;
    } else if (className.equals("CAÇADOR")) {
      hunterButton = selectButton;
    }

    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(PANEL_COLOR);
    buttonPanel.add(selectButton);

    panel.add(spritePanel, BorderLayout.WEST);
    panel.add(infoPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel createAttributesPanel() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(BACKGROUND_COLOR);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    // Título da seção
    JLabel attrTitle = new JLabel("DISTRIBUIÇÃO DE ATRIBUTOS", SwingConstants.CENTER);
    attrTitle.setFont(new Font("Arial", Font.BOLD, 18));
    attrTitle.setForeground(TEXT_COLOR);
    attrTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
    mainPanel.add(attrTitle, BorderLayout.NORTH);

    // Painel dos atributos
    JPanel attributesGrid = new JPanel(new GridBagLayout());
    attributesGrid.setBackground(BACKGROUND_COLOR);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 8, 5, 8);

    // Cabeçalhos
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    JLabel nameHeader = new JLabel("ATRIBUTO");
    nameHeader.setFont(new Font("Arial", Font.BOLD, 12));
    nameHeader.setForeground(TEXT_COLOR);
    attributesGrid.add(nameHeader, gbc);

    gbc.gridx = 2;
    gbc.anchor = GridBagConstraints.CENTER;
    JLabel valueHeader = new JLabel("VALOR");
    valueHeader.setFont(new Font("Arial", Font.BOLD, 12));
    valueHeader.setForeground(TEXT_COLOR);
    attributesGrid.add(valueHeader, gbc);

    gbc.gridx = 4;
    gbc.anchor = GridBagConstraints.WEST;
    JLabel bonusHeader = new JLabel("EFEITO");
    bonusHeader.setFont(new Font("Arial", Font.BOLD, 12));
    bonusHeader.setForeground(TEXT_COLOR);
    attributesGrid.add(bonusHeader, gbc);

    // Criar linhas dos atributos
    for (int i = 0; i < 6; i++) {
      createAttributeRow(attributesGrid, i, gbc);
    }

    mainPanel.add(attributesGrid, BorderLayout.CENTER);
    return mainPanel;
  }

  private void createAttributeRow(JPanel panel, int index, GridBagConstraints gbc) {
    int row = index + 1;

    // Nome + descrição
    JPanel namePanel = new JPanel();
    namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
    namePanel.setBackground(BACKGROUND_COLOR);

    attributeLabels[index] = new JLabel(ATTRIBUTE_NAMES[index]);
    attributeLabels[index].setFont(new Font("Arial", Font.BOLD, 14));
    attributeLabels[index].setForeground(TEXT_COLOR);

    JLabel descLabel = new JLabel(ATTRIBUTE_DESCRIPTIONS[index]);
    descLabel.setFont(new Font("Arial", Font.PLAIN, 10));
    descLabel.setForeground(new Color(160, 160, 160));

    namePanel.add(attributeLabels[index]);
    namePanel.add(descLabel);

    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(namePanel, gbc);

    // Botão -
    minusButtons[index] = new JButton("-");
    minusButtons[index].setPreferredSize(new Dimension(30, 25));
    minusButtons[index].setActionCommand("minus_" + index);
    minusButtons[index].addActionListener(this);
    minusButtons[index].setBackground(WARNING_COLOR);
    minusButtons[index].setForeground(Color.WHITE);
    minusButtons[index].setFont(new Font("Arial", Font.BOLD, 12));
    gbc.gridx = 1;
    gbc.anchor = GridBagConstraints.CENTER;
    panel.add(minusButtons[index], gbc);

    // Valor
    valueLabels[index] = new JLabel("5", SwingConstants.CENTER);
    valueLabels[index].setFont(new Font("Arial", Font.BOLD, 16));
    valueLabels[index].setForeground(TEXT_COLOR);
    valueLabels[index].setPreferredSize(new Dimension(30, 25));
    valueLabels[index].setOpaque(true);
    valueLabels[index].setBackground(PANEL_COLOR);
    valueLabels[index].setBorder(BorderFactory.createLoweredBevelBorder());
    gbc.gridx = 2;
    panel.add(valueLabels[index], gbc);

    // Botão +
    plusButtons[index] = new JButton("+");
    plusButtons[index].setPreferredSize(new Dimension(30, 25));
    plusButtons[index].setActionCommand("plus_" + index);
    plusButtons[index].addActionListener(this);
    plusButtons[index].setBackground(ACCENT_COLOR);
    plusButtons[index].setForeground(Color.WHITE);
    plusButtons[index].setFont(new Font("Arial", Font.BOLD, 12));
    gbc.gridx = 3;
    panel.add(plusButtons[index], gbc);

    // Bônus
    bonusLabels[index] = new JLabel();
    bonusLabels[index].setFont(new Font("Arial", Font.PLAIN, 12));
    bonusLabels[index].setForeground(new Color(150, 255, 150));
    gbc.gridx = 4;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(bonusLabels[index], gbc);
  }

  private JPanel createBottomPanel() {
    JPanel panel = new JPanel(new FlowLayout());
    panel.setBackground(BACKGROUND_COLOR);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 25, 20));

    // Botão Reset (inicialmente escondido)
    resetButton = new JButton("RESETAR");
    resetButton.setFont(new Font("Arial", Font.BOLD, 12));
    resetButton.setPreferredSize(new Dimension(100, 35));
    resetButton.addActionListener(this);
    resetButton.setActionCommand("reset");
    resetButton.setBackground(WARNING_COLOR);
    resetButton.setForeground(Color.WHITE);
    resetButton.setVisible(false);
    panel.add(resetButton);

    // Espaço
    panel.add(Box.createRigidArea(new Dimension(20, 0)));

    // Botão Confirmar (inicialmente escondido)
    confirmButton = new JButton("INICIAR AVENTURA");
    confirmButton.setFont(new Font("Arial", Font.BOLD, 12));
    confirmButton.setPreferredSize(new Dimension(150, 35));
    confirmButton.addActionListener(this);
    confirmButton.setActionCommand("confirm");
    confirmButton.setBackground(ACCENT_COLOR);
    confirmButton.setForeground(Color.WHITE);
    confirmButton.setVisible(false);
    panel.add(confirmButton);

    return panel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();

    if (command.equals("GUERREIRO")) {
      selectClass("Warrior");
    } else if (command.equals("MAGO")) {
      selectClass("Mage");
    } else if (command.equals("CAÇADOR")) {
      selectClass("Hunter");
    } else if (command.equals("reset")) {
      stats.resetToDefault();
      updateInterface();
    } else if (command.equals("confirm")) {
      if (stats.isValidDistribution()) {
        startGame();
      }
    } else if (command.startsWith("plus_")) {
      int index = Integer.parseInt(command.substring(5));
      incrementAttribute(index);
    } else if (command.startsWith("minus_")) {
      int index = Integer.parseInt(command.substring(6));
      decrementAttribute(index);
    }
  }

  private void selectClass(String className) {
    selectedClass = className;
    stats = new CharacterStats(className);

    // Atualizar botões de classe
    warriorButton.setText("Escolher");
    mageButton.setText("Escolher");
    hunterButton.setText("Escolher");

    if (className.equals("Warrior")) {
      warriorButton.setText("SELECIONADO");
    } else if (className.equals("Mage")) {
      mageButton.setText("SELECIONADO");
    } else if (className.equals("Hunter")) {
      hunterButton.setText("SELECIONADO");
    }

    // Mostrar painel de atributos e botões
    attributesPanel.setVisible(true);
    resetButton.setVisible(true);
    confirmButton.setVisible(true);

    // Atualizar labels
    classLabel.setText("Classe selecionada: " + className.toUpperCase());
    pointsLabel.setVisible(true);

    updateInterface();
    revalidate();
    repaint();
  }

  private void incrementAttribute(int index) {
    if (stats.getRemainingPoints() <= 0)
      return;

    boolean success = false;
    switch (index) {
      case 0:
        success = stats.setStrength(stats.getStrength() + 1);
        break;
      case 1:
        success = stats.setDexterity(stats.getDexterity() + 1);
        break;
      case 2:
        success = stats.setIntelligence(stats.getIntelligence() + 1);
        break;
      case 3:
        success = stats.setWisdom(stats.getWisdom() + 1);
        break;
      case 4:
        success = stats.setCharisma(stats.getCharisma() + 1);
        break;
      case 5:
        success = stats.setConstitution(stats.getConstitution() + 1);
        break;
    }

    if (success) {
      updateInterface();
    }
  }

  private void decrementAttribute(int index) {
    boolean success = false;
    switch (index) {
      case 0:
        success = stats.setStrength(stats.getStrength() - 1);
        break;
      case 1:
        success = stats.setDexterity(stats.getDexterity() - 1);
        break;
      case 2:
        success = stats.setIntelligence(stats.getIntelligence() - 1);
        break;
      case 3:
        success = stats.setWisdom(stats.getWisdom() - 1);
        break;
      case 4:
        success = stats.setCharisma(stats.getCharisma() - 1);
        break;
      case 5:
        success = stats.setConstitution(stats.getConstitution() - 1);
        break;
    }

    if (success) {
      updateInterface();
    }
  }

  private void updateInterface() {
    if (!attributesPanel.isVisible())
      return;

    // Atualizar valores
    valueLabels[0].setText(String.valueOf(stats.getStrength()));
    valueLabels[1].setText(String.valueOf(stats.getDexterity()));
    valueLabels[2].setText(String.valueOf(stats.getIntelligence()));
    valueLabels[3].setText(String.valueOf(stats.getWisdom()));
    valueLabels[4].setText(String.valueOf(stats.getCharisma()));
    valueLabels[5].setText(String.valueOf(stats.getConstitution()));

    // Destacar atributo principal
    for (int i = 0; i < 6; i++) {
      attributeLabels[i].setForeground(TEXT_COLOR);
    }

    if (selectedClass.equalsIgnoreCase("warrior")) {
      attributeLabels[0].setForeground(PRIMARY_COLOR);
    } else if (selectedClass.equalsIgnoreCase("hunter")) {
      attributeLabels[1].setForeground(PRIMARY_COLOR);
    } else if (selectedClass.equalsIgnoreCase("mage")) {
      attributeLabels[2].setForeground(PRIMARY_COLOR);
    }

    // Atualizar bônus
    bonusLabels[0].setText("Dano: +" + Math.max(0, stats.getStrength() - 5) + " | Slots: " + stats.getInventorySlots());
    bonusLabels[1].setText("Dano: +" + Math.max(0, stats.getDexterity() - 5) + " | Evasão: "
        + String.format("%.0f%%", stats.getEvasionChance() * 100));
    bonusLabels[2].setText("Dano: +" + Math.max(0, stats.getIntelligence() - 5) + " | Projéteis mágicos");
    bonusLabels[3].setText("XP: +" + String.format("%.0f%%", (stats.getXpMultiplier() - 1) * 100));
    bonusLabels[4].setText("Intimidação (futuro)");
    bonusLabels[5].setText(
        "Vida: " + stats.getMaxHealth() + "HP | Def: -" + String.format("%.0f%%", stats.getDamageReduction() * 100));

    // Atualizar pontos restantes
    int remaining = stats.getRemainingPoints();
    pointsLabel.setText("Pontos restantes: " + remaining);
    if (remaining < 0) {
      pointsLabel.setForeground(WARNING_COLOR);
    } else if (remaining == 0) {
      pointsLabel.setForeground(new Color(150, 255, 150));
    } else {
      pointsLabel.setForeground(TEXT_COLOR);
    }

    // Habilitar/desabilitar botões
    for (int i = 0; i < 6; i++) {
      plusButtons[i].setEnabled(remaining > 0 && getCurrentAttributeValue(i) < CharacterStats.MAX_ATTRIBUTE);
      minusButtons[i].setEnabled(getCurrentAttributeValue(i) > CharacterStats.MIN_ATTRIBUTE);
    }

    confirmButton.setEnabled(stats.isValidDistribution());
  }

  private int getCurrentAttributeValue(int index) {
    switch (index) {
      case 0:
        return stats.getStrength();
      case 1:
        return stats.getDexterity();
      case 2:
        return stats.getIntelligence();
      case 3:
        return stats.getWisdom();
      case 4:
        return stats.getCharisma();
      case 5:
        return stats.getConstitution();
      default:
        return 5;
    }
  }

  private void startGame() {
    String spritePath = "sprites/" + selectedClass + "Player.png";

    // Remover a tela atual
    parentFrame.getContentPane().removeAll();

    // Criar o painel do jogo
    GamePanel gamePanel = new GamePanel();
    gamePanel.setPlayerClass(selectedClass, spritePath, stats);

    parentFrame.add(gamePanel);
    parentFrame.revalidate();
    parentFrame.repaint();

    gamePanel.requestFocusInWindow();
  }
}