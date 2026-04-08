# Diagnóstico: Movimento Lento & Travado no Multiplayer

**Data**: 2026-04-08  
**Problema**: Player anda a ~5px/segundo, travando frequentemente, desconexões  
**Artefatos**: saída Player 1 (cliente) + Servidor

---

## 1. SINTOMAS OBSERVADOS

### Cliente
```
[ClientNetwork] Conectado a 127.0.0.1:7778 como player-7c3c4839
Game loop iniciado
Posição inicial: 638, 260 (Village)
Controles: WASD para mover, ESPAÇO para atacar, C para características
? PERF | frameCPU avg=0,09ms p95=0,17ms p99=0,28ms late=0,0% | fogAvg=0,000ms
```
- ✅ Performance de frame: **excelente** (0,09ms CPU, 0% late)
- ❌ Movimento: lento (5px/segundo estimado)
- ❌ Travamentos: sim, o player se move de forma travada, o sprite fica acelerado como se as configurações de movimento fossem feitas para que ele se mova devagar

### Servidor
```
[ServerNetwork] Cliente 1 conectado: /127.0.0.1:52545
[ServerNetwork] Handshake OK para player-7c3c4839
[ServerNetwork] SocketException no inputReader do cliente 1: Connection reset
[ServerNetwork] SocketException no snapshotWriter do cliente 1: Connection reset by peer
[ServerNetwork] Cliente 1 (player-7c3c4839) desconectado e desregistrado.
```
- ✅ Handshake: sucesso
- ❌ Desconexão prematura: **Connection reset** → problema de rede ou timeout
- ❌ Sem logs de recebimento de input/update de posição

---

## 2. POSSÍVEIS CAUSAS (Em Ordem de Probabilidade)

### 🔴 **CRÍTICA (Alta Probabilidade)**

#### C1: Cliente Aguardando Snapshot do Servidor para Cada Movimento
**Descrição**: Player input não aplica movimento local (client-side prediction)
- Input WASD → envia InputPacket pro servidor
- Cliente fica **esperando snapshot** do servidor antes de renderizar movimento
- Snapshot vira a cada ~50ms (20 TPS) ou até mais lento

**Sintomas Esperados**: Movimento lento (1/20avo = 50ms por update = ~5px/seg se speed=100px/seg)

**Checklist de Verificação**:
- [ ] Em `GameClient.java`, há `applyLocalPrediction()` ou movimento imediato?
- [ ] Em `GamePanel.update()`, movimento do player está vinculado a snapshots?
- [ ] Método `Player.update()` processa input diretamente ou aguarda servidor?

**Arquivos a Inspecionar**:
- `src/com/rpggame/client/GameClient.java` - linha que processa snapshot
- `src/com/rpggame/core/GamePanel.java` - update loop (L445+)
- `src/com/rpggame/entities/Player.java` - método update()

---

#### C2: Taxa de Tick do Servidor Muito Baixa
**Descrição**: Servidor enviando snapshots a 4 TPS em vez de 20 TPS
- ServerLoop @ 4 TPS para mapas "background" (sem players ativos)
- Cliente recebe posição nova a cada 250ms → movimento visível a 5px/seg

**Sintomas Esperados**: Movimento em "saltos" discretos, não suave

**Checklist de Verificação**:
- [ ] Em `ServerLoop.java`, mapa é marcado como ativo (`setHasActivePlayers(true)`)?
- [ ] Em `GamePanel.java` (lado cliente), há chamada para `setHasActivePlayers()`?
- [ ] `MapSimulation.hasActivePlayers()` retorna true ou false durante gameplay?
- [ ] Taxa efetiva de WorldSnapshot: 4 TPS ou 20 TPS?

**Arquivos a Inspecionar**:
- `src/com/rpggame/server/ServerLoop.java` - linha de setHasActivePlayers
- `src/com/rpggame/server/MapSimulation.java` - hasActivePlayers flag
- `src/com/rpggame/client/ClientNetwork.java` - log frequência de snapshots recebidas

---

#### C3: Timeout de Conexão Frequente (Connection Reset)
**Descrição**: Desconexão `Connection reset by peer` indica possível timeout
- Cliente ou servidor fechando conexão inesperadamente
- Intervalo entre input/snapshot muito grande → timeout

**Sintomas Esperados**: Desconexão após ~5-10 segundos, perda de movimentos

**Checklist de Verificação**:
- [ ] Em `ServerNetwork.java`, há timeout configurado? Qual é o valor?
- [ ] Em `ClientNetwork.java`, há ping/heartbeat para manter vivo?
- [ ] Frequência de envio de InputPacket vs timeout do servidor?
- [ ] Thread inputReader morreu (exceção não tratada)?

**Arquivos a Inspecionar**:
- `src/com/rpggame/server/ServerNetwork.java` - timeout config
- `src/com/rpggame/client/ClientNetwork.java` - heartbeat/ping
- Logs: Thread dump ou stack trace da exceção

---

### 🟠 **ALTA (Probabilidade Média)**

