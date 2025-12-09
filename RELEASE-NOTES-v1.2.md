# Release Notes - v1.2.0

**Data de Lançamento:** 9 de Dezembro de 2025  
**Versão:** 1.2.0 - "Conselho Goblin"

---

## 🎮 Novidades Principais

### 🏛️ Sistema de Conselho Goblin
Os líderes goblin agora se reúnem para tomar decisões estratégicas que mudam completamente a dinâmica do jogo!

**3 Decisões Possíveis:**

1. **⚔️ Aliança Contra o Jogador** (30% base)
   - Duração: 5 minutos
   - Todos os goblins param de lutar entre si
   - TODOS atacam apenas o jogador
   - Timer visível na HUD

2. **👑 Império Goblin** (20% base)
   - Todas as famílias se unem permanentemente
   - Um único líder comanda todos
   - Nome muda para "IMPÉRIO GOBLIN"
   - Efeito permanente no jogo

3. **🔧 Avanço Tecnológico** (50% base)
   - Força de todos os goblins DOBRA
   - Efeito permanente
   - Todos os ataques causam o dobro de dano

**Mecânicas do Conselho:**
- Reuniões automáticas a cada 45-60 segundos
- Requer mínimo de 2 famílias vivas
- +30% chance quando uma família é destruída
- Indicadores visuais na HUD

---

## 👑 Hierarquia de Clãs

### Decisões do Líder
- O líder toma decisões estratégicas para toda a família
- Considera território, intimidação do jogador e agressividade
- Membros do clã seguem as ordens do líder

**Níveis de Obediência:**
- **Goblin Comum:** 100% obediente
- **Goblin Tímido:** 100% obediente (tende a fugir)
- **Goblin Agressivo:** ~70% obediente (mais independente)

### Sistema de Intimidação
- Carisma do jogador influencia decisões dos líderes
- Níveis mais altos = maior intimidação
- Famílias podem recuar fora do território

---

## 🔄 Sistema de Respawn de Famílias

### Respawn Automático
- Quando família é derrotada → timer de 3 minutos inicia
- Nova família spawna automaticamente
- Máximo de 3 famílias simultâneas

### 20 Nomes Únicos de Famílias
```
Clã Pedra Negra         Horda Osso Quebrado      Família Lâmina Enferrujada
Tribo Dente Afiado      Tribo Sangue Podre       Tribo Crânio Rachado
Família Garra Suja      Clã Veneno Noturno       Clã Língua Venenosa
Bando Olho Vermelho     Bando Fogo Negro         Horda Grito Selvagem
Clã Sombra Verde        Bando Lua Sangrenta      Família Espinho Negro
Tribo Pântano Escuro    Clã Chifre Retorcido     Horda Presa Afiada
Bando Cinza Sombria     Família Caverna Profunda
```

**Características:**
- Seleção aleatória sem repetição
- Lista reseta quando todos os nomes forem usados
- 20% chance de iniciar em guerra com outra família

---

## 🎯 Melhorias de Gameplay

### Detecção de Família Derrotada
- ✅ Cabana destruída = família eliminada automaticamente
- ✅ Todos os goblins da família morrem instantaneamente
- ✅ Sistema de recompensa (100 XP por cabana)

### Logs Informativos
```
🏛️ CONSELHO GOBLIN CONVOCADO
⚔️ DECISÃO: ALIANÇA CONTRA O JOGADOR!
👑 IMPÉRIO GOBLIN FORMADO!
🔧 AVANÇO TECNOLÓGICO ATIVO!
💀 Família goblin destruída!
🆕 NOVA FAMÍLIA GOBLIN CHEGANDO
⏳ Nova família em X segundos...
```

### Interface Visual (HUD)
- Indicador de aliança ativa com timer
- Status do império goblin
- Notificação de avanço tecnológico
- Contador de famílias ativas

---

## 🔧 Correções e Ajustes

### Sistema de Guerra
- ✅ Cessar fogo automático em alianças
- ✅ Cessar fogo automático no império
- ✅ Melhor sincronização de estados

### Performance
- ✅ Sistema de detecção otimizado
- ✅ Logs de debug para diagnóstico
- ✅ Análise de performance documentada

---

## 📊 Estatísticas da Versão

- **Arquivos Modificados:** 5
- **Arquivos Novos:** 2 (`GoblinCouncil.java`, `PERFORMANCE_ANALYSIS.md`)
- **Linhas Adicionadas:** 642+
- **Linhas Removidas:** 15-

---

## 🎮 Como Jogar

### Observando o Conselho
1. Jogue normalmente e observe o console
2. Procure por: `"🏛️ ===== CONSELHO GOBLIN CONVOCADO ====="`
3. Veja a decisão tomada
4. Observe mudanças na HUD e comportamento dos goblins

### Destruindo Famílias
1. Mate todos os goblins de uma família
2. Cabana fica vulnerável (marcador amarelo)
3. Ataque a cabana com setas direcionais
4. Família é eliminada quando cabana é destruída
5. Nova família spawna em 3 minutos

### Estratégias
- **Alto Carisma:** Intimida líderes, reduz perseguições
- **Destruir Cabanas:** Acelera reuniões do conselho (+30%)
- **Durante Aliança:** FUJA! Todos os goblins te perseguirão
- **Império Formado:** Prepare-se para um exército unificado
- **Avanço Tech:** Cuidado! Dano dobrado de todos os goblins

---

## 🐛 Problemas Conhecidos

- Timer de respawn pode não disparar em casos raros (logs de debug adicionados)
- Performance pode diminuir com muitos goblins simultâneos

---

## 📝 Notas dos Desenvolvedores

Esta versão introduz um sistema político complexo para os goblins, tornando o jogo muito mais dinâmico e imprevisível. As decisões do conselho podem mudar completamente a situação do jogador em segundos!

O sistema de hierarquia faz os clãs agirem de forma mais coordenada e realista, com líderes tomando decisões estratégicas baseadas em múltiplos fatores.

---

## 🔜 Próximas Versões

- Otimizações de performance (FogOfWar, antialiasing)
- Mais tipos de decisões do conselho
- Sistema de diplomacia do jogador
- NPCs neutros
- Novas classes de personagem

---

**Download:** `RPG-Game-v1.2.jar`  
**Requisitos:** Java 11 ou superior  
**Compatibilidade:** Windows, Linux, macOS
