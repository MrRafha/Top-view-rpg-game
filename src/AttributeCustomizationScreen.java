import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Tela de customização de atributos do personagem
 */
public class AttributeCustomizationScreen extends JPanel implements ActionListener {
  private JFrame parentFrame;
  private String selectedClass;
  private String spritePath;
  private CharacterStats stats;

  // Componentes da interface
  private JLabel titleLabel;
  private JLabel pointsLabel;
  private JLabel classLabel;
  private JButton confirmButton;
  private JButton resetButton;

  // Componentes dos atributos
  private JLabel[] attributeLabels = new JLabel[6];
  private JLabel[] valueLabels = new JLabel[6];
  private JButton[] minusButtons = new JButton[6];
  private JButton[] plusButtons = new JButton[6];
  private JLabel[] bonusLabels = new JLabel[6];

  // Nomes dos atributos
  private final String[] ATTRIBUTE_NAMES = {
      "Força", "Destreza", "Inteligência", "Sabedoria", "Carisma", "Constituição"
  };

  private final String[] ATTRIBUTE_DESCRIPTIONS = {
      "(Atributo principal do Guerreiro)",
      "(Atributo principal do Caçador)",
      "(Atributo principal do Mago)",
      "(Campo de visão e XP +1% a cada 2 pts)",
      "(Interações com NPCs)",
      "(Vida do personagem +10HP por ponto)"
  };

  // Cores do tema
  private final Color BACKGROUND_COLOR = new Color(20, 30, 40);
  private final Color PANEL_COLOR = new Color(40, 50, 60);
  private final Color TEXT_COLOR = new Color(220, 220, 220);
  private final Color ACCENT_COLOR = new Color(100, 150, 200);
  private final Color PRIMARY_COLOR = new Color(255, 215, 0); // Dourado para atributo principal
  private final Color WARNING_COLOR = new Color(255, 100, 100);

  public AttributeCustomizationScreen(JFrame parentFrame, String selectedClass, String spritePath) {
    this.parentFrame = parentFrame;
    this.selectedClass = selectedClass;
    this.spritePath = spritePath;
    this.stats = new CharacterStats(selectedClass);

    initializeUI();
    updateInterface();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    setBackground(BACKGROUND_COLOR);

    // Painel superior com título e informações
    JPanel topPanel = createTopPanel();
    add(topPanel, BorderLayout.NORTH);

    // Painel central com atributos
    JPanel centerPanel = createAttributesPanel();
    add(centerPanel, BorderLayout.CENTER);

    // Painel inferior com botões
    JPanel bottomPanel = createBottomPanel();
    add(bottomPanel, BorderLayout.SOUTH);
  }

  private JPanel createTopPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(BACKGROUND_COLOR);
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

    GridBagConstraints gbc = new GridBagConstraints();

    // Título
    titleLabel = new JLabel("CUSTOMIZAÇÃO DE ATRIBUTOS");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
    titleLabel.setForeground(TEXT_COLOR);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.CENTER;
    panel.add(titleLabel, gbc);

    // Classe selecionada
    classLabel = new JLabel("Classe: " + selectedClass.toUpperCase());
    classLabel.setFont(new Font("Arial", Font.BOLD, 18));
    classLabel.setForeground(PRIMARY_COLOR);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(15, 0, 0, 0);
    panel.add(classLabel, gbc);

    // Pontos disponíveis
    pointsLabel = new JLabel();
    pointsLabel.setFont(new Font("Arial", Font.BOLD, 18));
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.EAST;
    panel.add(pointsLabel, gbc);

