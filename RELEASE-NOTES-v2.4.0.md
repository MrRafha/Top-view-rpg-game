# 🎮 Top-View RPG Game - Release Notes v2.4.0

## 🦎 **MIMIC ENEMY & LOCKPICKING OVERHAUL**

**Data de Lançamento:** 5 de Janeiro de 2026  
**Versão:** 2.4.0  
**Código:** "Hidden Treasures"

---

## 🌟 **Principais Novidades**

### 🧰 **Novo Inimigo: Mimic**
O perigoso baú mimético foi adicionado ao jogo com mecânicas únicas de ataque e captura!

#### **Características do Mimic**
- **200 HP** com comportamento de emboscada
- **Disfarce perfeito** como baú fechado (ClosedChest.png)
- **Sistema de estados**: DISGUISED → REVEALING → ATTACKING → ACTIVE
- **Detecção inteligente** quando o jogador se aproxima

#### **Sistema de Combate**
- **Ataque de Língua**:
  - Alcance de 150 pixels
  - Velocidade reduzida (5.0) para dar tempo de reação
  - Hitbox de 60px para detectar colisões
  - 10 de dano por acerto
  - Animação visual da língua estendendo e retraindo

- **Mecânica de Captura**:
  - Quando acerta com a língua, o Mimic **se puxa até o jogador** (velocidade 8.0)
  - **Sistema de grab/suffocation** quando alcança o jogador
  - Jogador precisa **apertar SPACE 15 vezes** para escapar
  - Dano contínuo de 10 por segundo enquanto capturado
  - Barra de progresso visual mostrando tentativas de escape (0-15)

#### **Visual e Animação**
- Sprite inicial: Baú fechado (indistinguível de baús normais)
- Sprite revelado: Mimic.png com aparência ameaçadora
- Animação de língua vermelha durante ataques
- Efeito de puxada quando acerta o jogador

---

### 🔓 **Sistema de Lockpicking Reimplementado**

O sistema de lockpicking foi completamente reescrito usando **pontos discretos** para eliminar bugs de detecção!

#### **Nova Arquitetura**
- **360 pontos discretos** (índices 0-359) formando o círculo
- Cada ponto renderizado como **círculo de 3px**
- **10 pontos consecutivos** sorteados aleatoriamente para zona verde
- Verificação simples por índice ao invés de cálculos de ângulos

#### **Melhorias**
- ✅ **Zero bugs de detecção** - sistema determinístico
- ✅ **Visual mais claro** - todos os pontos visíveis
- ✅ **Marcador dourado** destacado movendo-se pelos pontos
- ✅ **Zona verde bem definida** - 10 pontos consecutivos
- ✅ **Código mais simples** - sem conversões de coordenadas

#### **Como Funciona**
1. 360 pontos brancos formam o círculo
2. 10 pontos consecutivos ficam verdes (zona de sucesso)
3. Marcador dourado percorre os pontos
4. Jogador aperta **F** quando marcador está no verde
5. Verificação: `greenZoneIndices.contains(currentMarkerIndex)`

---

## 🎵 **Melhorias de Audio**

### **Música na Morte do Jogador**
- Música agora **para automaticamente** quando o jogador morre
- Evita sobreposição de som com tela de game over
- `musicManager.stopMusic()` integrado ao sistema de morte

---

## 🐛 **Correções de Bugs**

### **Sistema de Lockpicking**
- ❌ **Bug corrigido**: Detecção falhando mesmo com marcador visualmente na zona verde
- ❌ **Bug corrigido**: Conversões de ângulos causando imprecisões
- ✅ **Solução**: Reimplementação completa com sistema de índices discretos

### **Mimic**
- ✅ Hitbox da língua aumentada de 40px para 60px
- ✅ Sprites corretos em todas as fases (não usa mais MimicAttack1/2)
- ✅ Ataque de língua funciona corretamente durante perseguição
- ✅ Sistema de escape com tecla SPACE totalmente funcional

### **Audio**
- ✅ Música não continua tocando após morte do jogador

---

## 🗺️ **Mapas e Conteúdo**

### **Secret Area**
- Mapa secreto com **Mimic** e **baú normal**
- Acessível via portal na vila
- Área de testes para novas mecânicas
- OST exclusiva: SecretAreaOST.wav

---

## 📊 **Estatísticas de Desenvolvimento**

### **Commits desta Versão**
- `87a0c08` - Reimplementa sistema de lockpicking com pontos discretos
- `dd18e49` - Reduz velocidade da língua do Mimic para dar tempo de reação
- `1dfe627` - Corrige sistema de lockpicking, hitbox do Mimic e música na morte
- `1f7417e` - Sistema de menu, música, novo mapa secret_area

### **Arquivos Modificados**
- `src/com/rpggame/ui/LockpickingMinigame.java` - Reimplementação completa
- `src/com/rpggame/enemies/mimic/Mimic.java` - Novo inimigo com sistema de grab
- `src/com/rpggame/core/GamePanel.java` - Integração de escape e música
- `maps/secret_area.txt` - Novo mapa secreto

---

## 🎯 **Próximos Passos (v2.5.0)**

### **Planejado**
- [ ] Mais tipos de baús mimicos (variações)
- [ ] Sistema de loot para baús
- [ ] Mais inimigos especiais
- [ ] Expansão do mapa secret_area
- [ ] Novas habilidades de classe

---

## 🙏 **Agradecimentos**

Obrigado a todos que testaram e reportaram bugs no sistema de lockpicking!
O feedback foi essencial para a reimplementação do sistema.

---

## 📝 **Notas Técnicas**

### **Sistema de Pontos Discretos**
```java
// Estrutura simplificada
private static final int TOTAL_POINTS = 360;
private Set<Integer> greenZoneIndices = new HashSet<>();
private int currentMarkerIndex = 0;

// Verificação determinística
success = greenZoneIndices.contains(currentMarkerIndex);
```

### **Mimic State Machine**
```
DISGUISED (detecta player) 
    ↓
REVEALING (animação 2s)
    ↓  
ATTACKING (língua inicial)
    ↓
ACTIVE (perseguição + ataques)
    ↓
GRAB (captura + escape)
```

---

**Aproveite as novas mecânicas de combate e teste suas habilidades contra o Mimic! 🦎**

---

*Versão anterior: [v2.3.0 - Golem Boss & Endgame Update](RELEASE-NOTES-v2.3.0.md)*
