package com.rpggame.systems;

import com.rpggame.entities.GoblinFamily;
import java.util.List;
import java.util.Random;

/**
 * Sistema de reunião do conselho de líderes goblin
 * Os líderes se reúnem para tomar decisões estratégicas
 */
public class GoblinCouncil {
    
    // Enumeração das decisões possíveis
    public enum CouncilDecision {
        ALLIANCE_AGAINST_PLAYER,  // Aliança temporária contra o player (5 min)
        GOBLIN_EMPIRE,           // Junção permanente em um império
        TECHNOLOGICAL_ADVANCE,   // Avanço tecnológico (dobro de força)
        NO_DECISION             // Nenhuma decisão tomada
    }
    
    // Estado atual do conselho
    private CouncilDecision currentDecision = CouncilDecision.NO_DECISION;
    private int decisionTimer = 0;
    private static final int ALLIANCE_DURATION = 18000; // 5 minutos em frames (60fps * 60s * 5)
    
    // Modificadores de força
    private boolean technologicalAdvanceActive = false;
    private double strengthMultiplier = 1.0;
    
    // Controle de reuniões
    private int timeSinceLastCouncil = 0;
    private static final int COUNCIL_COOLDOWN = 3600; // 1 minuto entre reuniões
    private int familiesDestroyed = 0;
    
    private Random random = new Random();
    
    /**
     * Atualiza o sistema do conselho
     */
    public void update() {
        timeSinceLastCouncil++;
        
        // Decrementar timer de decisão
        if (decisionTimer > 0) {
            decisionTimer--;
            if (decisionTimer <= 0 && currentDecision == CouncilDecision.ALLIANCE_AGAINST_PLAYER) {
                System.out.println("⏰ A aliança goblin contra o jogador chegou ao fim!");
                currentDecision = CouncilDecision.NO_DECISION;
            }
        }
    }
    
    /**
     * Verifica se é hora de convocar uma reunião
     */
    public boolean shouldConveneCouncil(List<GoblinFamily> families) {
        // Não convocar se está em cooldown
        if (timeSinceLastCouncil < COUNCIL_COOLDOWN) {
            return false;
        }
        
        // Não convocar se já houver um império
        if (currentDecision == CouncilDecision.GOBLIN_EMPIRE) {
            return false;
        }
        
        // Precisa de pelo menos 2 famílias para reunião
        if (families.size() < 2) {
            return false;
        }
        
        // Chance aumentada: ~1 reunião a cada 45-60 segundos
        double baseChance = 0.0003; // ~3x mais frequente
        double destroyedFamilyBonus = familiesDestroyed * 0.0005; // Bônus maior quando famílias são destruídas
        
        return random.nextDouble() < (baseChance + destroyedFamilyBonus);
    }
    
    /**
     * Convoca reunião do conselho e toma decisão
     */
    public CouncilDecision conveneCouncil(List<GoblinFamily> families) {
        if (families.isEmpty()) {
            return CouncilDecision.NO_DECISION;
        }
        
        System.out.println("\n🏛️ ===== CONSELHO GOBLIN CONVOCADO =====");
        System.out.println("📍 Líderes de " + families.size() + " famílias se reúnem...");
        
        // Resetar cooldown
        timeSinceLastCouncil = 0;
        
        // Calcular pesos das decisões
        double allianceWeight = 30.0; // 30% base
        double empireWeight = 20.0;   // 20% base
        double techWeight = 50.0;     // 50% base
        
        // Bônus se famílias foram destruídas (+30% para aliança e tech)
        if (familiesDestroyed > 0) {
            allianceWeight += 30.0;
            techWeight += 30.0;
            System.out.println("⚠️ Famílias destruídas aumentam urgência de ação!");
        }
        
        // Normalizar pesos
        double total = allianceWeight + empireWeight + techWeight;
        allianceWeight /= total;
        empireWeight /= total;
        techWeight /= total;
        
        // Escolher decisão baseada nos pesos
        double roll = random.nextDouble();
        CouncilDecision decision;
        
        if (roll < allianceWeight) {
            decision = CouncilDecision.ALLIANCE_AGAINST_PLAYER;
        } else if (roll < allianceWeight + empireWeight) {
            decision = CouncilDecision.GOBLIN_EMPIRE;
        } else {
            decision = CouncilDecision.TECHNOLOGICAL_ADVANCE;
        }
        
        // Aplicar decisão
        applyDecision(decision, families);
        
        return decision;
    }
    
