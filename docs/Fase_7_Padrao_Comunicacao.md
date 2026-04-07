# Fase 7 — Padrões de Comunicação Servidor-Cliente

## Overview

A comunicação entre `GameServer` e múltiplos `GameClient` precisa ser:

1. **Confiável** — snapshots e input não podem ser perdidos
2. **Síncrona** — servidor avança sempre ao mesmo tempo
3. **Eficiente** — não enviar dados desnecessários
4. **Resiliente** — desconexão de um cliente não afeta outros

---

## Fluxo Básico por Frame

```
┌─ TICK 1 (20ms no servidor) ──────────────────────┐
│                                                   │
│  [Server]                      [Client 1]  [Client 2]
│  ┌──────────────────┐          ┌──────┐    ┌──────┐
│  │ Collect Input    │◄─────────┤Input │    │Input │
│  │  from clients    │          └──────┘    └──────┘
│  └────────┬─────────┘             │           │
│           │                        │           │
│  ┌────────▼─────────┐            │           │
│  │ Update Simulation │            │           │
│  │ (EnemyManager,   │            │           │
│  │  Physics, etc)   │            │           │
│  └────────┬─────────┘            │           │
│           │                       │           │
│  ┌────────▼──────────┐           │           │
│  │ Build Snapshot    │           │           │
│  │ (World state)     │           │           │
│  └────────┬──────────┘           │           │
│           │                      │           │
│           └─────────────────────►├──────────┤
│               WorldSnapshot      │ Render @ │
│                                  │ 60 FPS   │
│                                  └──────────┘
│
└──────────────────────────────────────────────────┘
```

---

## Protocolo de Comunicação (TCP)

### 1. Conexão inicial (Handshake)

**Cliente → Servidor**

```json
{
  "type": "HANDSHAKE",
  "playerId": "player_abc123",
  "gameVersion": "1.0.0",
  "characterClass": "WARRIOR"
}
```

**Servidor responde com**

```json
{
  "type": "HANDSHAKE_ACK",
  "success": true,
  "serverId": "server_xyz789",
  "mapId": "village",
  "spawnX": 100,
  "spawnY": 150,
  "otherPlayers": [
    {
      "id": "player_def456",
      "x": 200,
      "y": 200,
      "class": "MAGE"
    }
  ]
}
```

### 2. Ciclo normal: Input → Snapshot

**Cliente envia a cada frame de input (~60 FPS, ou quando input muda)**

```json
{
  "type": "INPUT",
  "tick": 42,
  "playerId": "player_abc123",
  "up": true,
  "down": false,
  "left": false,
  "right": false,
  "attack": false,
  "skill1": false,
  "mouseX": 640,
  "mouseY": 480
}
```

**Servidor acumula todos os inputs, executa simulação**

```
Tick 42:
  - Receber INPUT de player_abc123
  - Receber INPUT de player_def456
  - Update EnemyManager com ambos os inputs
  - Calcular nova posição, colisões, danos
  
Tick 43:
  - Montar WorldSnapshot com estado atual
  - Enviar para TODOS os clientes
```

**Servidor envia a cada tick de simulação (20 TPS = 50 ms)**

```json
{
  "type": "SNAPSHOT",
  "tick": 42,
  "mapId": "village",
  "timestamp": 1234567890000,
  
  "players": [
    {
      "id": "player_abc123",
      "x": 105,
      "y": 150,
      "hp": 100,
      "maxHp": 100,
      "state": "MOVING",
      "facingLeft": false,
      "class": "WARRIOR"
    },
    {
      "id": "player_def456",
      "x": 210,
      "y": 200,
      "hp": 85,
      "maxHp": 100,
      "state": "IDLE",
      "facingLeft": true,
      "class": "MAGE"
    }
  ],
  
  "enemies": [
    {
      "id": "goblin_1",
      "type": "GOBLIN",
      "x": 300,
      "y": 250,
      "hp": 15,
      "maxHp": 20,
      "aiState": "CHASING",
      "spritePath": "sprites/goblin.png"
    }
  ],
  
  "projectiles": [
    {
      "id": "proj_1",
      "x": 320,
      "y": 280,
      "direction": 0.7854  // radians
    }
  ],
  
  "factionStatus": {
    "goblinReputation": -30,
    "humanReputation": 15,
    "mapOwner": "GOBLINS",
    "mapContested": false
  }
}
```

