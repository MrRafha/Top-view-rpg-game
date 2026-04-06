# Relatorio Tecnico - Fase 2 da Arquitetura Multiplayer

## Contexto

Este relatorio documenta a implementacao da Fase 2 do planejamento de arquitetura multiplayer do projeto, com foco em avaliacao de codigo, rastreabilidade de mudancas e orientacoes para evolucao.

A Fase 2 tinha quatro objetivos centrais no planejamento original:

- criar um `WorldSnapshot` como DTO
- fazer o `ServerLoop` produzir snapshots por tick
- fazer o `GamePanel` consumir snapshot em vez de ler estado interno diretamente
- remover `Graphics2D` de `EnemyManager`, `Player` e entidades

O checklist correspondente foi atualizado como concluido em [docs/Planejamento_Arquitetura_Multiplayer.md](docs/Planejamento_Arquitetura_Multiplayer.md).

## Sumario Executivo

A Fase 2 foi implementada como uma separacao estrutural entre estado de simulacao e estado de apresentacao. O servidor passou a consolidar o mundo em snapshots imutaveis, e o cliente passou a renderizar a partir desses snapshots com interpolacao temporal para reduzir stuttering.

Do ponto de vista de avaliacao de codigo, a principal melhoria nao foi apenas a criacao de um novo DTO. O ganho real veio de tres mudancas de desenho:

1. o estado renderizavel saiu das entidades e foi centralizado em `WorldSnapshot`
2. o pipeline de render passou a ser dirigido por dados, nao por acesso direto ao modelo
3. a camada de apresentacao ganhou interpolacao entre ticks para compensar a taxa fixa de 20 TPS

## Evidencias Principais

### 1. O cliente nao usa mais o estado interno como fonte primaria de render

O `GamePanel` agora guarda dois snapshots e usa ambos para interpolar a renderizacao:

- estado de snapshot anterior e atual em [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L103-L104)
- atualizacao do contexto de snapshot em [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L536)
- calculo de interpolacao em [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L575)
- render orientado a snapshot em [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L581)

Interpretacao tecnica:
- o `GamePanel` ainda continua sendo o orquestrador visual
- mas o dado que ele desenha passou a ser derivado do snapshot, e nao diretamente do `Player` ou do `EnemyManager`
- isso reduz o acoplamento de apresentacao com simulação

### 2. O servidor agora publica o estado de mundo por tick

O `ServerLoop` tornou-se o ponto de consolidacao do snapshot:

