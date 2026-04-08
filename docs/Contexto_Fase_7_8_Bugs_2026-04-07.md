# Contexto de Implementacao - Fase 7 e Fase 8 (2026-04-07)

## 1) Objetivo das fases

## Fase 7 (Lobby + Multiplayer)
- Permitir 3 fluxos no inicio: Jogar Solo, Criar Servidor, Entrar em Servidor.
- Conectar cliente via TCP em servidor local/dedicado.
- Permitir 2 instancias simultaneas.

## Fase 8 (Respawn)
- Trocar fluxo de morte de "novo jogo" para "renascer".
- Manter personagem atual.
- Zerar apenas XP do nivel atual.

---

## 2) Incrementos aplicados (ordem cronologica)

1. Adicionado GameLauncher com 3 opcoes de entrada.
- Arquivo: src/com/rpggame/ui/GameLauncher.java

2. Integracao do launcher no bootstrap do jogo.
- Arquivo: src/com/rpggame/core/Game.java
- Resultado: jogo abre no launcher e direciona para fluxo solo/rede.

3. Encadeamento de parametros de rede no fluxo de telas.
- Arquivos:
  - src/com/rpggame/ui/MainMenuScreen.java
  - src/com/rpggame/ui/CombinedCharacterScreen.java
  - src/com/rpggame/core/GamePanel.java
- Resultado: host/porta/mode seguem ate a tela de jogo.

4. Fase 8 implementada na tela de morte.
- Arquivos:
  - src/com/rpggame/core/GamePanel.java
  - src/com/rpggame/systems/ExperienceSystem.java
- Resultado:
  - botao "Renascer"
  - respawn no spawn
  - reset de XP do nivel atual

5. Correcao de warning de codigo morto.
- Arquivo: src/com/rpggame/core/GamePanel.java
- Resultado: metodo restartGame removido (nao usado).

6. Multiplayer: IDs de player unicos por instancia.
- Arquivo: src/com/rpggame/core/GamePanel.java
- Resultado: evita colidir como "player-1" em duas instancias.

7. Handshake de rede com fallback para ID duplicado.
- Arquivo: src/com/rpggame/server/ServerNetwork.java
- Resultado: se ID ja existe, servidor ajusta sufixo.

8. ServerLoop headless para dedicado.
- Arquivo: src/com/rpggame/server/ServerLoop.java
- Resultado: servidor dedicado passou a processar input de clientes no loop.

9. Contexto inicial do servidor dedicado.
- Arquivo: src/com/rpggame/server/GameServer.java
- Resultado: mapa inicial e contexto de snapshot inicializados.

10. Correcao de "flicker" por mistura de snapshot local+rede.
- Arquivo: src/com/rpggame/core/GamePanel.java
- Resultado: no modo rede, cliente puro (sem ServerLoop local).

11. Barra de status estabilizada pelo playerId local.
- Arquivo: src/com/rpggame/core/GamePanel.java
- Resultado: UI nao depende mais do primeiro player da lista.

12. Servidor dedicado sem fechar instantaneamente quando porta ocupada.
- Arquivo: src/com/rpggame/server/GameServer.java
- Resultado: fallback automatico para proxima porta livre.

13. Spawn de player de rede ajustado no servidor dedicado.
- Arquivo: src/com/rpggame/server/ServerLoop.java
- Resultado: player headless deixa de nascer em (0,0) e vai para spawn da village.

---

## 3) Bugs encontrados ate aqui

## Resolvidos

1. Travava no menu apos Criar Servidor e clicar Jogar.
- Sintoma: permanecia na tela principal sem ir para criacao de personagem.
- Ajuste principal: troca de conteudo de frame via setContentPane no fluxo.
- Arquivo: src/com/rpggame/ui/MainMenuScreen.java

2. Warning de metodo sem uso (restartGame).
- Arquivo: src/com/rpggame/core/GamePanel.java

