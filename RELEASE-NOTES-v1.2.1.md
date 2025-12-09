# 🎮 RPG Game - Release Notes v1.2.1

**Data de Lançamento:** 09 de Dezembro de 2025

## 🐛 Correções Críticas (Bug Fixes)

### Sistema de Combate
- ✅ **Projéteis não atravessam mais paredes**
  - Implementado sistema de detecção de colisão com paredes para todos os tipos de projéteis
  - Projéteis (flechas, magias, sword slash) agora colidem e desaparecem ao atingir paredes
  - Método `checkWallCollision()` adicionado à classe `Projectile`
  - Verificação aplicada a cada frame no update do Player

- ✅ **Ataques não afetam inimigos através de paredes**
  - Adicionado algoritmo de linha de visão (Bresenham) em `checkProjectileCollisions()`
  - Projéteis só causam dano se houver caminho limpo entre eles e o alvo
  - Previne exploits de atacar inimigos sem se expor

### Sistema de Fog of War
- ✅ **Fog of War não revela mais através de paredes**
  - Corrigida lógica em `hasLineOfSight()` para verificar paredes ANTES de marcar tiles como visíveis
  - Paredes agora bloqueiam completamente a visão do player
  - Fix específico para bug onde aproximar-se de uma parede revelava tiles do outro lado
  - Tiles de parede não são mais marcados como visíveis mesmo com caminho limpo

### Sistema de Spawn
- ✅ **Goblins não ficam mais presos em paredes ao spawnar**
  - Implementado sistema de "Spawn Safety" com duração de 1 segundo (60 frames)
  - Durante spawn safety, goblins podem atravessar paredes temporariamente
  - Sistema automático de detecção e escape de paredes próximas
  - Algoritmo de repulsão baseado em distância para movimento natural
  - Velocidade de escape 3x mais rápida durante spawn safety
  - Após 1 segundo, colisões normais voltam automaticamente

## ⚙️ Melhorias de Performance

### Balanceamento
- ⏱️ **Tempo de respawn de famílias reduzido**
  - Famílias derrotadas agora reaparecem em **1 minuto** (era 3 minutos)
  - Mudança de `10800 frames` para `3600 frames` a 60 FPS
  - Gameplay mais dinâmico e engajante

## 🛠️ Mudanças Técnicas

### Arquitetura de Código
- Adicionado `import com.rpggame.core.GamePanel` em `Goblin.java`
- Método `isInSpawnSafety()` público em `Goblin` para comunicação com `Enemy`
- Método `handleSpawnSafety()` privado implementando lógica de escape
- `Enemy.updatePosition()` agora verifica spawn safety antes de aplicar colisões
- Projéteis verificam colisão com TileMap a cada frame

### Algoritmos Implementados
- **Algoritmo de Bresenham** para traçar linha entre projétil e alvo
- **Sistema de repulsão vetorial** para escape de paredes (Goblin spawn safety)
- **Detecção radial de obstáculos** em raio de 2 tiles

## 📊 Arquivos Modificados

```
src/com/rpggame/entities/Enemy.java          (+13 linhas)
src/com/rpggame/entities/Goblin.java         (+70 linhas)
src/com/rpggame/entities/Player.java         (+3 linhas)
src/com/rpggame/entities/Projectile.java     (+16 linhas)
src/com/rpggame/systems/EnemyManager.java    (+53 linhas)
src/com/rpggame/world/FogOfWar.java          (+10 linhas)
```

**Total:** 6 arquivos modificados, ~165 linhas adicionadas

## 🎯 Impacto no Gameplay

### Antes da v1.2.1
❌ Jogadores podiam atacar inimigos através de paredes sem risco  
❌ Fog revelava áreas inacessíveis ao aproximar de paredes  
❌ Goblins spawnavam presos em paredes, tornando-se alvos fáceis  
❌ Respawn de 3 minutos tornava o jogo lento após eliminar famílias  

### Depois da v1.2.1
✅ Combate balanceado - necessário posicionamento estratégico  
✅ Fog of War funcional - informação visual confiável  
✅ Spawn seguro - goblins sempre em posição combatível  
✅ Ritmo dinâmico - novas famílias em 1 minuto  

## 🔄 Compatibilidade

- ✅ Compatível com saves da v1.2.0
- ✅ Todas as features anteriores preservadas
- ✅ Sem breaking changes na API

## 📝 Notas de Desenvolvimento

Esta release foca em **correções de bugs críticos** reportados após o lançamento da v1.2.0:
1. Sistema de combate através de paredes
2. Revelação de fog através de obstáculos
3. Spawn de mobs em posições inválidas

Todas as correções foram implementadas com **testes manuais** validando:
- Projéteis param ao atingir paredes
- Fog não revela mais áreas bloqueadas
- Goblins escapam automaticamente de paredes em 1 segundo
- Performance mantida (60 FPS estável)

## 🚀 Próximos Passos (v1.3 - Planejado)

- Otimização de FogOfWar (remover Math.sqrt, desabilitar antialiasing)
- Sistema de partículas para efeitos visuais
- Mais variedade de inimigos
- Sistema de quests/missões

---

**Link do Repositório:** https://github.com/MrRafha/Top-view-rpg-game  
**Branch de Desenvolvimento:** `desenvolvimento`  
**Tag desta Release:** `v1.2.1`
