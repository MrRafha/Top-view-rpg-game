# Multiplayer - Arquitetura, Funcionamento e Evolucao

## Objetivo

Este documento consolida a visao de arquitetura, implementacao e evolucao do multiplayer do Top-view RPG Game. A ideia e explicar, em um unico lugar, como o multiplayer funciona hoje, quais decisoes arquiteturais foram tomadas e o que ainda pode ser melhorado no futuro.

---

## Visao Geral

O multiplayer foi desenhado em torno de uma separacao clara entre simulacao e apresentacao:

- **GameServer**: simula o mundo de forma autoritativa, sem Swing e sem render.
- **GameClient**: captura input, recebe snapshots e renderiza o que o servidor enviou.
- **WorldState**: guarda o estado persistente de todos os mapas no servidor.
- **WorldSnapshot**: transporta o estado necessario para o cliente desenhar o mundo e as entidades.
- **InputPacket**: transporta input do jogador para o servidor.

Na pratica, o servidor decide o que existe e como o mundo evolui; o cliente decide apenas como isso aparece na tela.

---

## Como o Multiplayer Funciona Hoje

### 1. Inicializacao do servidor

O processo headless sobe com `GameServer`.

Responsabilidades do servidor:

- criar o `WorldState`
- iniciar o `ServerLoop`
- aceitar conexoes em `ServerNetwork`
- registrar cada jogador em uma `PlayerSimulation`
- publicar snapshots por tick

### 2. Conexao do cliente

O cliente inicia via `GamePanel`/launcher e conecta ao servidor.

Fluxo tipico:

1. o cliente abre a conexao TCP
2. envia handshake com `playerId` e `playerClass`
3. recebe confirmacao do servidor
4. passa a enviar `InputPacket`
5. passa a receber `WorldSnapshot`

### 3. Ciclo de simulacao

O loop do servidor roda em taxa fixa.

- jogadores enviam input continuamente
- o servidor aplica esse input na simulacao
- inimigos, NPCs, projeteis e faccoes continuam evoluindo
- o servidor monta um snapshot e envia para os clientes conectados

### 4. Ciclo de renderizacao

No cliente, a renderizacao e separada da simulacao.

- o cliente recebe snapshots
- interpola posicoes entre ticks para suavizar movimento
- desenha o estado recebido em 60 FPS
- a camera e a UI continuam locais

---

## Estrutura Tecnica Principal

### Servidor

- `GameServer`: entry point headless
- `ServerLoop`: executa a simulacao e publica snapshots
- `ServerNetwork`: aceita conexoes e faz o bridge TCP
- `WorldState`: guarda todos os mapas e estado persistente
- `MapSimulation`: estado de um mapa especifico
- `PlayerSimulation`: aplica input em um player
- `WorldSnapshotAssembler`: monta o DTO de snapshot

### Cliente

- `GamePanel`: orquestra update, input e render
- `ClientNetwork`: abre conexao TCP e troca mensagens
- `ClientInput`: captura input local e monta `InputPacket`
- `GameClient`: integra snapshots ao ciclo do cliente
- `SnapshotRenderSystem`: desenha entidades com interpolacao

### Compartilhado

- `WorldSnapshot`: contrato de leitura do mundo
- `InputPacket`: contrato de input do jogador
- `ServerInfo`: metadados para lobby e descoberta

---

## Protocolo de Comunicacao

### Handshake

No inicio da conexao, o cliente identifica o jogador e a classe escolhida.

Exemplo conceitual:

```json
{
  "playerId": "player-1",
  "playerClass": "Warrior"
}
```

O servidor responde com confirmacao e informacoes iniciais da sessao.

### InputPacket

O input representa o que o jogador esta tentando fazer naquele frame.

Campos tipicos:

- direcao
- ataque
- skills
- interacao
- posicao do mouse
- identificador do player

### WorldSnapshot

O snapshot representa o estado visivel do mundo em um tick.

Pode conter:

- players
- enemies
- projectiles
- NPCs
- baus
- estado de faccoes
- eventos discretos

---

## Lobby e Descoberta de Servidores

A evolucao da Fase 7 adicionou a ideia de lobby e descoberta de servidores na rede local.

