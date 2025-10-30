# Instruções para Git

## Inicializando o Repositório

### Método Manual

1. **Navegue até a pasta do projeto**
```bash
cd rpg-2d-java
```

2. **Inicialize o repositório Git**
```bash
git init
```

3. **Adicione todos os arquivos**
```bash
git add .
```

4. **Faça o primeiro commit**
```bash
git commit -m "Initial commit: RPG 2D Java v1.0.0

- Sistema completo de RPG 2D top-down
- 3 classes de personagem (Guerreiro, Mago, Caçador)
- Sistema de atributos com 6 stats
- Combate com projéteis e evasão
- Fog of War com line-of-sight
- Mapas customizáveis 15x15
- Sistema de colisão robusto
- Interface unificada de criação de personagem"
```

## Conectando ao GitHub

### 1. Repositório já existe no GitHub
- URL: https://github.com/MrRafha/Top-view-rpg-game.git
- Nome: `Top-view-rpg-game`
- Descrição: "🎮 RPG 2D top-down adventure game em Java"

### 2. Conecte o repositório local ao remoto
```bash
git remote add origin https://github.com/MrRafha/Top-view-rpg-game.git
```

### 3. Configure sua branch principal
```bash
git branch -M main
```

### 4. Faça o push inicial
```bash
git push -u origin main
```

## Fluxo de Trabalho Recomendado

### Para novas funcionalidades:
```bash
# Criar nova branch
git checkout -b feature/nome-da-funcionalidade

# Fazer alterações e commits
git add .
git commit -m "Add: descrição da funcionalidade"

# Push da branch
git push origin feature/nome-da-funcionalidade

# Criar Pull Request no GitHub
# Após aprovação, fazer merge na main
```

### Para correções de bugs:
```bash
# Criar branch de bugfix
git checkout -b bugfix/nome-do-bug

# Fazer correção e commit
git add .
git commit -m "Fix: descrição da correção"

# Push e Pull Request
git push origin bugfix/nome-do-bug
```

### Para releases:
```bash
# Atualizar CHANGELOG.md
# Fazer commit das mudanças
git add CHANGELOG.md
git commit -m "Update changelog for v1.1.0"

# Criar tag da versão
git tag -a v1.1.0 -m "Release version 1.1.0"

# Push com tags
git push origin main --tags
```

## Comandos Úteis

```bash
# Ver status dos arquivos
git status

# Ver histórico de commits
git log --oneline

# Ver diferenças não commitadas
git diff

# Desfazer alterações não commitadas
git checkout -- arquivo.java

# Voltar para versão anterior (cuidado!)
git reset --hard HEAD~1

# Ver branches
git branch -a

# Trocar de branch
git checkout nome-da-branch

# Atualizar repositório local
git pull origin main
```

## Estrutura Recomendada de Commits

### Tipos de Commit:
- `Add:` - Nova funcionalidade
- `Fix:` - Correção de bug
- `Update:` - Atualização de funcionalidade existente
- `Remove:` - Remoção de código/arquivo
- `Refactor:` - Refatoração sem mudança de funcionalidade
- `Docs:` - Atualizações na documentação
- `Style:` - Mudanças de formatação/estilo
- `Test:` - Adição/correção de testes

### Exemplo de Mensagens:
```
Add: sistema de inventário com 20 slots
Fix: colisão não funcionando com tiles de água
Update: balanceamento das classes de personagem
Refactor: reorganização do sistema de combate
Docs: atualização do README com novas instruções
```

## Ignorando Arquivos

O arquivo `.gitignore` já está configurado para ignorar:
- Arquivos compilados (*.class)
- Arquivos temporários
- Configurações de IDE
- Arquivos de sistema operacional

Se precisar ignorar algo específico, adicione no `.gitignore`.

## Colaboração

### Para contribuidores:
1. Fork do repositório
2. Clone do seu fork
3. Criar branch para feature
4. Fazer alterações e commits
5. Push para seu fork
6. Criar Pull Request

### Para revisar Pull Requests:
1. Testar as alterações localmente
2. Verificar se segue os padrões do projeto
3. Verificar se a documentação foi atualizada
4. Aprovar ou solicitar alterações