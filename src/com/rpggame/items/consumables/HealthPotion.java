package com.rpggame.items.consumables;

import com.rpggame.items.Item;
import com.rpggame.items.ItemType;
import com.rpggame.entities.Player;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * Poção que restaura vida do jogador.
 */
public class HealthPotion extends Item {
  private int healAmount;
  private Player player;

  /**
   * Construtor da poção de vida.
   */
  public HealthPotion(Player player, int healAmount) {
    super(
        "Poção de Vida",
        "Restaura " + healAmount + " pontos de vida",
        loadPotionSprite("sprites/HealingPotion.png"),
        ItemType.CONSUMABLE,
        true, // Empilhável
        10 // Máximo 10 por pilha
    );
    this.healAmount = healAmount;
    this.player = player;
  }

  @Override
  public void use() {
    if (player != null) {
      player.heal(healAmount);
      player.addFloatingText("+" + healAmount + " HP", new Color(220, 20, 60)); // Vermelho sangue (Crimson)
      System.out.println("✨ Usou Poção de Vida! +" + healAmount + " HP");
    }
  }

  /**
   * Carrega sprite da poção de um arquivo.
   */
  private static BufferedImage loadPotionSprite(String path) {
    try {
      // Tentar carregar como recurso do classpath (JAR)
      InputStream is = HealthPotion.class.getClassLoader().getResourceAsStream(path);
      if (is != null) {
        BufferedImage img = ImageIO.read(is);
        is.close();
        System.out.println("✅ Sprite da Poção de Vida carregado: " + path);
        return img;
      }

      // Fallback: arquivo externo (desenvolvimento)
      File file = new File(path);
      if (file.exists()) {
        System.out.println("✅ Sprite da Poção de Vida carregado do arquivo: " + path);
        return ImageIO.read(file);
      }
    } catch (IOException e) {
      System.err.println("⚠️ Erro ao carregar sprite da Poção de Vida: " + e.getMessage());
    }

    // Fallback: criar sprite simples se não encontrar arquivo
    System.out.println("⚠️ Usando sprite fallback para Poção de Vida");
    BufferedImage fallback = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
    return fallback;
  }
}
