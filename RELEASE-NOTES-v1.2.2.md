# Release Notes - Version 1.2.2

**Data de Lançamento:** 10 de Dezembro de 2025

## 🎮 Novidades Principais

### Sistema de Portais Bidirecionais
- ✨ **Portais entre mapas**: Implementado sistema completo de portais bidirecionais
- 🌀 **Transição circular animada**: Efeito visual suave de fade circular (iris effect) durante mudanças de mapa
- 🗺️ **Múltiplos mapas**: Suporte para navegação entre Territórios Goblin e Vila

### Novo Mapa: Vila com Praia
- 🏖️ **Mapa da Vila (25x25)**: Novo cenário com praia, água (V) e areia (S)
- 👥 **NPCs exclusivos da Vila**:
  - 💰 Merchant NPC - Comerciante
  - 🧑 Villager NPC - Aldeão
  - 🧙 Wise Man NPC - Sábio
- 🌊 **Tema praia**: Área costeira com água e areia

### Sistema de Spawn Aprimorado
- 📍 **Spawn consistente**: Pontos de spawn específicos por mapa
  - Territórios Goblin: tile (12, 3) - entrada dos portais
  - Vila: tile (12, 22) - acima dos portais
- ⚔️ **Guards posicionados estrategicamente**: 2 Guards protegendo entrada da vila nos territórios (tiles 10,3 e 14,3)
- 🚫 **Área de spawn limpa**: Removidos obstáculos que bloqueavam spawn

## 🔧 Melhorias Técnicas

### Arquitetura de Mapas
- 🆔 **Sistema baseado em IDs**: MapManager com identificação por string ("village", "goblin_territories", "cave")
- 🎯 **Detecção escalável**: Substituído sistema de detecção por tiles por IDs para melhor manutenibilidade
- 🏗️ **Preparado para expansão**: Arquitetura pronta para adicionar novos mapas facilmente

### Gerenciamento de NPCs
- 🗺️ **NPCs específicos por mapa**: Sistema que spawna NPCs diferentes baseado no mapa atual
- 🛡️ **Guards apenas nos Territórios**: Guards aparecem somente em áreas de perigo
- 🏘️ **NPCs pacíficos na Vila**: Merchant, Villager e Wise Man exclusivos da vila

### Sistema de Goblins
- ✅ **Goblins somente em áreas apropriadas**: Não spawnam em mapas seguros (vila, caverna)
- 🏠 **Limpeza de estruturas**: Cabanas goblin removidas ao trocar de mapa
- 🔄 **EnemyManager aprimorado**: Método `setCurrentMapId()` para controle contextual

### Transições de Mapa
- 🎬 **MapTransition melhorado**: Efeito de iris circular usando java.awt.geom.Area
- ⚡ **Performance otimizada**: Transição suave a 0.03f de velocidade
- 🖤 **Fade out/in**: Tela escurece em círculo e clareia ao revelar novo mapa

## 🐛 Correções de Bugs

### Spawn System
- ✅ **Corrigido spawn em portais**: Player agora spawna nos pontos corretos definidos no MapManager
- ✅ **Removidos obstáculos**: Pedras que bloqueavam spawn nos Territórios Goblin removidas
- ✅ **Consistência de spawn**: Sempre retorna ao mesmo ponto ao voltar de outro mapa

### NPCs e Inimigos
- ✅ **NPCs não repetem entre mapas**: Cada mapa tem seus NPCs exclusivos
- ✅ **Goblins não spawnam na Vila**: Sistema de detecção por ID previne spawn em mapas seguros
- ✅ **Estruturas limpas**: Cabanas goblin removidas ao sair dos Territórios

### Portais
- ✅ **Destinos corretos**: Portais da Vila agora levam aos Territórios (não mais loop)
- ✅ **Transição visível**: Efeito circular aparece corretamente (não mais tela preta instantânea)

## 📁 Arquivos Modificados

### Core
- `GamePanel.java` - Sistema de NPCs por mapa, transições, spawn management
- `Game.java` - Inicialização do sistema de portais

### World
- `TileMap.java` - `setupPortals(String mapId)`, `reloadMap()` com ID
- `MapManager.java` - Definição de spawn points por mapa
- `MapTransition.java` - Novo efeito circular de transição
- `ResourceResolver.java` - Carregamento de recursos de mapas

### Systems
- `EnemyManager.java` - `currentMapId` field, `setCurrentMapId()`, detecção por ID

### Maps
- `village.txt` - Novo mapa 25x25 com praia
- `goblin_territories_25x25.txt` - Atualizado com portais e spawn limpo

## 🎯 Compatibilidade

- ✅ Java 8+
- ✅ Mantém compatibilidade com saves anteriores
- ✅ Sistema de atributos preservado
- ✅ Experiência e progressão mantidos

## 📊 Estatísticas da Versão

- **Linhas de código modificadas**: ~150+
- **Novos arquivos**: MapTransition.java, village.txt, RELEASE-NOTES-v1.2.2.md
- **Arquivos atualizados**: 8 arquivos principais
- **Bugs corrigidos**: 9
- **Novas funcionalidades**: 5 sistemas principais

## 🚀 Próximos Passos

Planejado para versões futuras:
- 🗺️ Mapa de Caverna (cave) com desafios únicos
- 🏰 Mais estruturas interativas
- 💬 Sistema de diálogo expandido
- 🎒 Inventário e itens
- 🏪 Sistema de comércio funcional

---

**Versão anterior:** 1.1.0  
**Repositório:** [Top-view-rpg-game](https://github.com/MrRafha/Top-view-rpg-game)  
**Branch:** desenvolvimento → main
