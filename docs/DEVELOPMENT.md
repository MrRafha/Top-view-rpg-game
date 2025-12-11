# Guia de Desenvolvimento - RPG 2D Java

## Arquitetura do Sistema

### Padrões de Design Utilizados

#### 1. Model-View-Controller (MVC)
- **Model**: `Player`, `TileMap`, `CharacterStats`
- **View**: `GamePanel`, `CombinedCharacterScreen`  
- **Controller**: `Game`, Input handlers

#### 2. Component Pattern
- Sistemas modulares como `FogOfWar`, `Camera`, `Projectile`
- Fácil adição de novos componentes

#### 3. Strategy Pattern
- Diferentes classes de personagem com comportamentos únicos
- Sistema de tipos de tile extensível

### Fluxo de Execução

```
Game.main()
  ├── JFrame setup
  ├── CombinedCharacterScreen (Character creation)
  └── GamePanel
      ├── initializeGame()
      ├── startGameLoop()
      └── Loop principal:
          ├── update()
          ├── render()
          └── Thread.sleep()
```

### Sistema de Coordenadas

- **Mundo**: Coordenadas absolutas em pixels
- **Tiles**: Grid 15x15, cada tile = 48px
- **Tela**: 1024x800 pixels
- **Câmera**: Offset para world-to-screen conversion

### Sistemas Principais

#### Sistema de Movimento
1. Input capturado via KeyListener
2. Velocidade calculada com normalização diagonal
3. Verificação de colisão nos 4 cantos do player
4. Atualização de posição se válida

#### Sistema de Combate
1. Cooldown baseado em frames
2. Projéteis instanciados com direção
3. Cálculo de dano com base nos atributos
4. Sistema de evasão probabilístico

#### Sistema de Habilidades (v2.0)
1. **Arquitetura modular** com classe base `Skill`
2. **SkillManager** gerencia slots e execução
3. **Cooldowns visuais** com UI de slots
4. **Aprendizado via NPCs** - sistema de progressão
5. **Balanceamento por atributos** - cada classe tem fórmula específica

#### Fog of War
1. Algoritmo de Bresenham para line-of-sight
2. Alcance baseado em Wisdom
3. Cache de visibilidade para performance
4. Renderização com alpha blending

## Adicionando Funcionalidades

### Novo Tipo de Tile

1. **TileType.java**
```java
LAVA(6, "Lava", false, true), // walkable=false, damaging=true
```

2. **MapLoader.java** - adicionar no `fromChar()`:
```java
case 'L': return LAVA;
```

3. **TileMap.java** - adicionar cor no `getTileColor()`:
```java
case LAVA: return Color.RED;
```

### Nova Classe de Personagem

1. **CharacterStats.java** - adicionar na enum:
```java
ROGUE("Ladino", 1, 3, 1, 2, 1, 1)
```

2. **Player.java** - adicionar lógica de ataque específica

3. **CombinedCharacterScreen.java** - adicionar à interface

### Nova Habilidade

1. **Criar classe herdando de Skill**:
```java
public class NewSkill extends Skill {
    public NewSkill() {
        super("Nome", "Descrição", cooldownSeconds, "classe");
    }
    
    @Override
    protected void performSkill(Player player) {
        // Implementar lógica da habilidade
    }
    
    @Override
    public void render(Graphics2D g, Camera camera) {
        // Implementar efeitos visuais
    }
}
```

2. **Adicionar no SkillManager.initializeSkills()**:
```java
case "classe":
    skills.put(2, new NewSkill()); // Slot 2
    break;
```

3. **Atualizar WiseManNPC** para ensinar a nova habilidade

### Novo Sistema (ex: Inventário)

1. Criar classe `Inventory.java`
2. Adicionar referência no `Player.java`
3. Criar interface em `GamePanel.java`
4. Implementar input handling

## Performance e Otimizações

### Renderização
- Culling de tiles fora da tela
- Fog of War calculado apenas quando necessário
- Double buffering nativo do Swing
- **UI de Habilidades** com renderização otimizada e antialiasing
- **Círculos de progresso** para cooldowns em tempo real

### Memória
- Reutilização de objetos Projectile
- Cache de sprites gerados
- Limpeza de textos flutuantes expirados

### Sugestões para Melhorias

1. **Entity Component System (ECS)**
   - Separar lógica em componentes
   - Melhor organização e performance

2. **Asset Pipeline**
   - Carregamento de sprites reais
   - Sistema de animação frame-based

3. **Networking**
   - Multiplayer com sockets
   - Sincronização de estado

4. **Scripting**
   - Sistema de eventos Lua/JavaScript
   - NPCs e diálogos configuráveis

## Debugging

### Ativando Debug Mode
Adicionar flags no `Game.java`:
```java
public static final boolean DEBUG_MODE = true;
public static final boolean DEBUG_COLLISION = true;
public static final boolean DEBUG_FOW = true;
```

### Logs Úteis
- Posição do player
- Tiles em colisão
- FPS counter
- Estado do fog of war

### Ferramentas Recomendadas
- **IDE**: IntelliJ IDEA Community
- **Profiler**: JProfiler ou VisualVM
- **Git**: Para controle de versão
- **Maven/Gradle**: Para gerenciamento de dependências futuro

## Convenções de Código

### Naming
- Classes: `PascalCase`
- Métodos: `camelCase`
- Constantes: `UPPER_SNAKE_CASE`
- Variáveis: `camelCase`

### Estrutura de Arquivos
```
src/
├── core/           # Classes principais (Game, GamePanel)
├── entities/       # Entidades do jogo (Player, Enemy, etc.)
├── systems/        # Sistemas específicos (SkillManager, ExperienceSystem)
│   └── skills/     # Habilidades específicas (v2.0)
├── ui/            # Interface de usuário (SkillSlotUI, CharacterScreen)
├── world/         # Sistema de mundo (TileMap, Camera, etc.)
├── npcs/          # NPCs (WiseManNPC, MerchantNPC, etc.)
└── enemies/       # Inimigos (Goblins, etc.)
```

### Comentários
- Javadoc para métodos públicos
- Comentários inline para lógica complexa
- TODO para funcionalidades futuras

## Testes

### Testes Unitários Sugeridos
- `CharacterStats` calculations
- `TileMap` collision detection
- `FogOfWar` line of sight
- `MapLoader` file parsing

### Testes de Integração
- Player movement system
- Combat system end-to-end
- Map loading and rendering

### Framework Recomendado
- **JUnit 5** para testes unitários
- **Mockito** para mocking
- **TestFX** para testes de UI (futuro)