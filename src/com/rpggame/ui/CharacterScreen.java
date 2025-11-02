package com.rpggame.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.rpggame.core.GamePanel;
import com.rpggame.entities.Player;
import com.rpggame.systems.CharacterStats;

/**
 * Tela de características do personagem onde o jogador pode distribuir pontos
 * de atributo.
 */
public class CharacterScreen extends JPanel implements ActionListener, KeyListener {
  private GamePanel gamePanel;
  private Player player;
  private CharacterStats stats;
  private Font font;
  private Font titleFont;

  // Componentes da interface
  private JButton[] increaseButtons;
  private JButton[] decreaseButtons;
  private JLabel[] attributeLabels;
  private JLabel[] attributeValueLabels;
  private JLabel availablePointsLabel;
  private JButton confirmButton;
  private JButton cancelButton;

  // Cores
  private Color backgroundColor = new Color(40, 40, 50);
  private Color panelColor = new Color(60, 60, 70);
  private Color textColor = Color.WHITE;
  private Color buttonColor = new Color(80, 80, 90);
  private Color buttonHoverColor = new Color(100, 100, 110);

  // Atributos temporários para preview
  private int tempStrength;
  private int tempDexterity;
  private int tempIntelligence;
  private int tempWisdom;
  private int tempCharisma;
  private int tempConstitution;
  private int tempAvailablePoints;

  // Nomes dos atributos
  private String[] attributeNames = {
      "Força (STR)", "Destreza (DEX)", "Inteligência (INT)",
      "Sabedoria (WIS)", "Carisma (CHA)", "Constituição (CON)"
  };

  // Descrições dos atributos
  private String[] attributeDescriptions = {
      "Aumenta dano corpo a corpo e slots de inventário",
      "Aumenta chance de evasão e precisão",
      "Aumenta dano mágico e mana máxima",
      "Aumenta XP ganho e campo de visão",
      "Melhora interações com NPCs",
      "Aumenta vida máxima e resistência"
  };

  public CharacterScreen(GamePanel gamePanel, Player player) {
    this.gamePanel = gamePanel;
    this.player = player;
    this.stats = player.getStats();

    // Configurar fonts
    try {
      font = new Font("Arial", Font.BOLD, 16);
      titleFont = new Font("Arial", Font.BOLD, 24);
    } catch (Exception e) {
      font = new Font("SansSerif", Font.BOLD, 16);
      titleFont = new Font("SansSerif", Font.BOLD, 24);
    }

    initializeComponents();
    setupLayout();
    copyCurrentStats();

    setFocusable(true);
    addKeyListener(this);
    setBackground(backgroundColor);
  }

  private void initializeComponents() {
    // Inicializar arrays de componentes
    increaseButtons = new JButton[6];
    decreaseButtons = new JButton[6];
    attributeLabels = new JLabel[6];
    attributeValueLabels = new JLabel[6];

    // Criar labels dos atributos
    for (int i = 0; i < 6; i++) {
      attributeLabels[i] = new JLabel(attributeNames[i]);
      attributeLabels[i].setForeground(textColor);
      attributeLabels[i].setFont(font);

      attributeValueLabels[i] = new JLabel("5");
      attributeValueLabels[i].setForeground(textColor);
      attributeValueLabels[i].setFont(font);
      attributeValueLabels[i].setHorizontalAlignment(SwingConstants.CENTER);

      // Botões de aumentar/diminuir
      increaseButtons[i] = createStyledButton("+");
      decreaseButtons[i] = createStyledButton("-");

      final int index = i;
      increaseButtons[i].addActionListener(e -> increaseAttribute(index));
      decreaseButtons[i].addActionListener(e -> decreaseAttribute(index));
    }

    // Label de pontos disponíveis
    availablePointsLabel = new JLabel("Pontos disponíveis: 0");
    availablePointsLabel.setForeground(Color.YELLOW);
    availablePointsLabel.setFont(titleFont);
    availablePointsLabel.setHorizontalAlignment(SwingConstants.CENTER);

    // Botões de ação
    confirmButton = createStyledButton("Confirmar");
    cancelButton = createStyledButton("Cancelar");

    confirmButton.addActionListener(this);
    cancelButton.addActionListener(this);

    // Tamanho será definido no setupLayout()
  }

