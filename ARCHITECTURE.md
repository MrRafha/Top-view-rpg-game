# 🌐 Arquitetura Multiplayer - Documentação Técnica

## Status do Projeto

O **Top-view RPG Game** está em desenvolvimento de **arquitetura multiplayer local** com separação completa entre cliente e servidor.

---

## Fases de Implementação

| # | Fase | Status | Descrição |
|---|---|---|---|
| **1** | Extrair Simulação | ✅ Concluída | WorldState, MapSimulation, ServerLoop |
| **2** | Separar Rendering | ✅ Concluída | WorldSnapshot, SnapshotRenderSystem |
| **3** | Separar Input | ✅ Concluída | InputPacket, ClientInput |
| **4** | Transporte In-Process | ✅ Concluída | BlockingQueue, InProcessTransport |
| **5** | Segundo Jogador | ✅ Concluída | Múltiplos players no mesmo server |
| **6** | TCP Loopback | ✅ Concluída | GameServer headless, servidor 100% separado |
| **7** | Server Lobby | 🚀 **PRÓXIMA** | GameLauncher, UDP discovery, multiplicador |
| **8** | Admin UI | 📋 Planejada | Status, kick, persistência |

---

## 📚 Documentação por Nível

### 🎓 Iniciantes (Quer apenas usar)

