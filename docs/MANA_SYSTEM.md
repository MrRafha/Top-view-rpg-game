# Sistema de Mana - Atualização

## 📋 Resumo das Mudanças

Implementado sistema completo de consumo e regeneração de mana para as habilidades do jogo.

## ✅ Modificações Realizadas

### 1. **Classe Base Skill** (`Skill.java`)

**Alterações:**
- Adicionado campo `protected int manaCost`
- Criado construtor sobrecargado com parâmetro de custo de mana
- Modificado método `execute()` para verificar e consumir mana antes de executar habilidade
- Adicionado método `getManaCost()` para retornar o custo da habilidade

**Comportamento:**
```java
// Verificação antes de executar
if (manaCost > 0 && player.getCurrentMana() < manaCost) {
    System.out.println("❌ Mana insuficiente!");
    return false;
}

// Consumir mana após verificações
if (manaCost > 0) {
    player.consumeMana(manaCost);
}
```

---

### 2. **CharacterStats** (`CharacterStats.java`)

**Alterações:**
- Atualizado comentário de `wisdom` para "regeneração de mana" ao invés de "XP"
- Criado método `getManaRegen()` baseado em Sabedoria
- Mantido `getXpMultiplier()` como DEPRECATED para compatibilidade

**Fórmula de Regeneração:**
```java
public float getManaRegen() {
    int wisdomBonus = wisdom - BASE_ATTRIBUTE;
    return 0.5f + (wisdomBonus * 0.1f); // 0.5 base + 0.1 por ponto
}
```

**Tabela de Regeneração:**
| Sabedoria | Mana/segundo |
|-----------|--------------|
| 5         | 0.5          |
| 10        | 1.0          |
| 15        | 1.5          |
| 20        | 2.0          |
| 25        | 2.5          |

---

### 3. **Player** (`Player.java`)

**Alterações:**
- Adicionado campo `private int manaRegenTimer = 0`
- Criado método `consumeMana(int amount)` que:
  - Reduz mana atual
  - Mostra texto flutuante azul "-X MP"
- Criado método privado `regenerateMana()` que:
  - Regenera mana a cada 60 frames (1 segundo)
  - Usa `stats.getManaRegen()` para calcular quantidade
- Chamada de `regenerateMana()` adicionada no final do `update()`

**Implementação:**
```java
private void regenerateMana() {
    if (currentMana >= maxMana) {
        manaRegenTimer = 0;
        return;
    }
    
    manaRegenTimer++;
    
    if (manaRegenTimer >= 60) {
        manaRegenTimer = 0;
        float manaRegen = stats.getManaRegen();
        int manaToRegen = (int) Math.ceil(manaRegen);
        currentMana = Math.min(maxMana, currentMana + manaToRegen);
    }
}
```

---

### 4. **Habilidades Atualizadas**

#### Slot 1 - Custo: 20 Mana

| Classe  | Habilidade          | Custo | Cooldown |
|---------|---------------------|-------|----------|
| Mago    | Bola de Fogo        | 20    | 30s      |
| Arqueiro| Flecha Perfurante   | 20    | 20s      |
| Guerreiro| Golpe Horizontal   | **0** | 15s      |

#### Slot 2 - Custo: 30 Mana

| Classe  | Habilidade            | Custo | Cooldown |
|---------|-----------------------|-------|----------|
| Guerreiro| Intimidação Colossal | 30    | 10s      |
| Mago    | Congelamento          | 30    | 5s       |
| Arqueiro| Salto Veloz           | 30    | 6s       |

**Arquivos Modificados:**
- `FireballSkill.java`
- `PiercingArrowSkill.java`
- `IntimidatingShoutSkill.java`
- `FreezingSkill.java`
- `QuickDashSkill.java`

---

### 5. **UI - Tela de Customização** (`AttributeCustomizationScreen.java`)

**Alterações:**
- Descrição de Sabedoria alterada:
  - ❌ Antes: `"(Campo de visão e XP +1% a cada 2 pts)"`
  - ✅ Agora: `"(Campo de visão e Regen de Mana)"`

- Label de bônus atualizado:
  - ❌ Antes: `"Visão/XP: +X%"`
  - ✅ Agora: `"Visão | Regen Mana: +X.X/s"`

