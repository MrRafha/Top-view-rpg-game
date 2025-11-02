package com.rpggame.entities;

/**
 * Enum que define os tipos de personalidade dos goblins
 */
public enum GoblinPersonality {
    COMMON("sprites/CommonGoblin.png", 1.0, 1.0, 25, 8),
    AGGRESSIVE("sprites/AgresiveGoblin.png", 1.3, 1.2, 35, 12),
    TIMID("sprites/TinyGoblin.png", 0.8, 1.4, 15, 5),
    LEADER("sprites/goblinLeader.png", 1.1, 1.0, 40, 10);

    private final String spritePath;
    private final double strengthMultiplier;
    private final double speedMultiplier;
    private final int baseHealth;
    private final int baseDamage;

    GoblinPersonality(String spritePath, double strengthMultiplier, double speedMultiplier, 
                     int baseHealth, int baseDamage) {
        this.spritePath = spritePath;
        this.strengthMultiplier = strengthMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
    }

    public String getSpritePath() { return spritePath; }
    public double getStrengthMultiplier() { return strengthMultiplier; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public int getBaseHealth() { return baseHealth; }
    public int getBaseDamage() { return baseDamage; }
}