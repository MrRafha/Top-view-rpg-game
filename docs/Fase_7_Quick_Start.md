# Fase 7 — Quick Start: Servidor de Amigos

## O que você quer fazer

Você e seus amigos em máquinas diferentes querendo jogar juntos. Alguém cria um servidor, outros conectam. Simples.

---

## Como vai funcionar

### Situação atual (Fases 1-6)

```
[Seu PC]
  GamePanel (cliente local)
      ↕
  ServerLoop (servidor local, mesma thread)
      ↕
  Mundo simulado
```

### Com Fase 7 

```
[Máquina do João]              [Sua Máquina]
┌─────────────────┐            ┌─────────────┐
│ GameLauncher    │            │ GameLauncher│
│  [CREATE]       │  UDP broadcast
│                 │ ←─────────→ [REFRESH]
└────────┬────────┘            └──────┬──────┘
         │                            │
    [CREATE] pressed             [CONNECT] pressed
         │                            │
    GameServer (headless)         TCP :7777
    ServerLoop                   ↑
    localhost:7777 ←─TCP────────┘
         │                     GamePanel
    Mundo            +
                  ClientInput
```

---

## Checklist de Implementação (Ordem sugerida)

### 1️⃣ Tipos de dados compartilhados

**Arquivo:** `src/com/rpggame/shared/ServerInfo.java`

```java
public class ServerInfo {
  private String id;              // UUID
  private String name;            // "João's RPG"
  private String hostAddress;     // "192.168.1.50:7777"
  private int maxPlayers;
  private int currentPlayers;
  private long createdAt;
  
  // Getters + toJson/fromJson
}
```

**Objetivo:** Você publica um servidor com essas informações. Clientes as recebem.

---

### 2️⃣ Descoberta de rede (UDP broadcast)

**Arquivo:** `src/com/rpggame/server/ServerBroadcaster.java`

```java
public class ServerBroadcaster {
  private static final int BROADCAST_PORT = 5555;
  
  public void broadcast(ServerInfo info) {
    // A cada 5 segundos, envia ServerInfo em UDP para 255.255.255.255:5555
  }
  
  public void stop() {
    // Para de enviar
  }
}
```

**Arquivo:** `src/com/rpggame/client/ServerDiscovery.java`

```java
public class ServerDiscovery {
  private static final int DISCOVERY_PORT = 5555;
  
  public List<ServerInfo> scan() {
    // Escuta em 5555 por 3 segundos, coleta ServerInfo de todos os broadcasts
    // Retorna lista de servidores descobertos
  }
}
```

**Objetivo:** João executa `broadcaster.broadcast(info)`. Você executa `scan()` e vê o servidor dele.

---

### 3️⃣ Conexão TCP do cliente

**Arquivo:** `src/com/rpggame/client/GameConnection.java`

```java
public class GameConnection {
  private Socket socket;
  private String playerId;
  
  public void connectToServer(String hostAddress, String playerId) throws IOException {
    // hostAddress = "192.168.1.50:7777"
    String[] parts = hostAddress.split(":");
    socket = new Socket(parts[0], Integer.parseInt(parts[1]));
    
    // Enviar handshake
    InputPacket handshake = new InputPacket(playerId, 0, true);
    sendInputPacket(handshake);
    
    // Thread de recepção
    new Thread(this::receiveSnapshots).start();
  }
  
  public void sendInputPacket(InputPacket packet) {
    // Envia seu input (teclas, mouse)
  }
  
  public WorldSnapshot receiveSnapshot() {
    // Retorna snapshot do servidor (ou null se não disponível)
  }
}
```

**Objetivo:** Quando você clica "CONNECT", `GameConnection` abre TCP ao servidor de João.

---

### 4️⃣ Tela de Lobby

**Arquivo:** `src/com/rpggame/ui/GameLauncher.java`

