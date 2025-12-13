# 📜 Changelog - Top-View RPG Game

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

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
- FreezingSkill agora é linear de 2 tiles (não circular)
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