- loop principal e publicacao em [src/com/rpggame/server/ServerLoop.java](src/com/rpggame/server/ServerLoop.java#L37-L37)
- tick de background em [src/com/rpggame/server/ServerLoop.java](src/com/rpggame/server/ServerLoop.java#L110-L110)
- atualizacao do contexto em [src/com/rpggame/server/ServerLoop.java](src/com/rpggame/server/ServerLoop.java#L147-L147)
- publicacao do snapshot em [src/com/rpggame/server/ServerLoop.java](src/com/rpggame/server/ServerLoop.java#L164-L164)

Interpretacao tecnica:
- o servidor ainda opera no mesmo processo da aplicacao nesta fase
- porem, conceitualmente, ele ja se comporta como fonte de verdade do estado
- a publicacao por tick cria a base necessaria para depois migrar para fila ou TCP sem reescrever a modelagem

### 3. O snapshot passou a ser um contrato de apresentacao e resumo global

A definicao do DTO esta em [src/com/rpggame/shared/WorldSnapshot.java](src/com/rpggame/shared/WorldSnapshot.java#L9-L17) e os principais subtipos estao em:

- `SnapshotPlayer` em [src/com/rpggame/shared/WorldSnapshot.java](src/com/rpggame/shared/WorldSnapshot.java#L93-L105)
- `SnapshotEnemy` em [src/com/rpggame/shared/WorldSnapshot.java](src/com/rpggame/shared/WorldSnapshot.java#L160-L172)
- `SnapshotNpc` em [src/com/rpggame/shared/WorldSnapshot.java](src/com/rpggame/shared/WorldSnapshot.java#L257-L266)
- `SnapshotChest` em [src/com/rpggame/shared/WorldSnapshot.java](src/com/rpggame/shared/WorldSnapshot.java#L305-L311)

O assembler que popula esses campos esta em [src/com/rpggame/server/WorldSnapshotAssembler.java](src/com/rpggame/server/WorldSnapshotAssembler.java#L19-L51).

Interpretacao tecnica:
- o snapshot nao captura apenas posicoes
- ele tambem carrega metadados suficientes para render realista, como spritePath, dimensoes e estado de faccoes
- isso reduz a necessidade de o cliente conhecer a semantica interna das entidades

### 4. O render foi deslocado para um sistema dedicado

A renderizacao principal por snapshot esta concentrada em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L27-L34).

Metodos relevantes:

- render de inimigos em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L34-L34)
- render de player em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L85-L85)
- render de projeteis em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L117-L117)
- render de NPCs em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L138-L138)
- render de baus em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L168-L168)
- interpolacao em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L260-L260)

Interpretacao tecnica:
- a renderizacao virou uma funcao de dados + camera + alpha
- o sistema agora pode ser testado com snapshots artificiais, sem necessidade de instanciar toda a simulacao
- isso melhora auditabilidade e facilita future refactors para multiplayer real

### 5. O antigo acoplamento de render foi removido das classes centrais

No codigo atual, as classes de simulacao deixaram de manter `Graphics2D` como responsabilidade principal.

Referencias que marcam a transicao:

- `EnemyManager` sem render acoplado em [src/com/rpggame/systems/EnemyManager.java](src/com/rpggame/systems/EnemyManager.java)
- `Player` sem render acoplado em [src/com/rpggame/entities/Player.java](src/com/rpggame/entities/Player.java)
- `Enemy` sem render acoplado em [src/com/rpggame/entities/Enemy.java](src/com/rpggame/entities/Enemy.java)

Interpretacao tecnica:
- o modelo passou a expor estado, nao pixels
- isso e necessario para qualquer separacao real entre servidor e cliente
- a regra pratica agora e: simulacao produz estado; render consome estado

## O Que Essa Implementacao Resolveu

### Separacao de autoridade

A autoridade da simulacao ficou mais clara: o servidor monta o snapshot, enquanto o cliente apenas apresenta.

### Persistencia de estado por mapa

Como a Fase 2 continua apoiada no `WorldState`, o estado de mapas e entidades persistentes nao depende do redraw local do cliente.

### Reducao de stutter

A interpolacao entre snapshots tira o movimento visual do modo "salto por tick" e aproxima o comportamento de 60 FPS mesmo com simulação em 20 TPS.

### Base para multiplayer local

Ao tornar o snapshot o contrato primario, a migracao para filas in-process ou TCP loopback fica mais direta, porque o cliente ja nao depende de acesso a objetos vivos do servidor.

## Pontos de Cuidado Para Avaliacao de Codigo

### 1. O render principal ainda convive com UI e sistemas legados

Apesar da renderizacao principal estar orientada a snapshot, o `GamePanel` ainda coordena UI, dialogos, minigame, transicao e outros sistemas de tela.

Caminho relevante:
- [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L540-L620)

Leitura tecnica:
- isso nao invalida a Fase 2
- mas indica que a separacao completa cliente/render ainda nao esta finalizada
- o proximo passo e extrair camadas de UI e efeitos para render systems dedicados

### 2. A interpolacao depende de snapshots regulares

A interpolacao em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L260-L260) pressupoe chegada consistente de snapshots.

Se o tick do servidor atrasar ou parar de publicar, o cliente ainda funciona, mas a suavidade visual degrada.

Implicacao para avaliacao:
- o sistema e robusto o suficiente para a fase atual
- porem, em multiplayer real, a cadencia do snapshot precisara de monitoramento e possivel fallback

### 3. Os sprites dependem de convencoes de naming

O render com sprites reais depende de metadata e convencoes de arquivo. Isso e util para a fase atual, mas tambem cria acoplamento leve com o layout de assets.

Leitura tecnica:
- a solucao e pratica para prototipo e integracao local
- em uma evolucao posterior, pode valer a pena criar um registry de sprites por tipo, para reduzir dependencia de strings de caminho

### 4. O snapshot hoje cobre o mapa ativo e um resumo de background

A Fase 2 ja incluiu resumo global, mas ainda nao transporta toda a simulacao de todos os mapas em detalhe.

Isso aparece na montagem de background em [src/com/rpggame/server/WorldSnapshotAssembler.java](src/com/rpggame/server/WorldSnapshotAssembler.java#L177-L220).

Leitura tecnica:
- suficiente para HUD e leitura de estado global
- insuficiente para sincronizacao de mundo completo entre clientes remotos

## Orientacoes Para Proximas Modificacoes

### Se o objetivo for estabilidade visual

1. Mantenha `WorldSnapshot` como contrato unico de leitura do cliente.
2. Preserve a interpolacao entre snapshots em [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L536-L581).
3. Evite reintroduzir desenhar direto a partir de entidades vivas.

### Se o objetivo for evoluir para multiplayer real

1. Substitua o transporte in-process por fila ou socket sem mudar o formato do snapshot.
2. Mantenha o assembler como ponto unico de serializacao do mundo.
3. Introduza `InputPacket` como contrato simetrico do lado do cliente.

### Se o objetivo for facilitar review e manutencao

1. Separe render de entidade em sistemas pequenos e previsiveis.
2. Evite concentrar novos estados visuais em classes de simulacao.
3. Prefira adicionar campos ao snapshot em vez de acessar o modelo diretamente no cliente.

### Se for preciso auditar regressao

Use esta ordem de verificação:

1. `GamePanel` ainda usa snapshot e alfa de interpolacao em [src/com/rpggame/core/GamePanel.java](src/com/rpggame/core/GamePanel.java#L575-L581)
2. `ServerLoop` ainda publica snapshot por tick em [src/com/rpggame/server/ServerLoop.java](src/com/rpggame/server/ServerLoop.java#L164-L180)
3. `WorldSnapshotAssembler` continua sendo a unica fonte de montagem do contrato em [src/com/rpggame/server/WorldSnapshotAssembler.java](src/com/rpggame/server/WorldSnapshotAssembler.java#L21-L51)
4. `SnapshotRenderSystem` continua sem depender de estado vivo da simulacao em [src/com/rpggame/render/SnapshotRenderSystem.java](src/com/rpggame/render/SnapshotRenderSystem.java#L34-L260)

## Leitura Tecnica Final

Do ponto de vista de arquitetura, a Fase 2 foi mais do que um refactor de render. Ela transformou o jogo de um modelo em que a apresentacao lia os objetos da simulacao para um modelo em que a apresentacao consome um contrato de estado.

Esse e o principal ganho para avaliacao de codigo:

- reduz acoplamento entre simulacao e UI
- melhora a previsibilidade do comportamento visual
- torna possivel medir e testar o mundo por snapshot
- prepara a base para transporte real e multiplayer local

## Resumo para Uso em Code Review

Use este criterio ao revisar mudancas futuras:

- se a mudanca toca regras de gameplay, ela deve ficar no lado da simulacao
- se a mudanca toca sprite, camera, efeito visual ou interpolacao, ela deve ficar no lado do render
- se a mudanca precisa atravessar a fronteira servidor/cliente, ela deve entrar no `WorldSnapshot` ou em outro DTO explicito

Em termos praticos, a regra de ouro da Fase 2 ficou assim: o servidor decide o que existe; o cliente decide como isso aparece.
