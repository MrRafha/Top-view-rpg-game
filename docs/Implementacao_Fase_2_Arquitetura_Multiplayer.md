# Manual de Implementação — Fase 6: TCP Loopback

## Objetivo

Substituir o transporte in-process (`BlockingQueue` / `InProcessTransport`) por um transporte TCP real,
mantendo o contrato `WorldSnapshot` → servidor e `InputPacket` → cliente **sem alterar nada fora da
camada de transporte**.

Ao final desta fase:

- O servidor roda como processo headless independente (sem Swing, sem janela)
- O cliente conecta via `127.0.0.1:7777` e opera identicamente ao comportamento atual
- Derrubar o cliente não afeta o servidor
- Um segundo cliente pode se conectar do mesmo PC ou da mesma LAN

---

## Pré-requisitos obrigatórios

Antes de começar qualquer classe nova, confirme que:

- [x] `WorldSnapshot` está em `com.rpggame.shared` e **não importa nada de Swing**
- [x] `InputPacket` está em `com.rpggame.shared` e **não importa nada de Swing**
- [x] `ServerLoop` não importa nada de `java.awt` ou `javax.swing`
- [x] `GamePanel` (cliente) não chama métodos de simulação diretamente — usa apenas `GameClient`
- [x] `ClientInput` é a única classe que toca `KeyEvent`

Se qualquer item estiver quebrado, corrija antes de prosseguir. Misturar rede com dependências de Swing
causará erros de classloader ao rodar o servidor headless.

---

## Arquitetura final da Fase 6

```
Processo A — GameServer (headless)            Processo B — GameClient (Swing)
┌──────────────────────────────────┐          ┌──────────────────────────────────┐
│  ServerLoop @ 20 TPS             │          │  GamePanel (Swing thread)        │
│  WorldState + MapSimulations     │          │  SnapshotRenderSystem            │
│  PlayerSimulation (por jogador)  │          │  ClientInput (KeyEvent)          │
│  WorldSnapshotAssembler          │          │  GameClient                      │
│                                  │          │                                  │
│  ServerNetwork                   │◄────────►│  ClientNetwork                   │
│    ServerSocket(:7777)           │  TCP/IP  │    Socket(127.0.0.1:7777)        │
│    Thread por cliente conectado  │          │    Thread de leitura (snapshot)  │
│    serialização JSON             │          │    Thread de escrita (input)     │
└──────────────────────────────────┘          └──────────────────────────────────┘
```

Cada cliente conectado no servidor ganha:
- Uma thread de leitura (recebe `InputPacket` do cliente)
- Uma thread de escrita (envia `WorldSnapshot` para o cliente)
- Um `InProcessTransport` interno (mesmo contrato já existente)

---

## Serialização

Use **JSON via Gson** (sem dependência extra se já for usada) ou **Jackson**.  
Se o projeto ainda não tiver nenhuma das duas, adicione Gson ao classpath:

```
lib/gson-2.10.1.jar
```

Regra: `WorldSnapshot` e `InputPacket` devem serializar/desserializar com **campos públicos ou getters**
simples. Evite tipos não-serializáveis (BufferedImage, Color, Graphics2D) nesses DTOs — se existirem,
remova antes de começar.

Protocolo de mensagem (framing):

```
[4 bytes big-endian: comprimento do payload JSON em bytes][payload UTF-8]
```

Isso evita problemas de fragmentação de pacotes TCP. Implemente `writeMessage(OutputStream, String)` e
`readMessage(InputStream)` como utilitários estáticos reutilizáveis.

---

## Classes a criar

### `src/com/rpggame/server/ServerNetwork.java`

Responsabilidade: aceitar conexões TCP e gerenciar o ciclo de vida de cada cliente conectado.

```
ServerNetwork
  - serverSocket: ServerSocket
  - serverLoop: ServerLoop           ← referência para registerPlayer / unregisterPlayer
  - gson: Gson
  - running: volatile boolean

  + start(port)                      ← abre ServerSocket, lança thread de accept em loop
  + stop()                           ← fecha ServerSocket, interrompe threads
  - acceptLoop()                     ← loop: aceita Socket → cria ClientHandler → registra player
```

**`ClientHandler`** (classe interna ou separada):

```
ClientHandler implements Runnable
  - socket: Socket
  - transport: InProcessTransport    ← obtido de serverLoop.registerPlayer(simulation)
  - simulation: PlayerSimulation
  - playerId: String
  - gson: Gson

  + run()                            ← lança inputReader thread + snapshotWriter thread
  - inputReaderLoop()                ← lê InputPacket do socket → transport.publishInput()
  - snapshotWriterLoop()             ← drena transport.pollSnapshot() → escreve no socket
  - cleanup()                        ← serverLoop.unregisterPlayer(playerId), fecha socket
```