#### C4: Interpolação de Movimento Muito Lenta
**Descrição**: Snapshot recebido, mas interpolação suaviza movimento muito lentamente
- Easing/lerp tempo = 500ms+
- Player lerpa entre posições em passos pequenos

**Sintomas Esperados**: Movimento suave mas lento

**Checklist de Verificação**:
- [ ] Em `Player.java` ou `GameClient.java`, há `interpolatePosition()`?
- [ ] Constante `INTERPOLATION_TIME_MS` ou similar?
- [ ] Valor dessa constante: é 500ms, 1000ms ou mais?

**Arquivos a Inspecionar**:
- `src/com/rpggame/entities/Player.java` - método interpolate
- `src/com/rpggame/client/GameClient.java` - aplicação de snapshot

---

#### C5: Input Não Está Sendo Enviado/Processado
**Descrição**: InputPacket não chega ao servidor, servidor não processa
- `GamePanel.keyPressed()` não alimenta `ClientNetwork.sendInput()`
- Servidor não recebe input ou não replica movimento

**Sintomas Esperados**: Movimento não responde ao input

**Checklist de Verificação**:
- [ ] Em `GamePanel.java` (L1250+), há `ClientNetwork.sendInput(packet)`?
- [ ] Em `ClientNetwork.java`, método `sendInput()` envia dados?
- [ ] Em `ServerNetwork.java`, handler processa InputPacket?

**Arquivos a Inspecionar**:
- `src/com/rpggame/core/GamePanel.java` - keyPressed/keyReleased
- `src/com/rpggame/client/ClientNetwork.java` - sendInput()
- `src/com/rpggame/server/ServerNetwork.java` - ClientHandler.run()

---

#### C6: Erro de Sincronização de Thread / Race Condition
**Descrição**: GamePanel e ClientNetwork.snapshotReader() lutando por estruturas
- `Player` sendo atualizado por 2 threads simultaneamente
- Posição lida errada ou atualizada parcialmente

**Sintomas Esperados**: Movimento errático, travamentos aleatórios

**Checklist de Verificação**:
- [ ] Em `Player.java`, `setPosition()` é synchronized?
- [ ] Em `GamePanel.update()`, há cópia de estado segura?
- [ ] `ClientNetwork` usa locks ou CopyOnWrite para estruturas compartilhadas?

**Arquivos a Inspecionar**:
- `src/com/rpggame/entities/Player.java` - synchronized methods
- `src/com/rpggame/client/ClientNetwork.java` - thread safety
- `src/com/rpggame/core/GamePanel.java` - sincronização com threads

---

### 🟡 **MÉDIA (Probabilidade Baixa)**

#### C7: Buffer de Input Congestionado
**Descrição**: InputPacket enfileirando no servidor, processadas lentamente
- Servidor não tem velocidade de processamento suficiente
- Lag acumulativo

**Checklist de Verificação**:
- [ ] Logs de "queue size" ou "pending inputs"?
- [ ] Servidor tem apenas 1 thread para processar tudo?

---

#### C8: Sprite Animation Loop Preso
**Descrição**: Animação do sprite consome CPU, deixa update lento
- Performance logs mostram 0,09ms ok, mas há picos?

**Checklist de Verificação**:
- [ ] `getAnimationFrame()` é O(1)?

---

## 3. PLANO DE INVESTIGAÇÃO (Ordem de Execução)

### **Fase 1: Verificação Rápida (5 min)**

1. **Verificar Taxa de Tick Efetiva**
   - Arquivo: `src/com/rpggame/client/ClientNetwork.java`
   - Buscar: frequência de snapshots recebidas
   - **Ação**: Adicionar log `System.out.println("Snapshot recebido: t=" + System.currentTimeMillis());` em cada recepção
   - **Esperado**: Log a cada 50ms (20 TPS) ou a cada 250ms (4 TPS)?

2. **Verificar Client-Side Movement**
   - Arquivo: `src/com/rpggame/core/GamePanel.java`
   - Buscar: `Player.update()` em `update()` método
   - **Ação**: Verificar se há `player.updateLocalMovement()` ou se tudo vem de snapshot
   - **Esperado**: Movimento local imediato ANTES do snapshot

3. **Verificar Log de Desconexão**
   - Arquivo: `src/com/rpggame/server/ServerNetwork.java`
   - Buscar: "Connection reset"
   - **Ação**: Adicionar stack trace: `e.printStackTrace();` antes de desconectar
   - **Esperado**: Ver por que a conexão fecha

---

### **Fase 2: Análise Estruturada (15 min)**

1. **Fluxo de Movimento Completo**
   - [ ] Input: WASD → GamePanel.keyPressed()
   - [ ] Envio: ClientNetwork.sendInputPacket()
   - [ ] Servidor: ServerNetwork recebe InputPacket
   - [ ] Aplicação: PlayerSimulation.applyInput() → posição atualizada
   - [ ] Snapshot: WorldSnapshotAssembler.assembleSnapshot() contém posição nova
   - [ ] Recepção: ClientNetwork.onSnapshotReceived()
   - [ ] Renderização: GamePanel.paintComponent() usa posição nova