3. Conflito de IDs (duas conexoes como player-1).
- Sintoma: um cliente sobrescrevia estado do outro.
- Arquivos:
  - src/com/rpggame/core/GamePanel.java
  - src/com/rpggame/server/ServerNetwork.java

4. Servidor dedicado fechando instantaneamente.
- Causa: porta em uso (bind error).
- Arquivo: src/com/rpggame/server/GameServer.java

5. Flicker geral de jogador/NPC/UI.
- Causa: mistura de snapshots locais e remotos no cliente em modo rede.
- Arquivo: src/com/rpggame/core/GamePanel.java

## Em aberto (estado atual)

1. Cliente conecta e desconecta logo apos handshake no dedicado.
- Evidencia de log:
  - Handshake OK
  - SocketException no inputReader
  - cliente desregistrado em seguida
- Impacto: outro player nao aparece de forma confiavel.

2. Em alguns testes, "nada aparece" mesmo com servidor no ar.
- Hipoteses:
  - desconexao precoce apos handshake
  - falta de persistencia de sessao no cliente apos transicao de telas
  - estado de player no servidor sem atualizar continuamente para snapshot util

---

## 4) Locais de investigacao para amanha

## Prioridade alta

1. src/com/rpggame/client/ClientNetwork.java
- Verificar ciclo de vida do socket apos trocar de tela.
- Verificar se alguma excecao encerra running e fecha conexao cedo.
- Pontos:
  - connect
  - snapshotReaderLoop
  - inputWriterLoop
  - disconnect

2. src/com/rpggame/server/ServerNetwork.java
- Investigar origem exata do SocketException no inputReader.
- Adicionar log da exception.getMessage no caminho atual de erro.
- Confirmar se o close vem do cliente ou do servidor.

3. src/com/rpggame/core/GamePanel.java
- Confirmar que initWorldState em modo rede roda uma unica vez por sessao.
- Garantir que nao exista reconstrucao de painel que mate conexao ativa.
- Validar uso de localPlayerId na UI e no envio de input.

4. src/com/rpggame/ui/MainMenuScreen.java e src/com/rpggame/ui/CombinedCharacterScreen.java
- Conferir transicao de telas para nao descartar painel/conexao no meio do fluxo.
- Garantir que Start Game nao cria fluxo duplicado.

## Prioridade media

5. src/com/rpggame/server/ServerLoop.java
- Auditar fluxo de update headless:
  - input drain por cliente
  - applyInput
  - update do player
  - publishSnapshot
- Confirmar consistencia de mapa ativo e contexto de snapshot.

6. src/com/rpggame/server/WorldSnapshotAssembler.java
- Verificar se lista de players montada no snapshot contem todos os conectados em cada tick.
- Validar ordem/estabilidade da lista (importante para debug visual).

---

## 5) Instrumentacao sugerida para acelerar diagnostico

1. Logar eventos de conexao/desconexao com playerId e motivo no cliente e no servidor.

2. No servidor, logar a cada N ticks:
- quantidade de players em playerSimulations
- IDs ativos
- posicao de cada player

3. No cliente, logar a cada N snapshots:
- tick recebido
- total de players no snapshot
- se localPlayerId foi encontrado no snapshot

---

## 6) Comandos uteis de reproducao

1. Servidor dedicado:
- java -cp bin com.rpggame.server.GameServer 7777

2. Cliente:
- java -cp bin com.rpggame.core.Game

3. Build rapido completo:
- javac -d bin (todos os fontes)
- jar cfm dist/Top-view-rpg-game-fixed.jar MANIFEST.MF -C dist/bin .

---

## 7) Estado atual resumido

- Fase 7 parcialmente funcional (fluxo de launcher e conexao implementados).
- Fase 8 funcional (respawn com preservacao de personagem e reset de XP do nivel).
- Bug critico restante: estabilidade da sessao TCP no servidor dedicado apos handshake.
- Proximo foco recomendado: rastrear desconexao imediata no par ClientNetwork/ServerNetwork.
