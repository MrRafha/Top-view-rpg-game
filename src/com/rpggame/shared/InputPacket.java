package com.rpggame.shared;

/**
 * DTO de input do cliente para o servidor.
 *
 * Representa o estado de todas as teclas relevantes em um frame de input.
 * Imutável — criado pelo ClientInput a cada frame e consumido pelo
 * PlayerSimulation no servidor.
 *
 * Ao usar InputPacket como fronteira cliente/servidor, o servidor nunca
 * precisa conhecer KeyEvent ou qualquer API de Swing — apenas este contrato.
 */
public final class InputPacket {

  private final String playerId;
  private final long clientFrame;

  // Movimento
  private final boolean up;
  private final boolean down;
  private final boolean left;
  private final boolean right;

  // Ações
  private final boolean attack;
  private final boolean skill1;
  private final boolean skill2;
  private final boolean skill3;
  private final boolean skill4;
  private final boolean interact;

  // Posição autoritativa do player no cliente (modo cliente-autoritativo)
  private final double playerX;
  private final double playerY;

  // Posição do mouse (coordenadas de tela — usadas por habilidades direcionais)
  private final float mouseX;
  private final float mouseY;

  private InputPacket(Builder b) {
    this.playerId   = b.playerId;
    this.clientFrame = b.clientFrame;
    this.up         = b.up;
    this.down       = b.down;
    this.left       = b.left;
    this.right      = b.right;
    this.attack     = b.attack;
    this.skill1     = b.skill1;
    this.skill2     = b.skill2;
    this.skill3     = b.skill3;
    this.skill4     = b.skill4;
    this.interact   = b.interact;
    this.playerX    = b.playerX;
    this.playerY    = b.playerY;
    this.mouseX     = b.mouseX;
    this.mouseY     = b.mouseY;
  }

  // ---- Getters ----

  public String getPlayerId()   { return playerId; }
  public long   getClientFrame(){ return clientFrame; }
  public boolean isUp()         { return up; }
  public boolean isDown()       { return down; }
  public boolean isLeft()       { return left; }
  public boolean isRight()      { return right; }
  public boolean isAttack()     { return attack; }
  public boolean isSkill1()     { return skill1; }
  public boolean isSkill2()     { return skill2; }
  public boolean isSkill3()     { return skill3; }
  public boolean isSkill4()     { return skill4; }
  public boolean isInteract()   { return interact; }
  public double  getPlayerX()   { return playerX; }
  public double  getPlayerY()   { return playerY; }
  public float   getMouseX()    { return mouseX; }
  public float   getMouseY()    { return mouseY; }

  /** Retorna true se alguma tecla de movimento estiver pressionada. */
  public boolean hasMovement() {
    return up || down || left || right;
  }

  @Override
  public String toString() {
    return "InputPacket{player=" + playerId
        + ", frame=" + clientFrame
        + ", move=[" + (up?"U":"") + (down?"D":"") + (left?"L":"") + (right?"R":"") + "]"
        + ", attack=" + attack
        + ", interact=" + interact
        + "}";
  }

  // ---- Builder ----

  public static Builder builder(String playerId, long clientFrame) {
    return new Builder(playerId, clientFrame);
  }

  public static final class Builder {
    private final String playerId;
    private final long clientFrame;
    private boolean up, down, left, right;
    private boolean attack, skill1, skill2, skill3, skill4, interact;
    private double playerX, playerY;
    private float mouseX, mouseY;

    private Builder(String playerId, long clientFrame) {
      this.playerId    = playerId;
      this.clientFrame = clientFrame;
    }

    public Builder up(boolean v)       { this.up = v;       return this; }
    public Builder down(boolean v)     { this.down = v;     return this; }
    public Builder left(boolean v)     { this.left = v;     return this; }
    public Builder right(boolean v)    { this.right = v;    return this; }
    public Builder attack(boolean v)   { this.attack = v;   return this; }
    public Builder skill1(boolean v)   { this.skill1 = v;   return this; }
    public Builder skill2(boolean v)   { this.skill2 = v;   return this; }
    public Builder skill3(boolean v)   { this.skill3 = v;   return this; }
    public Builder skill4(boolean v)   { this.skill4 = v;   return this; }
    public Builder interact(boolean v) { this.interact = v; return this; }
    public Builder playerPos(double x, double y) { this.playerX = x; this.playerY = y; return this; }
    public Builder mouse(float x, float y) { this.mouseX = x; this.mouseY = y; return this; }

    public InputPacket build() {
      return new InputPacket(this);
    }
  }
}
