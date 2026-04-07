# Fase 7 — Server Lobby e Descoberta Multiplayer

## Objetivo

Criar uma tela de lobby onde jogadores podem:
1. **Criar servidor** — iniciar uma instância de jogo aberta para amigos se conectarem
2. **Listar servidores ativos** — descobrir e entrar em servidores hospedados
3. **Gerenciar conexões** — desconectar, kickar, chatear

Isso transforma o jogo de "tudo local" para "localmente hospedado para múltiplos clientes".

---

## Arquitetura de alto nível

```
┌─────────────────────────────────────────────────────────┐
│              GameServer (processo separado)             │
│                                                         │
│  ServerLoop @ 20 TPS                                    │
│  ServerNetwork (escuta em 0.0.0.0:7777)                │
│  ServerRegistry (publica via mDNS/broadcast)           │
│  PlayerManager (múltiplos players conectados)          │
│  SimulationThread (actualiza mundo enquanto espera)    │
└──────────────────┬──────────────────────────────────────┘
                   │  TCP (0.0.0.0:7777)
                   │
        ┌──────────┼──────────┐
        │          │          │
   Client 1    Client 2    Client 3
     :7778       :7779       :7780

┌─────────────────────────────────────────────────────────┐
│            GameLauncher (tela de lobby)                 │
│                                                         │
│ ┌───────────────────────────────────────────────────┐  │
│ │ CREATE SERVER                                     │  │
│ │ [Name: _________]  [Max Players: 4]  [CREATE]   │  │
│ ├───────────────────────────────────────────────────┤  │
│ │ AVAILABLE SERVERS                                 │  │
│ │ ┌─────────────────────────────────────────────┐   │  │
│ │ │ John's Game     (localhost:7777)  [2/4]    │   │  │
│ │ │ Sarah's RPG     (192.168.1.50:7777) [1/4]  │   │  │
│ │ │ Local Test      (127.0.0.1:7778)   [3/4]   │   │  │
│ │ └─────────────────────────────────────────────┘   │  │
│ │ [CONNECT] [DELETE] [REFRESH]                     │  │
│ └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Novos componentes necessários

### 1. `ServerRegistry` — descoberta de servidores

Responsabilidade: manter e descobrir servidores ativos.

```java
// src/com/rpggame/server/ServerRegistry.java

public class ServerRegistry {
  
  // Armazena informações dos servidores conhecidos
  private final Map<String, ServerInfo> knownServers = new ConcurrentHashMap<>();
  
  // Broadcast local (mDNS ou UDP broadcast)
  private ServerBroadcaster broadcaster;
  private ServerDiscovery discoverer;
  
  public ServerRegistry() {
    this.broadcaster = new ServerBroadcaster();
    this.discoverer = new ServerDiscovery();
  }
  
  // Quando um servidor inicia
  public void publishServer(ServerInfo info) {
    knownServers.put(info.getId(), info);
    broadcaster.broadcast(info);  // mDNS ou UDP
  }
  
  // Quando um servidor encerra
  public void unpublishServer(String serverId) {
    knownServers.remove(serverId);
    broadcaster.stopBroadcast(serverId);
  }
  
  // Cliente descobre servidores
  public List<ServerInfo> discoverServers() {
    List<ServerInfo> discovered = discoverer.scan();
    discovered.forEach(s -> knownServers.putIfAbsent(s.getId(), s));
    return new ArrayList<>(knownServers.values());
  }
  
  // Atualizar dados de um servidor (players online, etc)
  public void updateServerInfo(String serverId, ServerInfo updated) {
    knownServers.put(serverId, updated);
  }
  
  public ServerInfo getServer(String serverId) {
    return knownServers.get(serverId);
  }
}
```

### 2. `ServerInfo` — metadados do servidor

```java
// src/com/rpggame/shared/ServerInfo.java

public class ServerInfo {
  private final String id;                    // UUID único
  private final String name;                  // "John's Game"
  private final String hostAddress;           // "192.168.1.50:7777"
  private final int maxPlayers;
  private final int currentPlayers;
  private final long createdAt;
  private final String gameVersion;
  