### 3. Desconexão / Reconexão

**Cliente desconecta (voluntariamente)**

```json
{
  "type": "DISCONNECT",
  "playerId": "player_abc123",
  "reason": "USER_QUIT"
}
```

**Cliente desconecta (involuntariamente, timeout)**

Servidor detecta socket fechado ou timeout (ex: 10s sem comunicação).

```
Servidor remove player automaticamente:
  playerManager.removePlayer("player_abc123")
  
WorldSnapshot próximo já não inclui esse player.
```

**Cliente tenta reconectar**

Repetir handshake com **mesmo playerId**. Servidor pode rejeitar ou aceitar conforme política:

```java
// Opção 1: Rejeitar (precisa criar novo player)
if (playerManager.playerExists(playerId)) {
  reject("Player already connected");
}

// Opção 2: Aceitar (resume sessão)
Player existing = playerManager.getPlayer(playerId);
if (existing.isConnected()) {
  reject("Player already connected");
} else {
  existing.reconnect();  // Resume
}
```

### 4. Eventos discretos (Optional, para notificações)

Além do snapshot contínuo, pode haver mensagens únicas:

**Servidor → Cliente (asynchronous)**

```json
{
  "type": "EVENT",
  "eventType": "PLAYER_KILLED",
  "playerId": "player_abc123",
  "killedBy": "goblin_42"
}
```

```json
{
  "type": "EVENT",
  "eventType": "CHAT_MESSAGE",
  "fromPlayer": "player_def456",
  "message": "Hey, need help?"
}
```

---

## Serialização (JSON manual, sem deps)

### ServerInfo.toJson()

```java
public static String toJson(ServerInfo info) {
  StringBuilder sb = new StringBuilder();
  sb.append("{");
  sb.append("\"id\":\"").append(escapeJson(info.id)).append("\",");
  sb.append("\"name\":\"").append(escapeJson(info.name)).append("\",");
  sb.append("\"hostAddress\":\"").append(escapeJson(info.hostAddress)).append("\",");
  sb.append("\"maxPlayers\":").append(info.maxPlayers).append(",");
  sb.append("\"currentPlayers\":").append(info.currentPlayers);
  sb.append("}");
  return sb.toString();
}

private static String escapeJson(String s) {
  return s.replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "\\r");
}
```

### WorldSnapshot.toJson()

Maior, mas mesmo padrão:

```java
public static String toJson(WorldSnapshot snap) {
  StringBuilder sb = new StringBuilder();
  sb.append("{");
  sb.append("\"type\":\"SNAPSHOT\",");
  sb.append("\"tick\":").append(snap.tick).append(",");
  sb.append("\"mapId\":\"").append(snap.mapId).append("\",");
  
  // Players
  sb.append("\"players\":[");
  boolean first = true;
  for (SnapshotPlayer p : snap.players) {
    if (!first) sb.append(",");
    sb.append(playerToJson(p));
    first = false;
  }
  sb.append("],");
  
  // Enemies
  sb.append("\"enemies\":[");
  first = true;
  for (SnapshotEnemy e : snap.enemies) {
    if (!first) sb.append(",");
    sb.append(enemyToJson(e));
    first = false;
  }
  sb.append("]");
  
  sb.append("}");
  return sb.toString();
}

private static String playerToJson(SnapshotPlayer p) {
  return String.format(
    "{\"id\":\"%s\",\"x\":%f,\"y\":%f,\"hp\":%d,\"maxHp\":%d," +
    "\"state\":\"%s\",\"facingLeft\":%s}",
    escapeJson(p.id), p.x, p.y, p.hp, p.maxHp,
    p.state, p.facingLeft ? "true" : "false"
  );
}
```

---

## Gerenciamento de Conexões

