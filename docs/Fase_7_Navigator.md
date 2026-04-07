# Fase 7 — Navigator: Seu Guia de Implementação

## 🎯 Objetivo Final

Você e seus amigos em PCs diferentes jogando junto. Um cria servidor, outros conectam via launcher de lobby.

---

## 📚 Documentos da Fase 7

| Doc | Foco | Para Quem |
|---|---|---|
| **[Fase_7_Quick_Start.md](Fase_7_Quick_Start.md)** | Visão geral rápida, checklist | Todos (comece aqui!) |
| **[Fase_7_Server_Lobby_Multiplayer.md](Fase_7_Server_Lobby_Multiplayer.md)** | Arquitectura completa, componentes | Entender o desenho completo |
| **[Fase_7_Padrao_Comunicacao.md](Fase_7_Padrao_Comunicacao.md)** | Protocolo servidor↔cliente | Implementar rede |

---

## 🗺️ Roadmap Visual

```
┌─── START ───┐
│ Leia Quick Start
└──────┬──────┘
       │
       ▼
┌──────────────────────────────┐
│ ESCOLHA UM CAMINHO           │
├──────────────────────────────┤
│ A) "Quer implementar agora?"  │ → Quick Start
│    → Vá para Checklist        │   → Implementar por ordem
│                               │   → Testar cada passo
├──────────────────────────────┤
│ B) "Quer entender tudo?"      │ → Server Lobby Multiplayer
│    → Leia arquitetura         │   → Copie código
│    → Copie componentes        │   → Integre
├──────────────────────────────┤
│ C) "Quer detalhes de rede?"   │ → Padrao Comunicacao
│    → Veja protocolo           │   → JSON / TCP
│    → Implementar ClientHandler│   → Threading
└──────────────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│ TESTE LOCAL                  │
├──────────────────────────────┤
│ Terminal 1: GameServer       │
│ Terminal 2: GameLauncher     │
│ Click CREATE → Servidor inicia
│ Click REFRESH → Descobre
│ Click CONNECT → Entra
└──────────────┬───────────────┘
               │
               ▼
      ✅ SUCESSO = Fase 7!
```

---

## ⚡ Implementação Rápida (TL;DR)

Se você quer só os passos sem explicação:

### 1. Copy-paste os DTOs

```
src/com/rpggame/shared/ServerInfo.java
  ↑ (de Server_Lobby_Multiplayer.md)
```

### 2. Copie a rede local (UDP)

```
src/com/rpggame/server/ServerBroadcaster.java
src/com/rpggame/client/ServerDiscovery.java
  ↑ (de Server_Lobby_Multiplayer.md)
```

### 3. Copie a conexão TCP

```
src/com/rpggame/client/GameConnection.java
  ↑ (de Server_Lobby_Multiplayer.md)
```

### 4. Copie o Launcher

```
src/com/rpggame/ui/GameLauncher.java
  ↑ (de Server_Lobby_Multiplayer.md)
```

### 5. Estenda GameServer

```
src/com/rpggame/server/GameServer.java
  ↑ (de Server_Lobby_Multiplayer.md, seção "Servidor headless")
```

### 6. Integre GamePanel

```
Modifique em GamePanel.java:
- Adicione setGameConnection()
- Use GameConnection quando isRemote=true
  ↑ (de Quick_Start.md, seção "Integração do GamePanel")
```

### 7. Teste

```bash
java com.rpggame.ui.GameLauncher
```

Pronto! Fase 7 = ✅

---

## 🔍 Decision Tree: Qual doc importa agora?

```
┌─ Você está indo implementar agora?
│  └─ SIM → [Quick Start] checklist passo a passo
│  └─ NÃO → Continue abaixo
│
├─ Você precisa entender a arquitetura espacial?
│  └─ SIM → [Server Lobby Multiplayer] visão geral
│  └─ NÃO → Continue abaixo
│
└─ Você vai implementar o protocolo de rede?
   └─ SIM → [Padrao Comunicacao] JSON + TCP details
   └─ NÃO → Você provavelmente só quer copy-paste
```

---

## 🧩 Componentes Novos (Resumo)

| Componente | Arquivo | Responsabilidade |
|---|---|---|
| `ServerInfo` | `shared/ServerInfo.java` | DTO (nome, addr, max players) |
| `ServerBroadcaster` | `server/ServerBroadcaster.java` | Publicar servidor via UDP |
| `ServerDiscovery` | `client/ServerDiscovery.java` | Descobrir servidores via UDP |
| `GameConnection` | `client/GameConnection.java` | TCP client (receber snapshot, enviar input) |
| `GameLauncher` | `ui/GameLauncher.java` | GUI lobby (create/refresh/connect) |
| `GameServer` | `server/GameServer.java` | Arquivo headless (principal do servidor) |
| `ServerNetwork` | `server/ServerNetwork.java` | TCP server, aceitar múltiplas conexões |
| `ClientHandler` | `server/ClientHandler.java` | Lidar com 1 cliente TCP |

**Mudanças existentes:**
- `GamePanel.java` → adicionar `GameConnection` support
- `Game.java` → entry point vira `GameLauncher` em vez de direto para GamePanel

---

## 📊 Complexidade Estimada