2. **Verificar Cada Thread**
   - ClientNetwork.snapshotReader (thread reader)
   - ServerNetwork.ClientHandler (thread por cliente)
   - GamePanel Swing thread
   - Verificar se estão vivas e sem dead locks

3. **Medir Latência Real**
   - Adicionar timestamp em InputPacket
   - Adicionar timestamp em WorldSnapshot
   - Calcular RTT (round trip time)
   - **Esperado**: < 50ms em localhost

---

### **Fase 3: Testes Isolados (30 min)**

1. **Teste de Snapshot Frequency**
   ```java
   // Em ServerNetwork.sendSnapshot():
   System.out.println("[SNAPSHOT] Enviando em t=" + System.currentTimeMillis());
   
   // Em ClientNetwork.onSnapshot():
   System.out.println("[SNAPSHOT_RCV] Recebido em t=" + System.currentTimeMillis());
   ```
   - Executar por 5 seg
   - Calcular frequência média
   - **Esperado**: 20 Hz (50ms entre logs)

2. **Teste de Input Processing**
   ```java
   // Em GamePanel.keyPressed():
   System.out.println("[INPUT] WASD pressionado em t=" + System.currentTimeMillis());
   
   // Em ServerNetwork.ClientHandler.onInputReceived():
   System.out.println("[INPUT_RCV] Servidor recebeu em t=" + System.currentTimeMillis());
   
   // Em PlayerSimulation.applyInput():
   System.out.println("[POSITION_UPDATE] Nova posição: " + newX + ", " + newY);
   ```
   - Executar 5 movimentos
   - Medir tempo total
   - **Esperado**: cada movimento < 100ms

3. **Teste de Thread Crash**
   ```java
   // Em ClientNetwork.snapshotReader:
   try {
       // ... ler snapshot
   } catch (Exception e) {
       System.err.println("[FATAL] snapshotReader crashed: " + e);
       e.printStackTrace();
   }
   ```
   - Ver se thread morre silenciosamente

---

## 4. MATRIZ DE DIAGNÓSTICO

| Sintoma | C1 Await Snapshot | C2 Low TPS | C3 Timeout | C4 Lerp Lento | C5 No Input |
|---------|---|---|---|---|---|
| 5px/seg | ✅ HIGH | ✅ HIGH | ❌ | ⚠️ | ❌ |
| Travamento | ⚠️ | ❌ | ✅ HIGH | ❌ | ✅ |
| Connection reset | ❌ | ❌ | ✅ HIGH | ❌ | ❌ |
| Responde ao input | ❌ | ⚠️ | ⚠️ | ✅ | ✅ LOW |
| Movimento suave | ❌ | ❌ | ⚠️ | ✅ | ⚠️ |

---

## 5. PRÓXIMOS PASSOS RECOMENDADOS

1. **IMEDIATO**: Adicionar logs de timestamp em 3 pontos chave
   - Saída de InputPacket
   - Recepção de InputPacket no servidor
   - Envio de Snapshot

2. **DEPOIS**: Comparar frequência de eventos
   - Se snapshots a cada 250ms → C2 (baixa TPS)
   - Se input não aparece no servidor → C5 (input não enviado)
   - Se RTT > 200ms → C3 (conexão lenta/timeout)

3. **FINALMENTE**: Implementar client-side prediction
   - Não aguardar servidor para movimento
   - Aplicar input Local imediatamente
   - Reconciliar com snapshot do servidor

---

## 6. REFERÊNCIAS DE CÓDIGO

### Arquivos Principais
```
src/com/rpggame/client/ClientNetwork.java      → Recebe snapshots, envia input
src/com/rpggame/client/GameClient.java         → Aplica snapshot ao player?
src/com/rpggame/server/ServerNetwork.java      → Processa input, envia snapshot
src/com/rpggame/server/PlayerSimulation.java   → Aplica input à posição
src/com/rpggame/server/WorldSnapshotAssembler.java → Coleta posição
src/com/rpggame/core/GamePanel.java            → Loop de jogo (L345+)
src/com/rpggame/entities/Player.java           → update(), movimento
```

### Métodos Críticos
```
GamePanel.update()              → Linha 445+: onde movimento é processado
GamePanel.keyPressed()          → Linha 1250+: input handler
ClientNetwork.sendInputPacket() → envia input
ClientNetwork.onSnapshot()      → recebe snapshot
ServerNetwork.sendSnapshot()    → envia snapshot
PlayerSimulation.applyInput()   → aplica input à posição
```

---

## 7. NOTAS

- **Performance de frame (0,09ms)**: NÃO é o problema. Problema é **rede/sincronização**.
- **Desconexão Connection reset**: Indica timeout ou thread morte silenciosa.
- **5px/seg = ~250ms/movimento**: Sugere TPS muito baixa (4 TPS) ou aguardando snapshot.

