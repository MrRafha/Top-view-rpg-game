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
   * Aplica o packet ao estado de input do Player.
   * Chamado pelo ServerLoop antes de player.update().
   * Pacotes já processados (frame repetido) são ignorados.
   */
  public void applyInput(InputPacket packet) {
    if (packet == null) return;
    if (packet.getClientFrame() == lastProcessedFrame) return;
    lastProcessedFrame = packet.getClientFrame();

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
