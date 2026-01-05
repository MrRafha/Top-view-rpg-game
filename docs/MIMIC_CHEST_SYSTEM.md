# Sistema de Mimics e Baús

## 📦 Componentes Criados

### 1. **Mimic** (`src/com/rpggame/enemies/Mimic/Mimic.java`)
Inimigo disfarçado de baú que surpreende o jogador.

**Comportamento:**
- 💤 **Estado Disfarçado**: Imóvel, parece um baú fechado
- 👀 **Detecção**: Raio de 150 pixels ao redor
- 🎭 **Revelação**: 0.5 segundos de animação
- ⚠️ **Aviso de Ataque**: 2 segundos com indicador visual vermelho
- 💥 **Ataque Inicial**: 25 de dano se o player não esquivar
- 🏃 **Perseguição**: Após o ataque, persegue normalmente

**Sprites Usados:**
- `sprites/Mimic.png` - Forma disfarçada (baú fechado)
- `sprites/MimicAttack1.png` - Frame 1 do ataque
- `sprites/MimicAttack2.png` - Frame 2 do ataque (também usado quando ativo)

**Stats:**
- HP: 80
- Dano: 25
- Velocidade: 1.5
- XP: 100

---

### 2. **Chest** (`src/com/rpggame/entities/Chest.java`)
Baú verdadeiro que requer minigame para abrir.

**Características:**
- 📦 Indicador `[F] Abrir` quando o player está próximo
- 🎮 Abre o minigame de lockpicking ao pressionar F
- 🎁 Recompensa: 2 itens aleatórios
- ✅ Permanece aberto após ser saqueado

**Sprites Usados:**
- `sprites/ClosedChest.png` - Baú fechado
- `sprites/OpenedChest.png` - Baú aberto

**Itens Possíveis:**
- Health Potion
- Mana Potion

---

### 3. **LockpickingMinigame** (`src/com/rpggame/ui/LockpickingMinigame.java`)
Minigame de lockpicking estilo timing.

**Como Funciona:**
1. ⚫ Círculo preto aparece na tela
2. 🟢 Área verde aleatória no círculo
3. 🟡 Marcador dourado rotaciona continuamente
4. ⌨️ Pressione **F** quando o marcador estiver no verde
5. ✅ Sucesso = baú abre e dá 2 itens
6. ❌ Falha = baú não abre

**Visual:**
- Fundo escurecido (overlay)
- Círculo preto central (raio 100px)
- Zona verde (30° de arco)
- Marcador rotativo dourado
- Instruções na tela

---

## 🎮 Como Integrar no Jogo

### 1. **Adicionar Mimics ao EnemyManager**

```java
// Em EnemyManager.java
import com.rpggame.enemies.Mimic.Mimic;

// No método de spawn de inimigos
public void spawnMimic(double x, double y) {
    Mimic mimic = new Mimic(x, y);
    mimic.setTarget(player);
    mimic.setTileMap(tileMap);
    enemies.add(mimic);
    System.out.println("👹 Mimic spawnou em (" + x + ", " + y + ")");
}
```

### 2. **Adicionar Baús ao GamePanel**

```java
// Em GamePanel.java
import com.rpggame.entities.Chest;
import com.rpggame.ui.LockpickingMinigame;

private java.util.List<Chest> chests;
private LockpickingMinigame lockpickingMinigame;
private Chest currentChest;

// No initializeGame()
chests = new java.util.ArrayList<>();
lockpickingMinigame = new LockpickingMinigame();

// Spawnar baús
chests.add(new Chest(300, 300));
chests.add(new Chest(600, 400));

// No update()
for (Chest chest : chests) {
    chest.update(player);
}

// No keyPressed()
if (e.getKeyCode() == KeyEvent.VK_F) {
    if (lockpickingMinigame.isActive()) {
        lockpickingMinigame.handleInput(KeyEvent.VK_F);
        
        if (lockpickingMinigame.isFinished()) {
            if (lockpickingMinigame.isSuccess()) {
                currentChest.open();
                String[] rewards = currentChest.getRewards();
                // Dar itens ao player
                for (String item : rewards) {
                    player.getInventory().addItem(createItem(item));
                }
            }
            lockpickingMinigame.close();
        }
    } else {
        // Verificar baús próximos
        for (Chest chest : chests) {
            if (chest.canInteract()) {
                currentChest = chest;
                lockpickingMinigame.start();
                break;
            }
        }
    }
}

// No render()
for (Chest chest : chests) {
    chest.render(g2d, camera);
}

if (lockpickingMinigame.isActive()) {
    lockpickingMinigame.render(g2d);
}
```

### 3. **Spawnar Mimics e Baús no Mapa**

```java
// Exemplo: spawnar na secret_area
if ("secret_area".equals(currentMapId)) {
    // Spawnar 2 mimics e 2 baús verdadeiros
    enemyManager.spawnMimic(400, 400);
    enemyManager.spawnMimic(800, 600);
    
    chests.add(new Chest(200, 500));
    chests.add(new Chest(900, 300));
}
```

---

## 🎯 Estratégia de Gameplay

**Para o Player:**
- ❓ Não sabe qual é o Mimic e qual é o baú
- 🎲 Risco vs recompensa
- ⏱️ Tem 2 segundos para fugir quando o Mimic revela
- 🎮 Minigame de timing para abrir baús

**Dificuldade:**
- Área verde do minigame: 30° (ajuste `greenArcAngle` para mais fácil/difícil)
- Velocidade do marcador: 3.0 (ajuste `markerSpeed`)
- Tempo de aviso do Mimic: 2 segundos (120 frames)

---

## ✅ Status

- ✅ Classe Mimic criada e funcional
- ✅ Sistema de estados (Disfarçado → Revelando → Atacando → Ativo)
- ✅ Classe Chest criada
- ✅ Minigame de lockpicking implementado
- ✅ Sprites carregados corretamente
- ⏳ Integração com GamePanel (próximo passo)
- ⏳ Spawn automático em mapas

---

## 🐛 Debug

Para ver a área de detecção do Mimic, mude esta linha em `Mimic.java`:
```java
if (state == MimicState.DISGUISED && true) { // Mudou de false para true
```

Isso mostrará um círculo vermelho ao redor dos Mimics disfarçados.
