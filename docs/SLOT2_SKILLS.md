# Habilidades do Slot 2

Este documento descreve as habilidades do slot 2 implementadas para cada classe do jogo.

## Sistema de Desbloqueio

As habilidades do slot 2 são desbloqueadas quando o jogador atinge o **nível 5**. Uma mensagem de diálogo aparecerá na tela informando:

```
"SLOT 2 DESBLOQUEADO!\n\nVocê pode agora usar uma nova\nhabilidade na tecla 2!"
```

## Sistema de Mana

Todas as habilidades consomem mana, **exceto a habilidade de slot 1 do Guerreiro** (Golpe Horizontal).

### Custos de Mana

- **Slot 1 (Mago e Arqueiro):** 20 mana
  - Bola de Fogo (Mage): 20 mana
  - Flecha Perfurante (Archer): 20 mana
  
- **Slot 2 (Todas as Classes):** 30 mana
  - Intimidação Colossal (Warrior): 30 mana
  - Congelamento (Mage): 30 mana
  - Salto Veloz (Archer): 30 mana

### Regeneração de Mana

A mana regenera automaticamente a cada segundo, com a quantidade baseada no atributo **Sabedoria**:

**Fórmula:** `Mana Regen = 0.5 + (Sabedoria - 5) × 0.1`

**Exemplos:**
- Sabedoria 5: 0.5 mana/s
- Sabedoria 10: 1.0 mana/s
- Sabedoria 15: 1.5 mana/s
- Sabedoria 20: 2.0 mana/s

## Habilidades por Classe

### 🗡️ Warrior - Grito Intimidador

**Arquivo:** `IntimidatingShoutSkill.java`

**Descrição:** O guerreiro solta um grito intimidador que amedronta todos os inimigos em uma área de 3x3 tiles ao seu redor.

**Características:**
- **Alcance:** 144 pixels (3x3 tiles)
- **Cooldown:** 10 segundos
- **Duração do Efeito:** 2 segundos (120 frames)
- **Efeito nos Inimigos:** Inimigos entram em estado de fuga (fleeing), correndo na direção oposta ao jogador

**Efeito Visual:**
- Onda vermelha expansiva que pulsa do jogador
- Duas camadas de onda (externa clara + interna escura)
- Expansão de 0 até 144 pixels de raio durante 30 frames

**Mecânica:**
1. Verifica todos os inimigos no EnemyManager
2. Calcula distância de cada inimigo ao jogador
3. Inimigos dentro do raio são "intimidados"
4. Usa reflection para definir campos `fleeing=true` e `fleeTimer=120`
5. Calcula direção de fuga (dx, dy) baseada na posição relativa ao jogador

---

### 🔮 Mage - Congelamento

**Arquivo:** `FreezingSkill.java`

**Descrição:** O mago congela 2 tiles à sua frente, criando uma superfície de gelo que pode bloquear passagem ou capturar inimigos.

**Características:**
- **Alcance:** 2 tiles na direção que o player está olhando
- **Cooldown:** 5 segundos
- **Duração do Efeito:** 5 segundos (300 frames)
- **Direção:** Baseada em `getFacingDirection()` do player

**Efeito Visual:**
- Quadrados azuis transparentes (alpha 0.6) sobre os tiles
- Padrão de linhas diagonais brancas simulando gelo/cristais
- Fade out gradual conforme o efeito expira

**Mecânica:**
1. Calcula facing direction do player
2. Determina direção cardinal (Norte, Sul, Leste, Oeste)
3. Congela 2 tiles consecutivos na direção escolhida
4. Armazena FrozenTile com coordenadas (tileX, tileY) e duração
5. Método público `isTileFrozen(int tileX, int tileY)` para verificação de colisão

**Cálculo de Direção:**
```java
- facing < -2.356 ou > 2.356 → Oeste (dx=-1, dy=0)
- -2.356 <= facing < -0.785 → Norte (dx=0, dy=-1)
- -0.785 <= facing < 0.785 → Leste (dx=1, dy=0)
- 0.785 <= facing <= 2.356 → Sul (dx=0, dy=1)
```

---

### 🏹 Hunter/Archer - Salto Veloz

**Arquivo:** `QuickDashSkill.java`

**Descrição:** O caçador/arqueiro realiza um dash rápido de 3 tiles na direção que está olhando, saltando sobre paredes, água e lava.

**Características:**
- **Alcance:** 3 tiles (144 pixels)
- **Cooldown:** 6 segundos
- **Duração da Animação:** 0.25 segundos (15 frames)
- **Ignora Colisão:** Sim, durante todo o dash

**Efeito Visual:**
- Trail de posições anteriores do player (5 posições)
- Sprites semi-transparentes mostrando o caminho percorrido
- Linhas de velocidade atrás do trail
- Cor verde clara para indicar velocidade

**Mecânica:**
1. Calcula facing direction do player
2. Determina posição inicial (startX, startY)
3. Calcula posição final: `target = start + (cos(facing), sin(facing)) * 144`
4. Interpola posição durante 15 frames (progresso 0.0 a 1.0)
5. Usa reflection para modificar campos `x` e `y` diretamente, ignorando colisão
6. Método `applyDashMovement(Player)` é chamado pelo Player durante seu update

