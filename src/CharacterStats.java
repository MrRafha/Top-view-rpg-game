/**
 * Classe para gerenciar os atributos/estatísticas do personagem
 * 
 * BUILDS EXEMPLO:
 * 
 * TANQUE (Guerreiro): FOR:7 DEX:3 INT:3 SAB:5 CAR:5 CON:7
 * - Alto dano corpo a corpo, muitos slots, alta defesa
 * 
 * EVASIVO (Caçador): FOR:3 DEX:10 INT:3 SAB:5 CAR:4 CON:5
 * - 25% evasão, alto dano à distância, esquiva de ataques
 * 
 * GLASS CANNON (Mago): FOR:3 DEX:3 INT:10 SAB:7 CAR:5 CON:2
 * - Dano mágico máximo, XP extra, mas muito frágil
 * 
 * EXPLORADOR: FOR:5 DEX:4 INT:3 SAB:10 CAR:6 CON:2
 * - Máximo XP (+25%), foco em progressão rápida
 * 
 * SOBREVIVENTE: FOR:4 DEX:8 INT:3 SAB:6 CAR:4 CON:5
 * - 15% evasão + 5% XP, build defensiva balanceada
 * 
 * HÍBRIDO: FOR:6 DEX:6 INT:6 SAB:4 CAR:4 CON:4
 * - Balanceado: 5% evasão, bom dano, versátil
 */
public class CharacterStats {
  // Atributos base
  private int strength; // Força - atributo principal do Guerreiro
  private int dexterity; // Destreza - atributo principal do Caçador
  private int intelligence; // Inteligência - atributo principal do Mago
  private int wisdom; // Sabedoria - aumenta campo de visão e XP
  private int charisma; // Carisma - para interações com NPCs
  private int constitution; // Constituição - aumenta vida do personagem

  // Constantes
  public static final int BASE_ATTRIBUTE = 5;
  public static final int EXTRA_POINTS = 5;
  public static final int MIN_ATTRIBUTE = 1;
  public static final int MAX_ATTRIBUTE = 10;

  // Classe do personagem
  private String playerClass;

  public CharacterStats(String playerClass) {
    this.playerClass = playerClass;
    // Inicializar todos os atributos com valor base
    resetToDefault();
  }

  public void resetToDefault() {
    strength = BASE_ATTRIBUTE;
    dexterity = BASE_ATTRIBUTE;
    intelligence = BASE_ATTRIBUTE;
    wisdom = BASE_ATTRIBUTE;
    charisma = BASE_ATTRIBUTE;
    constitution = BASE_ATTRIBUTE;
  }

  // Getters
  public int getStrength() {
    return strength;
  }

  public int getDexterity() {
    return dexterity;
  }

  public int getIntelligence() {
    return intelligence;
  }

  public int getWisdom() {
    return wisdom;
  }

  public int getCharisma() {
    return charisma;
  }

  public int getConstitution() {
    return constitution;
  }

  public String getPlayerClass() {
    return playerClass;
  }

  // Setters com validação
  public boolean setStrength(int value) {
    if (value >= MIN_ATTRIBUTE && value <= MAX_ATTRIBUTE) {
      strength = value;
      return true;
    }
    return false;
  }

  public boolean setDexterity(int value) {
    if (value >= MIN_ATTRIBUTE && value <= MAX_ATTRIBUTE) {
      dexterity = value;
      return true;
    }
    return false;
  }

  public boolean setIntelligence(int value) {
    if (value >= MIN_ATTRIBUTE && value <= MAX_ATTRIBUTE) {
      intelligence = value;
      return true;
    }
    return false;
  }

  public boolean setWisdom(int value) {
    if (value >= MIN_ATTRIBUTE && value <= MAX_ATTRIBUTE) {
      wisdom = value;
      return true;
    }
    return false;
  }

  public boolean setCharisma(int value) {
    if (value >= MIN_ATTRIBUTE && value <= MAX_ATTRIBUTE) {
      charisma = value;
      return true;
    }
    return false;
  }

  public boolean setConstitution(int value) {
    if (value >= MIN_ATTRIBUTE && value <= MAX_ATTRIBUTE) {
      constitution = value;
      return true;
    }
    return false;
  }

  // Calcular pontos gastos
  public int getTotalPoints() {
    return strength + dexterity + intelligence + wisdom + charisma + constitution;
  }

  public int getUsedPoints() {
    return getTotalPoints() - (BASE_ATTRIBUTE * 6);
  }

  public int getRemainingPoints() {
    return EXTRA_POINTS - getUsedPoints();
  }

  // Calcular bônus baseado na classe
  public int getDamageBonus() {
    switch (playerClass.toLowerCase()) {
      case "warrior":
        return strength - BASE_ATTRIBUTE;
      case "hunter":
        return dexterity - BASE_ATTRIBUTE;
      case "mage":
        return intelligence - BASE_ATTRIBUTE;
      default:
        return 0;
    }
  }

  // Calcular vida baseada na constituição
  public int getMaxHealth() {
    int baseHealth = 100;
    int constitutionBonus = (constitution - BASE_ATTRIBUTE) * 10;
    return baseHealth + constitutionBonus;
  }

  // Calcular bônus de XP baseado na sabedoria
  public float getXpMultiplier() {
    int wisdomBonus = wisdom - BASE_ATTRIBUTE;
    return 1.0f + (wisdomBonus / 2.0f * 0.05f); // 5% por cada 2 pontos
  }

  // Calcular chance de evasão baseada na destreza
  public float getEvasionChance() {
    int dexterityBonus = dexterity - BASE_ATTRIBUTE;
    return Math.min(0.50f, dexterityBonus * 0.05f); // 5% por ponto, máximo 50%
  }

  // Calcular slots de inventário baseado na força
  public int getInventorySlots() {
    int baseSlots = 10;
    int strengthBonus = strength - BASE_ATTRIBUTE;
    return baseSlots + (strengthBonus * 2); // 2 slots por ponto de força
  }

  // Calcular redução de dano baseada na constituição
  public float getDamageReduction() {
    int constitutionBonus = constitution - BASE_ATTRIBUTE;
    return (constitutionBonus / 2.0f * 0.01f); // 1% por cada 2 pontos
  }

  // Validar se a distribuição de pontos é válida
  public boolean isValidDistribution() {
    return getRemainingPoints() >= 0 &&
        strength >= MIN_ATTRIBUTE && strength <= MAX_ATTRIBUTE &&
        dexterity >= MIN_ATTRIBUTE && dexterity <= MAX_ATTRIBUTE &&
        intelligence >= MIN_ATTRIBUTE && intelligence <= MAX_ATTRIBUTE &&
        wisdom >= MIN_ATTRIBUTE && wisdom <= MAX_ATTRIBUTE &&
        charisma >= MIN_ATTRIBUTE && charisma <= MAX_ATTRIBUTE &&
        constitution >= MIN_ATTRIBUTE && constitution <= MAX_ATTRIBUTE;
  }

  @Override
  public String toString() {
    return String.format("STR:%d DEX:%d INT:%d WIS:%d CHA:%d CON:%d",
        strength, dexterity, intelligence, wisdom, charisma, constitution);
  }
}