# Sistema de NPCs e Melhorias - Dezembro 2025

## 📋 Índice
- [Visão Geral](#visão-geral)
- [Sistema de Diálogos](#sistema-de-diálogos)
- [Arquitetura de NPCs](#arquitetura-de-npcs)
- [Tile de Portal](#tile-de-portal)
- [Melhorias Implementadas](#melhorias-implementadas)
- [Roadmap Futuro](#roadmap-futuro)

---

## 🎮 Visão Geral

Esta documentação cobre as implementações realizadas no sistema de NPCs do jogo, incluindo sistema de diálogos estilo Pokemon Fire Red, refatoração da arquitetura de NPCs usando herança e criação de tiles de portal para transição entre mapas.

**Data de Implementação:** 09 de Dezembro de 2025  
**Versão:** v1.3.0 (desenvolvimento)  
**Branch:** `desenvolvimento`

---

## 💬 Sistema de Diálogos

### DialogBox (Pokemon Fire Red Style)

Implementação de caixa de diálogo inspirada em Pokemon Fire Red com as seguintes características:

#### Características Visuais
- **Dimensões:** 700x120 pixels
- **Cores:**
  - Fundo: Preto semi-transparente (rgba 0,0,0,230)
  - Bordas: Branco (3px de espessura)
  - Texto: Branco
- **Placa de Nome:**
  - Tamanho: 150x30 pixels
  - Posição: Acima da caixa principal
  - Fundo: Cinza escuro (rgba 40,40,40,230)

#### Animação de Texto
- **Velocidade:** 2 frames por caractere
- **Quebra de linha:** Automática
- **Máximo de linhas:** 3 linhas simultâneas
- **Skip:** Tecla E pula a animação

#### Indicador Visual
- **Seta de continuar:** Triângulo piscando (ciclo de 30 frames)
- **Aparece:** Quando o texto termina de animar

#### Arquivo
```
src/com/rpggame/ui/DialogBox.java
```

#### Métodos Principais
```java
public void setText(String text)          // Define o texto
public void update()                      // Atualiza animação
public void skipAnimation()               // Pula animação
public void render(Graphics2D, String, int, int) // Renderiza
public boolean isTextComplete()           // Verifica se terminou
public void reset()                       // Reseta estado
```

---

## 🧑‍🤝‍🧑 Arquitetura de NPCs

### Estrutura com Herança

Nova arquitetura baseada em **herança** para facilitar criação e manutenção de NPCs.

#### Classe Base Abstrata
```
src/com/rpggame/npcs/NPC.java
```

**Características:**
- Classe `abstract` que define comportamento comum
- Método abstrato `initializeDialogues()` que cada subclasse implementa
- Gerenciamento automático de sprites e interação

**Atributos protegidos:**
```java
protected double x, y;                    // Posição no mundo
protected String name;                    // Nome do NPC
protected String[] dialogLines;           // Linhas de diálogo
protected BufferedImage sprite;           // Sprite visual
protected int INTERACTION_RANGE = 60;     // Alcance de interação (px)
```

**Métodos comuns:**
- `update(Player)` - Detecta proximidade do jogador
- `render(Graphics2D, Camera)` - Renderiza sprite e prompt "E"
- `canInteract()` - Verifica se está no alcance
- `getCurrentDialog()` - Retorna diálogo atual
- `nextDialog()` - Avança para próxima linha
- `resetDialog()` - Reinicia conversa

#### Subclasses Implementadas

##### 1. MerchantNPC (Mercador)
```
src/com/rpggame/npcs/MerchantNPC.java
```
- **Sprite:** `sprites/CommonGoblin.png`
- **Diálogos:** 4 linhas sobre comércio e caravanas

##### 2. GuardNPC (Guarda Real)
```
src/com/rpggame/npcs/GuardNPC.java
```
- **Sprite:** `sprites/goblinLeader.png`
- **Diálogos:** 5 linhas sobre proteção e goblins

##### 3. VillagerNPC (Aldeão)
```
src/com/rpggame/npcs/VillagerNPC.java
```
- **Sprite:** `sprites/TinyGoblin.png`
- **Diálogos:** 3 linhas sobre vida na vila

##### 4. WiseManNPC (Sábio)
```
src/com/rpggame/npcs/WiseManNPC.java
```
- **Sprite:** `sprites/AgresiveGoblin.png`
- **Diálogos:** 5 linhas sobre mecânicas do jogo

### Exemplo de Uso

**Antes (complexo):**
```java
NPC merchant = new NPC(
    500, 400,
    "Mercador",
    "sprites/CommonGoblin.png",
    "Olá, viajante!",
    "Tenho itens raros...",
    "Volte mais tarde!"
);
npcs.add(merchant);
```

**Agora (simples):**
```java
npcs.add(new MerchantNPC(500, 400));
```

### Como Criar um Novo NPC

```java
package com.rpggame.npcs;

public class BlacksmithNPC extends NPC {
  
  public BlacksmithNPC(double x, double y) {
    super(x, y, "Ferreiro", "sprites/blacksmith.png");
  }
  
  @Override
  protected String[] initializeDialogues() {
    return new String[] {
      "Precisa de armas? Eu forjo as melhores!",
      "Traga-me materiais e faço algo especial.",
      "Cuidado com sua espada, guerreiro!"
    };
  }
}
```

---

## 🚪 Tile de Portal

### Novo TileType: PORTAL

Tile especial para transição entre mapas, usando o sprite de grama mas com comportamento diferenciado.

#### Implementação
```
src/com/rpggame/world/TileType.java
src/com/rpggame/world/TileMap.java
```

#### Características
- **ID:** 6
- **Nome:** "Portal"
- **Walkable:** true (jogador pode andar sobre)
- **Sprite:** Mesmo da GRASS.png (reutiliza sprite)
- **Caractere no mapa:** 'P' ou 'p'

#### Uso em Arquivos de Mapa (.txt)
```
WWWWWWWW
W......W
W..P...W  <- 'P' representa o portal
W......W
WWWWWWWW
```

#### Detecção de Portal
```java
TileType currentTile = tileMap.getTileTypeAt(playerX, playerY);
if (currentTile == TileType.PORTAL) {
    // Carregar próximo mapa
    loadNextMap();
}
```

---

## ✨ Melhorias Implementadas

### 1. Anti-aliasing no DialogBox
- Renderização de texto com anti-aliasing
- Bordas suavizadas
- Melhor legibilidade

### 2. Ajuste de Posicionamento
- Texto ajustado 16px para baixo dentro da DialogBox
- Melhor espaçamento visual
- Alinhamento otimizado

### 3. Sistema de Interação
- Tecla **E** para interagir
- Prompt visual automático quando próximo ao NPC
- Feedback visual imediato

### 4. Organização de Código
- NPCs movidos de `entities` para `npcs`
- Separação clara de responsabilidades
- Código mais limpo e manutenível

---

## 🚀 Roadmap Futuro

### Sistema de Mapas
- [ ] **Sistema de Transição de Mapas**
  - Detectar quando player pisa no tile PORTAL
  - Carregar novo mapa dinamicamente
  - Animação de transição (fade in/out)
  - Salvar posição anterior para voltar
  
- [ ] **Múltiplos Mapas**
  - Criar mapas temáticos (Vila, Floresta, Caverna, etc.)
  - Sistema de coordenadas globais
  - Mapa-múndi para navegação

### Sistema de NPCs Avançado
- [ ] **Quest System**
  - NPCs oferecem missões
  - Tracking de objetivos
  - Recompensas (XP, itens, gold)
  
- [ ] **Inventário de NPCs**
  - Sistema de loja funcional
  - Compra e venda de itens
  - Preços dinâmicos
  
- [ ] **Condições de Diálogo**
  - Diálogos baseados em quests
  - Mudança de diálogo após eventos
  - Sistema de flags/variáveis

- [ ] **Animações de NPCs**
  - Sprites animados
  - Movimento básico (idle, walk)
  - Expressões faciais

### Tiles Especiais
- [ ] **Outros Tiles Interativos**
  - CHEST (baú) - contém itens
  - SIGN (placa) - mostra texto
  - DOOR (porta) - requer chave
  - TRAP (armadilha) - causa dano
  
- [ ] **Tiles com Estados**
  - Portas abertas/fechadas
  - Baús vazios/cheios
  - Alavancas ativadas/desativadas

### Interface e UX
- [ ] **Melhorias na DialogBox**
  - Avatar do NPC ao lado do nome
  - Sons de digitação
  - Efeitos visuais (shake, color)
  - Escolhas múltiplas (menu)
  
- [ ] **Minimapa**
  - Mostrar mapa atual
  - Posição do player
  - Localização de NPCs importantes

### Persistência
- [ ] **Sistema de Save**
  - Salvar progresso em quests
  - Posição atual do jogador
  - Estado de NPCs e mundo
  - Inventário e estatísticas
  
- [ ] **Formato de Save**
  - JSON ou XML
  - Múltiplos slots de save
  - Auto-save em portais

### Eventos e Scripts
- [ ] **Sistema de Eventos**
  - Eventos temporais (dia/noite)
  - Eventos climáticos (chuva, neve)
  - Eventos especiais (festas, invasões)
  
- [ ] **Scripting**
  - Scripts em Lua ou JavaScript
  - Eventos customizados
  - Cutscenes

---

## 📊 Estatísticas da Implementação

### Arquivos Criados
- `src/com/rpggame/npcs/NPC.java` (167 linhas)
- `src/com/rpggame/npcs/MerchantNPC.java` (18 linhas)
- `src/com/rpggame/npcs/GuardNPC.java` (20 linhas)
- `src/com/rpggame/npcs/VillagerNPC.java` (17 linhas)
- `src/com/rpggame/npcs/WiseManNPC.java` (20 linhas)
- `src/com/rpggame/ui/DialogBox.java` (216 linhas)

### Arquivos Modificados
- `src/com/rpggame/core/GamePanel.java`
- `src/com/rpggame/world/TileType.java`
- `src/com/rpggame/world/TileMap.java`

### Linhas de Código
- **Total adicionado:** ~450 linhas
- **Total refatorado:** ~100 linhas
- **Arquivos afetados:** 9

---

## 🔧 Compilação e Testes

### Compilar
```bash
cd src
javac -d ../bin -encoding UTF-8 com/rpggame/npcs/*.java \
    com/rpggame/core/*.java \
    com/rpggame/entities/*.java \
    com/rpggame/systems/*.java \
    com/rpggame/ui/*.java \
    com/rpggame/world/*.java \
    com/rpggame/enemies/Goblins/*.java
```

### Executar
```bash
cd ..
java -cp bin com.rpggame.core.Game
```

### Testar NPCs
1. Iniciar o jogo
2. Aproximar-se de um NPC até aparecer "E"
3. Pressionar **E** para iniciar conversa
4. Pressionar **E** novamente para avançar diálogos
5. Diálogo fecha automaticamente ao terminar

### Testar Portal (quando implementado)
1. Criar mapa com tile 'P'
2. Caminhar sobre o tile
3. Verificar transição de mapa

---

## 👥 Contribuidores

- **Desenvolvedor Principal:** MrRafha
- **Assistente IA:** GitHub Copilot
- **Data:** 09/12/2025

---

## 📝 Licença

Este projeto segue a licença definida no arquivo `LICENSE` na raiz do repositório.

---

## 📞 Suporte

Para dúvidas ou sugestões sobre o sistema de NPCs:
- Abrir issue no GitHub
- Consultar documentação adicional em `docs/`

---

**Última atualização:** 09 de Dezembro de 2025
