# 🎯 Próximos Passos - Sistema de Portais

## ✅ O que foi feito

Sistema de portais com transição circular **COMPLETO E COMPILADO**!

### Arquivos Criados:
- ✅ `src/com/rpggame/world/Portal.java` - Estrutura de dados de portal
- ✅ `src/com/rpggame/world/MapTransition.java` - Animação de fade circular
- ✅ `src/com/rpggame/world/MapManager.java` - Gerenciador de múltiplos mapas

### Arquivos Modificados:
- ✅ `src/com/rpggame/world/TileType.java` - Adicionado `PORTAL` (ID 6, char 'P')
- ✅ `src/com/rpggame/world/TileMap.java` - Métodos: setupPortals(), getPortalAt(), addPortal(), reloadMap()
- ✅ `src/com/rpggame/core/GamePanel.java` - Sistema de detecção e transição integrado
- ✅ `src/com/rpggame/entities/Player.java` - Método setPosition(x, y)
- ✅ `src/com/rpggame/systems/EnemyManager.java` - Método clearAllEnemies()
- ✅ `src/com/rpggame/world/FogOfWar.java` - Método resetFog()

### Git:
- ✅ Commit `5795b3b` - Sistema de portais completo
- ✅ Push para branch `desenvolvimento`

---

## 🧪 Como Testar

### 1. Adicionar Tile de Portal no Mapa

Edite um dos mapas (ex: `maps/goblin_territories_25x25.txt`) e adicione a letra **`P`** em algum lugar:

```
WWWWWWWWWWWWWWWWWWWWWWWWW
W.......................W
W.......................W
W........P..............W  <-- Portal aqui!
W.......................W
```

### 2. Executar o Jogo

```powershell
cd d:\rafs\Top-view-rpg-game
java -cp bin com.rpggame.core.Game
```

### 3. O que Deve Acontecer

1. **Ao caminhar sobre o tile 'P':**
   - ⚫ Um círculo preto começa a **expandir** do centro da tela
   - 🌀 A tela fica completamente preta
   - 🗺️ O mapa muda (atualmente configurado para "village" em TileMap.java linha 323)
   - ⚪ O círculo **retrai** revelando o novo mapa
   - 👤 O player aparece na nova posição (400, 400)

---

## ⚙️ Configurar Portais Customizados

### Método 1: Automático (atual)
O sistema detecta tiles 'P' e cria portais automáticos para "village" (400, 400).

**Arquivo:** `src/com/rpggame/world/TileMap.java` - linha 318-325

```java
private void setupPortals() {
  portals.clear();
  for (int y = 0; y < MAP_HEIGHT; y++) {
    for (int x = 0; x < MAP_WIDTH; x++) {
      if (map[y][x] == TileType.PORTAL) {
        // EDITE AQUI: targetMapId, targetX, targetY
        portals.add(new Portal(x, y, "village", 400, 400, "Portal da Vila"));
      }
    }
  }
}
```

### Método 2: Manual

Adicione portais específicos no `GamePanel.java` após carregar o mapa:

```java
// No initializeGame() após inicializar tileMap:
tileMap.addPortal(new Portal(5, 5, "maps/village.txt", 200, 200, "Vila"));
tileMap.addPortal(new Portal(10, 10, "maps/cave.txt", 300, 300, "Caverna"));
```

---

## 🗺️ Criar Novos Mapas

### 1. Criar o arquivo do mapa
```
maps/village.txt
maps/cave.txt
maps/forest.txt
```

### 2. Registrar no MapManager

**Arquivo:** `src/com/rpggame/world/MapManager.java` - linha 27-32

```java
private void initializeMaps() {
  maps.put("village", new MapData(
    "maps/village.txt",
    "Vila Inicial",
    200, 200  // posição de spawn (pixels)
  ));
  
  // ADICIONE AQUI seus novos mapas:
  maps.put("forest", new MapData(
    "maps/forest.txt",
    "Floresta Misteriosa",
    100, 100
  ));
}
```

---

## 🎨 Customizar a Transição

**Arquivo:** `src/com/rpggame/world/MapTransition.java`

```java
// Linha 16: Velocidade da transição
private static final float TRANSITION_SPEED = 0.03f; // Aumente = mais rápido

// Linha 81-106: Método render() - Efeito visual
// Atualmente: círculo invertido (máscara circular)
// Pode mudar para fade simples, cortina, etc.
```

---

## 🐛 Problemas Conhecidos

### ❌ Se der erro de compilação:

**Solução 1:** Limpar e recompilar
```powershell
cd d:\rafs\Top-view-rpg-game
Remove-Item -Recurse -Force bin\*
cd src
javac -d ..\bin -encoding UTF-8 com\rpggame\core\*.java com\rpggame\entities\*.java com\rpggame\systems\*.java com\rpggame\ui\*.java com\rpggame\world\*.java com\rpggame\enemies\Goblins\*.java com\rpggame\npcs\*.java
```

**Solução 2:** Verificar encoding
Se aparecer erro de BOM (illegal character '\ufeff'):
```powershell
# No diretório do arquivo problemático:
$content = Get-Content ARQUIVO.java -Raw
$content = $content.TrimStart([char]0xFEFF)
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Resolve-Path ARQUIVO.java), $content, $utf8NoBom)
```

### ❌ Portal não funciona:

1. Verifique se o tile 'P' está no mapa
2. Verifique console do jogo para mensagem: "🌀 Portal encontrado em (x, y)"
3. Verifique se o mapa de destino existe no MapManager

---

## 📋 Checklist de Testes

- [ ] Compilar o projeto sem erros
- [ ] Adicionar tile 'P' em um mapa
- [ ] Executar o jogo
- [ ] Caminhar sobre o portal
- [ ] Ver círculo preto expandir
- [ ] Ver mapa mudar
- [ ] Ver círculo revelar novo mapa
- [ ] Verificar se player está na posição correta
- [ ] Verificar se enemies foram limpos
- [ ] Verificar se fog of war foi resetada
- [ ] Voltar ao mapa anterior (se tiver portal de volta)

---

## 🚀 Melhorias Futuras

1. **Portais bidirecionais:** Portal automático de volta
2. **Efeitos sonoros:** Som ao entrar no portal
3. **Partículas:** Efeito visual no tile de portal
4. **Loading screen:** Texto "Carregando..." durante transição
5. **Salvar posição:** Lembrar de qual portal o player veio
6. **Portais condicionais:** Requer item/nível para usar
7. **Animação do tile:** Sprite animado para portal
8. **Mini mapa:** Mostrar conexões entre mapas

---

## 📞 Se Precisar de Ajuda

### Arquivo de Debug:
- Console do jogo mostra mensagens de portal (🌀, 🗺️, 🧹)
- Verifique console para erros de carregamento de mapa

### Logs Importantes:
```
🌀 Portal encontrado em (x, y)          // Portal detectado no mapa
🔵 Transição de portal iniciada          // Jogador entrou no portal
🗺️ Mapa recarregado: [path]             // Novo mapa carregado
🧹 Todos os inimigos foram removidos     // Enemies limpos
```

### Branch Git:
- **Branch atual:** `desenvolvimento`
- **Último commit:** `5795b3b` - Sistema de portais

---

**Boa sorte testando o sistema de portais! 🎮✨**
