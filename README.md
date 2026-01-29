# Top-view RPG Game 🎮

Um RPG 2D em Java com sistema completo de habilidades, classes únicas e combate estratégico.

![Java](https://img.shields.io/badge/Java-11+-orange)
![Version](https://img.shields.io/badge/Version-2.4.0-brightgreen)
![Status](https://img.shields.io/badge/Status-Complete-green)
![License](https://img.shields.io/badge/License-MIT-blue)
---

## 🎥 Demonstração da Gameplay

[![Veja o vídeo no YouTube](https://img.youtube.com/vi/wAn6dHbmP9M/0.jpg)](https://youtu.be/wAn6dHbmP9M)

📌 **Clique na imagem acima** para assistir ao vídeo da gameplay.

---

## 🚀 Como Jogar

### 📦 Versão Executável (Recomendado)

**Baixe e jogue em segundos:**

1. **Baixe** o arquivo `RPG-Game-v2.4.0.jar` na pasta [releases](https://github.com/MrRafha/Top-view-rpg-game/releases)
2. **Execute** o JAR:
   ```bash
   java -jar RPG-Game-v2.4.0.jar
   ```

**✅ Requisitos**: 
- Java 11 ou superior instalado
- Resolução mínima: 1024x800

### 🛠️ Desenvolvimento (Código Fonte)

Para desenvolvedores que querem modificar o código:

```bash
# Clone o repositório
git clone https://github.com/MrRafha/Top-view-rpg-game.git
cd Top-view-rpg-game

# Compile (Windows PowerShell)
cd src
javac -d ..\bin -encoding UTF-8 com\rpggame\core\Game.java

# Execute  
cd ..
java -cp bin com.rpggame.core.Game
```

## 🎮 Controles

| Tecla | Ação |
|-------|------|
| **WASD** | Movimentação do personagem |
| **Espaço** | Atacar inimigos |
| **1, 2, 3, 4** | Usar habilidades especiais |
| **E** | Interagir com NPCs |
| **C** | Abrir tela de características |
| **V** | Toggle campo de visão (modo debug) |

## ✨ **NOVIDADE v2.0:** Sistema de Habilidades

### 🔥 Habilidades Especiais por Classe

| Classe | Habilidade | Efeito | Cooldown |
|--------|-----------|--------|----------|
| **🗡️ Guerreiro** | Golpe Horizontal | Ataque semicircular que atinge múltiplos inimigos | 15s |
| **🧙 Mago** | Bola de Fogo | Projétil explosivo com dano em área | 30s |
| **🏹 Arqueiro/Hunter** | Flecha Perfurante | Projétil que atravessa todos os inimigos em linha | 20s |

### 🎮 Interface de Habilidades

- **4 Slots visuais** no canto direito da tela
- **Indicadores de cooldown** com círculos de progresso
- **Cores dinâmicas**: Verde (pronto), Cinza (cooldown), Vazio (não aprendido)
- **Contadores em tempo real** mostrando segundos restantes

### 📚 Como Usar Habilidades

1. **Chegue na vila** através dos portais no mapa inicial
2. **Converse com o NPC Sábio** (Wise Man) usando tecla **E**
3. **Aprenda sua primeira habilidade** através do diálogo
4. **Pressione teclas 1-4** para ativar habilidades aprendidas
5. **Gerencie cooldowns** estrategicamente durante o combate
6. **Dano escalado** com os atributos principais de cada classe
7. **Acompanhe visualmente** o progresso dos cooldowns nos slots UI

## ⚔️ Características do Jogo

### 🛡️ Sistema de Classes

| Classe | Especialidade | Vantagens |
|--------|---------------|-----------|
| **🗡️ Guerreiro** | Combate corpo a corpo | Alta resistência e força |
| **🧙 Mago** | Artes arcanas | Dano mágico e mana elevada |
| **🏹 Caçador** | Ataques à distância | Agilidade e precisão |

### 📊 Sistema de Atributos

| Atributo | Código | Efeito |
|----------|--------|--------|
| **Força** | STR | ⚔️ Aumenta dano corpo a corpo |
| **Destreza** | DEX | 🏃 Aumenta dano à distância e velocidade |
| **Inteligência** | INT | 🧠 Aumenta dano mágico e mana máxima |
| **Sabedoria** | WIS | 👁️ Aumenta visão e regeneração de mana |
| **Carisma** | CHA | 💬 Afeta interações sociais |
| **Constituição** | CON | ❤️ Aumenta a vida e a resistência |

### 👹 Sistema de Inimigos Inteligente

**🧌 Goblins com IA Avançada:**

- **Comum**: Comportamento padrão, balanceado
- **Agressivo**: Mais forte e persistente nos ataques
- **Tímido**: Mais rápido mas foge quando ferido
- **Líder**: Comanda outros goblins em grupo

**🏛️ Sistema de Famílias Goblin:**
- **Clãs organizados**: 20 nomes únicos de famílias
- **Territórios definidos**: Cada família defende sua área
- **Conselho Goblin**: Sistema de decisões estratégicas
- **Respawn automático**: Famílias reaparecem após eliminação
- **Estruturas**: Cabanas e acampamentos goblins

### ✨ Efeitos Visuais de Combate

- **⚠️ Preparação de Ataque**: Aviso visual 0.75s antes do ataque
- **💥 Efeito de Slash**: Animação visual durante ataques
- **📡 Sistema de Telegraphing**: Permite reação aos ataques inimigos

## 🗺️ Sistema de Mundo

### 🌍 Múltiplos Mapas
- **Territórios Goblin**: Mapa inicial 25x25 com goblins e estruturas
- **Vila**: Mapa 25x25 com praia, NPCs e área segura
- **Portais**: Sistema de teleporte bidirecionais entre mapas

## 🏗️ Estrutura do Projeto

```
📁 Top-view-rpg-game/
├── 📦 release/                # Builds executáveis
│   └── 🎮 RPG-Game-v2.0.jar  # Versão atual
│
├── 💻 src/com/rpggame/        # Código fonte Java
│   ├── 🎯 core/              # Engine principal (Game, GamePanel)
│   ├── 👤 entities/          # Jogador, NPCs, inimigos, estruturas
│   ├── 👥 npcs/              # NPCs específicos (Guard, Merchant, Sábio)
│   ├── ⚙️ systems/           # Sistemas de jogo
│   │   ├── 💪 CharacterStats, EnemyManager, ExperienceSystem
│   │   ├── 🎯 Skill, SkillManager
│   │   └── 🔥 skills/        # Habilidades específicas por classe
│   ├── 🖼️ ui/                # Interface do usuário
│   │   ├── 📊 CharacterScreen, DialogBox
│   │   └── 🎮 SkillSlotUI    # Interface de habilidades
│   └── 🗺️ world/             # Mundo do jogo
│       ├── 🗺️ TileMap, MapManager, MapLoader
│       ├── 🌀 MapTransition, Portal
│       └── 🌫️ FogOfWar, Camera
│
├── 🎨 sprites/               # Sprites de personagens e NPCs
```

## 🛠️ Requisitos Técnicos

### Mínimos
- **Java**: JDK 11 ou superior
- **SO**: Windows 7+, macOS 10.12+, Linux (qualquer distro)
- **RAM**: 512MB livres
- **Resolução**: 1024x800 ou superior
- **Espaço**: 100MB

### Recomendados  
- **Java**: JDK 17 ou superior
- **RAM**: 1GB livres
- **Resolução**: 1920x1080

## 📈 Changelog

### v2.0 - "Skills & Magic Update" (Atual - 11/12/2025)
- 🔥 **Sistema de Habilidades**: 3 habilidades únicas por classe
- 🎮 **Interface de Slots Visual**: 4 slots com indicadores de cooldown
- 👥 **NPCs com sprites customizados**: Guard, Sábio, Aldeão, Mercador
- 📚 **Sistema de aprendizado**: Aprenda habilidades através de NPCs
- 🎨 **Efeitos visuais**: Animações para cada habilidade
- ⚖️ **Balanceamento**: Dano baseado em atributos principais de cada classe
- 🎯 **Controles intuitivos**: Teclas 1-4 para habilidades

### v1.2.2 - Sistema de Portais (10/12/2025)
- 🌀 **Portais bidirecionais**: Teleporte entre mapas
- 🏖️ **Novo mapa: Vila**: 25x25 com praia e NPCs
- 🎬 **Transição animada**: Efeito circular de fade
- 🗺️ **MapManager**: Sistema escalável de gerenciamento de mapas
- 🛡️ **Guards estratégicos**: NPCs posicionados como sentinelas
- 📍 **Sistema de spawn**: Pontos consistentes por mapa

### v1.2.1 - Conselho Goblin (Anterior)
- 🏛️ Sistema de Conselho Goblin com decisões estratégicas
- 👑 Hierarquia de clãs com líderes
- 🔄 Respawn automático de famílias
- 20 nomes únicos de clãs

### v1.1 - Recursos e IA
- ✅ Sistema ResourceResolver para carregamento de recursos
- ✅ IA avançada: 4 personalidades de goblins
- ✅ Sistema de territórios e famílias
- ✅ Efeitos visuais de combate

### v1.0
- ✅ Sistema de combate básico
- ✅ IA de goblins inicial
- ✅ Sistema de atributos completo
- ✅ Fog of war implementado
- ✅ Mapas customizáveis
- ✅ Interface unificada

## 🤝 Contribuição

Quer ajudar a melhorar o jogo? Siga estes passos:

1. **Fork** este repositório
2. **Crie** uma branch (`git checkout -b feature/nova-feature`)
3. **Commit** suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. **Push** para a branch (`git push origin feature/nova-feature`)
5. **Abra** um Pull Request

### 🐛 Reportar Bugs
Encontrou um bug? [Abra uma issue](https://github.com/MrRafha/Top-view-rpg-game/issues) com:
- Descrição detalhada do problema
- Passos para reproduzir
- Sistema operacional e versão do Java
- Screenshots (se aplicável)

## 📄 Licença

Este projeto está sob a **Licença MIT**. Consulte o arquivo [`LICENSE`](LICENSE) para mais detalhes.

## 📞 Contato

- **GitHub**: [@MrRafha](https://github.com/MrRafha)
- **Issues**: [Reporte problemas aqui](https://github.com/MrRafha/Top-view-rpg-game/issues)

---

<div align="center">

**🎮 Divirta-se jogando!**

*Desenvolvido com ❤️ em Java*

![Game Preview](https://img.shields.io/badge/Ready%20to%20Play-🎯-success)

</div>
