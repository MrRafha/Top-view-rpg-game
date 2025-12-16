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
 * Poção que restaura mana do jogador.
 */
public class ManaPotion extends Item {
  private int manaAmount;
  private Player player;

  /**
   * Construtor da poção de mana.
   */
  public ManaPotion(Player player, int manaAmount) {
    super(
        "Poção de Mana",
        "Restaura " + manaAmount + " pontos de mana",
        loadPotionSprite("sprites/ManaPotion.png"),
        ItemType.CONSUMABLE,
        true, // Empilhável
        10 // Máximo 10 por pilha
    );
    this.manaAmount = manaAmount;
    this.player = player;
  }

  @Override
  public void use() {
    if (player != null) {
      player.restoreMana(manaAmount);
      player.addFloatingText("+" + manaAmount + " MP", new Color(100, 150, 255)); // Azul claro
      System.out.println("✨ Usou Poção de Mana! +" + manaAmount + " MP");
    }
  }

  /**
   * Carrega sprite da poção de um arquivo.
   */
  private static BufferedImage loadPotionSprite(String path) {
    try {
      // Tentar carregar como recurso do classpath (JAR)
      InputStream is = ManaPotion.class.getClassLoader().getResourceAsStream(path);
      if (is != null) {
        BufferedImage img = ImageIO.read(is);
        is.close();
        System.out.println("✅ Sprite da Poção de Mana carregado: " + path);
        return img;
      }

      // Fallback: arquivo externo (desenvolvimento)
      File file = new File(path);
      if (file.exists()) {
        System.out.println("✅ Sprite da Poção de Mana carregado do arquivo: " + path);
        return ImageIO.read(file);
      }
    } catch (IOException e) {
      System.err.println("⚠️ Erro ao carregar sprite da Poção de Mana: " + e.getMessage());
    }

    // Fallback: criar sprite simples se não encontrar arquivo
    System.out.println("⚠️ Usando sprite fallback para Poção de Mana");
    BufferedImage fallback = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
    return fallback;
  }
}