    return panel;
  }

  private JPanel createAttributesPanel() {
    JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.setBackground(BACKGROUND_COLOR);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 10, 8, 10);

    // Cabeçalhos
    JLabel nameHeader = new JLabel("ATRIBUTO");
    nameHeader.setFont(new Font("Arial", Font.BOLD, 14));
    nameHeader.setForeground(TEXT_COLOR);
    gbc.gridx = 0;
    gbc.gridy = 0;
    mainPanel.add(nameHeader, gbc);

    JLabel valueHeader = new JLabel("VALOR");
    valueHeader.setFont(new Font("Arial", Font.BOLD, 14));
    valueHeader.setForeground(TEXT_COLOR);
    gbc.gridx = 2;
    gbc.gridy = 0;
    mainPanel.add(valueHeader, gbc);

    JLabel bonusHeader = new JLabel("EFEITO");
    bonusHeader.setFont(new Font("Arial", Font.BOLD, 14));
    bonusHeader.setForeground(TEXT_COLOR);
    gbc.gridx = 4;
    gbc.gridy = 0;
    mainPanel.add(bonusHeader, gbc);

    // Criar linha para cada atributo
    for (int i = 0; i < 6; i++) {
      createAttributeRow(mainPanel, i, gbc);
    }

    return mainPanel;
  }

  private void createAttributeRow(JPanel panel, int index, GridBagConstraints gbc) {
    int row = index + 1;

    // Nome do atributo + descrição
    JPanel namePanel = new JPanel();
    namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
    namePanel.setBackground(BACKGROUND_COLOR);

    attributeLabels[index] = new JLabel(ATTRIBUTE_NAMES[index]);
    attributeLabels[index].setFont(new Font("Arial", Font.BOLD, 16));

    JLabel descLabel = new JLabel(ATTRIBUTE_DESCRIPTIONS[index]);
    descLabel.setFont(new Font("Arial", Font.PLAIN, 11));
    descLabel.setForeground(new Color(180, 180, 180));

    namePanel.add(attributeLabels[index]);
    namePanel.add(descLabel);

    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(namePanel, gbc);

    // Botão de diminuir
    minusButtons[index] = new JButton("-");
    minusButtons[index].setPreferredSize(new Dimension(40, 30));
    minusButtons[index].setActionCommand("minus_" + index);
    minusButtons[index].addActionListener(this);
    minusButtons[index].setBackground(WARNING_COLOR);
    minusButtons[index].setForeground(Color.WHITE);
    gbc.gridx = 1;
    gbc.gridy = row;
    gbc.anchor = GridBagConstraints.CENTER;
    panel.add(minusButtons[index], gbc);

    // Valor atual
    valueLabels[index] = new JLabel("5");
    valueLabels[index].setFont(new Font("Arial", Font.BOLD, 18));
    valueLabels[index].setForeground(TEXT_COLOR);
    valueLabels[index].setHorizontalAlignment(SwingConstants.CENTER);
    valueLabels[index].setPreferredSize(new Dimension(40, 30));
    gbc.gridx = 2;
    gbc.gridy = row;
    panel.add(valueLabels[index], gbc);

    // Botão de aumentar
    plusButtons[index] = new JButton("+");
    plusButtons[index].setPreferredSize(new Dimension(40, 30));
    plusButtons[index].setActionCommand("plus_" + index);
    plusButtons[index].addActionListener(this);
    plusButtons[index].setBackground(ACCENT_COLOR);
    plusButtons[index].setForeground(Color.WHITE);
    gbc.gridx = 3;
    gbc.gridy = row;
    panel.add(plusButtons[index], gbc);

    // Bônus/Efeito
    bonusLabels[index] = new JLabel();
    bonusLabels[index].setFont(new Font("Arial", Font.PLAIN, 13));
    bonusLabels[index].setForeground(new Color(150, 255, 150));
    gbc.gridx = 4;
    gbc.gridy = row;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(bonusLabels[index], gbc);
  }

  private JPanel createBottomPanel() {
    JPanel panel = new JPanel(new FlowLayout());
    panel.setBackground(BACKGROUND_COLOR);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

    // Botão Reset
    resetButton = new JButton("RESETAR");
    resetButton.setFont(new Font("Arial", Font.BOLD, 14));
    resetButton.setPreferredSize(new Dimension(120, 40));
    resetButton.addActionListener(this);
    resetButton.setActionCommand("reset");
    resetButton.setBackground(WARNING_COLOR);
    resetButton.setForeground(Color.WHITE);
    panel.add(resetButton);

    // Espaço
    panel.add(Box.createRigidArea(new Dimension(20, 0)));

    // Botão Confirmar
    confirmButton = new JButton("CONFIRMAR E JOGAR");
    confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
    confirmButton.setPreferredSize(new Dimension(200, 40));
    confirmButton.addActionListener(this);
    confirmButton.setActionCommand("confirm");
    confirmButton.setBackground(ACCENT_COLOR);
    confirmButton.setForeground(Color.WHITE);
    panel.add(confirmButton);

    return panel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();

    if (command.equals("reset")) {
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
    // Atualizar valores
    valueLabels[0].setText(String.valueOf(stats.getStrength()));
    valueLabels[1].setText(String.valueOf(stats.getDexterity()));
    valueLabels[2].setText(String.valueOf(stats.getIntelligence()));
    valueLabels[3].setText(String.valueOf(stats.getWisdom()));
    valueLabels[4].setText(String.valueOf(stats.getCharisma()));
    valueLabels[5].setText(String.valueOf(stats.getConstitution()));

    // Destacar atributo principal da classe
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

    // Atualizar bônus/efeitos
    bonusLabels[0].setText("Dano: +" + Math.max(0, stats.getStrength() - 5));
    bonusLabels[1].setText("Dano: +" + Math.max(0, stats.getDexterity() - 5));
    bonusLabels[2].setText("Dano: +" + Math.max(0, stats.getIntelligence() - 5));
    bonusLabels[3].setText(String.format("Visão/XP: +%.0f%%", (stats.getWisdom() - 5) / 2.0f));
    bonusLabels[4].setText("Interação com NPCs");
    bonusLabels[5].setText("Vida: " + stats.getMaxHealth() + " HP");

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
    // Remover a tela de customização
    parentFrame.getContentPane().removeAll();

    // Criar o painel do jogo
    GamePanel gamePanel = new GamePanel();

    // Definir a classe e atributos do jogador
    gamePanel.setPlayerClass(selectedClass, spritePath, stats);

    parentFrame.add(gamePanel);
    parentFrame.revalidate();
    parentFrame.repaint();

    // Focar no jogo para receber input
    gamePanel.requestFocusInWindow();
  }
}