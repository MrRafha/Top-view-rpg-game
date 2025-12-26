package com.rpggame.ui;

import com.rpggame.entities.Player;
import com.rpggame.entities.Enemy;
import com.rpggame.systems.ExperienceSystem;
import com.rpggame.systems.EnemyManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Console de desenvolvedor para comandos de debug/cheat
 */
public class DeveloperConsole {
  private boolean visible = false;
  private String currentInput = "";
  private List<String> commandHistory = new ArrayList<>();
  private List<String> outputMessages = new ArrayList<>();
  private int maxOutputLines = 10;

  private Player player;
  private EnemyManager enemyManager;

  // Configurações visuais
  private static final int CONSOLE_HEIGHT = 250;
  private static final int PADDING = 10;
  private static final Font CONSOLE_FONT = new Font("Courier New", Font.PLAIN, 14);
  private static final Color BG_COLOR = new Color(0, 0, 0, 220);
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color INPUT_COLOR = Color.WHITE;

  public DeveloperConsole(Player player) {
    this.player = player;
    this.enemyManager = player.getEnemyManager();
    addOutputMessage("Console de Desenvolvedor - Digite um comando e pressione Enter");
    addOutputMessage("Comandos: noclip, maxlevel, kill");
  }

  public void toggle() {
    visible = !visible;
    if (visible) {
      currentInput = "";
    }
  }

  public boolean isVisible() {
    return visible;
  }

  public boolean keyPressed(KeyEvent e) {
    if (!visible)
      return false;

    int keyCode = e.getKeyCode();
    char keyChar = e.getKeyChar();

    // ESC para fechar
    if (keyCode == KeyEvent.VK_ESCAPE) {
      visible = false;
      return true;
    }

    // Enter para executar comando
    if (keyCode == KeyEvent.VK_ENTER) {
      if (!currentInput.trim().isEmpty()) {
        executeCommand(currentInput.trim());
        commandHistory.add(currentInput);
        currentInput = "";
      }
      return true;
    }

    // Backspace para apagar
    if (keyCode == KeyEvent.VK_BACK_SPACE) {
      if (currentInput.length() > 0) {
        currentInput = currentInput.substring(0, currentInput.length() - 1);
      }
      return true;
    }

    // Adicionar caracteres
    if (Character.isLetterOrDigit(keyChar) || keyChar == ' ') {
      currentInput += keyChar;
      return true;
    }

    return false;
  }

  private void executeCommand(String command) {
    addOutputMessage("> " + command);

    String cmd = command.toLowerCase().trim();

    // Verificar se é comando com argumentos
    if (cmd.startsWith("kill ")) {
      executeKill(command);
      return;
    }

    switch (cmd) {
      case "noclip":
        executeNoclip();
        break;

      case "maxlevel":
        executeMaxLevel();
        break;

      case "spawngolem":
        executeSpawnGolem();
        break;

      case "help":
        addOutputMessage("Comandos disponíveis:");
        addOutputMessage("  noclip - Atravessa paredes e aumenta velocidade");
        addOutputMessage("  maxlevel - Sobe para level 10");
        addOutputMessage("  kill <entidade> <quantidade|all> - Mata inimigos");
        addOutputMessage("    Exemplos: kill goblins all, kill goblins 5");
        addOutputMessage("  spawngolem - Força o spawn do Golem boss");
        break;

      default:
        addOutputMessage("Comando desconhecido: " + command);
        addOutputMessage("Digite 'help' para ver comandos disponíveis");
        break;
    }
  }

  private void executeNoclip() {
    boolean newState = !player.isNoclipEnabled();
    player.setNoclip(newState);

    if (newState) {
      player.setSpeed(8.0); // Velocidade aumentada
      addOutputMessage("NOCLIP ATIVADO - Atravessando paredes, velocidade: 8.0");
    } else {
      player.setSpeed(4.0); // Velocidade normal
      addOutputMessage("NOCLIP DESATIVADO - Velocidade normal: 4.0");
    }
  }

  private void executeMaxLevel() {
    ExperienceSystem exp = player.getExperienceSystem();
    int currentLevel = exp.getCurrentLevel();

    if (currentLevel >= 10) {
      addOutputMessage("Você já está no level 10!");
      return;
    }

    // Subir nível por nível até 10 para desbloquear habilidades corretamente
    int levelsToGain = 10 - currentLevel;
    addOutputMessage("Subindo " + levelsToGain + " níveis (aguarde " + (levelsToGain * 0.5) + "s)...");

    // Usar Thread para fazer level up gradual com delay
    new Thread(() -> {
      try {
        for (int i = 0; i < levelsToGain; i++) {
          Thread.sleep(500); // Meio segundo de delay entre cada level

          ExperienceSystem expSystem = player.getExperienceSystem();
          int xpNeeded = expSystem.getXpToNextLevel();

          // Usar gainExperience para processar level up corretamente
          player.gainExperience(xpNeeded);

          addOutputMessage("Level " + expSystem.getCurrentLevel() + " alcançado!");
        }

        addOutputMessage("Level 10 alcançado! Todas as habilidades desbloqueadas!");
      } catch (InterruptedException e) {
        addOutputMessage("Erro ao subir de nível: " + e.getMessage());
      }
    }).start();
  }