```java
public class GameLauncher extends JFrame {
  
  private void createServer() {
    // Botão "CREATE"
    String name = serverNameField.getText();
    int maxPlayers = (Integer) maxPlayersSpinner.getValue();
    
    ServerInfo info = new ServerInfo(UUID.randomUUID().toString(), 
                                      name, "localhost:7777", 
                                      maxPlayers, 1);
    
    // Inicia GameServer em thread separada
    new Thread(() -> {
      GameServer server = new GameServer(info);
      server.start();
    }).start();
    
    // Publica o servidor
    broadcaster.broadcast(info);
    
    // Entra como primeiro jogador
    gameConnection.connectToServer("localhost:7777", "player1");
    
    // Abre GamePanel
    openGamePanel(name);
  }
  
  private void refreshServers() {
    // Botão "REFRESH"
    List<ServerInfo> servers = discovery.scan();
    listModel.clear();
    for (ServerInfo s : servers) {
      listModel.addElement(s.getDisplayLabel());
    }
  }
  
  private void connectToServer() {
    // Botão "CONNECT"
    ServerInfo selected = getSelectedServer();
    gameConnection.connectToServer(selected.getHostAddress(), "player_" + UUID.randomUUID());
    openGamePanel(selected.getName());
  }
}
```

**Objetivo:** Novo entry point do jogo. `Game.main()` → `GameLauncher.main()`.

---

### 5️⃣ Servidor headless

**Arquivo:** `src/com/rpggame/server/GameServer.java`

```java
public class GameServer {
  
  private ServerInfo info;
  private ServerNetwork network;
  private ServerLoop loop;
  private WorldState worldState;
  
  public GameServer(ServerInfo info) throws IOException {
    this.info = info;
    this.worldState = new WorldState();
    this.network = new ServerNetwork(info.getHostAddress(), worldState);
    this.loop = new ServerLoop(worldState);
  }
  
  public void start() throws IOException {
    worldState.initializeDefaultMaps();
    network.start();
    loop.start();
    
    System.out.println("Server '" + info.getName() + 
                       "' listening on " + info.getHostAddress());
  }
  
  public void stop() {
    loop.stop();
    network.stop();
  }
  
  public static void main(String[] args) {
    // Para testes: rodar servidor direto
    ServerInfo info = new ServerInfo(...);
    GameServer server = new GameServer(info);
    server.start();
  }
}
```

**Objetivo:** Servidor corre sem Swing, sem GUI. Só simulação + rede.

---

### 6️⃣ Integração do GamePanel

**Mudança em:** `src/com/rpggame/core/GamePanel.java`

```java
public class GamePanel extends JPanel {
  
  private GameConnection gameConnection;
  private boolean isRemote = false;
  
  public void setGameConnection(GameConnection connection) {
    this.gameConnection = connection;
    this.isRemote = true;
  }
  
  @Override
  public void update() {
    if (isRemote && gameConnection != null) {
      // Receber snapshot do servidor remoto
      WorldSnapshot snap = gameConnection.receiveSnapshot();
      if (snap != null) {
        previousWorldSnapshot = latestWorldSnapshot;
        latestWorldSnapshot = snap;
        latestSnapshotNanos = System.nanoTime();
      }
      
      // Enviar input
      InputPacket packet = clientInput.buildPacket();
      gameConnection.sendInputPacket(packet);
    } else {
      // Comportamento local anterior (in-process)
      // ... código existente ...
    }
  }
}
```

**Objetivo:** `GamePanel` consegue se conectar a servidor remoto ou rodar local (development).

---

## Sequência prática de teste

### Teste 1: Descoberta local

```bash
# Terminal 1 — servidor
javac -d bin src/com/rpggame/server/GameServer.java
java -cp bin com.rpggame.server.GameServer

# Terminal 2 — cliente
javac -d bin src/com/rpggame/client/ServerDiscovery.java
java -cp bin com.rpggame.client.ServerDiscovery
# Resultado: vê servidor do terminal 1
```

### Teste 2: Conexão TCP

```bash
# Terminal 1 — servidor
java -cp bin com.rpggame.server.GameServer &

# Terminal 2 — cliente conecta
java -cp bin com.rpggame.client.GameConnection localhost 7777
# Resultado: cliente recebe snapshots
```

### Teste 3: Lobby funcional