    /**
     * Aplica a decisão tomada pelo conselho
     */
    private void applyDecision(CouncilDecision decision, List<GoblinFamily> families) {
        currentDecision = decision;
        
        switch (decision) {
            case ALLIANCE_AGAINST_PLAYER:
                System.out.println("⚔️ DECISÃO: ALIANÇA CONTRA O JOGADOR!");
                System.out.println("   Duração: 5 minutos");
                System.out.println("   Todos os goblins focarão apenas no player!");
                decisionTimer = ALLIANCE_DURATION;
                
                // Cessar todas as guerras
                ceaseFire(families);
                break;
                
            case GOBLIN_EMPIRE:
                System.out.println("👑 DECISÃO: IMPÉRIO GOBLIN FORMADO!");
                System.out.println("   Todas as famílias se unem sob um único líder!");
                
                // Unir todas as famílias
                formEmpire(families);
                
                // Cessar todas as guerras
                ceaseFire(families);
                break;
                
            case TECHNOLOGICAL_ADVANCE:
                System.out.println("🔧 DECISÃO: AVANÇO TECNOLÓGICO!");
                System.out.println("   Força de todos os goblins DOBRADA!");
                technologicalAdvanceActive = true;
                strengthMultiplier = 2.0;
                break;
                
            default:
                break;
        }
        
        System.out.println("=====================================\n");
    }
    
    /**
     * Cessar fogo entre todas as famílias
     */
    private void ceaseFire(List<GoblinFamily> families) {
        boolean hadWars = false;
        for (GoblinFamily family : families) {
            if (family.isAtWar()) {
                hadWars = true;
                family.endWar();
            }
        }
        
        if (hadWars) {
            System.out.println("🕊️ Todas as guerras entre famílias foram cessadas!");
        }
    }
    
    /**
     * Forma império unindo todas as famílias
     */
    private void formEmpire(List<GoblinFamily> families) {
        if (families.isEmpty()) return;
        
        // A primeira família vira o império
        GoblinFamily empire = families.get(0);
        
        // Transferir todos os membros das outras famílias
        for (int i = 1; i < families.size(); i++) {
            GoblinFamily family = families.get(i);
            for (com.rpggame.entities.Goblin goblin : family.getMembers()) {
                empire.addMember(goblin);
            }
        }
        
        // Renomear para império
        empire.setFamilyName("IMPÉRIO GOBLIN");
    }
    
    /**
     * Registra que uma família foi destruída
     */
    public void registerFamilyDestroyed() {
        familiesDestroyed++;
        System.out.println("💀 Família goblin destruída! Total: " + familiesDestroyed);
        System.out.println("   Chance de reunião de emergência aumentada!");
    }
    
    // Getters
    public CouncilDecision getCurrentDecision() {
        return currentDecision;
    }
    
    public boolean isAllianceAgainstPlayerActive() {
        return currentDecision == CouncilDecision.ALLIANCE_AGAINST_PLAYER && decisionTimer > 0;
    }
    
    public boolean isGoblinEmpireActive() {
        return currentDecision == CouncilDecision.GOBLIN_EMPIRE;
    }
    
    public double getStrengthMultiplier() {
        return strengthMultiplier;
    }
    
    public boolean isTechnologicalAdvanceActive() {
        return technologicalAdvanceActive;
    }
    
    public int getAllianceTimeRemaining() {
        return decisionTimer;
    }
}