Se você quer **jogar o jogo**, vá para [Como Jogar](README.md#-como-jogar).

### 👨‍💻 Desenvolvedores (Quer entender)

Se você quer **entender como o código está organizado**:

1. **Comece:** [Multiplayer - Arquitetura, Funcionamento e Evolucao](docs/Multiplayer_Arquitetura_e_Evolucao.md)
  - Entenda como o multiplayer funciona hoje
  - Veja os pontos de evolucao futura
   
2. **Aprofunde:** [Relatório Técnico - Fase 2](Relatorio_Tecnico_Fase_2_Avaliacao.md)
   - Análise linha por linha do código
   - Pontos de integração e acoplamentos
   - Riscos e considerações arquiteturais

### 🚀 Implementadores (Quer fazer)

Se você quer **implementar ou evoluir o multiplayer**:

**Comece aqui:** [Multiplayer - Arquitetura, Funcionamento e Evolucao](docs/Multiplayer_Arquitetura_e_Evolucao.md)
- Veja a visão geral do fluxo
- Revise as melhorias futuras sugeridas

---

## 🎯 O que cada fase entrega

### ✅ Fase 1 — Simulação Persistente

**Objetivo:** Mapas continuarem simulando quando o jogador não está neles.

**O que muda:**
- `WorldState` armazena todos os mapas
- `MapSimulation` por mapa com estado independente
- `ServerLoop` faz tick de background em 4 TPS

**Resultado:** Goblins continuam patrulhando enquanto você explora outro mapa.

### ✅ Fase 2 — Rendering Orientado a Dados

**Objetivo:** Cliente não acessa estado vivo do servidor.

**O que muda:**
- Novo DTO: `WorldSnapshot` (dados imutáveis)
- `SnapshotRenderSystem` renderiza via snapshot
- `GamePanel` consome snapshot com interpolação
- Remoção de `Graphics2D` de entidades

**Resultado:** Rendering suave @ 60 FPS mesmo com simulação em 20 TPS.

### ✅ Fase 3 — Input Separado

**Objetivo:** Input capturado separadamente, não afeta simulação diretamente.

**O que muda:**
- `InputPacket` DTO com estado de teclado
- `ClientInput` captura input local
- `PlayerSimulation` aplica input ao modelo

**Resultado:** Input padronizado, pronto p/ rede.

### ✅ Fase 4 — Transporte Local

**Objetivo:** Servidor e cliente se comunicam via fila, não compartilham memória.

**O que muda:**
- `InProcessTransport` com BlockingQueue
- `GameServer` publica snapshots
- `GameClient` consome snapshots

**Resultado:** Simulação de cliente/servidor na mesma JVM, sem race conditions.

### ✅ Fase 5 — Segundo Jogador

**Objetivo:** Múltiplos players no mesmo server.

**O que muda:**
- `Player.getPlayerId()` identifica cada player
- `ServerLoop` gerencia múltiplos `PlayerSimulation`
- `WorldSnapshotAssembler.assembleMulti()` inclui todos os players
- `SnapshotRenderSystem.renderPlayer()` itera múltiplos players

**Resultado:** Dois jogadores veem um ao outro se movimentando.

### ✅ Fase 6 — TCP Headless

**Objetivo:** Servidor roda sem GUI, clientes conectam via TCP.

**O que muda:**
- `GameServer` entry point separado (sem Swing)
- `ServerNetwork` aceita múltiplas conexões TCP
- `ClientNetwork` conecta a servidor remoto
- Serialização JSON (sem deps)

**Resultado:** Servidor em um terminal, clientes em outros, comunicam via TCP.

### 🚀 Fase 7 — Server Lobby (PRÓXIMA)

**Objetivo:** Um jogador cria um server, amigos descobrem e conectam.

**O que vai mudar:**
- `ServerInfo` DTO com nome, addr, maxPlayers
- `ServerBroadcaster` publica servidor via UDP
- `ServerDiscovery` encontra servidores na LAN
- `GameLauncher` UI para criar/conectar
- `GameConnection` TCP client com reconnect

**Resultado:** Amigos em diferentes máquinas jogam juntos!

### 📋 Fase 8 — Admin & Persistência (Planejada)

**Objetivo:** Funcionalidades operacionais.

**O que vai mudar:**
- Status page do servidor (ping, players, TPS real)
- Admin commands (kick, pause, settings)
- Save/load de estado no disco
- Chat in-game entre clientes

---

## 💾 Onde está cada coisa

```
src/com/rpggame/

SIMULAÇÃO (Server-side)
├─ server/
│  ├─ GameServer.java             # Entry point headless
│  ├─ ServerLoop.java            # 20 TPS simulation loop
│  ├─ WorldState.java            # Todos os mapas
│  ├─ MapSimulation.java         # Estado de um mapa
│  ├─ WorldSnapshotAssembler.java # Monta snapshots
│  ├─ ServerNetwork.java         # TCP server
│  ├─ ServerRegistry.java        # Descoberta de servidores
│  └─ ServerBroadcaster.java     # UDP broadcast
│
APRESENTAÇÃO (Client-side)
├─ core/
│  ├─ GamePanel.java             # Main render + game loop
│  └─ Game.java                  # Main tradicional
│
├─ client/
│  ├─ GameClient.java            # Client com conexão
│  ├─ GameConnection.java        # TCP client
│  ├─ ClientInput.java           # Captura input local
│  ├─ ServerDiscovery.java       # Encontra servidores
│  └─ ClientNetwork.java         # TCP client handler
│
├─ render/
│  └─ SnapshotRenderSystem.java  # Render orientado a snapshot
│
├─ ui/
│  ├─ GameLauncher.java          # Tela de lobby
│  └─ ... (outras UIs)
│
COMPARTILHADO (ambos usam)
└─ shared/
   ├─ WorldSnapshot.java         # DTO mundo
   ├─ InputPacket.java           # DTO input
   ├─ ServerInfo.java            # DTO descoberta
   └─ ... (eventos, tipos)
```

---

## 🔌 Pontos de Integração

### Cliente → Servidor

```
InputPacket (JSON)
  up, down, left, right
  attack, skill1, skill2, skill3, skill4
  interact, mouseX, mouseY
    ↓
ServerLoop.tickAllMaps()
  ↓
Player.update() (aplica input)
  ↓
WorldSnapshotAssembler.assemble()
```

### Servidor → Cliente

```
WorldSnapshot (JSON)
  players: [{id, x, y, hp, state, ...}]
  enemies: [{id, type, x, y, hp, aiState, ...}]
  projectiles, npcs, chests, factionStatus, events
    ↓
GamePanel.update()
  ↓
SnapshotRenderSystem.render*()
  ↓
Tela exibe mundo
```

---

## 📊 Taxa de Comunicação

| Direção | Taxa | Tamanho | Bandwidth |
|---------|------|---------|-----------|
| Client → Server | 60 FPS (input) | ~200B | ~12 KB/s |
| Server → Client | 20 TPS (snapshot) | ~2.5 KB | ~50 KB/s |
| **Total por Client** | **~62 KB/s** | — | — |
| **4 Clients** | — | — | **~250 KB/s** |

💡 Compressão gzip reduz ~5x.

---

## 🛠️ Como Contribuir

### Implementar Fase 7

1. Leia o documento consolidado de multiplayer
2. Use os pontos de evolucao como guia para a proxima iteracao
3. Revise `GameServer`, `ServerLoop` e `ClientNetwork` antes de mudar o protocolo

### Revisar Código Existente

Use o [Relatório Técnico - Fase 2](Relatorio_Tecnico_Fase_2_Avaliacao.md) para entender:
- Mudanças arquiteturais
- Análise de risco
- Pontos de manutenção

---

## 🐛 Suporte Técnico

| Pergunta | Resposta |
|----------|----------|
| Como a simulação roda sem cliente? | `ServerLoop` em thread dedicada, 20 TPS |
| Sem é feita a interpolação? | `SnapshotRenderSystem.computeInterpolationAlpha()` |
| Como o snapshots chega tão rápido? | BlockingQueue (in-process) ou TCP (multiplo cliente) |
| O que impede double-tick? | Mapa ativo ticked por `GamePanel`, não por `ServerLoop` |
| Segurança? | V1: versão + rate-limit; fazer melhor depois |

---

## 📜 Referências

- **Planejamento Completo:** [Multiplayer - Arquitetura, Funcionamento e Evolucao](docs/Multiplayer_Arquitetura_e_Evolucao.md)
- **Fase 2 Análise:** [Relatorio_Tecnico_Fase_2_Avaliacao.md](Relatorio_Tecnico_Fase_2_Avaliacao.md)
- **Fase 2 e 7:** [Multiplayer - Arquitetura, Funcionamento e Evolucao](docs/Multiplayer_Arquitetura_e_Evolucao.md)

---

## 🚀 Próximas Passos

1. **Curto prazo:** Implementar Fase 7
2. **Médio prazo:** Admin UI (Fase 8)
3. **Longo prazo:** Matchmaking online, mods, progression salvo

Boa sorte! 💪
