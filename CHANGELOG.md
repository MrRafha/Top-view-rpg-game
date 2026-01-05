# 📜 Changelog - Top-View RPG Game

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

---

## [2.4.0] - 2026-01-05

### ⚡ Adicionado
- **Novo Inimigo: Mimic** 🦎
  - 200 HP com comportamento de emboscada
  - Disfarce perfeito como baú fechado (ClosedChest.png)
  - Sistema de estados: DISGUISED → REVEALING → ATTACKING → ACTIVE
  - Ataque de língua com alcance de 150 pixels e 10 de dano
  - Velocidade da língua ajustada (5.0) para melhor jogabilidade
  - Mecânica de captura: Mimic se puxa até o jogador ao acertar
  - Sistema de grab/suffocation com escape por SPACE (15x)
  - Dano contínuo de 10/segundo enquanto capturado
  - Barra de progresso visual para tentativas de escape
  - Sprites: ClosedChest.png (disfarce) e Mimic.png (revelado)
  - Animação de língua vermelha durante ataques
- **Mapa Secret Area** 🗺️
  - Novo mapa secreto acessível via portal na vila
  - Contém Mimic e baú normal para testes
  - OST exclusiva: SecretAreaOST.wav

### 🔧 Melhorado
- **Sistema de Lockpicking - Reimplementação Completa** 🔓
  - Substituído sistema de ângulos por **pontos discretos** (0-359)
  - 360 pontos renderizados como círculos de 3px
  - 10 pontos consecutivos aleatórios para zona verde
  - Marcador dourado percorre os pontos
  - Verificação determinística: `greenZoneIndices.contains(currentMarkerIndex)`
  - Visual mais claro com todos os pontos visíveis
  - Elimina bugs de conversão de coordenadas
- **Sistema de Audio** 🎵
  - Música para automaticamente quando jogador morre
  - Evita sobreposição com tela de game over

### 🐛 Corrigido
- Lockpicking detectando falha mesmo com marcador na zona verde
- Imprecisões causadas por conversões entre sistemas de ângulos
- Hitbox da língua do Mimic (aumentada de 40px para 60px)
- Sprites incorretos do Mimic (não usa mais MimicAttack1/2)
- Ataque de língua não funcionando durante perseguição
- Sistema de escape com SPACE não respondendo
- Música continuando após morte do jogador

---

## [2.3.0] - 2025-12-26

### ⚡ Adicionado
- **Boss Golem - Guardião do Equilíbrio** 🗿
  - 500 HP com 50% de resistência a dano
  - Sistema de visão com ray casting
  - Spawn inteligente fora da visão do jogador (até 50 tentativas)
  - Pausa automática de goblins durante a batalha
  - Modo Enrage aos 30% HP ou após 1 minuto
  - 8 novos sprites (4 direcionais + 4 pedras animadas)
  - Animação de balanço suave durante movimento
  - Ataque de pedras com área 2x2 tiles
  - Sistema de preparação visual (1.5s windup)
  - Kiting inteligente mantendo distância ideal
  - 500 XP de recompensa
- **Sistema de Quests** 📋
  - QuestManager completo
  - Tipos: KILL, COLLECT, TALK
  - Quest UI com progresso em tempo real
  - Quest Choice Box para aceitar/recusar
- **Sistema de Loja e Economia** 💰
  - Shop UI funcional
  - Gold UI mostrando recursos
  - Merchant NPC interativo
  - Sistema de compra/venda
- **Itens Equipáveis** ⚔️
  - Old Sword (corpo a corpo)
  - Old Bow (alcance)
  - Old Staff (mágico)
  - Sistema EquippableItem para expansões
- **Developer Console** 🛠️
  - Comando `kill goblins all/N` (elimina e concede XP)
  - Comando `spawngolem` (spawn forçado para testes)
  - Comando `maxlevel` (força level 10, libera todas skills)
  - Comando `noclip` (remove colisão e aumenta velocidade)
  - Tecla V para debug de campo de visão

### 🔧 Modificado
- **Enemy.java** com método abstrato `initializeStats()`
- **Sistema de carregamento de sprites** com dual-path (JAR + desenvolvimento)
- **EnemyManager.java** com lógica de spawn do Golem
- **Player.java** com suporte melhorado a floating texts
- **Golem spawn** após todas famílias goblin derrotadas (50% chance)

