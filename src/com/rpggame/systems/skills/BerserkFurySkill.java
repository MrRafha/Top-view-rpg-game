package com.rpggame.systems.skills;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.Iterator;
import com.rpggame.entities.Player;
import com.rpggame.world.Camera;
import com.rpggame.systems.Skill;

/**
 * Habilidade Ultimate: Fúria Berserk
 * O guerreiro entra em estado de fúria, ficando extremamente poderoso
 */
public class BerserkFurySkill extends Skill {

  private class FuryParticle {
    double x, y;
    double velocityX, velocityY;
    int lifetime;
    float alpha;

    FuryParticle(double x, double y) {
      this.x = x;
      this.y = y;
      this.velocityX = (Math.random() - 0.5) * 2;
      this.velocityY = (Math.random() - 0.5) * 2;
      this.lifetime = 30 + (int) (Math.random() * 30);
      this.alpha = 1.0f;
    }

    void update() {
      x += velocityX;
      y += velocityY;
      lifetime--;
      alpha = Math.max(0, lifetime / 60.0f);
    }

    boolean isAlive() {
      return lifetime > 0;
    }
  }

  private boolean berserkActive;
  private int berserkTimer;
  private static final int BERSERK_DURATION = 480; // 8 segundos a 60 FPS
  private ArrayList<FuryParticle> particles;
  private Player currentPlayer;

  public BerserkFurySkill() {
    super("Fúria Berserk",
        "Entre em fúria: +100% velocidade, +200% dano, regen 5%/s por 8s",
        45, // 45 segundos de cooldown
        "Warrior",
        60); // 60 de mana

    this.berserkActive = false;
    this.berserkTimer = 0;
    this.particles = new ArrayList<>();
  }

  @Override
  protected void performSkill(Player player) {
    this.currentPlayer = player;
    this.berserkActive = true;
    this.berserkTimer = BERSERK_DURATION;

    // Aplicar os buffs do berserk
    player.setBerserkActive(true);

    System.out.println("⚔️ FÚRIA BERSERK ATIVADA!");
    System.out.println("💪 Buffs ativos por 8 segundos:");
    System.out.println("   +100% Velocidade de Movimento");
    System.out.println("   +150% Velocidade de Ataque");
    System.out.println("   +200% Dano");
    System.out.println("   Regenera 5% vida/segundo");
    System.out.println("   Imune a atordoamento e medo");
    System.out.println("   -50% Dano recebido");
  }

  @Override
  public void update() {
    super.update();

    if (!berserkActive) {
      particles.clear();
      return;
    }

    // Atualizar timer do berserk
    berserkTimer--;
    if (berserkTimer <= 0) {
      berserkActive = false;
      if (currentPlayer != null) {
        currentPlayer.setBerserkActive(false);
      }
      particles.clear();
      System.out.println("⚔️ Fúria Berserk dissipada!");
      return;
    }

    // Regenerar vida (5% por segundo = 5% / 60 frames)
    if (currentPlayer != null && berserkTimer % 60 == 0) {
      int maxHealth = currentPlayer.getStats().getMaxHealth();
      int regenAmount = maxHealth / 20; // 5%
      currentPlayer.heal(regenAmount);
      System.out.println("💚 Regenerou " + regenAmount + " de vida!");
    }

    // Gerar novas partículas
    if (currentPlayer != null && Math.random() < 0.3) { // 30% de chance por frame
      double playerX = currentPlayer.getX() + currentPlayer.getWidth() / 2;
      double playerY = currentPlayer.getY() + currentPlayer.getHeight() / 2;

      // Criar partícula em posição aleatória ao redor do player
      double angle = Math.random() * Math.PI * 2;
      double distance = 20 + Math.random() * 30;
      double particleX = playerX + Math.cos(angle) * distance;
      double particleY = playerY + Math.sin(angle) * distance;

      particles.add(new FuryParticle(particleX, particleY));
    }

    // Atualizar partículas existentes
    Iterator<FuryParticle> iterator = particles.iterator();
    while (iterator.hasNext()) {
      FuryParticle particle = iterator.next();
      particle.update();
      if (!particle.isAlive()) {
        iterator.remove();
      }
    }
  }

  @Override
  public void render(Graphics2D g, Camera camera) {
    if (!berserkActive || currentPlayer == null) {
      return;
    }

    int playerScreenX = (int) (currentPlayer.getX() - camera.getX());
    int playerScreenY = (int) (currentPlayer.getY() - camera.getY());
    int playerCenterX = playerScreenX + currentPlayer.getWidth() / 2;
    int playerCenterY = playerScreenY + currentPlayer.getHeight() / 2;

    // Salvar composite original
    java.awt.Composite originalComposite = g.getComposite();

    // Aura pulsante vermelha ao redor do guerreiro
    float pulse = 0.3f + 0.2f * (float) Math.sin(berserkTimer * 0.1);
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));

    // Círculos concêntricos de aura
    for (int i = 3; i >= 1; i--) {
      int radius = 25 + (i * 15);
      g.setColor(new Color(255, 0, 0, 80 / i));
      g.fillOval(playerCenterX - radius, playerCenterY - radius, radius * 2, radius * 2);
    }

    // Renderizar partículas de fúria
    for (FuryParticle particle : particles) {
      int screenX = (int) (particle.x - camera.getX());
      int screenY = (int) (particle.y - camera.getY());

      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, particle.alpha));

      // Partícula vermelha brilhante
      g.setColor(new Color(255, 50, 0));
      g.fillOval(screenX - 3, screenY - 3, 6, 6);

      // Brilho interno
      g.setColor(new Color(255, 150, 0));
      g.fillOval(screenX - 1, screenY - 1, 2, 2);
    }

    // Indicador de tempo restante acima do player
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
    g.setColor(Color.RED);
    g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
    String timeText = "FÚRIA: " + (berserkTimer / 60 + 1) + "s";
    java.awt.FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(timeText);
    g.drawString(timeText, playerCenterX - textWidth / 2, playerScreenY - 20);

    // Restaurar composite original
    g.setComposite(originalComposite);
  }

  public boolean isBerserkActive() {
    return berserkActive;
  }

  public int getBerserkTimer() {
    return berserkTimer;
  }
}
