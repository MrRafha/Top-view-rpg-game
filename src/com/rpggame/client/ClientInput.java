package com.rpggame.client;

import com.rpggame.server.InProcessTransport;
import com.rpggame.shared.InputPacket;

import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Captura o estado do teclado/mouse e produz {@link InputPacket} por frame.
 *
 * Esta classe é a única que conhece KeyEvent e coordenadas de tela.
 * O restante do sistema (PlayerSimulation, ServerLoop) opera apenas com
 * InputPacket — sem dependência de Swing.
 *
 * Uso em GamePanel:
 * <pre>
 *   clientInput = new ClientInput("player-1");
 *   // no keyPressed:
 *   clientInput.onKeyPressed(e);
 *   // no keyReleased:
 *   clientInput.onKeyReleased(e);
 *   // no update():
 *   InputPacket packet = clientInput.buildPacket();
 * </pre>
 */
public class ClientInput {

  private final String playerId;
  private final AtomicLong frameCounter = new AtomicLong(0);

  // Estado atual das teclas (volatile para thread safety entre EDT e game loop)
  private volatile boolean up, down, left, right;
  private volatile boolean attack;
  private volatile boolean interact;
  private volatile float mouseX, mouseY;

  // Skills são one-shot (ativadas e consumidas no mesmo packet)
  private volatile boolean skill1Pulse, skill2Pulse, skill3Pulse, skill4Pulse;

  // Fase 4: transporte opcional — quando definido, buildPacket() publica direto na fila
  private InProcessTransport transport;

  public ClientInput(String playerId) {
    this.playerId = playerId;
  }

  /**
   * Registra tecla pressionada. Chamado pelo KeyListener do GamePanel.
   */
  public void onKeyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_W: case KeyEvent.VK_UP:    up    = true; break;
      case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  down  = true; break;
      case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  left  = true; break;
      case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right = true; break;
      case KeyEvent.VK_SPACE:                     attack = true; break;
      case KeyEvent.VK_E:                         interact = true; break;
      case KeyEvent.VK_1: case KeyEvent.VK_NUMPAD1: skill1Pulse = true; break;
      case KeyEvent.VK_2: case KeyEvent.VK_NUMPAD2: skill2Pulse = true; break;
      case KeyEvent.VK_3: case KeyEvent.VK_NUMPAD3: skill3Pulse = true; break;
      case KeyEvent.VK_4: case KeyEvent.VK_NUMPAD4: skill4Pulse = true; break;
    }
  }

  /**
   * Registra tecla liberada. Chamado pelo KeyListener do GamePanel.
   */
  public void onKeyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_W: case KeyEvent.VK_UP:    up      = false; break;
      case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  down    = false; break;
      case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  left    = false; break;
      case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right   = false; break;
      case KeyEvent.VK_SPACE:                     attack  = false; break;
      case KeyEvent.VK_E:                         interact = false; break;
      // Skills são one-shot (pulse) — não há estado hold para limpar
    }
  }

  /**
   * Atualiza a posição do mouse (coordenadas de tela).
   */
  public void onMouseMoved(float x, float y) {
    this.mouseX = x;
    this.mouseY = y;
  }

  /**
   * Constrói um InputPacket com o estado atual e avança o contador de frames.
   * Skills pulse são consumidas (zeradas) após cada packet para garantir
   * que cada ativação seja processada exatamente uma vez.
   *
   * @param px posição X atual do player no cliente (modo cliente-autoritativo)
   * @param py posição Y atual do player no cliente (modo cliente-autoritativo)
   */
  public InputPacket buildPacket(double px, double py) {
    long frame = frameCounter.incrementAndGet();

    InputPacket packet = InputPacket.builder(playerId, frame)
        .up(up).down(down).left(left).right(right)
        .attack(attack)
        .skill1(skill1Pulse).skill2(skill2Pulse)
        .skill3(skill3Pulse).skill4(skill4Pulse)
        .interact(interact)
        .playerPos(px, py)
        .mouse(mouseX, mouseY)
        .build();

    // Consumir pulsos de skill (one-shot)
    skill1Pulse = false;
    skill2Pulse = false;
    skill3Pulse = false;
    skill4Pulse = false;

    // Fase 4: publicar na fila se transporte estiver configurado
    if (transport != null) {
      transport.publishInput(packet);
    }

    return packet;
  }

  /** Sobrecarga sem posição — usada no modo in-process (single-player). */
  public InputPacket buildPacket() {
    return buildPacket(0.0, 0.0);
  }

  /** Conecta o transporte para que buildPacket() publique inputs direto na fila. */
  public void setTransport(InProcessTransport transport) {
    this.transport = transport;
  }

  public String getPlayerId() {
    return playerId;
  }
}