### ServerNetwork — Aceitar múltiplas conexões

```java
public class ServerNetwork {
  
  private ServerSocket serverSocket;
  private Map<String, ClientHandler> activeConnections = new ConcurrentHashMap<>();
  
  public void start(int port) throws IOException {
    serverSocket = new ServerSocket(port);
    
    new Thread(() -> {
      while (true) {
        try {
          Socket clientSocket = serverSocket.accept();
          ClientHandler handler = new ClientHandler(clientSocket, this);
          new Thread(handler).start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
  
  public void registerConnection(String playerId, ClientHandler handler) {
    activeConnections.put(playerId, handler);
  }
  
  public void unregisterConnection(String playerId) {
    activeConnections.remove(playerId);
  }
  
  // Broadcast snapshot para TODOS os clientes
  public void broadcastSnapshot(WorldSnapshot snap) {
    String json = WorldSnapshot.toJson(snap);
    
    for (ClientHandler handler : activeConnections.values()) {
      handler.sendMessage(json);  // Non-blocking queue
    }
  }
  
  public void stop() throws IOException {
    serverSocket.close();
  }
}
```

### ClientHandler — Lidar com um cliente

```java
public class ClientHandler implements Runnable {
  
  private Socket socket;
  private InputStream in;
  private OutputStream out;
  private String playerId;
  private ServerNetwork network;
  private BlockingQueue<String> outQueue = new LinkedBlockingQueue<>(16);
  
  public ClientHandler(Socket socket, ServerNetwork network) {
    this.socket = socket;
    this.network = network;
  }
  
  @Override
  public void run() {
    try {
      in = socket.getInputStream();
      out = socket.getOutputStream();
      
      // Thread de envio (non-blocking)
      new Thread(this::sendLoop).start();
      
      // Thread de recepção (blocking)
      receiveLoop();
      
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      cleanup();
    }
  }
  
  private void receiveLoop() throws IOException {
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(in));
    
    String line;
    while ((line = reader.readLine()) != null) {
      handleMessage(line);
    }
  }
  
  private void handleMessage(String json) {
    // Parse json manualmente ou com Gson
    if (json.contains("\"type\":\"HANDSHAKE\"")) {
      handleHandshake(json);
    } else if (json.contains("\"type\":\"INPUT\"")) {
      handleInput(json);
    } else if (json.contains("\"type\":\"DISCONNECT\"")) {
      handleDisconnect(json);
    }
  }
  
  private void handleHandshake(String json) {
    // Extrair playerId, playerClass, etc
    String extractedId = extractJsonString(json, "playerId");
    this.playerId = extractedId;
    
    // Registrar conexão
    network.registerConnection(playerId, this);
    
    // Enviar ACK com estado inicial
    String ack = buildHandshakeAck(playerId);
    sendMessage(ack);
  }
  
  private void handleInput(String json) {
    // Extrair input do json
    // Passar para ServerLoop processar
  }
  
  private void handleDisconnect(String json) {
    // Desregistrar
    network.unregisterConnection(playerId);
    
    // Encerrar socket
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private void sendLoop() {
    PrintWriter writer = new PrintWriter(out, true);
    
    while (true) {
      try {
        String message = outQueue.take();
        writer.println(message);
      } catch (InterruptedException e) {
        break;
      }
    }
  }
  
  public void sendMessage(String json) {
    // Non-blocking: adiciona à fila
    try {
      outQueue.offer(json, 100, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // Fila cheia ou thread interrompida
    }
  }
  
  private void cleanup() {
    network.unregisterConnection(playerId);
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static String extractJsonString(String json, String key) {
    // "playerId":"abc123"
    int idx = json.indexOf("\"" + key + "\":\"");
    if (idx < 0) return "";
    
    idx += key.length() + 4;  // Pular "key":"
    int endIdx = json.indexOf("\"", idx);
    return json.substring(idx, endIdx);
  }
}
```

---

## Fluxo Completo (Exemplo)

### Cliente 1 conecta