  public ServerInfo(String id, String name, String hostAddress, 
                    int maxPlayers, int currentPlayers) {
    this.id = id;
    this.name = name;
    this.hostAddress = hostAddress;
    this.maxPlayers = maxPlayers;
    this.currentPlayers = currentPlayers;
    this.createdAt = System.currentTimeMillis();
    this.gameVersion = "1.0.0";
  }
  
  // Getters + JSON serialization (Jackson/Gson)
  public String getId() { return id; }
  public String getName() { return name; }
  public String getHostAddress() { return hostAddress; }
  public int getMaxPlayers() { return maxPlayers; }
  public int getCurrentPlayers() { return currentPlayers; }
  public boolean isFull() { return currentPlayers >= maxPlayers; }
  
  // Para exibição no lobby
  public String getDisplayLabel() {
    return String.format("%s (%s) [%d/%d]", name, hostAddress, currentPlayers, maxPlayers);
  }
  
  // Serializar para JSON
  public static String toJson(ServerInfo info) {
    // Implementar com Jackson ou Gson
    return "{\"id\":\"" + info.id + "\",\"name\":\"" + info.name + "\"...}";
  }
  
  public static ServerInfo fromJson(String json) {
    // Parse JSON
    return new ServerInfo(/*...*/);
  }
}
```

### 3. `ServerBroadcaster` — publicar servidor localmente

```java
// src/com/rpggame/server/ServerBroadcaster.java

public class ServerBroadcaster {
  
  private static final int BROADCAST_PORT = 5555;
  private static final String BROADCAST_ADDR = "255.255.255.255";
  
  private DatagramSocket socket;
  private volatile boolean broadcasting = false;
  private Thread broadcastThread;
  
  public ServerBroadcaster() throws IOException {
    this.socket = new DatagramSocket();
    this.socket.setBroadcast(true);
  }
  
  public void broadcast(ServerInfo info) {
    if (broadcasting) return;
    broadcasting = true;
    
    broadcastThread = new Thread(() -> {
      String message = ServerInfo.toJson(info);
      byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
      
      try {
        InetAddress addr = InetAddress.getByName(BROADCAST_ADDR);
        while (broadcasting) {
          DatagramPacket packet = new DatagramPacket(
              buffer, buffer.length, addr, BROADCAST_PORT);
          socket.send(packet);
          Thread.sleep(5000);  // Re-broadcast a cada 5s
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });
    
    broadcastThread.setDaemon(true);
    broadcastThread.start();
  }
  
  public void stopBroadcast(String serverId) {
    broadcasting = false;
    if (broadcastThread != null) {
      broadcastThread.interrupt();
    }
  }
  
  public void close() {
    if (socket != null) {
      socket.close();
    }
  }
}
```

### 4. `ServerDiscovery` — descobrir servidores

```java
// src/com/rpggame/client/ServerDiscovery.java

public class ServerDiscovery {
  
  private static final int DISCOVERY_PORT = 5555;
  private static final int DISCOVERY_TIMEOUT_MS = 3000;
  
  public List<ServerInfo> scan() {
    List<ServerInfo> servers = new ArrayList<>();
    
    DatagramSocket socket = null;
    try {
      socket = new DatagramSocket(DISCOVERY_PORT);
      socket.setBroadcast(true);
      socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);
      
      byte[] buffer = new byte[4096];
      
      long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT_MS) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(packet);
          String message = new String(
              packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
          ServerInfo info = ServerInfo.fromJson(message);
          servers.add(info);
        } catch (SocketTimeoutException e) {
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (socket != null) {
        socket.close();
      }
    }
    
    return servers;
  }
}
```

### 5. `GameLauncher` — tela de lobby em Swing

```java
// src/com/rpggame/ui/GameLauncher.java

public class GameLauncher extends JFrame {
  
  private JTextField serverNameField;
  private JSpinner maxPlayersSpinner;
  private JButton createButton;
  