Fluxo esperado:

- um jogador cria um servidor
- o servidor divulga sua presenca na rede
- outros clientes descobrem a sessao ativa
- cada cliente pode conectar ao host escolhido

Os componentes pensados para isso incluem:

- `GameLauncher`
- `ServerInfo`
- `ServerBroadcaster`
- `ServerDiscovery`

Isso transforma o jogo de um modelo apenas local para um modelo em que uma instancia vira host para varios clientes.

---

## Pontos Ja Resolvidos pela Arquitetura Atual

### Separacao de simulacao e render

O cliente deixou de depender diretamente do estado interno da simulacao para desenhar o mundo.

### Base para multiplos clientes

A estrutura de servidor suporta mais de uma conexao por vez, com identificacao por `playerId`.

### Persistencia de estado por mapa

O `WorldState` permite manter mapas e simulacoes vivas mesmo quando o jogador muda de area.

### Interpolacao visual

A renderizacao por snapshot com interpolacao reduz o efeito de salto visual entre ticks.

### Servidor separado do cliente

O `GameServer` pode rodar sem Swing, o que abre caminho para servidores dedicados.

---

## Limites e Melhorias Futuras

### 1. Manifesto inicial do mundo

Melhoria desejada: o cliente receber o layout/base do mundo no connect, para reconstruir mapas localmente.

Beneficio:

- reduz dependencia de snapshot para tiles
- evita efeitos visuais como rollback de mapa
- diminui trafego por rede

### 2. Tiles e mapa estatico no cliente

O ideal e que o cliente desenhe o mapa estatico a partir de um manifesto ou seed, e use snapshots apenas para entidades dinamicas.

### 3. Deltas de estado em vez de resending completo

Mudancas permanentes, como portas abertas, baus abertos ou estruturas destruidas, devem viajar como eventos pontuais.

### 4. Client-side prediction mais clara

Em movimentos mais sensiveis, o cliente pode prever localmente o proprio movimento e reconciliar depois com o snapshot do servidor.

### 5. Sincronizacao de mapa e camera

Se houver muita divergencia entre tick do servidor e render do cliente, a interpolacao pode precisar ser refinada para reduzir jitter.

### 6. Lobby mais completo

O sistema de lobby pode evoluir para incluir:

- nome da sala
- maximo de jogadores
- lista de servidores na LAN
- reconexao
- kick
- status da partida

### 7. Persistencia real de sessao

Em uma evolucao futura, o servidor pode salvar o estado do mundo em disco para retomada posterior.

### 8. Melhor separacao entre estado dinamico e estatico

Quanto mais clara for a separacao entre o que muda por tick e o que e fixo do mapa, menor o acoplamento e maior a estabilidade visual.

---

## Riscos Tecnicos

- Se o cliente reconstruir o mapa com ordem diferente da do servidor, portais e transicoes podem quebrar.
- Se o mapa nao for deterministico, seed nao basta e sera preciso transmitir o layout completo.
- Se o snapshot continuar carregando informacao estatica do mapa, o efeito visual de rollback pode persistir.
- Se um cliente tentar renderizar dados de simulacao enquanto o servidor tambem os altera, podem surgir races visuais.

---

## Como Pensar nas Responsabilidades

Regra pratica:

- **Simulacao**: vida, dano, IA, colisao, progresso, faccoes, respawn.
- **Apresentacao**: camera, sprite, efeitos visuais, texto flutuante, UI.
- **Transporte**: input, snapshot, handshake, descoberta de servidor.

Isso ajuda a evitar que o cliente vire dono de regras que deveriam continuar no servidor.

---

## Resumo Final

O multiplayer do jogo funciona com um servidor autoritativo, varios clientes conectados por TCP e um contrato claro de entrada e saida entre eles.

Hoje a arquitetura ja permite:

- rodar o servidor separado do cliente
- manter estado persistente por mapa
- trocar input por snapshot sem compartilhar memoria
- preparar base para multiplayer local e lobby

O principal passo futuro e reduzir ainda mais a dependencia do cliente em snapshots para partes estaticas do mapa, enviando manifesto inicial e deltas em vez de redraw completo.