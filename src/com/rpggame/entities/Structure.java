package com.rpggame.entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import com.rpggame.world.Camera;

/**
 * Representa uma estrutura no mapa (como cabanas de goblins)
 */
public class Structure {
    private double x, y;
    private int width, height;
    private BufferedImage sprite;
    private String structureType;
    private boolean isOccupied;
    
    // Sistema de saúde e vulnerabilidade
    private int maxHealth;
    private int currentHealth;
    private boolean isVulnerable;
    private boolean isDestroyed;
    
    /**
     * Construtor para estrutura
     */
    public Structure(double x, double y, String structureType, String spritePath) {
        this.x = x;
        this.y = y;
        this.structureType = structureType;
        this.width = 64; // Cabana menor - 64x64 pixels (1.3x1.3 tiles)
        this.height = 64;
        this.isOccupied = false;
        
        // Inicializar saúde
        this.maxHealth = 100;
        this.currentHealth = maxHealth;
        this.isVulnerable = false;
        this.isDestroyed = false;
        
        loadSprite(spritePath);
    }
    
    /**
     * Carrega sprite da estrutura
     */
    private void loadSprite(String spritePath) {
        try {
            String resolvedPath = com.rpggame.world.ResourceResolver.getResourcePath(spritePath);
            File spriteFile = new File(resolvedPath);
            if (spriteFile.exists()) {
                BufferedImage originalSprite = ImageIO.read(spriteFile);
                
                // Redimensionar para tamanho adequado
                sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = sprite.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                   RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.drawImage(originalSprite, 0, 0, width, height, null);
                g2d.dispose();
                
                System.out.println("Estrutura carregada: " + resolvedPath);
            } else {
                System.out.println("Sprite de estrutura não encontrado: " + spritePath);
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar sprite da estrutura: " + e.getMessage());
        }
    }
    
    /**
     * Renderiza a estrutura
     */
    public void render(Graphics2D g, Camera camera) {
        int screenX = (int) (x - camera.getX());
        int screenY = (int) (y - camera.getY());
        
        if (isDestroyed) {
            // Renderizar cabana destruída (cinza escuro apenas)
            g.setColor(new Color(64, 64, 64));
            g.fillRect(screenX, screenY, width, height);
            g.setColor(Color.RED);
            g.drawRect(screenX, screenY, width - 1, height - 1);
        } else {
            if (sprite != null) {
                g.drawImage(sprite, screenX, screenY, null);
            } else {
                // Fallback: retângulo colorido
                g.setColor(new Color(139, 69, 19)); // Marrom
                g.fillRect(screenX, screenY, width, height);
                g.setColor(Color.BLACK);
                g.drawRect(screenX, screenY, width - 1, height - 1);
            }
            
            // Borda vermelha se vulnerável
            if (isVulnerable) {
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(2));
                g.drawRect(screenX - 1, screenY - 1, width + 1, height + 1);
                g.setStroke(new BasicStroke(1)); // Restaurar stroke padrão
            }
            
            // Barra de vida se vulnerável e danificada
            if (isVulnerable && currentHealth < maxHealth) {
                int barWidth = width;
                int barHeight = 6;
                int barX = screenX;
                int barY = screenY - 10;
                
                // Fundo da barra
                g.setColor(Color.RED);
                g.fillRect(barX, barY, barWidth, barHeight);
                
                // Vida atual
                int healthWidth = (int)((double)currentHealth / maxHealth * barWidth);
                g.setColor(Color.GREEN);
                g.fillRect(barX, barY, healthWidth, barHeight);
                
                // Borda da barra
                g.setColor(Color.BLACK);
                g.drawRect(barX, barY, barWidth, barHeight);
            }
        }
    }
    
    /**
     * Verifica se um ponto está dentro da estrutura
     */
    public boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
    
    /**
     * Calcula distância do centro da estrutura a um ponto
     */
    public double distanceTo(double px, double py) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        return Math.sqrt(Math.pow(px - centerX, 2) + Math.pow(py - centerY, 2));
    }
    
    // Getters e Setters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getStructureType() { return structureType; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { this.isOccupied = occupied; }
    
    public Point getCenter() {
        return new Point((int)(x + width/2), (int)(y + height/2));
    }
    
    /**
     * Torna a estrutura vulnerável a ataques
     */
    public void makeVulnerable() {
        this.isVulnerable = true;
        System.out.println("Cabana ficou vulnerável a ataques!");
    }
    
    /**
     * Ataca a estrutura causando dano
     */
    public boolean takeDamage(int damage) {
        if (!isVulnerable || isDestroyed) {
            return false;
        }
        
        currentHealth -= damage;
        System.out.println("Cabana atacada! Vida: " + currentHealth + "/" + maxHealth);
        
        if (currentHealth <= 0) {
            isDestroyed = true;
            System.out.println("Cabana foi destruída!");
            return true;
        }
        
        return false;
    }
    
    // Getters para o sistema de saúde
    public int getMaxHealth() { return maxHealth; }
    public int getCurrentHealth() { return currentHealth; }
    public boolean isVulnerable() { return isVulnerable; }
    public boolean isDestroyed() { return isDestroyed; }
}