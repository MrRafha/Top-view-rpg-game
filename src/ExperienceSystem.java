/**
 * Sistema de experiência e níveis do jogador.
 */
public class ExperienceSystem {
  private int currentLevel;
  private int currentXp;
  private int xpToNextLevel;

  // Configurações do sistema
  private static final int BASE_XP_REQUIRED = 50;
  private static final double XP_MULTIPLIER = 0.20; // 20%

  /**
   * Construtor do sistema de experiência.
   */
  public ExperienceSystem() {
    this.currentLevel = 1;
    this.currentXp = 0;
    this.xpToNextLevel = BASE_XP_REQUIRED;
  }

  /**
   * Adiciona experiência ao jogador.
   */
  public boolean addExperience(int xp) {
    currentXp += xp;

    boolean leveledUp = false;

    // Verificar se subiu de nível
    while (currentXp >= xpToNextLevel) {
      levelUp();
      leveledUp = true;
    }

    return leveledUp;
  }

  /**
   * Faz o jogador subir de nível.
   */
  private void levelUp() {
    currentXp -= xpToNextLevel; // XP excedente vai para o próximo nível
    currentLevel++;

    // Calcular XP necessário para o próximo nível
    // Fórmula: XP anterior + 20% do XP anterior
    xpToNextLevel = (int) Math.ceil(xpToNextLevel * (1 + XP_MULTIPLIER));

    System.out.println("LEVEL UP! Nível " + currentLevel + " alcançado!");
    System.out.println("XP necessário para próximo nível: " + xpToNextLevel);
  }

  /**
   * Calcula a porcentagem de progresso para o próximo nível.
   */
  public float getProgressPercentage() {
    return (float) currentXp / xpToNextLevel;
  }

  /**
   * Calcula XP necessário baseado no nível.
   */
  public static int calculateXpRequired(int level) {
    if (level <= 1)
      return BASE_XP_REQUIRED;

    int xp = BASE_XP_REQUIRED;
    for (int i = 2; i <= level; i++) {
      xp = (int) Math.ceil(xp * (1 + XP_MULTIPLIER));
    }
    return xp;
  }

  // Getters
  public int getCurrentLevel() {
    return currentLevel;
  }

  public int getCurrentXp() {
    return currentXp;
  }

  public int getXpToNextLevel() {
    return xpToNextLevel;
  }

  public int getTotalXpForCurrentLevel() {
    // XP total que o jogador já teve (incluindo níveis anteriores)
    int totalXp = currentXp;
    int tempXp = BASE_XP_REQUIRED;

    for (int i = 1; i < currentLevel; i++) {
      totalXp += tempXp;
      tempXp = (int) Math.ceil(tempXp * (1 + XP_MULTIPLIER));
    }

    return totalXp;
  }

  // Para debugging
  @Override
  public String toString() {
    return String.format("Level %d (%d/%d XP) - %.1f%%",
        currentLevel, currentXp, xpToNextLevel, getProgressPercentage() * 100);
  }
}