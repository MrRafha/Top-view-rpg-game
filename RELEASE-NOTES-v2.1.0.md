# 🎮 Top-View RPG Game - Release Notes v2.1

## ⚡ **ULTIMATE SKILLS UPDATE**

**Data de Lançamento:** 12 de Dezembro de 2025  
**Versão:** 2.1.0  
**Código:** "Skills & Magic Update"

---

## ⭐ **Principais Novidades**

### 🌟 **Habilidades Ultimates (Slot 4)**
Cada classe agora possui uma habilidade ultimate devastadora desbloqueada no nível 10!

#### ⚔️ **GUERREIRO - Fúria Berserker**
- **Transformação em estado de fúria pura por 8 segundos**
- 🏃 +100% velocidade de movimento
- 💚 Regenera 5% de vida por segundo
- 🛡️ -50% de dano recebido
- 🔒 Imunidade a stun e fear
- ✨ Partículas de fúria vermelhas e aura brilhante
- 💎 **65 mana | ⏱️ 45s cooldown**

#### 🔮 **MAGO - Meteoro Arcano**
- **Invoca um meteoro massivo que cai em área 4x4 tiles**
- 📍 Sistema de 3 fases (Mira → Queda → Impacto)
- 💥 Dano central: 80 + (INT × 5)
- 🌊 Dano externo: 50 + (INT × 3)
- 💨 Empurra inimigos para fora da explosão
- 🔥 Queimadura contínua: 5 dano/s por 3 segundos
- ✨ Meteoro com rastro de fogo, explosão com ondas de choque
- 💎 **70 mana | ⏱️ 50s cooldown**

#### 🏹 **CAÇADOR - Chuva de Flechas**
- **20 flechas caem do céu em área 5x5 tiles durante 4 segundos**
- 🎯 Cada flecha: 15 + (DEX × 1.5) de dano
- 🩸 30% chance de sangramento (3 dano/s por 5s)
- 🐌 Reduz velocidade dos inimigos em 40% durante a chuva
- 👁️ Revela inimigos invisíveis na área
- ✨ Círculo verde brilhante, flechas com trail luminoso, partículas flutuantes
- 💎 **65 mana | ⏱️ 40s cooldown**

---

## 🎨 **Melhorias Visuais**

### ✨ **Efeitos Visuais Avançados**
- **Sistema de partículas** para Fúria Berserker (partículas vermelhas/laranjas)
- **Animação em 3 fases** para Meteoro Arcano
- **Ondas de choque** e explosões com alpha blending
- **Trails luminosos** nas flechas da Chuva de Flechas
- **Efeitos de sangramento** visual com gotas caindo
- **Auras circulares** pulsantes durante buffs

### 🌊 **Novos Tiles de Ambiente**
- **WATER** (`Wather.png`) - Água não atravessável mas transparente
- **WALKABLE_WATER** (`WalknableWather.png`) - Vitórias-régias atravessáveis
- 👁️ **Fog of War atualizado** - água não bloqueia visão (apenas WALL e STONE)

---

## 🎯 **Balanceamento**

### ⚔️ **Sistema de Estados Aprimorado**
- **Imunidade durante Berserk** - não pode ser atordoado ou amedrontado
- **Redução de dano** balanceada para evitar invencibilidade
- **Slow effect** nas ultimates para controle de área
- **DoT (Damage over Time)** com timers precisos

### 🧙 **Sistema de Mana**
- Custos altos para ultimates (60-70 mana)
- Cooldowns longos (40-50 segundos)
- Balanceamento baseado em inteligência/destreza

---

## 🛠️ **Mudanças Técnicas**

### 🔧 **Arquitetura**
- **ArrowRainSkill.java** - Sistema de flechas caindo com física
- **ArcaneMeteorSkill.java** - Máquina de estados com 3 fases
- **BerserkFurySkill.java** - Sistema de buffs com partículas
- **Refatoração no Enemy.java** - Controle de estados aprimorado
- **Player.java** - Integração com sistema de berserk

### 🎨 **Rendering**
- Uso avançado de `AlphaComposite` para transparências
- `BasicStroke` para linhas grossas e efeitos
- Polígonos para formas complexas (pontas de flechas)
- Gradientes radiais para explosões

---

## 🐛 **Correções de Bugs**

### ✅ **Fixes**
- Corrigido freeze do FreezingSkill para formato linear de 2 tiles
- Sistema de charm agora funciona corretamente (Enemy.updateAI)
- Goblin.updateAI verifica estado de charm antes de executar
- Paths do MapManager corrigidos
- ClassCastException do Player resolvido

---

## 📊 **Estatísticas do Update**

- ✨ **3 novas habilidades ultimates**
- 🎨 **2 novos tipos de tiles**
- 🔧 **5 arquivos principais modificados**
- 🎯 **4 novos sistemas** (partículas, meteoro, chuva, berserk)
- 📝 **1000+ linhas de código adicionadas**

---

## 🎮 **Como Usar as Ultimates**

1. **Alcance o nível 10** com seu personagem
2. **Pressione a tecla 4** para ativar sua ultimate
3. **Gerencie sua mana** - ultimates custam muito!
4. **Aguarde o cooldown** antes de usar novamente
5. **Combine com outras habilidades** para combos devastadores

---

## 📋 **Próximos Passos (v2.2)**

- 🎯 Mais efeitos visuais para habilidades base
- 🏆 Sistema de conquistas para uso de ultimates
- 💥 Combos entre habilidades
- 🎨 Partículas adicionais para outros elementos
- 📊 Estatísticas de combate detalhadas

---

## 📥 **Download**

Baixe o arquivo `Top-View-RPG-v2.1.0.jar` na pasta `release/`

---

**Divirta-se explorando as novas habilidades ultimates!** 🎉

*Desenvolvido com ☕ e 💚 em Java*
