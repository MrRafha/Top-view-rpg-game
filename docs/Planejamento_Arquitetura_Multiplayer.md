# Planejamento — Arquitetura Server/Client e Multiplayer Local

## Objetivo

Separar o jogo em dois processos independentes:

- **GameServer** — simula o mundo inteiro, headless, sem rendering
- **GameClient** — exibe o estado recebido, envia input, sem lógica de simulação

Isso resolve três problemas ao mesmo tempo:

1. **Persistência de mundo** — mapas continuam existindo e sendo simulados quando o jogador não está neles
2. **Background simulation** — raids, IA goblin, guerra territorial, timers, famílias continuam rodando em paralelo
3. **Base para multiplayer local** — cada cliente adicional é só mais uma conexão ao mesmo servidor

---

## Visão geral da separação

```
┌─────────────────────────────────────┐
│              GameServer             │
│                                     │
│  WorldSimulation                    │
│   ├─ MapManager (todos os mapas)    │
│   ├─ EnemyManager (por mapa)        │
│   ├─ FactionSystem (global)         │
│   ├─ NPCManager (por mapa)          │
│   ├─ ProjectileManager (por mapa)   │
│   └─ PlayerState (por jogador)      │
│                                     │
│  ServerLoop @ 20 TPS                │
│  ServerNetwork (socket local)       │
└──────────────┬──────────────────────┘
               │  TCP local / loopback
               │  (ou SharedMemory)
┌──────────────▼──────────────────────┐
│              GameClient             │
│                                     │
│  Recebe: WorldSnapshot              │
│  Envia:  InputPacket                │
│                                     │
│  ClientRenderer @ 60 FPS            │
│  ClientInput (teclado/mouse)        │
│  Interpolação de posições           │
└─────────────────────────────────────┘
```

---

## Estado de simulação vs estado de apresentação

### Estado de simulação — fica no servidor

Tudo que afeta gameplay, colisão, IA e progressão.

**Por jogador:**
- Posição (x, y), velocidade (dx, dy)
- HP, Mana, Stats, XP, Level
- Inventário, Gold, Quests
- Habilidades e cooldowns
- Estado: stunned, grabbed, berserk, inDialog
- Mapa atual

**Por mapa:**
- Lista de inimigos (posição, HP, estado de IA)
- Lista de projéteis
- Estado de baús (aberto/fechado)
- Estado de NPCs (diálogo ativo, quest disponível)
- Estruturas (cabanas)
- Fog of war por jogador

**Global:**
- FactionSystem (reputação, raids, territórios)
- GoblinCouncil (decisões estratégicas)
- WorldLayout (grafo de mapas)
- GoblinFamily (única, com líder)

### Estado de apresentação — fica no cliente

Tudo que afeta apenas o que o jogador vê.

- Sprites e animações
- Camera (posição, offset)
- Textos flutuantes (dano, crítico)
- Efeitos visuais de habilidades
- Todas as telas de UI (inventário, shop, quests)
- Sons e música
- Input local (teclas pressionadas)

---

## Protocolo de comunicação

### Direção: Servidor → Cliente

Enviado a cada tick (20 TPS). O cliente interpola entre snapshots para exibir a 60 FPS.

```json
WorldSnapshot {
  tick: int,
  mapId: string,

  players: [
    {
      id: string,
      x: float, y: float,
      hp: int, maxHp: int,
      mana: int, maxMana: int,
      animState: string,
      facingLeft: bool
    }
  ],

  enemies: [
    {
      id: string,
      type: string,
      x: float, y: float,
      hp: int,
      aiState: string
    }
  ],

  projectiles: [
    { id: string, x: float, y: float, direction: float }
  ],

  npcs: [
    { id: string, x: float, y: float, hasQuest: bool }
  ],

  chests: [
    { id: string, x: float, y: float, open: bool }
  ],

  factionStatus: {
    goblinReputation: int,
    humanReputation: int,
    goblinSuspicion: int,
    currentMapOwner: string,
    mapContested: bool,
    raidActive: bool
  },

  events: [
    { type: string, data: object }
  ]
}
```

### Direção: Cliente → Servidor

Enviado a cada frame de input (ou quando muda).

```json
InputPacket {
  playerId: string,
  tick: int,
  up: bool,
  down: bool,
  left: bool,
  right: bool,
  attack: bool,
  skill1: bool,
  skill2: bool,
  skill3: bool,
  skill4: bool,
  interact: bool,
  mouseX: float,
  mouseY: float
}
```

### Eventos discretos (servidor → cliente)

Notificações one-shot para o cliente exibir feedback:

```json
{ type: "REPUTATION_CHANGED",  faction: "GOBLINS", delta: -15, newValue: -30 }
{ type: "SUSPICION_CHANGED",   faction: "GOBLINS", newState: "ALERTED" }
{ type: "TERRITORY_CONTESTED", mapId: "goblin_territories" }
{ type: "RAID_STARTED",        attacker: "GOBLINS", targetMapId: "village" }
{ type: "MAP_OWNER_CHANGED",   mapId: "neutral_1", newOwner: "GOBLINS" }
{ type: "DIALOG_OPEN",         npcId: "wise_man", lines: ["..."] }
{ type: "QUEST_AVAILABLE",     questId: "q1", title: "..." }
{ type: "LEVEL_UP",            newLevel: 5 }
{ type: "PLAYER_DIED" }
```

---

## Persistência de entidades por mapa

### Problema atual

O `EnemyManager` é reinicializado a cada mudança de mapa. Inimigos, posições e estados de guerra são perdidos.

### Solução: WorldState

Criar uma classe `WorldState` no servidor que mantém o estado completo de todos os mapas simultaneamente.

```
WorldState
  ├─ Map<String, MapSimulation>  — um por mapId
  └─ FactionSystem               — global, compartilhado

MapSimulation (por mapId)
  ├─ List<EnemyState>            — inimigos e suas IAs
  ├─ List<ProjectileState>
  ├─ List<ChestState>
  ├─ List<NpcState>
  ├─ GoblinFamily (se aplicável)
  ├─ long lastTickTime           — para simular background
  └─ boolean hasActivePlayers    — otimização de tick rate
```

### Background simulation

Mapas sem jogadores ativos continuam sendo simulados em tick rate reduzido (ex: 4 TPS em vez de 20 TPS), para:

- Raids continuarem progredindo
- Timers de respawn diminuírem
- FactionWarManager acumular progresso
- GoblinLeaderBrain manter estado de guerra

Quando um jogador entra no mapa, o tick rate volta a 20 TPS e o estado já reflete o que aconteceu no background.

---

## Arquitetura de classes proposta

### Servidor

```
com.rpggame.server
  ├─ GameServer.java              — entry point headless, inicia ServerLoop
  ├─ ServerLoop.java              — tick fixo de 20 TPS, orquestra simulação
  ├─ ServerNetwork.java           — aceita conexões, envia snapshots, recebe input
  └─ WorldState.java              — estado completo e persistente de todos os mapas

com.rpggame.server.simulation
  ├─ MapSimulation.java           — simula um mapa: inimigos, projéteis, NPCs
  ├─ PlayerSimulation.java        — processa input, atualiza posição e stats
  └─ SimulationClock.java         — gerencia tick rate por mapa (ativo vs background)
```

### Cliente

```
com.rpggame.client
  ├─ GameClient.java              — entry point, conecta ao servidor
  ├─ ClientNetwork.java           — recebe WorldSnapshot, envia InputPacket
  ├─ ClientRenderer.java          — renderiza snapshots a 60 FPS com interpolação
  └─ ClientInput.java             — captura teclado/mouse, cria InputPacket

com.rpggame.client.ui
  ├─ HUD.java                     — vida, mana, gold, quests
  ├─ InventoryUI.java
  ├─ DialogUI.java
  └─ WorldMapUI.java
```

### Compartilhado (sem dependência de Swing ou rede)

```
com.rpggame.shared
  ├─ WorldSnapshot.java           — DTO de estado do mundo
  ├─ InputPacket.java             — DTO de input
  ├─ GameEvent.java               — eventos discretos
  └─ Constants.java               — TILE_SIZE, TPS, etc.
```

---

## Transporte local

Para multiplayer local (mesma máquina), duas opções ordenadas por simplicidade:

### Opção A — TCP loopback (recomendado para início)

- Servidor escuta em `127.0.0.1:7777`
- Cada cliente abre uma conexão TCP
- Simples, funciona para multiplayer na mesma LAN com mínima mudança
- Serialização: JSON (Jackson/Gson) ou binário simples com DataInputStream

### Opção B — Memória compartilhada (in-process)

- Servidor e cliente rodam na mesma JVM
- Comunicação via fila de mensagens (`BlockingQueue`)
- Zero latência, ideal para testes unitários de server/client
- Migração para TCP depois é só trocar o transporte

**Recomendação:** começar com Opção B para validar a separação, depois migrar para Opção A quando quiser testar LAN.

---

## Sequência de implementação

### Fase 1 — Extrair simulação (sem rede ainda) ✅ CONCLUÍDA