| Componente | LOC | Dificuldade | Tempo |
|---|---|---|---|
| `ServerInfo` | ~50 | ★☆☆ | 5 min |
| `ServerBroadcaster` | ~80 | ★★☆ | 20 min |
| `ServerDiscovery` | ~60 | ★★☆ | 20 min |
| `GameConnection` | ~150 | ★★★ | 40 min |
| `GameLauncher` | ~200 | ★★☆ | 45 min |
| `GameServer` | ~80 | ★☆☆ | 15 min |
| `ServerNetwork` + `ClientHandler` | ~250 | ★★★ | 60 min |
| Integração `GamePanel` | ~30 | ★☆☆ | 10 min |
| **TOTAL** | **~900** | **★★☆** | **~3.5h** |

*(Com código copy-paste dos docs: ~2h)*

---

## 🐛 Troubleshooting Rápido

| Probleminha | Solução |
|---|---|
| "Connection refused" | Servidor não está rodando ou porta bloqueada |
| "Discover retorna lista vazia" | UDP não chega (firewall). Use IP manual. |
| "Snapshot vazio" | `WorldSnapshot.toJson()` com erro. Validar JSON. |
| "Dois clientes veem diferentes mundos" | Falta `broadcastSnapshot()` para todos |
| "Servidor trava quando cliente conecta" | Thread de ClientHandler não isolada. Adicionar try-catch. |
| "Player 2 não aparece pro Player 1" | Snapshot não inclui P2. Checar `assembleMulti()`. |

Mais detalhes → Ver [Padrao_Comunicacao.md] seção "Armadilhas comuns"

---

## 🧪 Checklist de Teste

Marque conforme vai testando:

- [ ] `ServerInfo` serializa/desserializa JSON
- [ ] `ServerBroadcaster` envia UDP a cada 5s
- [ ] `ServerDiscovery.scan()` recebe e retorna lista
- [ ] `GameConnection.connectToServer()` abre TCP sem erro
- [ ] `GameLauncher` abre minimizado (sem crashes)
- [ ] Botão "CREATE SERVER" inicia GameServer headless
- [ ] Botão "REFRESH" popula lista de servidores
- [ ] Botão "CONNECT" abre GamePanel com player
- [ ] Two GamePanel instances veem o mesmo mundo
- [ ] Input de P1 move P1 em ambas as telas
- [ ] Input de P2 move P2 em ambas as telas
- [ ] Desconectar P1 remove P1 do lado de P2
- [ ] Servidor continua rodando se 1 cliente sai

---

## 🔗 Referências Rápidas

**Se você quer copy-paste código específico:**

| Coisa | Achar em |
|---|---|
| Servidor UDP broadcast | `Server_Lobby_Multiplayer.md` → ServerBroadcaster |
| Cliente descobrir servidores | `Server_Lobby_Multiplayer.md` → ServerDiscovery |
| TCP client connection | `Server_Lobby_Multiplayer.md` → GameConnection |
| TCP server handling | `Padrao_Comunicacao.md` → ServerNetwork |
| JSON serialization manual | `Padrao_Comunicacao.md` → jsonToString() |
| Swing GUI lobby | `Server_Lobby_Multiplayer.md` → GameLauncher |
| Threading para rede | `Padrao_Comunicacao.md` → ClientHandler.run() |

---

## 💡 Dicas de Implementação

1. **Comece com localhost**
   ```java
   // Teste com "localhost:7777" antes de testar IP real
   ```

2. **Adicione muitos logs**
   ```java
   System.out.println("Client connected: " + socket.getInetAddress());
   System.out.println("Snapshot sent to " + activeConnections.size() + " clients");
   ```

3. **Teste com 2 GameLauncher abertos**
   - Terminal 1: `java GameLauncher`
   - Terminal 2: `java GameLauncher`
   - Validar que ambas veem o mesmo servidor

4. **UI pode esperar**
   - Comece só com JList e botão básico
   - Embelezar depois

5. **Rede é difícil**
   - Se der erro, suspeita é firewall, DNS ou serialização JSON
   - Não é o seu código (provavelmente)

---

## 📝 Próximas Fases (Preview)

**Fase 8** — Admin e Status
- Página de status do servidor (ping, players online)
- Kick de players
- Persistência (salvar/carregar mundo)

**Fase 9** — Online Matchmaking
- Servidor central para descobrir servers
- Não mais só UDP local

**Fase 10** — Voice & Chat
- Chat in-game
- Possível integração Discord

---

## ✅ Definição de Sucesso (Fase 7)

Quando você conseguir:

1. Clicar "CREATE SERVER" e servidor rodar sem GUI
2. Clicar "REFRESH" e ver o servidor na lista
3. Clicar "CONNECT" e entrar no jogo
4. Novo GameLauncher em outro processo conectar
5. Ambos os GamePanel sincronizarem perfeito (velocidade, posição)
6. Goblins se mexerem igual nos dois
7. Desconectar um cliente não afeta o outro

**Quando tudo isso funcionar = 🎉 Multiplayer Local Completo! 🎉**

---

## 🚀 Let's Go!

Escolha seu caminho:

- 👶 **Iniciante?** → [Quick Start](Fase_7_Quick_Start.md)
- 🎯 **Prático?** → [Server Lobby Multiplayer](Fase_7_Server_Lobby_Multiplayer.md)
- 🔬 **Técnico?** → [Padrão Comunicação](Fase_7_Padrao_Comunicacao.md)

Boa sorte! Você consegue fazer essa fase! 💪
