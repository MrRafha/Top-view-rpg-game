# TODO — Bugs e Ataques (2026-04-07)

## Status de cada item: [ ] pendente  [x] feito

---

## BUGS CORRIGIDOS NESTA SESSÃO

[x] 1. Velocidade "1 pixel" no modo rede
- Arquivo: src/com/rpggame/core/GamePanel.java
- Fix: reconcilia posição do player só quando chega snapshot novo (lastReconciledSnapshotTick)
- Antes: setPosition chamado 60x/s com posição stale do snapshot → snap-back cancelava movimento

[x] 2. NPCs não aparecem no modo rede
- Arquivo: src/com/rpggame/core/GamePanel.java
- Fix: fallback para renderizar NPCs localmente quando snapshot não os contém
- Causa: servidor dedicado nunca recebe NPCs (updateSnapshotContext com emptyList)

[x] 3. Desconexão imediata por RuntimeException no snapshotReaderLoop
- Arquivo: src/com/rpggame/client/ClientNetwork.java
- Fix: try-catch RuntimeException dentro do while loop para não matar thread
- Causa: NumberFormatException do parser JSON custom encerrava thread silenciosamente

[x] 4. Deadlock no servidor — snapshotWriterLoop falhava e inputReader ficava preso
- Arquivo: src/com/rpggame/server/ServerNetwork.java
- Fix: fechar socket no catch IOException/SocketException do snapshotWriterLoop
- Causa: inputReader.join() nunca retornava se snapshotWriter morresse sem fechar o socket

---

## BUGS PENDENTES

[ ] 5. Dupla publicação de input em modo in-process (solo)
- Arquivo: src/com/rpggame/core/GamePanel.java + ClientInput.java
- Causa: clientInput.buildPacket() publica no transport E serverLoop.submitInput() também publica
- Fix sugerido: NÃO chamar clientInput.setTransport(p1Transport) no modo in-process (solo).
  Em modo solo, só o serverLoop.submitInput deve publicar. Em modo rede, só o clientInput.buildPacket.
- Código: initWorldState() linha ~416: remover clientInput.setTransport(p1Transport)
  Mas garantir que em modo rede (useNetwork=true) o transport continue sendo setado.

[ ] 6. Servidor dedicado não simula NPCs
- Arquivo: src/com/rpggame/server/GameServer.java
- Causa: NPCs são criados no GamePanel (cliente), servidor usa emptyList para activeNpcs
- Fix longo prazo: mover criação de NPCs para ServerLoop/MapSimulation
  Ou: cliente TCP envia posições de NPCs para o servidor periodicamente
- Workaround atual: renderização local de NPCs no cliente já implementada (item 2 acima)

[ ] 7. initWorldState() sem guard — pode ser chamado 2x se setPlayerClass for chamado 2x
- Arquivo: src/com/rpggame/core/GamePanel.java
- Fix: adicionar guard no topo de initWorldState():
  if (useNetwork && clientNetwork != null && clientNetwork.isConnected()) return;
- Causa: se o fluxo de telas chamar setPlayerClass múltiplas vezes, abre duas conexões TCP

[ ] 8. setPosition usando double (já corrigido na sessão — verificar se o cast int foi removido)
- Arquivo: src/com/rpggame/core/GamePanel.java linha ~657
- Verificar: player.setPosition(sp.getX(), sp.getY()) sem cast int  ← já feito

---

## ATAQUES DAS CLASSES — PENDENTES

### Warrior — Estocada em linha reta

[ ] 9. Direção do ataque do Warrior não bate com a direção visual
- Arquivo: src/com/rpggame/entities/Player.java — método attack()
- Causa: `facing` é atualizado só quando o player SE MOVE. Se estiver parado, `facing` aponta
  para onde ele moveu pela última vez, mas `facingLeft` indica direção visual corretamente.
