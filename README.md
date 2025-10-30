# Top-View RPG Game 🎮

Um RPG 2.5D top-down desenvolvido em Java usando Swing, com sistema de classes, combate, fog of war e mapas customizáveis.

![Java](https://img.shields.io/badge/Java-21-orange)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-blue)
![License](https://img.shields.io/badge/License-MIT-green)
![GitHub](https://img.shields.io/badge/GitHub-MrRafha-blue)

## 📋 Sumário

- [Características](#-características)
- [Capturas de Tela](#-capturas-de-tela)
- [Instalação](#-instalação)
- [Como Jogar](#-como-jogar)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Sistema de Classes](#-sistema-de-classes)
- [Mapas Customizáveis](#-mapas-customizáveis)
- [Desenvolvimento](#-desenvolvimento)
- [Contribuição](#-contribuição)
- [Licença](#-licença)

## 🚀 Características

### ⚔️ **Sistema de Combate**
- 3 classes jogáveis: Guerreiro, Mago e Caçador
- Sistema de projéteis com diferentes tipos de ataque
- Sistema de evasão baseado em destreza
- Textos flutuantes para feedback visual
- Cooldown de ataques balanceado

### 📊 **Sistema de Atributos**
- **Força**: Aumenta dano de ataques
- **Destreza**: Aumenta taxa de evasão
- **Inteligência**: Aumenta dano mágico
- **Sabedoria**: Aumenta alcance de visão
- **Carisma**: Reduz cooldown de ataques
- **Constituição**: Aumenta vida e reduz dano recebido

### 🗺️ **Sistema de Mapas**
- Mapas 15x15 com tiles de 48px
- 6 tipos de terreno diferentes
- Sistema de colisão robusto
- Carregamento de mapas via arquivos .txt
- Geração procedural como fallback

### 👁️ **Fog of War**
- Sistema de visibilidade baseado em linha de visão
- Alcance determinado pelo atributo Sabedoria
- Algoritmo de Bresenham para cálculos precisos
- Renderização em tempo real

### 🎨 **Visual e Interface**
- Resolução 1024x800 otimizada
- Sistema de câmera suave
- Sprites redimensionáveis
- Interface de criação de personagem unificada
- Barras de vida dinâmicas

## 🎯 Como Jogar

### Controles
- **WASD** ou **Setas**: Movimento
- **ESPAÇO**: Atacar
- **Mouse**: Clique para focar a janela

### Objetivo
1. Crie seu personagem escolhendo classe e atributos
2. Explore o mapa evitando obstáculos
3. Use o sistema de combate para enfrentar desafios
4. Navegue usando o fog of war para descobrir áreas

## 🛠️ Instalação

### Pré-requisitos
- Java 8 ou superior
- Sistema operacional: Windows, macOS ou Linux

### Passos para Instalação

1. **Clone o repositório**
```bash
git clone https://github.com/MrRafha/Top-view-rpg-game.git
cd Top-view-rpg-game
```

2. **Compile o projeto**
```bash
cd src
javac *.java
```

3. **Execute o jogo**
```bash
java Game
```

### Execução Alternativa
Se preferir, use um IDE como IntelliJ IDEA, Eclipse ou VS Code com extensão Java.

## 📁 Estrutura do Projeto

```
rpg-2d-java/
├── src/                     # Código fonte Java
│   ├── Game.java           # Classe principal
│   ├── GamePanel.java      # Loop principal e renderização
│   ├── Player.java         # Lógica do jogador
│   ├── TileMap.java        # Sistema de mapas
│   ├── FogOfWar.java       # Sistema de visibilidade
│   ├── Camera.java         # Sistema de câmera
│   ├── CharacterStats.java # Sistema de atributos
│   ├── CombinedCharacterScreen.java # Interface de criação
│   ├── TileType.java       # Tipos de terreno
│   ├── MapLoader.java      # Carregador de mapas
│   ├── Projectile.java     # Sistema de projéteis
│   └── FloatingText.java   # Textos flutuantes
├── maps/                   # Mapas customizáveis
│   ├── new_map_15x15.txt  # Mapa principal
│   └── example.txt        # Mapa de exemplo
├── sprites/               # Assets visuais (se houver)
├── docs/                  # Documentação adicional
└── README.md             # Este arquivo
```

## 🏛️ Sistema de Classes

### 🛡️ Guerreiro (Warrior)
- **Especialidade**: Combate corpo a corpo
- **Ataque**: Golpe de espada em área
- **Bônus**: +2 Força, +1 Constituição
- **Estilo**: Tank/DPS físico

### 🧙‍♂️ Mago (Mage)  
- **Especialidade**: Magia e conhecimento
- **Ataque**: Projétil mágico
- **Bônus**: +2 Inteligência, +1 Sabedoria
- **Estilo**: DPS mágico/Suporte

### 🏹 Caçador (Hunter)
- **Especialidade**: Agilidade e precisão
- **Ataque**: Flecha direcionada
- **Bônus**: +2 Destreza, +1 Sabedoria
- **Estilo**: DPS à distância/Mobilidade

## 🗺️ Mapas Customizáveis

### Formato de Arquivo (.txt)
```
WWWWWWWWWWWWWWW
WGGGGGGGGGGGGGW  
WGGGGGTTTTGGGGG
WGGGGGTTTTGGGGG
WWWWWWWWWWWWWWW
```

### Tipos de Tile
- **W** = Parede (Wall) - Não caminhável
- **G** = Grama (Grass) - Caminhável  
- **T** = Água (Water) - Não caminhável
- **S** = Pedra (Stone) - Não caminhável
- **D** = Terra (Dirt) - Caminhável
- **A** = Areia (Sand) - Caminhável

### Criando Mapas
1. Crie um arquivo .txt na pasta `maps/`
2. Use os caracteres acima para definir o terreno
3. Mantenha dimensões 15x15 para compatibilidade
4. O jogo carregará automaticamente

## 🔧 Desenvolvimento

### Arquitetura
- **Padrão MVC**: Separação clara entre lógica e apresentação
- **Component-Based**: Sistemas modulares e reutilizáveis
- **Event-Driven**: Input handling baseado em eventos
- **Data-Driven**: Mapas e configurações externalizadas

### Principais Classes
- `Game`: Ponto de entrada e configuração da janela
- `GamePanel`: Loop principal, renderização e input
- `Player`: Lógica do jogador, movimento e combate
- `TileMap`: Sistema de mundo e colisões
- `FogOfWar`: Sistema de visibilidade

### Adicionando Funcionalidades
1. **Novos Tipos de Tile**: Edite `TileType.java`
2. **Novas Classes**: Modifique `CharacterStats.java`
3. **Novos Mapas**: Adicione arquivos .txt em `maps/`
4. **Novos Projéteis**: Estenda `Projectile.java`

## 🤝 Contribuição

Contribuições são bem-vindas! Para contribuir:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

### Issues e Sugestões
- Use as [Issues](../../issues) para reportar bugs
- Sugira melhorias através de [Discussions](../../discussions)
- Siga o template de issue quando disponível

## 📈 Roadmap

### Versão Atual (v1.0)
- [x] Sistema básico de movimento
- [x] Classes de personagem
- [x] Sistema de combate
- [x] Fog of War
- [x] Mapas customizáveis
- [x] Sistema de colisão

### Próximas Versões
- [ ] NPCs e diálogos
- [ ] Sistema de inventário
- [ ] Múltiplos níveis/mapas
- [ ] Sistema de save/load
- [ ] Efeitos sonoros
- [ ] Animações de sprites
- [ ] Multiplayer local
- [ ] Editor de mapas in-game

## 🐛 Problemas Conhecidos

- Sprites são gerados proceduralmente (sem assets gráficos)
- Sistema de save não implementado
- Balanceamento de classes pode precisar ajustes

## 📝 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 👨‍💻 Autores

- **MrRafha** - *Desenvolvedor Principal* - [GitHub](https://github.com/MrRafha)

## 🙏 Agradecimentos

- Comunidade Java por recursos e tutoriais
- Inspiração em RPGs clássicos top-down
- Feedback da comunidade de desenvolvedores

---

⭐ **Se este projeto foi útil para você, considere dar uma estrela!** ⭐

� **Repositório**: [Top-view RPG Game](https://github.com/MrRafha/Top-view-rpg-game)