  private void executeKill(String command) {
    if (enemyManager == null) {
      addOutputMessage("ERRO: EnemyManager não disponível");
      return;
    }

    // Parse do comando: kill <entidade> <quantidade|all>
    String[] parts = command.trim().split("\\s+");
    if (parts.length != 3) {
      addOutputMessage("Uso: kill <entidade> <quantidade|all>");
      addOutputMessage("Exemplo: kill goblins all, kill goblins 5");
      return;
    }

    String entity = parts[1].toLowerCase();
    String amountStr = parts[2].toLowerCase();

    // Verificar tipo de entidade
    if (!entity.equals("goblins")) {
      addOutputMessage("Entidade desconhecida: " + entity);
      addOutputMessage("Entidades disponíveis: goblins");
      return;
    }

    // Obter lista de inimigos vivos
    ArrayList<Enemy> aliveEnemies = new ArrayList<>();
    for (Enemy enemy : enemyManager.getEnemies()) {
      if (enemy.isAlive()) {
        aliveEnemies.add(enemy);
      }
    }

    if (aliveEnemies.isEmpty()) {
      addOutputMessage("Nenhum goblin vivo encontrado");
      return;
    }

    int toKill = 0;
    if (amountStr.equals("all")) {
      toKill = aliveEnemies.size();
    } else {
      try {
        toKill = Integer.parseInt(amountStr);
        if (toKill <= 0) {
          addOutputMessage("Quantidade deve ser maior que 0");
          return;
        }
        toKill = Math.min(toKill, aliveEnemies.size());
      } catch (NumberFormatException e) {
        addOutputMessage("Quantidade inválida: " + amountStr);
        addOutputMessage("Use um número ou 'all'");
        return;
      }
    }

    // Matar os inimigos e dar XP
    int totalXP = 0;
    int killed = 0;
    for (int i = 0; i < toKill && i < aliveEnemies.size(); i++) {
      Enemy enemy = aliveEnemies.get(i);
      int enemyXP = enemy.getExperienceReward();

      // Matar o inimigo instantaneamente
      enemy.takeDamage(999999);

      // Dar XP ao player
      player.gainExperience(enemyXP);
      totalXP += enemyXP;
      killed++;
    }

    addOutputMessage("💀 " + killed + " goblins eliminados!");
    addOutputMessage("✨ +" + totalXP + " XP ganho");
    addOutputMessage("Level atual: " + player.getExperienceSystem().getCurrentLevel());
  }

  private void executeSpawnGolem() {
    if (enemyManager == null) {
      addOutputMessage("ERRO: EnemyManager não disponível");
      return;
    }

    // Forçar spawn do Golem usando reflexão para acessar método privado
    try {
      java.lang.reflect.Method spawnMethod = enemyManager.getClass().getDeclaredMethod("spawnGolem");
      spawnMethod.setAccessible(true);
      spawnMethod.invoke(enemyManager);
      addOutputMessage("🗿 Golem forçado a spawnar!");
    } catch (Exception e) {
      addOutputMessage("❌ Erro ao spawnar Golem: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void addOutputMessage(String message) {
    outputMessages.add(message);

    // Manter apenas as últimas N mensagens
    if (outputMessages.size() > maxOutputLines) {
      outputMessages.remove(0);
    }
  }

  public void render(Graphics2D g, int screenWidth, int screenHeight) {
    if (!visible)
      return;

    int consoleY = screenHeight - CONSOLE_HEIGHT;

    // Fundo do console
    g.setColor(BG_COLOR);
    g.fillRect(0, consoleY, screenWidth, CONSOLE_HEIGHT);

    // Borda superior
    g.setColor(TEXT_COLOR);
    g.drawLine(0, consoleY, screenWidth, consoleY);

    g.setFont(CONSOLE_FONT);
    FontMetrics fm = g.getFontMetrics();
    int lineHeight = fm.getHeight();

    // Renderizar mensagens de saída
    int y = consoleY + PADDING + lineHeight;
    for (String message : outputMessages) {
      g.setColor(TEXT_COLOR);
      g.drawString(message, PADDING, y);
      y += lineHeight;
    }

    // Linha de input
    y = screenHeight - PADDING - lineHeight;
    g.setColor(INPUT_COLOR);
    String inputLine = "$ " + currentInput + "_";
    g.drawString(inputLine, PADDING, y);

    // Indicador de console aberto
    g.setColor(TEXT_COLOR);
    String hint = "[ESC para fechar]";
    int hintWidth = fm.stringWidth(hint);
    g.drawString(hint, screenWidth - hintWidth - PADDING, consoleY + PADDING + lineHeight);
  }
}