Precauções:
- Use `try-with-resources` em todos os streams
- Nunca feche o `ServerSocket` dentro do `ClientHandler` — apenas o `Socket` do cliente
- Trate `SocketException` e `EOFException` como desconexão normal (não logar como erro crítico)
- Chame `cleanup()` no `finally` do `run()` para garantir desregistro mesmo em crash

---

### `src/com/rpggame/server/GameServer.java`

Entry point headless. Não deve importar nada de Swing.

```java
public class GameServer {
  public static void main(String[] args) throws Exception {
    // 1. Criar WorldState
    // 2. Criar ServerLoop (sem GamePanel)
    // 3. Criar ServerNetwork
    // 4. serverLoop.start()
    // 5. serverNetwork.start(7777)
    // 6. Aguardar (Thread.currentThread().join() ou shutdown hook)
  }
}
```

Precauções:
- `ServerLoop` não deve ter referência a `GamePanel` — já que agora são processos separados,
  remova qualquer campo `GamePanel` que ainda exista nele
- Adicione shutdown hook com `Runtime.getRuntime().addShutdownHook(...)` para chamar
  `serverLoop.stop()` e `serverNetwork.stop()` ao receber Ctrl+C

---

### `src/com/rpggame/client/ClientNetwork.java`

Responsabilidade: manter a conexão TCP com o servidor, publicar inputs e consumir snapshots.

```
ClientNetwork
  - socket: Socket
  - transport: InProcessTransport    ← o mesmo já usado por GameClient e ClientInput
  - gson: Gson
  - running: volatile boolean

  + connect(host, port)              ← abre Socket, lança snapshotReader + inputWriter threads
  + disconnect()                     ← para threads, fecha socket
  - snapshotReaderLoop()             ← lê JSON do socket → desserializa WorldSnapshot → transport.publishSnapshot()
  - inputWriterLoop()                ← drena transport (input side) → serializa → escreve no socket
```

Precauções:
- `snapshotReaderLoop` e `inputWriterLoop` devem rodar em threads separadas com nomes descritivos
  (`"client-snapshot-reader"` e `"client-input-writer"`)
- Em `disconnect()`, sete `running = false` e feche o socket — isso desbloqueará os `read()` bloqueados
- `GameClient` e `ClientInput` **não mudam** — continuam usando `InProcessTransport` normalmente;
  apenas o `ClientNetwork` alimenta/drena esse transporte pela rede

---

## Mudanças em classes existentes

### `GamePanel.java`

Troque a criação de `InProcessTransport` direta por conexão via `ClientNetwork`:

```java
// Antes (Fase 4/5):
InProcessTransport transport = serverLoop.registerPlayer(playerSim);
clientInput.setTransport(transport);
gameClient = new GameClient(transport);

// Depois (Fase 6):
ClientNetwork clientNetwork = new ClientNetwork(transport);
clientNetwork.connect("127.0.0.1", 7777);
// clientInput e gameClient continuam usando o mesmo transport
```

Se você quiser manter o modo in-process para desenvolvimento local (sem precisar iniciar dois processos),
mantenha o caminho antigo como fallback controlado por uma flag:

```java
private static final boolean USE_NETWORK = false; // mude para true na Fase 6
```

### `ServerLoop.java`

Verifique se `registerPlayer(PlayerSimulation)` e `unregisterPlayer(String)` já funcionam sem referência
a `GamePanel`. Se `ServerLoop` ainda guardar referência a `GamePanel`, remova-a — o servidor headless
não pode depender de classes Swing.

---

## Protocolo de handshake (mínimo)

Quando um cliente conecta:

1. **Cliente → Servidor:** envia JSON `{ "playerId": "player-1", "playerClass": "Warrior" }`
2. **Servidor → Cliente:** confirma com JSON `{ "ok": true, "assignedPlayerId": "player-1" }`
3. A partir daí, o loop normal começa: servidor envia snapshots, cliente envia inputs

Isso permite que o servidor crie o `PlayerSimulation` com os dados corretos antes de começar a enviar snapshots.

---

## Sequência de implementação (sub-fases)

### Sub-fase 6.1 — Utilitários de framing TCP ✅ CONCLUÍDA
- [x] Criar `TcpFraming.java` em `com.rpggame.server` com `writeMessage(OutputStream, String)` e `readMessage(InputStream)` → `src/com/rpggame/server/TcpFraming.java`
- [x] `JsonUtil.java` criado em `com.rpggame.server` — serialização/desserialização JSON manual sem deps externas para `WorldSnapshot` e `InputPacket` → `src/com/rpggame/server/JsonUtil.java`
- [x] Compilação limpa confirmada

### Sub-fase 6.2 — ServerNetwork + GameServer ✅ CONCLUÍDA
- [x] Criar `ServerNetwork.java` com `ClientHandler` interno → `src/com/rpggame/server/ServerNetwork.java`
- [x] Criar `GameServer.java` (entry point headless, sem Swing) → `src/com/rpggame/server/GameServer.java`
- [x] `ServerLoop.getOrCreateSimulationForPlayer(String)` adicionado para handshake TCP
- [x] Compilação limpa confirmada: `javac -d bin -encoding UTF-8 -sourcepath src src/com/rpggame/server/GameServer.java`
- [ ] Rodar `GameServer` e confirmar que escuta na porta 7777 com `netstat -an | findstr 7777` *(teste manual)*