```
1. GUI: User clicks [CREATE SERVER]
2. GameLauncher inicia GameServer headless
3. ServerNetwork inicia escutando em :7777
4. GameLauncher inicia GameClient
5. GameClient conecta TCP a localhost:7777
6. Envia: {"type":"HANDSHAKE","playerId":"p1","class":"WARRIOR"}
7. Servidor responde: {"type":"HANDSHAKE_ACK","success":true,"mapId":"village",...}
8. Client recebe, cria Player em tela
9. Client renderiza GamePanel
```

### Cliente 2 conecta

```
10. Outro PC: GUI: User clicks [REFRESH]
11. ServerDiscovery faz UDP scan
12. Encontra broadcast do servidor
13. User clicks [CONNECT]
14. GameClient2 conecta TCP a 192.168.1.50:7777
15. Envia: {"type":"HANDSHAKE","playerId":"p2","class":"MAGE"}
16. Servidor responde com snapshot + Player 1 data
17. Client 2 renderiza, vê Player 1 na tela
```

### Ambos no mesmo mapa

```
18. Tick 50: 
    - Servidor recebe INPUT de p1: up=true
    - Servidor recebe INPUT de p2: right=true
    - Atualiza EnemyManager, posições
    - Monta WorldSnapshot com p1 em (105,100), p2 em (210,210)
    - Envia snapshot para p1 E p2
    
19. Cada client renderiza seu próprio snapshot com interpolação
20. Ambos veem o outro se movendo em tempo real
```

### Client 1 desconecta

```
21. User closes game
22. Client 1 sends: {"type":"DISCONNECT"}
23. Handler em ServerNetwork fecha conexão
24. Tick seguinte: WorldSnapshot não inclui p1
25. Client 2 vê player 1 sumir da tela
26. Servidor continua rodando (p2 ainda online)
```

---

## Testes unitários

### Teste: Broadcast para múltiplos clientes

```java
@Test
public void testBroadcastSnapshot() {
  ServerNetwork network = new ServerNetwork();
  
  ClientHandler client1 = createMockClient();
  ClientHandler client2 = createMockClient();
  
  network.registerConnection("p1", client1);
  network.registerConnection("p2", client2);
  
  WorldSnapshot snap = createTestSnapshot();
  network.broadcastSnapshot(snap);
  
  // Verificar que ambos receberam
  assertTrue(client1.receivedMessage());
  assertTrue(client2.receivedMessage());
}
```

### Teste: Desconexão

```java
@Test
public void testClientDisconnect() {
  ServerNetwork network = new ServerNetwork();
  ClientHandler client = createMockClient();
  
  network.registerConnection("p1", client);
  network.broadcastSnapshot(snap1);
  
  client.disconnect();
  network.unregisterConnection("p1");
  
  // Broadcast não deve falhar
  network.broadcastSnapshot(snap2);
  
  assertEquals(0, network.getActiveConnections().size());
}
```

---

## Performance

### Tamanho de Snapshot

Exemplo com 4 players + 10 inimigos:

```
{
  "type": "SNAPSHOT",           // 26 bytes
  "tick": 12345,                // 16 bytes
  ... (4 * 150) players         // ~600 bytes
  ... (10 * 120) enemies        // ~1200 bytes
  ... projectiles, chests, etc  // ~200 bytes
}
= ~2.5 KB por snapshot
```

Taxa: 20 TPS × 2.5 KB = **50 KB/s por cliente**

Para 4 clientes em um servidor: **200 KB/s** (largura de banda local)

### Recomendações

1. **Compressão:** JSON comprimido com gzip (~5x redução)
2. **Delta snapshots:** Enviar apenas mudanças
3. **LOD (Level of Detail):** Inimigos distantes com menos detalhe
4. **Culling:** Não enviar inimigos fora da visão

---

## Resumo

- **Handshake:** Inicial, identifica player e fornece spawn
- **Input:** Contínuo, cliente envia teclas
- **Snapshot:** Contínuo, servidor envia mundo
- **Events:** Occasional, notificações únicas
- **Serialização:** JSON manual (sem deps)
- **Threading:** Não-bloqueante, filas para envio

Esse é o coração da Fase 7!