**Exemplo Visual:**
```
┌─────────────────────────────────────────────┐
│ Sabedoria                                   │
│ (Campo de visão e Regen de Mana)           │
│ [ - ]  15  [ + ]  Visão | Regen Mana: +1.0/s│
└─────────────────────────────────────────────┘
```

---

### 6. **Documentação**

#### `SLOT2_SKILLS.md`
- Adicionada seção "Sistema de Mana"
- Documentado custos de todas as habilidades
- Explicada fórmula de regeneração
- Tabela de exemplos de regeneração por Sabedoria

#### `README.md`
- Atualizada tabela de atributos:
  - ❌ Antes: `"👁️ Aumenta experiência ganha e visão"`
  - ✅ Agora: `"👁️ Aumenta visão e regeneração de mana"`

---

## 🎮 Impacto no Gameplay

### Vantagens do Sistema

1. **Balanceamento Natural**: Habilidades poderosas agora têm limitações além de cooldown
2. **Build Diversity**: Sabedoria se torna importante para todas as classes (exceto builds de Warrior puro)
3. **Gestão de Recursos**: Jogadores precisam pensar estrategicamente sobre quando usar habilidades

### Estratégias por Classe

#### 🗡️ Guerreiro
- **Habilidade gratuita**: Golpe Horizontal não consome mana
- **Sabedoria**: Útil apenas se usar Intimidação Colossal frequentemente
- **Build**: Pode ignorar Sabedoria e focar em Força/Constituição

#### 🔮 Mago
- **Mais dependente**: Ambas habilidades consomem mana
- **Sabedoria**: Altamente recomendado (15-20)
- **Build**: Inteligência (dano/mana max) + Sabedoria (regen)

#### 🏹 Arqueiro
- **Média dependência**: Usa mana em ambas habilidades
- **Sabedoria**: Recomendado (10-15)
- **Build**: Destreza (dano) + Sabedoria moderada para sustain

---

## 🧪 Testes Necessários

- [ ] Verificar regeneração de mana com diferentes valores de Sabedoria
- [ ] Confirmar que texto flutuante "-X MP" aparece ao usar habilidades
- [ ] Testar que Golpe Horizontal (Warrior) não consome mana
- [ ] Verificar mensagem de "Mana insuficiente" quando não há mana
- [ ] Confirmar que cooldown não é ativado se não houver mana
- [ ] Testar regeneração durante combate e fora de combate
- [ ] Verificar que mana não ultrapassa máximo ao regenerar

---

## 📊 Valores de Referência

### Build Mago (Caster Heavy)
- **Inteligência**: 18 → Mana Max: ~165
- **Sabedoria**: 15 → Regen: 1.5/s
- **Sustain**: Pode usar Bola de Fogo (20) a cada ~13s
- **Burst**: 8 Bolas de Fogo seguidas (160 mana)

### Build Arqueiro Balanceado
- **Inteligência**: 10 → Mana Max: 125
- **Sabedoria**: 12 → Regen: 1.2/s
- **Sustain**: Pode usar Flecha (20) a cada ~17s
- **Dash**: Salto Veloz (30) disponível frequentemente

### Build Guerreiro Tanque (Ignorando Mana)
- **Inteligência**: 5 → Mana Max: 100
- **Sabedoria**: 5 → Regen: 0.5/s
- **Problema**: Intimidação (30) leva 60s para regenerar
- **Solução**: Usar apenas Golpe Horizontal (gratuito)

---

## 🔧 Compilação

```powershell
cd src
javac -encoding UTF-8 -d ../bin -cp ".;../bin" ^
  com/rpggame/systems/Skill.java ^
  com/rpggame/systems/CharacterStats.java ^
  com/rpggame/entities/Player.java ^
  com/rpggame/ui/AttributeCustomizationScreen.java ^
  com/rpggame/systems/skills/*.java
```

---

## 📝 Notas Técnicas

1. **Texto Flutuante**: Mana consumida aparece em azul claro `Color(100, 150, 255)`
2. **Timer**: Regeneração acontece a cada 60 frames (1 segundo a 60 FPS)
3. **Arredondamento**: `Math.ceil()` garante que sempre regenera pelo menos 1 mana
4. **Thread-safe**: Não há problemas de concorrência pois tudo roda na thread de rendering

---

**Data de Implementação:** 12 de Dezembro de 2025  
**Versão:** 1.2.3+