  private JList<String> serverList;
  private DefaultListModel<String> listModel;
  private JButton connectButton;
  private JButton refreshButton;
  
  private ServerRegistry registry;
  private GameConnection gameConnection;
  
  public GameLauncher() {
    setTitle("RPG Game — Server Lobby");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(600, 500);
    setLocationRelativeTo(null);
    
    this.registry = new ServerRegistry();
    this.gameConnection = new GameConnection();
    
    initializeUI();
  }
  
  private void initializeUI() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    
    // ========== SEÇÃO: CREATE SERVER ==========
    JPanel createPanel = new JPanel(new GridLayout(3, 2, 10, 10));
    createPanel.setBorder(new TitledBorder("Create Server"));
    
    createPanel.add(new JLabel("Server Name:"));
    serverNameField = new JTextField(15);
    serverNameField.setText("My Game");
    createPanel.add(serverNameField);
    
    createPanel.add(new JLabel("Max Players:"));
    maxPlayersSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
    createPanel.add(maxPlayersSpinner);
    
    createButton = new JButton("CREATE SERVER");
    createButton.addActionListener(e -> createServer());
    createPanel.add(createButton);
    
    mainPanel.add(createPanel, BorderLayout.NORTH);
    
    // ========== SEÇÃO: SERVER LIST ==========
    JPanel listPanel = new JPanel(new BorderLayout(10, 10));
    listPanel.setBorder(new TitledBorder("Available Servers"));
    
    listModel = new DefaultListModel<>();
    serverList = new JList<>(listModel);
    serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(serverList);
    listPanel.add(scrollPane, BorderLayout.CENTER);
    
    JPanel buttonPanel = new JPanel(new FlowLayout());
    refreshButton = new JButton("REFRESH");
    refreshButton.addActionListener(e -> refreshServerList());
    buttonPanel.add(refreshButton);
    
    connectButton = new JButton("CONNECT");
    connectButton.addActionListener(e -> connectToServer());
    connectButton.setEnabled(false);
    serverList.addListSelectionListener(ev -> {
      connectButton.setEnabled(serverList.getSelectedIndex() >= 0);
    });
    buttonPanel.add(connectButton);
    
    listPanel.add(buttonPanel, BorderLayout.SOUTH);
    mainPanel.add(listPanel, BorderLayout.CENTER);
    
    add(mainPanel);
  }
  
  private void createServer() {
    String serverName = serverNameField.getText().trim();
    int maxPlayers = (Integer) maxPlayersSpinner.getValue();
    
    if (serverName.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Server name cannot be empty");
      return;
    }
    
    // Criar servidor em thread separada
    new Thread(() -> {
      try {
        String serverId = UUID.randomUUID().toString();
        String hostAddress = "localhost:7777";  // Simplificado; depois usar IP público
        
        ServerInfo info = new ServerInfo(
            serverId, serverName, hostAddress, maxPlayers, 1);
        
        // Iniciar GameServer
        GameServer gameServer = new GameServer(info);
        gameServer.start();
        
        // Publicar no registry
        registry.publishServer(info);
        
        // Conectar como primeiro player
        gameConnection.connectToServer(hostAddress, "player1");
        
        // Abrir tela de jogo
        EventQueue.invokeLater(() -> {
          GamePanel gamePanel = new GamePanel();
          gamePanel.setGameConnection(gameConnection);
          
          JFrame gameFrame = new JFrame("RPG Game — " + serverName);
          gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          gameFrame.add(gamePanel);
          gameFrame.setSize(1280, 800);
          gameFrame.setLocationRelativeTo(null);
          gameFrame.setVisible(true);
          
          GameLauncher.this.dispose();  // Fechar launcher
        });
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error creating server: " + e.getMessage());
        e.printStackTrace();
      }
    }).start();
  }
  