### 🐛 Corrigido
- ConcurrentModificationException em múltiplos locais (update e render)
- Sistema de carregamento de sprites com fallback para arquivos
- Map ID check usando `startsWith()` ao invés de `equals()`
- Problemas de compilação com métodos abstratos
- Sprite loading do Golem e GolemStone

### 📊 Estatísticas
- 41 arquivos alterados
- 3468 linhas adicionadas
- 74 linhas removidas

---

## [2.2.0] - 15.12.2025

### ⚡ Adicionado
- **Nova feature de inventário e poções.**
  - **Poção de cura (cura 50 pv);**
  - **Poção de mana (Restaura 50 MP).**
- **Ambientação primaria no vilarejo (mapa inicial).**
- **Console para melhorar os testes de gameplay (') com comandos funcionais:**
  - **Comando maxlevel (força o aumento de level do player até o 10 liberando todas as skills com level);**
  - **Comando Noclip (deixa o player sem colisão e aumenta a velocidade).**

---

## [2.1.0] - 2025-12-12

### ⚡ Adicionado
- **Habilidades Ultimates (Slot 4)** para todas as classes
  - ⚔️ Guerreiro: Fúria Berserker (+100% velocidade, regen, -50% dano, imunidade)
  - 🔮 Mago: Meteoro Arcano (sistema 3 fases, AoE massivo, knockback, queimadura)
  - 🏹 Caçador: Chuva de Flechas (20 flechas, sangramento, slow 40%)
- **Novos tiles de ambiente**:
  - `WATER` - Água não atravessável mas transparente
  - `WALKABLE_WATER` - Vitórias-régias atravessáveis
- **Sistema de partículas** para Fúria Berserker
- **Efeitos visuais avançados**:
  - Animação em 3 fases do meteoro
  - Trails luminosos nas flechas
  - Ondas de choque e explosões
  - Gotas de sangramento visual
  - Auras pulsantes

### 🔧 Modificado
- **Fog of War** agora permite visão através de água (apenas WALL e STONE bloqueiam)
- **Player.java** com sistema de berserk e imunidades
- **Enemy.java** com melhor controle de estados (berserk immunity)
- **TileType.java** com suporte a WALKABLE_WATER
- **SkillManager.java** com slot 4 para todas as classes

### 🐛 Corrigido
- Sistema de charm funcionando corretamente
- Goblin.updateAI verifica charm antes de executar
- Paths do MapManager corrigidos
- ClassCastException do Player resolvido

---

## [2.0.0] - 2025-12-11

### ⚡ Adicionado
- **Sistema de habilidades completo**
- **Habilidades por classe** (slots 1-3):
  - ⚔️ Guerreiro: Corte Horizontal, Grito Intimidante, Investida do Touro
  - 🔮 Mago: Bola de Fogo, Congelamento, Encantamento
  - 🏹 Caçador: Flecha Perfurante, Dash Rápido, Armadilha Mortal
- **Sistema de mana** (40 base + INT×5)
- **Sistema de cooldown** visual nos slots
- **Estados de inimigos**: frozen, feared, charmed, stunned
- **Sistema de aprendizado** através de NPCs
- **Interface de slots** visual no canto direito

### 🔧 Modificado
- Sistema de combate com integração de habilidades
- UI redesenhada com slots de habilidades
- Sistema de NPCs com ensino de habilidades

---

## [1.2.2] - 2025-11-28

### 🐛 Corrigido
- Correções de bugs menores no sistema de combate

---

## [1.2.1] - 2025-11-25

### 🐛 Corrigido
- Bugs no sistema de portais
- Melhorias na transição entre mapas

---

## [1.2.0] - 2025-11-20

### ⚡ Adicionado
- Sistema de portais entre mapas
- Novos mapas: village, goblin territories
- Sistema de transição suave entre mapas

---

## [1.1.0] - 2025-11-10

### ⚡ Adicionado
- Sistema de fog of war (névoa de guerra)
- Sistema de câmera dinâmica
- Melhorias no sistema de NPCs

---

## [1.0.0] - 2025-11-01

### ⚡ Inicial
- Sistema básico de RPG top-down
- Criação de personagem com classes
- Sistema de combate básico
- Sistema de inimigos (Goblins)
- Sistema de NPCs básico
- Mapas iniciais