### Sub-fase 6.3 — ClientNetwork ✅ CONCLUÍDA
- [x] Criar `ClientNetwork.java` → `src/com/rpggame/client/ClientNetwork.java`
- [x] Integrar em `GamePanel.initWorldState()` com flag `USE_NETWORK = false` (padrão de desenvolvimento)
- [x] Flag `NETWORK_HOST = "127.0.0.1"` e `NETWORK_PORT = 7777` configuráveis em `GamePanel`
- [x] `ConnectionListener` para notificar desconexão via `SwingUtilities.invokeLater`
- [x] Compilação limpa confirmada: `javac -d bin -encoding UTF-8 -sourcepath src src/com/rpggame/core/GamePanel.java`
- [ ] Testar conexão real: servidor rodando separado, cliente com `USE_NETWORK=true` *(teste manual)*

### Sub-fase 6.4 — Handshake e segundo cliente ✅ CONCLUÍDA
- [x] Handshake implementado em `ServerNetwork.ClientHandler.doHandshake()` e `ClientNetwork.doHandshake()`
- [x] Protocolo: cliente envia `{ "playerId": "...", "playerClass": "..." }`, servidor responde `{ "ok": true, "assignedPlayerId": "..." }`
- [x] Múltiplos clientes suportados: `ServerNetwork` aceita N conexões simultâneas, cada uma com `ClientHandler` + `InProcessTransport` dedicados
- [ ] Testar dois clientes simultâneos *(teste manual)*

### Sub-fase 6.5 — Remover modo in-process (opcional)
- [ ] Quando estiver pronto para produção: mudar `USE_NETWORK = true` em `GamePanel`
- [ ] Garantir que `ServerLoop` não tem mais referência a `GamePanel` (já está limpo)

---

## Precauções gerais

### Thread safety
- `ServerLoop` já usa `ConcurrentHashMap` para `playerSimulations` e `clientTransports` — não quebre isso
- Nunca acesse `WorldSnapshot` ou `InputPacket` com locks explícitos — eles são imutáveis por design
- Threads de rede não devem chamar métodos de Swing diretamente; use `SwingUtilities.invokeLater` se precisar atualizar UI a partir de uma thread de rede

### Serialização
- `WorldSnapshot` pode ter campos `null` (ex: `factionStatus` se mapa não tiver facção) — o desserializador precisa tolerar isso
- `InputPacket` tem `long clientFrame` — preserve-o na serialização para o servidor ignorar frames duplicados
- Não serialize `BufferedImage` — remova qualquer campo desse tipo dos DTOs antes de começar

### Erros de rede
- Toda leitura de socket deve estar em `try/catch(IOException)` com log e `cleanup()`
- Se a conexão cair durante o jogo, o cliente deve tentar reconectar (com retry exponencial simples) antes de mostrar tela de erro
- Se o servidor não estiver disponível no `connect()`, mostre mensagem ao usuário em vez de lançar exceção não tratada

### Latência e jitter
- O cliente já interpola entre snapshots — isso absorve variação de latência de até ~50ms sem artefatos visíveis
- Em LAN real, a latência será <1ms; em loopback será <0.1ms — ambos invisíveis para o jogador
- Não adicione `Thread.sleep()` artificial nas threads de rede

### Segurança (loopback)
- Esta fase cobre apenas `127.0.0.1` — não expor em `0.0.0.0` sem controle de acesso
- Não valide autenticação nesta fase — o handshake mínimo é suficiente para multiplayer local confiável

---

## Verificação final

A Fase 6 estará completa quando todos os itens abaixo forem verdadeiros:

- [ ] `GameServer` roda sem janela, sem Swing — apenas logs no terminal
- [ ] `GameClient` (via `GamePanel`) conecta em `127.0.0.1:7777` e o jogo funciona normalmente
- [ ] Fechar a janela do cliente não mata o processo do servidor
- [ ] Um segundo `GamePanel` pode conectar e ver o mesmo mundo simultaneamente
- [ ] `ServerLoop` não importa nenhuma classe de `java.awt` ou `javax.swing`
- [ ] Compilação limpa sem warnings nas novas classes

---

## O que NÃO muda nesta fase

- `WorldSnapshot` e `InputPacket` — apenas serialização é adicionada, nenhum campo novo
- `ServerLoop` internamente — mesmo tick loop, mesma lógica de simulação
- `GameClient` — continua consumindo `InProcessTransport` normalmente
- `ClientInput` — continua publicando em `InProcessTransport` normalmente
- `SnapshotRenderSystem` — zero alterações
- `PlayerSimulation` — zero alterações
- `WorldSnapshotAssembler` — zero alterações

A única coisa que muda é **como os bytes chegam de um lado para o outro**.