  private void refreshServerList() {
    new Thread(() -> {
      List<ServerInfo> servers = registry.discoverServers();
      
      EventQueue.invokeLater(() -> {
        listModel.clear();
        for (ServerInfo server : servers) {
          listModel.addElement(server.getDisplayLabel());
        }
      });
    }).start();
  }
  
  private void connectToServer() {
    int selectedIndex = serverList.getSelectedIndex();
    if (selectedIndex < 0) return;
    
    List<ServerInfo> servers = new ArrayList<>(
        registry.knownServers.values());
    if (selectedIndex >= servers.size()) return;
    
    ServerInfo selected = servers.get(selectedIndex);
    
    if (selected.isFull()) {
      JOptionPane.showMessageDialog(this, "Server is full!");
      return;
    }
    
    new Thread(() -> {
      try {
        // Conectar ao servidor remoto
        gameConnection.connectToServer(
            selected.getHostAddress(), 
            "player_" + UUID.randomUUID().toString());
        
        // Abrir tela de jogo
        EventQueue.invokeLater(() -> {
          GamePanel gamePanel = new GamePanel();
          gamePanel.setGameConnection(gameConnection);
          
          JFrame gameFrame = new JFrame("RPG Game — " + selected.getName());
          gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          gameFrame.add(gamePanel);
          gameFrame.setSize(1280, 800);
          gameFrame.setLocationRelativeTo(null);
          gameFrame.setVisible(true);
          
          GameLauncher.this.dispose();
        });
      } catch (IOException e) {
        JOptionPane.showMessageDialog(
            GameLauncher.this, 
            "Error connecting to server: " + e.getMessage());
        e.printStackTrace();
      }
    }).start();
  }
  
  public static void main(String[] args) {
    EventQueue.invokeLater(() -> {
      GameLauncher launcher = new GameLauncher();
      launcher.setVisible(true);
    });
  }
}
```

### 6. `GameConnection` — gerencia conexão TCP

```java
// src/com/rpggame/client/GameConnection.java

public class GameConnection {
  
  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  
  private String playerId;
  private volatile boolean connected = false;
  
  public void connectToServer(String hostAddress, String playerId) throws IOException {
    String[] parts = hostAddress.split(":");
    String host = parts[0];
    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 7777;
    
    this.socket = new Socket(host, port);
    this.out = new ObjectOutputStream(socket.getOutputStream());
    this.in = new ObjectInputStream(socket.getInputStream());
    this.playerId = playerId;
    this.connected = true;
    
    // Enviar handshake
    InputPacket handshake = new InputPacket(playerId, 0, true);
    sendInputPacket(handshake);
    
    // Thread de recepção de snapshots
    new Thread(this::receiveSnapshots).start();
  }
  