- [x] Criar `WorldState` com `Map<String, MapSimulation>` → `src/com/rpggame/server/WorldState.java`
- [x] Criar `MapSimulation` com estado persistente por mapa (enemies, families, structures, timers) → `src/com/rpggame/server/MapSimulation.java`
- [x] Fazer `MapSimulation` persistir entre trocas de mapa via `saveToSimulation` / `loadFromSimulation` em `EnemyManager`
- [x] Criar `ServerLoop` que faz tick de background em mapas sem jogadores → `src/com/rpggame/server/ServerLoop.java`
- [x] Criar `SimulationClock` para tick rate 20 TPS (ativo) / 4 TPS (background) → `src/com/rpggame/server/SimulationClock.java`
- [x] `GamePanel.changeMap()` usa save/load em vez de clear+reinit
- [x] `GamePanel.initWorldState()` inicializa WorldState e ServerLoop na criação do personagem

**Verificação:** inimigos do mapa A continuam vivos quando você vai para o mapa B e volta.

> Nota: a lógica de simulação ainda vive no `EnemyManager` (Fase 1 intencional). A migração completa
> para `MapSimulation` acontece na Fase 2 junto com a separação de rendering.

### Fase 2 — Separar rendering de simulação

- [x] Criar `WorldSnapshot` como DTO
- [x] Fazer `ServerLoop` produzir um `WorldSnapshot` por tick
- [x] Fazer `GamePanel` consumir `WorldSnapshot` em vez de ler estado direto
- [x] Remover referências a `Graphics2D` de `EnemyManager`, `Player` e entidades

**Verificação:** o cliente renderiza corretamente sem acessar estado interno do servidor.

### Fase 3 — Separar input de simulação ✅ CONCLUÍDA

- [x] Criar `InputPacket` → `src/com/rpggame/shared/InputPacket.java`
- [x] Criar `ClientInput` → `src/com/rpggame/client/ClientInput.java` — captura KeyEvents, produz InputPacket; única classe que conhece Swing input
- [x] Criar `PlayerSimulation` → `src/com/rpggame/server/PlayerSimulation.java` — aplica InputPacket às flags do Player via `setInputUp/Down/Left/Right/Attack` e `triggerSkill`
- [x] `GamePanel.keyPressed/keyReleased` delegam para `ClientInput` em vez de `player.keyPressed` diretamente
- [x] `GamePanel.update()` chama `clientInput.buildPacket()` → `playerSimulation.applyInput()` antes de `player.update()`
- [x] `ServerLoop` expõe `setPlayerSimulation()` e `submitInput()` para receber pacotes de input
- [x] `Player` ganhou `setInputUp/Down/Left/Right/Attack()` e `triggerSkill(int)` para ser controlado por InputPacket

**Verificação:** trocar o input por valores hardcoded no `InputPacket` move o player corretamente.

### Fase 4 — Transporte in-process (Opção B)

- [x] Criar `InProcessTransport` com `BlockingQueue<WorldSnapshot>` (cap 2) e `BlockingQueue<InputPacket>` (cap 8) → `src/com/rpggame/server/InProcessTransport.java`
- [x] `ServerLoop` publica snapshots na fila via `transport.publishSnapshot()` após cada tick
- [x] `ServerLoop.submitInput()` publica InputPacket na fila via `transport.publishInput()` (além do fallback volatile)
- [x] `ServerLoop.tickAllMaps()` drena inputs da fila antes de processar ticks
- [x] `ClientInput` publica InputPacket via `transport.publishInput()` em `buildPacket()` — único ponto de envio
- [x] `GameClient` criado → `src/com/rpggame/client/GameClient.java` — consome snapshots via `transport.pollSnapshot()`
- [x] `GamePanel.update()` chama `gameClient.pollSnapshot()` e usa seus snapshots no render em vez de `serverLoop.getLatestSnapshot()`
- [x] `GamePanel.initWorldState()` conecta transporte a `ClientInput` e `GameClient`

**Verificação:** dois threads (server + client) se comunicando via fila, sem acesso compartilhado ao estado.

> **Estado atual:** servidor e cliente ainda rodam na mesma JVM (in-process).
> A separação em dois processos vem na Fase 6 (TCP). O contrato já está pronto — apenas o transporte muda.

### Fase 5 — Segundo jogador local ✅ CONCLUÍDA

