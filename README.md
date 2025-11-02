# Top-view RPG Game 🎮

Um RPG 2D em Java com sistema de combate, exploração e progressão de personagem.

![Java](https://img.shields.io/badge/Java-8+-orange)
![Status](https://img.shields.io/badge/Status-Complete-green)
![License](https://img.shields.io/badge/License-MIT-blue)

## 🚀 Como Jogar

### 📦 Versão Executável (Recomendado)

**Baixe e jogue em segundos:**

1. **Baixe** o arquivo `RPG-2D-Game-v1.1-FIXED.zip`
2. **Extraia** o conteúdo em qualquer pasta
3. **Execute**:
   - **Windows**: Duplo-clique em `RPG-Game.exe.bat`
   - **Linux/Mac**: `chmod +x RPG-Game.sh && ./RPG-Game.sh`
   - **Manual**: `java -cp "lib:resources" com.rpggame.core.Game`

**✅ Requisito único**: Java 8 ou superior instalado

### 🛠️ Desenvolvimento (Código Fonte)

Para desenvolvedores que querem modificar o código:

```bash
# Clone o repositório
git clone https://github.com/MrRafha/Top-view-rpg-game.git
cd Top-view-rpg-game

# Compile
javac -encoding UTF-8 -d build -cp . src/com/rpggame/**/*.java

# Execute  
java -cp build com.rpggame.core.Game
```

## 🎮 Controles

| Tecla | Ação |
|-------|------|
| **WASD** | Movimentação do personagem |
| **Espaço** | Atacar inimigos |
| **C** | Abrir tela de características |
| **ESC** | Sair do jogo |

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
| **Sabedoria** | WIS | 👁️ Aumenta experiência ganha e visão |
| **Carisma** | CHA | 💬 Afeta interações sociais |
| **Constituição** | CON | ❤️ Aumenta vida máxima |

### 👹 Sistema de Inimigos Inteligente

**🧌 Goblins com IA Avançada:**

- **Comum**: Comportamento padrão, balanceado
- **Agressivo**: Mais forte e persistente nos ataques
- **Tímido**: Mais rápido mas foge quando ferido
- **Líder**: Comanda outros goblins em grupo

### ✨ Efeitos Visuais de Combate

- **⚠️ Preparação de Ataque**: Aviso visual 0.75s antes do ataque
- **💥 Efeito de Slash**: Animação visual durante ataques
- **📡 Sistema de Telegraphing**: Permite reação aos ataques inimigos
- **🎯 Feedback Visual**: Textos flutuantes para dano e ações

## 🗺️ Sistema de Mundo

### 🌫️ Fog of War
- **Visibilidade realista** baseada em linha de visão
- **Exploração dinâmica** do mapa
- **Alcance determinado** pelo atributo Sabedoria

### 🗃️ Mapas Customizáveis
- **Formato simples**: Arquivos `.txt` editáveis
- **Tiles variados**: 6 tipos diferentes de terreno
- **Colisão inteligente**: Sistema robusto de física
- **Mapas inclusos**: Vários mapas pré-criados

## 🏗️ Estrutura do Projeto

```
📁 Top-view-rpg-game/
├── 📦 dist/                    # Versão executável
│   ├── 📚 lib/                # Classes compiladas (.class)
│   ├── 🎨 resources/          # Assets (sprites, mapas)
│   ├── 🖥️ RPG-Game.exe.bat   # Executável Windows
│   └── 🐧 RPG-Game.sh         # Executável Linux/Mac
│
├── 💻 src/com/rpggame/        # Código fonte Java
│   ├── 🎯 core/              # Engine principal
│   ├── 👤 entities/          # Jogador, inimigos, objetos
│   ├── ⚙️ systems/           # Sistemas (XP, stats, combate)
│   ├── 🖼️ ui/                # Interface do usuário
│   └── 🗺️ world/             # Mundo (tiles, câmera, mapas)
│
├── 🎨 sprites/               # Imagens e sprites
├── 🗺️ maps/                 # Mapas personalizados
└── 📦 RPG-2D-Game-v1.1-FIXED.zip  # Pacote de distribuição
```

## 🛠️ Requisitos Técnicos

### Mínimos
- **Java**: 8 ou superior
- **SO**: Windows 7+, macOS 10.12+, Linux (qualquer distro)
- **RAM**: 256MB livres
- **Espaço**: 50MB

### Recomendados  
- **Java**: 11 ou superior
- **RAM**: 512MB livres
- **CPU**: Dual-core 2GHz+

## 📈 Changelog

### v1.1 (Atual)
- ✅ **Sistema ResourceResolver**: Carregamento inteligente de recursos
- ✅ **Correção crítica**: Sprites e mapas funcionando no executável
- ✅ **Efeitos visuais de combate**: Animações e telegraphing dos goblins
- ✅ **IA avançada**: 4 personalidades de goblins únicas
- ✅ **Sistema de territórios**: Famílias de goblins e guerras
- ✅ **Estrutura organizada**: Pacotes Java modulares
- ✅ **Executável corrigido**: Cross-platform totalmente funcional

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