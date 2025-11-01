import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma família de goblins com território e hierarquia
 */
public class GoblinFamily {
    private static int nextFamilyId = 1;
    
    private int familyId;
    private String familyName;
    private ArrayList<Goblin> members;
    private Goblin leader;
    private Rectangle territory;
    private Point hutPosition;
    
    // Estados da família
    private boolean atWar = false;
    private GoblinFamily enemyFamily = null;
    private int aggressionLevel = 0; // 0-10, quanto maior, mais agressiva
    
    // Configurações de território
    private static final int TERRITORY_SIZE = 300; // Território maior para mapa 25x25
    private static final int MAX_FAMILY_SIZE = 4;
    
    /**
     * Construtor da família de goblins
     */
    public GoblinFamily(Point hutPosition, String name) {
        this.familyId = nextFamilyId++;
        this.familyName = name != null ? name : "Família " + familyId;
        this.hutPosition = new Point(hutPosition);
        this.members = new ArrayList<>();
        
        // Definir território ao redor da cabana
        this.territory = new Rectangle(
            hutPosition.x - TERRITORY_SIZE/2,
            hutPosition.y - TERRITORY_SIZE/2,
            TERRITORY_SIZE,
            TERRITORY_SIZE
        );
        
        // Nível de agressão aleatório (personalidade da família)
        this.aggressionLevel = (int)(Math.random() * 5) + 3; // 3-7
    }
    
    /**
     * Adiciona um goblin à família
     */
    public void addMember(Goblin goblin) {
        if (members.size() < MAX_FAMILY_SIZE) {
            goblin.setFamily(this);
            members.add(goblin);
            
            // O primeiro goblin adicionado se torna o líder
            if (leader == null && goblin.getPersonality() == GoblinPersonality.LEADER) {
                leader = goblin;
            }
        }
    }
    
    /**
     * Remove um goblin da família (quando morre)
     */
    public boolean removeMember(Goblin goblin) {
        members.remove(goblin);
        if (goblin == leader) {
            // Escolher novo líder
            electNewLeader();
        }
        
        // Retorna true se a família foi completamente derrotada
        return members.isEmpty();
    }
    
    /**
     * Elege um novo líder da família
     */
    private void electNewLeader() {
        leader = null;
        // Procurar por um líder nato
        for (Goblin goblin : members) {
            if (goblin.getPersonality() == GoblinPersonality.LEADER) {
                leader = goblin;
                return;
            }
        }
        // Se não houver líder nato, escolher o mais agressivo
        for (Goblin goblin : members) {
            if (goblin.getPersonality() == GoblinPersonality.AGGRESSIVE) {
                leader = goblin;
                return;
            }
        }
        // Último recurso: qualquer goblin
        if (!members.isEmpty()) {
            leader = members.get(0);
        }
    }
    
    /**
     * Verifica se uma posição está dentro do território da família
     */
    public boolean isInTerritory(double x, double y) {
        return territory.contains(x, y);
    }
    
    /**
     * Verifica se o player está no território
     */
    public boolean isPlayerInTerritory(Player player) {
        return isInTerritory(player.getX(), player.getY());
    }
    
  /**
   * Toma decisão sobre perseguir o player fora do território
   */
  public boolean shouldPursuePlayer(Player player) {
    if (leader == null) return false;
    
    // Se player está no território, sempre perseguir
    if (isPlayerInTerritory(player)) {
      return true;
    }
    
    // Fora do território, considerar intimidação do player
    double intimidationFactor = calculateIntimidationFactor(player);
    
    // Decisão baseada na agressividade ajustada pela intimidação
    double distanceToTerritory = getDistanceToTerritory(player.getX(), player.getY());
    double pursueThreshold = aggressionLevel * 30 * (1.0 - intimidationFactor); // Intimidação reduz perseguição
    
    boolean shouldPursue = distanceToTerritory < pursueThreshold;
    
    // Log da decisão do líder
    if (intimidationFactor > 0.3) {
      System.out.println("🛡️ " + familyName + " intimidado pelo carisma do jogador! Chance reduzida de perseguição.");
    }
    
    return shouldPursue;
  }
  
  /**
   * Calcula fator de intimidação baseado no carisma do player
   */
  private double calculateIntimidationFactor(Player player) {
    int playerCharisma = player.getStats().getCharisma();
    int playerLevel = player.getExperienceSystem().getCurrentLevel();
    
    // Fator base do carisma (0.0 a 0.5)
    double charismaFactor = Math.min(0.5, (playerCharisma - 5) * 0.05); // Carisma 5 = 0%, Carisma 15 = 50%
    
    // Bônus de nível (0.0 a 0.3)
    double levelFactor = Math.min(0.3, (playerLevel - 1) * 0.05); // Cada nível adiciona 5% até 30%
    
    // Ajuste pela personalidade do líder
    double personalityResistance = 1.0;
    if (leader != null) {
      switch (leader.getPersonality()) {
        case AGGRESSIVE:
          personalityResistance = 0.5; // Mais resistente à intimidação
          break;
        case LEADER:
          personalityResistance = 0.7; // Moderadamente resistente
          break;
        case TIMID:
          personalityResistance = 1.5; // Mais suscetível à intimidação
          break;
        case COMMON:
        default:
          personalityResistance = 1.0; // Resistência normal
          break;
      }
    }
    
    return Math.min(0.8, (charismaFactor + levelFactor) * personalityResistance);
  }    /**
     * Calcula distância de um ponto ao território
     */
    private double getDistanceToTerritory(double x, double y) {
        double dx = Math.max(0, Math.max(territory.x - x, x - (territory.x + territory.width)));
        double dy = Math.max(0, Math.max(territory.y - y, y - (territory.y + territory.height)));
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Declara guerra contra outra família
     */
    public void declareWarAgainst(GoblinFamily enemy) {
        this.atWar = true;
        this.enemyFamily = enemy;
        enemy.atWar = true;
        enemy.enemyFamily = this;
    }
    
    /**
     * Verifica se dois goblins são inimigos (famílias diferentes em guerra)
     */
    public boolean isEnemyOf(GoblinFamily otherFamily) {
        return this.atWar && this.enemyFamily == otherFamily;
    }
    
    // Getters
    public int getFamilyId() { return familyId; }
    public String getFamilyName() { return familyName; }
    public List<Goblin> getMembers() { return new ArrayList<>(members); }
    public Goblin getLeader() { return leader; }
    public Rectangle getTerritory() { return new Rectangle(territory); }
    public Point getHutPosition() { return new Point(hutPosition); }
    public boolean isAtWar() { return atWar; }
    public GoblinFamily getEnemyFamily() { return enemyFamily; }
    public int getAggressionLevel() { return aggressionLevel; }
    public int getMemberCount() { return members.size(); }
    
    /**
     * Verifica se a família foi derrotada (sem membros vivos)
     */
    public boolean isDefeated() {
        return members.isEmpty();
    }
}