  private JButton createStyledButton(String text) {
    JButton button = new JButton(text);
    button.setBackground(buttonColor);
    button.setForeground(textColor);
    button.setFont(font);
    button.setBorder(BorderFactory.createRaisedBevelBorder());
    button.setFocusPainted(false);

    // Tamanho mínimo para botões pequenos (+/-)
    if (text.equals("+") || text.equals("-")) {
      button.setPreferredSize(new Dimension(30, 25));
    }

    // Efeito hover
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(buttonHoverColor);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(buttonColor);
      }
    });

    return button;
  }

  private void setupLayout() {
    setLayout(new BorderLayout());

    // Painel principal
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(backgroundColor);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    mainPanel.setPreferredSize(new Dimension(900, 600)); // Garantir tamanho adequado

    // Título
    JLabel titleLabel = new JLabel("Características do Personagem");
    titleLabel.setForeground(textColor);
    titleLabel.setFont(titleFont);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    // Painel de atributos
    JPanel attributesPanel = new JPanel(new GridBagLayout());
    attributesPanel.setBackground(panelColor);
    attributesPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createRaisedBevelBorder(),
        BorderFactory.createEmptyBorder(15, 15, 15, 15)));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 10, 5, 10);

    // Header
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    attributesPanel.add(availablePointsLabel, gbc);

    gbc.gridwidth = 1;
    gbc.gridy = 1;

    // Cabeçalhos das colunas
    JLabel headerAttr = new JLabel("Atributo");
    headerAttr.setForeground(Color.CYAN);
    headerAttr.setFont(font);
    gbc.gridx = 0;
    attributesPanel.add(headerAttr, gbc);

    JLabel headerValue = new JLabel("Valor");
    headerValue.setForeground(Color.CYAN);
    headerValue.setFont(font);
    gbc.gridx = 2;
    attributesPanel.add(headerValue, gbc);

    // Adicionar linhas de atributos
    for (int i = 0; i < 6; i++) {
      gbc.gridy = i + 2;

      // Nome do atributo
      gbc.gridx = 0;
      gbc.anchor = GridBagConstraints.WEST;
      attributesPanel.add(attributeLabels[i], gbc);

      // Botão diminuir
      gbc.gridx = 1;
      gbc.anchor = GridBagConstraints.CENTER;
      attributesPanel.add(decreaseButtons[i], gbc);

      // Valor do atributo
      gbc.gridx = 2;
      attributeValueLabels[i].setPreferredSize(new Dimension(50, 25));
      attributesPanel.add(attributeValueLabels[i], gbc);

      // Botão aumentar
      gbc.gridx = 3;
      attributesPanel.add(increaseButtons[i], gbc);
    }

    // Painel de informações (reduzido e com scroll)
    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.setBackground(backgroundColor);
    infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    infoPanel.setPreferredSize(new Dimension(300, 400)); // Limitar tamanho

    JTextArea infoText = new JTextArea(20, 25); // Definir linhas e colunas
    infoText.setBackground(panelColor);
    infoText.setForeground(textColor);
    infoText.setFont(new Font("Arial", Font.PLAIN, 11));
    infoText.setEditable(false);
    infoText.setLineWrap(true);
    infoText.setWrapStyleWord(true);
    infoText.setText(getStatsInfo());

    JScrollPane scrollPane = new JScrollPane(infoText);
    scrollPane.setPreferredSize(new Dimension(280, 350));
    scrollPane.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLoweredBevelBorder(),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));

    infoPanel.add(scrollPane, BorderLayout.CENTER);

    // Painel de botões (mais visível e mais para cima)
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
    buttonPanel.setBackground(backgroundColor);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 25, 10)); // Menos padding em cima, mais em baixo

    // Fazer botões maiores e mais visíveis
    confirmButton.setPreferredSize(new Dimension(120, 40));
    cancelButton.setPreferredSize(new Dimension(120, 40));
    confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
    cancelButton.setFont(new Font("Arial", Font.BOLD, 14));

    buttonPanel.add(confirmButton);
    buttonPanel.add(cancelButton);

    // Adicionar tudo ao painel principal
    mainPanel.add(titleLabel, BorderLayout.NORTH);
    mainPanel.add(attributesPanel, BorderLayout.CENTER);
    mainPanel.add(infoPanel, BorderLayout.EAST);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel, BorderLayout.CENTER);
  }

  private void copyCurrentStats() {
    tempStrength = stats.getStrength();
    tempDexterity = stats.getDexterity();
    tempIntelligence = stats.getIntelligence();
    tempWisdom = stats.getWisdom();
    tempCharisma = stats.getCharisma();
    tempConstitution = stats.getConstitution();
    tempAvailablePoints = player.getExperienceSystem().getAvailableAttributePoints();
    updateDisplay();
  }

  private void increaseAttribute(int attributeIndex) {
    if (tempAvailablePoints <= 0)
      return;

    int currentValue = getCurrentTempValue(attributeIndex);
    if (currentValue >= CharacterStats.MAX_ATTRIBUTE)
      return;

    setTempValue(attributeIndex, currentValue + 1);
    tempAvailablePoints--;
    updateDisplay();
  }

  private void decreaseAttribute(int attributeIndex) {
    int currentValue = getCurrentTempValue(attributeIndex);
    int originalValue = getOriginalValue(attributeIndex);

    if (currentValue <= originalValue)
      return;

    setTempValue(attributeIndex, currentValue - 1);
    tempAvailablePoints++;
    updateDisplay();
  }

  private int getCurrentTempValue(int index) {
    switch (index) {
      case 0:
        return tempStrength;
      case 1:
        return tempDexterity;
      case 2:
        return tempIntelligence;
      case 3:
        return tempWisdom;
      case 4:
        return tempCharisma;
      case 5:
        return tempConstitution;
      default:
        return 0;
    }
  }

  private int getOriginalValue(int index) {
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
        return 0;
    }
  }

  private void setTempValue(int index, int value) {
    switch (index) {
      case 0:
        tempStrength = value;
        break;
      case 1:
        tempDexterity = value;
        break;
      case 2:
        tempIntelligence = value;
        break;
      case 3:
        tempWisdom = value;
        break;
      case 4:
        tempCharisma = value;
        break;
      case 5:
        tempConstitution = value;
        break;
    }
  }

  private void updateDisplay() {
    // Atualizar valores dos atributos
    attributeValueLabels[0].setText(String.valueOf(tempStrength));
    attributeValueLabels[1].setText(String.valueOf(tempDexterity));
    attributeValueLabels[2].setText(String.valueOf(tempIntelligence));
    attributeValueLabels[3].setText(String.valueOf(tempWisdom));
    attributeValueLabels[4].setText(String.valueOf(tempCharisma));
    attributeValueLabels[5].setText(String.valueOf(tempConstitution));

    // Atualizar pontos disponíveis
    availablePointsLabel.setText("Pontos disponíveis: " + tempAvailablePoints);

    // Habilitar/desabilitar botões
    for (int i = 0; i < 6; i++) {
      increaseButtons[i].setEnabled(tempAvailablePoints > 0 &&
          getCurrentTempValue(i) < CharacterStats.MAX_ATTRIBUTE);
      decreaseButtons[i].setEnabled(getCurrentTempValue(i) > getOriginalValue(i));
    }

    // Atualizar botão confirmar
    boolean hasChanges = tempStrength != stats.getStrength() ||
        tempDexterity != stats.getDexterity() ||
        tempIntelligence != stats.getIntelligence() ||
        tempWisdom != stats.getWisdom() ||
        tempCharisma != stats.getCharisma() ||
        tempConstitution != stats.getConstitution();

    confirmButton.setEnabled(hasChanges);
  }

  private String getStatsInfo() {
    StringBuilder info = new StringBuilder();
    info.append("=== EFEITOS DOS ATRIBUTOS ===\n\n");

    for (int i = 0; i < attributeNames.length; i++) {
      info.append(attributeNames[i]).append(":\n");
      info.append("• ").append(attributeDescriptions[i]).append("\n\n");
    }

    info.append("=== CLASSE ATUAL ===\n");
    info.append("Classe: ").append(stats.getPlayerClass()).append("\n");
    info.append("Nível: ").append(player.getExperienceSystem().getCurrentLevel()).append("\n\n");

    info.append("=== BÔNUS ATUAIS ===\n");
    info.append("Vida máxima: ").append(stats.getMaxHealth()).append("\n");
    info.append("Mana máxima: ").append(stats.getMaxMana()).append("\n");
    info.append("Chance evasão: ").append(String.format("%.1f%%", stats.getEvasionChance() * 100)).append("\n");
    info.append("Multiplicador XP: ").append(String.format("%.2fx", stats.getXpMultiplier())).append("\n");
    info.append("Slots inventário: ").append(stats.getInventorySlots()).append("\n");

    return info.toString();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == confirmButton) {
      applyChanges();
      gamePanel.closeCharacterScreen();
    } else if (e.getSource() == cancelButton) {
      gamePanel.closeCharacterScreen();
    }
  }

  private void applyChanges() {
    // Aplicar as mudanças aos stats do personagem
    stats.setStrength(tempStrength);
    stats.setDexterity(tempDexterity);
    stats.setIntelligence(tempIntelligence);
    stats.setWisdom(tempWisdom);
    stats.setCharisma(tempCharisma);
    stats.setConstitution(tempConstitution);

    // Gastar os pontos de atributo utilizados
    int pointsUsed = player.getExperienceSystem().getAvailableAttributePoints() - tempAvailablePoints;
    for (int i = 0; i < pointsUsed; i++) {
      player.getExperienceSystem().spendAttributePoint();
    }

    // Atualizar vida e mana do jogador baseado nos novos stats
    player.updateStatsFromAttributes();

    System.out.println("Atributos atualizados: " + stats.toString());
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_C) {
      gamePanel.closeCharacterScreen();
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }
}