```bash
# Terminal 1 — mude entry point para GameLauncher.main()
javac -d bin src/com/rpggame/ui/GameLauncher.java
java -cp bin com.rpggame.ui.GameLauncher

# Janela abre:
# - Clique "CREATE SERVER"
# - Servidor inicia em background
# - Você entra como player 1
# - Jogo abre

# Terminal 2 — outro cliente
java -cp bin com.rpggame.ui.GameLauncher

# Janela abre:
# - Clique "REFRESH"
# - Vê seu servidor
# - Clique "CONNECT"
# - Amigo Player 1 vê um segundo personagem entrando no mapa
```

---

## Apontações técnicas

### 1. UDP broadcast pode não funcionar em redes complexas

Se estiver atrás de firewall corporativo ou roteador restritivo, broadcast UDP não chega.

**Alternativa simples:** Digitar IP manualmente (v1).

```java
// Em vez de broadcast, ter campo de texto
hostAddressField.setText("192.168.1.50:7777");
gameConnection.connectToServer(hostAddressField.getText(), playerId);
```

### 2. Threading

Todos os I/O deve estar em threads separadas para não travar UI.

```java
// ✗ ERRADO
ServerInfo[] servers = discovery.scan();  // Congela UI

// ✓ CERTO
new Thread(() -> {
  ServerInfo[] servers = discovery.scan();
  EventQueue.invokeLater(() -> updateUI(servers));
}).start();
```

### 3. Persistência do estado

Quando novo cliente conecta, ele recebe snapshot **atual** do servidor.

- Cliente 1 mata 5 goblins
- Cliente 2 conecta 1 minuto depois
- Cliente 2 vê 5 goblins **mortos** (estado final)

Isso é correto! O servidor é fonte de verdade.

### 4. Limite de jogadores

Simples: server não aceita mais que `maxPlayers` conexões.

```java
if (playerManager.getPlayerCount() >= serverInfo.getMaxPlayers()) {
  reject("Server full");
}
```

---

## Armadilhas comuns

| Erro | Sintoma | Solução |
|---|---|---|
| Firewall bloqueando porta 7777 | "Connection refused" | Abrir porta ou usar localhost |
| Broadcast UDP multicast bloqueado | Discover retorna lista vazia | Digitar IP manualmente |
| Thread de recepção morrer silenciosamente | Snapshot para chegando | Adicionar try-catch com log em ServerDiscovery |
| Serialização JSON quebrada | Snapshot chegando vazio | Validar JSON em WorldSnapshot.toJson/fromJson |
| GamePanel não reconhecendo GameConnection | Jogo não se mexe | Verificar `setGameConnection()` foi chamado |
| Servidor criado mas não publicado | List sempre vazia | Verificar `broadcaster.broadcast()` está ativo |

---

## Próximos passos (após Fase 7 funcionar)

1. **Fase 8:** Status de servidor (ping, players online, kick)
2. **Chat in-game:** Mensagens entre players
3. **Persistência:** Savegame do servidor (estado salvo em disco)
4. **Matchmaking:** Servidor central para descobrir games internacionais
5. **Voice:** Discord integration ou sistema de voice próprio

---

## Recursos úteis

- **UDP Broadcast:** https://docs.oracle.com/javase/tutorial/networking/datagrams/broadcasting.html
- **TCP Sockets:** https://docs.oracle.com/javase/tutorial/networking/sockets/
- **Swing Threading:** https://docs.oracle.com/javase/tutorial/uiswing/concurrency/
- **JSON manual (sem deps):** Usar `String.format()` + `split()` para casos simples

---

## Summary

Fase 7 = Lobby → Descoberta → Conexão → Jogo Multiplayer

Código mínimo para testar:
1. `ServerInfo.java` (DTO)
2. `ServerBroadcaster.java` + `ServerDiscovery.java` (UDP)
3. `GameConnection.java` (TCP)
4. `GameLauncher.java` (UI)
5. Estender `GameServer.java` (suportar múltiplos clientes)
6. Integrar `GamePanel` (usar conexão remota)
7. Testar com dois clientes

Essa é a Fase 7. Você consegue! 🚀