**Trail System:**
```java
- MAX_TRAIL_POSITIONS = 5
- trailX[] e trailY[] armazenam últimas 5 posições
- Índice circular (trailIndex) atualiza a cada frame
- Transparência diminui para posições mais antigas (alpha fade)
```

**Integração com Player:**
O método `applyDashMovement()` é chamado em `Player.update()`:
```java
if (dashSkill instanceof QuickDashSkill) {
    QuickDashSkill quickDash = (QuickDashSkill) dashSkill;
    quickDash.applyDashMovement(this);
}
```

---

## Arquitetura do Sistema

### Classe Base: Skill

Todas as habilidades herdam de `com.rpggame.systems.Skill`, que fornece:
- `execute(Player)` - Verifica cooldown e chama performSkill
- `performSkill(Player)` - Método abstrato para lógica da skill
- `update()` - Atualiza cooldown e timers
- `render(Graphics2D, Camera)` - Renderiza efeitos visuais
- `isLearned()`, `setLearned(boolean)` - Controle de aprendizado
- `isOnCooldown()`, `getCooldownInSeconds()` - Informações de cooldown

### SkillManager

O `SkillManager` gerencia todas as habilidades do jogador:

```java
private void initializeSkills() {
    switch (playerClass.toLowerCase()) {
        case "warrior":
            skills.put(1, new HorizontalSlashSkill());
            skills.put(2, new IntimidatingShoutSkill());
            break;
        case "mage":
            skills.put(1, new FireballSkill());
            skills.put(2, new FreezingSkill());
            break;
        case "archer":
        case "hunter":
            skills.put(1, new PiercingArrowSkill());
            skills.put(2, new QuickDashSkill());
            break;
    }
}
```

### Player Integration

O `Player.java` chama:
1. `skillManager.update()` - Atualiza todas as skills
2. `skillManager.render(g, camera)` - Renderiza efeitos visuais
3. `skillManager.useSkill(slot)` - Executa skill quando tecla é pressionada
4. `checkSkillUnlock()` - Verifica desbloqueio ao atingir níveis 5, 7, 10

---

## Controles

- **Tecla 2:** Usar habilidade do slot 2
- Habilidade só funciona se estiver desbloqueada (nível >= 5)
- Cooldown aparece na UI do jogador

---

## Próximos Passos

### Slots 3 e 4

- **Slot 3:** Desbloqueado no nível 7
- **Slot 4:** Desbloqueado no nível 10

### Melhorias Sugeridas

1. **IntimidatingShoutSkill:**
   - Adicionar som de grito intimidador
   - Partículas de medo nos inimigos afetados
   - Inimigos mais fracos podem ficar "stunnados" além de assustados

2. **FreezingSkill:**
   - Adicionar detecção de colisão para inimigos
   - Inimigos pisando no gelo escorregam ou ficam lentos
   - Som de congelamento
   - Efeito de quebra quando o gelo expira

3. **QuickDashSkill:**
   - Adicionar invulnerabilidade durante o dash
   - Som de "whoosh" de velocidade
   - Dano/knockback se colidir com inimigo durante dash
   - Partículas de vento no trail

---

## Testes Necessários

- [ ] Verificar que slot 2 desbloqueia corretamente no nível 5
- [ ] Testar cooldowns de todas as habilidades
- [ ] Verificar efeitos visuais renderizam corretamente
- [ ] Testar Intimidating Shout com múltiplos inimigos
- [ ] Verificar Freezing em todas as direções (N, S, L, O)
- [ ] Testar Quick Dash atravessando paredes e água
- [ ] Validar que habilidades não funcionam durante diálogo
- [ ] Verificar integração com sistema de combate

---

## Documentação Técnica

### Reflection Usage

Algumas habilidades usam Java Reflection para acessar campos privados:

**IntimidatingShoutSkill:**
```java
Field fleeingField = enemy.getClass().getDeclaredField("fleeing");
fleeingField.setAccessible(true);
fleeingField.set(enemy, true);
```

**QuickDashSkill:**
```java
Field xField = player.getClass().getDeclaredField("x");
xField.setAccessible(true);
xField.set(player, newX);
```

> ⚠️ **Nota:** O uso de reflection pode causar problemas em versões futuras se os nomes dos campos mudarem. Considere adicionar métodos públicos `setFleeing()` e `setPosition()` nas classes relevantes.

---

## Compilação

```powershell
cd src
javac -encoding UTF-8 -d ../bin -cp ".;../bin" ^
  com/rpggame/systems/skills/IntimidatingShoutSkill.java ^
  com/rpggame/systems/skills/FreezingSkill.java ^
  com/rpggame/systems/skills/QuickDashSkill.java ^
  com/rpggame/systems/SkillManager.java ^
  com/rpggame/entities/Player.java
```

---

**Data de Implementação:** Dezembro 2024  
**Versão do Jogo:** 1.2.2+
