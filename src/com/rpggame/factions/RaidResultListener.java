package com.rpggame.factions;

/**
 * Callback de resultado de raid — quebra a dependência circular
 * entre RaidManager e FactionWarManager.
 */
public interface RaidResultListener {
    void onRaidSucceeded(FactionType attacker, String targetMapId);
    void onRaidFailed(FactionType attacker, String targetMapId);
}