- Fix: usar facingLeft para determinar ângulo do SWORD_SLASH quando não há movimento:
  ```java
  case "warrior":
      double warriorFacing = (dx != 0 || dy != 0) ? facing : (facingLeft ? Math.PI : 0.0);
      double slashX = startX + Math.cos(warriorFacing) * 30;
      double slashY = startY + Math.sin(warriorFacing) * 30;
      projectile = new Projectile(slashX, slashY, warriorFacing, Projectile.SWORD_SLASH, totalDamage);
      break;
  ```
  Mas dx e dy não estão acessíveis no método attack() (são campos da classe, então SIM acessíveis).
  Verificar: player tem `this.dx` e `this.dy` como campos de instância → usar diretamente.

### Mage — Magia que explode em área no ataque básico (SPACE)

[ ] 10. MAGIC_BOLT não tem efeito de área no hit — precisa explodir
- Arquivo: src/com/rpggame/systems/EnemyManager.java — método checkProjectileCollisions()
- Fix: quando MAGIC_BOLT colide, adicionar dano em área nos inimigos próximos (radius ~80px):
  ```java
  if (enemyBounds.intersects(projBounds)) {
      if (Projectile.MAGIC_BOLT.equals(projectile.getType())) {
          // Dano direto no alvo
          enemy.takeDamage(projectile.getDamage());
          // Splash em raio de 80px
          double explodeX = projectile.getX(), explodeY = projectile.getY();
          for (Enemy other : enemies) {
              if (other != enemy && other.isAlive()) {
                  double dist = Math.hypot(other.getX() - explodeX, other.getY() - explodeY);
                  if (dist <= 80.0) {
                      int splashDmg = (int)(projectile.getDamage() * 0.6 * (1.0 - dist/80.0));
                      other.takeDamage(splashDmg);
                  }
              }
          }
      } else {
          enemy.takeDamage(projectile.getDamage());
      }
      projIterator.remove();
      break;
  }
  ```
- Também adicionar efeito visual de explosão (círculo fade) no ponto de impacto.
  Alternativa: registrar posições de explosão em GamePanel e renderizar no paintComponent.

### Hunter — Flecha não-perfurante (verificar, deve estar OK)

[ ] 11. Verificar que ARROW não perfura inimigos
- Arquivo: src/com/rpggame/systems/EnemyManager.java — método checkProjectileCollisions()
- Status atual: já tem `projIterator.remove() + break` → flecha para no primeiro inimigo. ✓
- PiercingArrowSkill (slot 1, skill) é diferente: tem própria lógica de colisão e SIM perfura.
- Verificar: certifique-se que o ataque básico SPACE do Hunter usa Player.attack() → ARROW type,
  e não PiercingArrowSkill. Parece correto pelo código.
- Se o usuário reportar que a flecha está perfurando, checar se PiercingArrowSkill está sendo
  ativada por algum bug ao invés do ataque básico.

---

## OUTROS PROBLEMAS A VERIFICAR

[ ] 12. SnapshotRenderSystem — alpha de interpolação usa SNAPSHOT_INTERVAL_NANOS fixo (50ms)
- Arquivo: src/com/rpggame/render/SnapshotRenderSystem.java
- Se o ServerLoop estiver rodando mais devagar que 20TPS (ex: GC pause), o alpha
  chega em 1.0 rapidamente e o player "pula" para a próxima posição do snapshot.
- Solução: usar o timestampNanos do WorldSnapshot para calcular o intervalo real entre snapshots.

[ ] 13. buildPlayers legado (método assemble) usa hardcoded "player-1" como ID
- Arquivo: src/com/rpggame/server/WorldSnapshotAssembler.java linha 126
- Agora que o localPlayerId é baseado em UUID, o findSnapshotPlayerById pode não achar o player
  se o método assemble (não assembleMulti) for usado em algum path.
- Verificar: GamePanel usa assembleMulti via ServerLoop.publishSnapshot() que chama assembleMulti.
  O método assemble() está sendo chamado em algum outro lugar?

---

## COMANDOS DE BUILD

# Compilar todos os fontes
javac -d bin -sourcepath src $(find src -name "*.java")

# Rodar servidor dedicado
java -cp bin com.rpggame.server.GameServer 7777

# Rodar cliente
java -cp bin com.rpggame.core.Game

# JAR completo
jar cfm dist/Top-view-rpg-game.jar MANIFEST.MF -C bin .