  public void sendInputPacket(InputPacket packet) {
    try {
      synchronized (out) {
        out.writeObject(packet);
        out.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
      connected = false;
    }
  }
  
  public WorldSnapshot receiveSnapshot() {
    // Non-blocking, retorna null se não houver snapshot
    // implementar com BlockingQueue
    return null;
  }
  
  private void receiveSnapshots() {
    while (connected) {
      try {
        Object obj = in.readObject();
        if (obj instanceof WorldSnapshot) {
          // Processar snapshot
        }
      } catch (IOException | ClassNotFoundException e) {
        connected = false;
        break;
      }
    }
  }
  
  public void disconnect() {
    connected = false;
    try {
      if (socket != null) socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
```

### 7. `GameServer` — instância de servidor headless

```java
// src/com/rpggame/server/GameServer.java

public class GameServer {
  
  private final ServerInfo serverInfo;
  private final ServerNetwork serverNetwork;
  private final ServerLoop serverLoop;
  private final WorldState worldState;
  
  private volatile boolean running = false;
  
  public GameServer(ServerInfo serverInfo) throws IOException {
    this.serverInfo = serverInfo;
    this.worldState = new WorldState();
    this.serverNetwork = new ServerNetwork(serverInfo, worldState);
    this.serverLoop = new ServerLoop(worldState);
  }
  
  public void start() throws IOException {
    running = true;
    
    // Inicializar mundo
    worldState.initializeDefaultMaps();
    
    // Iniciar rede
    serverNetwork.start();
    
    // Iniciar loop de simulação
    serverLoop.start();
    
    System.out.println("Server '" + serverInfo.getName() + 
                       "' started on " + serverInfo.getHostAddress());
  }
  
  public void stop() {
    running = false;
    serverLoop.stop();
    serverNetwork.stop();
    System.out.println("Server stopped.");
  }
  
  public static void main(String[] args) throws IOException {
    // Para testes: rodar servidor em thread separada
    ServerInfo info = new ServerInfo(
        UUID.randomUUID().toString(),
        "Test Server",
        "localhost:7777",
        4, 1);
    
    GameServer server = new GameServer(info);
    server.start();
    
    // Fake stdin para encerrar
    System.out.println("Type 'quit' to stop...");
    Scanner scanner = new Scanner(System.in);
    while (scanner.hasNextLine()) {
      if (scanner.nextLine().equals("quit")) {
        server.stop();
        break;
      }
    }
  }
}
```

---

## Integração com GamePanel

Modificar `GamePanel` para suportar conexão remota:

```java
// Em src/com/rpggame/core/GamePanel.java

public class GamePanel extends JPanel {
  
  // Novo: suporte a conexão remota
  private GameConnection gameConnection;
  private boolean useRemoteServer = false;
  
  public void setGameConnection(GameConnection connection) {
    this.gameConnection = connection;
    this.useRemoteServer = true;
  }
  
  @Override
  public void update() {
    if (useRemoteServer && gameConnection != null) {
      // Receber snapshot do servidor remoto
      WorldSnapshot newSnapshot = gameConnection.receiveSnapshot();
      if (newSnapshot != null) {
        previousWorldSnapshot = latestWorldSnapshot;
        latestWorldSnapshot = newSnapshot;
        latestSnapshotNanos = System.nanoTime();
      }
      
      // Enviar input ao servidor remoto
      InputPacket packet = clientInput.buildPacket();
      gameConnection.sendInputPacket(packet);
    } else {
      // Comportamento local (in-process) anterior
      if (serverLoop != null && mapManager != null) {
        serverLoop.updateSnapshotContext(
            mapManager.getCurrentMapId(), player, 
            factionSystem, npcs, chests);
        WorldSnapshot newSnapshot = serverLoop.getLatestSnapshot();
        // ... resto do comportamento local
      }
    }
  }
}
```

---

## Sequência de implementação (Fase 7)

### Parte 1 — Discovery e Registry

- [ ] Criar `ServerInfo` (`src/com/rpggame/shared/ServerInfo.java`)
- [ ] Criar `ServerRegistry` (`src/com/rpggame/server/ServerRegistry.java`)
- [ ] Criar `ServerBroadcaster` e `ServerDiscovery` (UDP broadcast)
- [ ] Testes: publicar e descobrir servidor local

**Verificação:** `ServerDiscovery.scan()` retorna `ServerInfo` publicado por `ServerBroadcaster`.

### Parte 2 — Tela de Lobby

- [ ] Criar `GameLauncher` (`src/com/rpggame/ui/GameLauncher.java`)
- [ ] Implementar botão "CREATE SERVER"
- [ ] Implementar botão "REFRESH"
- [ ] Implementar botão "CONNECT"

**Verificação:** launcher abre, lista aparece ao clicar REFRESH, pode selecionar servidor.

### Parte 3 — GameConnection

- [ ] Criar `GameConnection` (`src/com/rpggame/client/GameConnection.java`)
- [ ] Implementar `connectToServer(hostAddress, playerId)`
- [ ] Implementar fila de snapshots
- [ ] Implementar envio de InputPacket

**Verificação:** cliente conecta, recebe e envia pacotes.

### Parte 4 — GameServer Headless

- [ ] Criar `GameServer` (`src/com/rpggame/server/GameServer.java`)
- [ ] Integrar `ServerNetwork` existente
- [ ] Integrar `ServerLoop` existente
- [ ] Entrada de linha de comando para iniciar

**Verificação:** rodar `java com.rpggame.server.GameServer` sem Swing, sem GUI.

### Parte 5 — Integração

- [ ] Modificar `GamePanel` para usar `GameConnection` quando remoto
- [ ] Mudar entry point: `Game.main()` → `GameLauncher.main()`
- [ ] Testar criar e conectar ao servidor local

**Verificação:** launcher → create → jogo abre com player hospedado.

---

## Considerações de rede

### Endereçamento

Para multiplayer real (não só localhost), precisamos:

```java
// Descobrir IP local da máquina
public static String getLocalIpAddress() throws SocketException {
  for (Enumeration<NetworkInterface> ifaces = 
       NetworkInterface.getNetworkInterfaces(); 
       ifaces.hasMoreElements();) {
    NetworkInterface iface = ifaces.nextElement();
    for (Enumeration<InetAddress> addresses = 
         iface.getInetAddresses(); 
         addresses.hasMoreElements();) {
      InetAddress addr = addresses.nextElement();
      if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
        return addr.getHostAddress();
      }
    }
  }
  return "127.0.0.1";
}
```

### Porta

Usar porta configurável:

```java
int port = 7777;  // Default
if (args.length > 0) {
  try {
    port = Integer.parseInt(args[0]);
  } catch (NumberFormatException e) {
    // usar default
  }
}
```

### Firewall

Documentar que a porta 7777 precisa estar aberta (ou usar UPnP depois).

---

## Tratamento de desconexão

Quando um cliente se desconectar:

```java
// Em ServerNetwork
private void handleClientDisconnect(String playerId) {
  playerManager.removePlayer(playerId);
  
  // Atualizar ServerInfo
  int remaining = playerManager.getPlayerCount();
  serverInfo.updateCurrentPlayers(remaining);
  
  // Se nenhum player restante, servidor pode encerrar
  if (remaining == 0) {
    System.out.println("All players disconnected. Server may shut down.");
  }
}
```

---

## Persistência de servidor

Opção inicial (simples):

- Servidor roda enquanto o host estiver conectado
- Ao desconectar todos, servidor encerra

Opção futura:

- Guardar estado em arquivo/banco de dados
- Servidor pode ser pausado/retomado

---

## Segurança (mínima)

Para esta fase, implementações básicas:

```java
// Validar versão do jogo
if (!client.getGameVersion().equals(SERVER_VERSION)) {
  reject("Version mismatch");
}

// Validar nome de player (sem caracteres especiais perigosos)
if (!playerId.matches("^[a-zA-Z0-9_-]{1,20}$")) {
  reject("Invalid player ID");
}

// Rate-limit conexões por IP
Map<String, Long> lastConnectionTime = ...;
if (System.currentTimeMillis() - lastConnectionTime.getOrDefault(ip, 0) < 1000) {
  reject("Connection rate limit");
}
```

---

## Checklist de sucesso

Fase 7 completa quando:

- [ ] Servidor local pode ser criado, publicado e descoberto
- [ ] Cliente pode conectar a servidor remoto via hostname/IP:port
- [ ] Dois clientes no mesmo servidor veem o mesmo mundo
- [ ] Snapshot é transmitido em tempo real entre server e client
- [ ] Input is enviado de forma confiável
- [ ] Cliente desconectado não derruba o servidor
- [ ] Servidor sem clientes continua simulando (mundo em background)
- [ ] Novo cliente que conecta entra no estado atual do mundo
- [ ] GUI de lobby e é intuitiva

---

## Próximos passos (Fase 8)

Após Fase 7 funcionar, considerar:

- **Matchmaking online** — contar com servidor central de descoberta
- **Persistência** — salvar servidor e estado no disco
- **Admin UI** — kick player, mudar senha, etc.
- **Status do servidor** — ping, latência, TPS real
- **Chat in-game** — mensagens entre players
- **Performance tunning** — bandwidth de snapshot, compression