- [x] `Player` ganhou `getPlayerId()` / `setPlayerId()` — identidade única por instância
- [x] `ServerLoop` mantém `Map<playerId, PlayerSimulation>` e `Map<playerId, InProcessTransport>` via `registerPlayer()` / `unregisterPlayer()`
- [x] `WorldSnapshotAssembler.assembleMulti()` recebe `List<Player>` e monta snapshot com todos os players
- [x] `ServerLoop.publishSnapshot()` usa `assembleMulti` e publica em todos os transportes de clientes registrados
- [x] `GamePanel` ganhou `player2`, `clientInput2`, `playerSimulation2`, `gameClient2` e `player2Enabled`
- [x] `GamePanel.enablePlayer2(spritePath)` cria P2, registra no ServerLoop e conecta pipeline dedicado
- [x] `GamePanel.update()` atualiza P2 (input + player.update + gameClient2.pollSnapshot) quando ativo
- [x] `SnapshotRenderSystem.renderPlayer()` itera todos os players do snapshot; P2+ renderizado em verde; interpolação por ID

**Como ativar P2:** chamar `gamePanel.enablePlayer2("sprites/WarriorPlayer.png")` após criar o personagem.

**Verificação:** dois personagens se movendo independentemente, vendo o mesmo mundo.

> P2 usa teclas IJKL mapeadas em `ClientInput` — adicionar mapeamento IJKL em próxima sessão se necessário.

### Fase 6 — TCP loopback (Opção A) ✅ CONCLUÍDA (implementação)

- [x] Criar `TcpFraming.java` — framing TCP com prefixo de 4 bytes → `src/com/rpggame/server/TcpFraming.java`
- [x] Criar `JsonUtil.java` — serialização JSON manual sem deps externas para WorldSnapshot/InputPacket → `src/com/rpggame/server/JsonUtil.java`
- [x] Criar `ServerNetwork.java` com `ServerSocket` e `ClientHandler` por conexão → `src/com/rpggame/server/ServerNetwork.java`
- [x] Criar `GameServer.java` — entry point headless, sem Swing → `src/com/rpggame/server/GameServer.java`
- [x] Criar `ClientNetwork.java` — conecta via TCP, alimenta/drena `InProcessTransport` → `src/com/rpggame/client/ClientNetwork.java`
- [x] `GamePanel` integrado com flag `USE_NETWORK` (false=in-process, true=TCP)
- [x] Handshake cliente↔servidor implementado
- [x] `ServerLoop.getOrCreateSimulationForPlayer()` para suporte ao handshake TCP
- [ ] **Para ativar:** mudar `USE_NETWORK = true` em `GamePanel.java` e rodar `GameServer` como processo separado

**Como rodar em modo TCP:**
```
# Terminal 1 — servidor headless
javac -d bin -encoding UTF-8 -sourcepath src src/com/rpggame/server/GameServer.java
java -cp bin com.rpggame.server.GameServer

# Terminal 2 — cliente (mude USE_NETWORK=true em GamePanel antes de compilar)
java -cp bin com.rpggame.core.Game
```

---

## Acoplamentos críticos a resolver antes

Esses pontos precisam ser desfeitos para a separação funcionar:

| Problema | Localização | Solução |
|---|---|---|
| `Graphics2D` em `Enemy.render()` | `Enemy.java` e subclasses | Extrair para `EnemyRenderer` no client |
| `Graphics2D` em `Player.render()` | `Player.java` | Extrair para `PlayerRenderer` no client |
| `Camera` usada dentro de entidades | Vários arquivos | Camera fica apenas no client |
| `FogOfWar` calculado com player real | `TileMap.java` | Servidor calcula, envia como bool[][] |
| Input flags em `Player` (`up`, `down`) | `Player.java` | Substituir por `InputPacket` |
| `KeyListener` em `GamePanel` | `GamePanel.java` | Mover para `ClientInput` |
| `BufferedImage` em `Player` e `Enemy` | Várias classes | Mover para renderers no client |

---

## O que não muda

- Toda lógica de colisão com `TileMap` — continua no servidor
- `FactionSystem` inteiro — continua no servidor
- `QuestManager` — continua no servidor (dados)
- `GoblinFamily` e `GoblinLeaderBrain` — continuam no servidor
- `WorldGenerator` e `WorldLayout` — gerados uma vez no servidor, enviados ao client como metadado

---

## Definição de sucesso

Esta arquitetura estará implementada corretamente quando:

- Entrar no mapa A, ir ao mapa B, voltar ao A — inimigos do mapa A estão onde foram deixados
- Goblins no mapa B continuam patrulhando enquanto você está no mapa A
- Raid iniciada no mapa C progride e resolve mesmo com o jogador em outro mapa
- Um segundo processo cliente conecta e enxerga o mesmo mundo
- O servidor roda sem janela, sem Swing, sem Graphics2D
- Derrubar o cliente não derruba o servidor
