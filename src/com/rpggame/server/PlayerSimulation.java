package com.rpggame.server;

import com.rpggame.entities.Player;
import com.rpggame.shared.InputPacket;

/**
 * Aplica {@link InputPacket} ao {@link Player} e avança a simulação dele.
 *
 * Na Fase 3, PlayerSimulation é uma camada fina entre o input do cliente
 * e o Player existente: ela traduz o contrato InputPacket para as flags
 * internas (up/down/left/right/spacePressed) sem reescrever Player.
 *
 * Isso isola o Player de KeyEvent — apenas PlayerSimulation conhece a
 * semântica do input, e o resto do servidor opera com InputPacket.
 *
 * Nas fases seguintes, quando Player for headless (sem Graphics2D), a lógica
 * de movimento e ataque pode migrar inteiramente para cá.
 */
public class PlayerSimulation {

  private final Player player;
  private long lastProcessedFrame = -1;

  public PlayerSimulation(Player player) {
    this.player = player;
  }

  /**
   * Aplica o packet ao estado do Player.
   *
   * Modo cliente-autoritativo (playerX/Y != 0):
   *   O cliente envia sua posição calculada localmente a 60 FPS.
   *   O servidor apenas espelha essa posição — sem recalcular movimento.
   *   Isso elimina rollback/rubber-band causado pela diferença de tick rate.
   *
   * Modo legado in-process (playerX/Y == 0):
   *   Mantém o comportamento anterior: seta flags e deixa player.update() calcular.
   */
  public void applyInput(InputPacket packet) {
    if (packet == null) return;
    // Long.MIN_VALUE é sentinel de "zerar movimento" — sempre processar.
    // Frames normais (>= 1) são deduplicados para evitar double-apply.
    if (packet.getClientFrame() != Long.MIN_VALUE) {
      if (packet.getClientFrame() == lastProcessedFrame) return;
      lastProcessedFrame = packet.getClientFrame();
    }

    // Modo cliente-autoritativo: posição vinda do cliente tem prioridade.
    if (packet.getPlayerX() != 0.0 || packet.getPlayerY() != 0.0) {
      player.setPosition(packet.getPlayerX(), packet.getPlayerY());
    }

    player.setInputUp(packet.isUp());
    player.setInputDown(packet.isDown());
    player.setInputLeft(packet.isLeft());
    player.setInputRight(packet.isRight());
    player.setInputAttack(packet.isAttack());

    // Skills são one-shot: só ativa se o packet as sinalizar
    if (packet.isSkill1()) player.triggerSkill(1);
    if (packet.isSkill2()) player.triggerSkill(2);
    if (packet.isSkill3()) player.triggerSkill(3);
    if (packet.isSkill4()) player.triggerSkill(4);
  }

  public Player getPlayer() {
    return player;
  }
}
