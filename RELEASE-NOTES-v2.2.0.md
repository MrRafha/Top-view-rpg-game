#  Top-View RPG Game - Release Notes v2.2.0

##  **INVENTORY & VILLAGE UPDATE**

**Data de Lançamento:** 15 de Dezembro de 2025  
**Versão:** 2.2.0  
**Código:** "Inventory & Village Expansion"

---

##  **Principais Novidades**

###  **Sistema de Inventário Completo**
Um sistema robusto de inventário foi implementado com navegação por teclado!

####  **Características do Inventário**
- **20 slots fixos** organizados em grade 5x4
- **Tecla I** para abrir/fechar o inventário
- **Navegação WASD** para mover entre slots
- **Enter** para usar itens
- **Seta dourada** indicando o slot selecionado
- **Balão de descrição** mostrando informações do item
- **Sistema de empilhamento automático** para consumíveis

####  **Poções Implementadas**
- **Poção de Vida**  - Restaura 50 HP instantaneamente
- **Poção de Mana**  - Restaura 30 MP instantaneamente

---

##  **Expansão do Vilarejo**

###  **Estruturas Decorativas**
O mapa village agora possui estruturas detalhadas e intransitáveis!

####  **Construções Adicionadas**
- **3 Casas** (120x120px cada) - Totalmente colisíveis
- ** Igreja** (192x192px) - Estrutura imponente
- ** Tenda de Mercador** (144x144px) - Área comercial
- ** 4 Lâmpadas** (72x72px cada) - Iluminação decorativa

---

##  **Novos Tiles**

###  **GRASS_PATH (Caminho de Grama)**
- **Tile ID: 8 | Caractere: 'r'**
- **Visual:** Pedras sobre grama para ruas do vilarejo

---

##  **Lista Completa de Mudanças**

###  Adicionado
- Sistema completo de inventário (20 slots)
- Interface de inventário com navegação WASD
- Poções de vida e mana com feedback visual
- 3 casas decorativas no vilarejo
- Igreja no centro do vilarejo
- Tenda de mercador
- 4 lâmpadas decorativas
- Tile GRASS_PATH para ruas
- Sistema de empilhamento de itens
- FloatingText para uso de poções

###  Modificado
- Sistema de Structure expandido
- TileType enum com novo tile GRASS_PATH
- Player com métodos para FloatingText
- EnemyManager com estruturas do vilarejo

---

##  **Como Usar**

###  Inventário
- Pressione **I** para abrir
- Use **WASD** para navegar
- Pressione **Enter** para usar item
- Pressione **ESC** para fechar

---

##  **Download**

Arquivo JAR: **RPG-2D-v2.2.0.jar**

`ash
java -jar RPG-2D-v2.2.0.jar
`

**Requisitos:** Java 17+

---

**Divirta-se explorando o novo vilarejo! **
