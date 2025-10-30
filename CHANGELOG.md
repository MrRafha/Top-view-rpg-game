# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
e este projeto segue [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-10-30

### Adicionado
- Sistema básico de RPG 2D top-down
- 3 classes de personagem (Guerreiro, Mago, Caçador)
- Sistema de atributos completo com 6 stats
- Sistema de combate com projéteis
- Fog of War com algoritmo de line-of-sight
- Sistema de colisão robusto
- Mapas customizáveis via arquivos .txt
- 6 tipos de terreno diferentes
- Interface unificada de criação de personagem
- Sistema de câmera suave
- Textos flutuantes para feedback
- Sistema de evasão baseado em destreza

### Características Técnicas
- Resolução 1024x800
- Mapa 15x15 tiles (48px cada)
- 60 FPS target
- Engine baseada em Java Swing
- Arquitetura modular e extensível

### Sistema de Classes
- **Guerreiro**: Foco em força e resistência
- **Mago**: Especializado em magia e conhecimento  
- **Caçador**: Ágil e preciso à distância

### Tipos de Terreno
- Grama (caminhável)
- Paredes (bloqueiam movimento e visão)
- Água (não caminhável)
- Pedra (obstáculo)
- Terra (caminhável)
- Areia (caminhável)

### Controles
- WASD / Setas: Movimento
- Espaço: Atacar
- Mouse: Foco da janela

## [Unreleased]

### Planejado para Próximas Versões
- [ ] Sistema de NPCs e diálogos
- [ ] Inventário e itens
- [ ] Múltiplos mapas/níveis
- [ ] Sistema de save/load
- [ ] Efeitos sonoros
- [ ] Sprites animados
- [ ] Editor de mapas in-game
- [ ] Multiplayer local
- [ ] Sistema de quests
- [ ] Melhorias de performance

### Melhorias Técnicas Planejadas
- [ ] Refatoração para Entity Component System
- [ ] Asset pipeline para sprites reais
- [ ] Sistema de configuração por arquivos
- [ ] Testes unitários abrangentes
- [ ] Documentação de API completa