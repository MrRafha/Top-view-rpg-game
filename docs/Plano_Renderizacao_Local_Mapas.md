# Plano de Implementacao - Renderizacao Local de Mapas

## Objetivo

Mover a responsabilidade de renderizacao de tiles e layout do mapa para o cliente, recebendo do servidor apenas o manifesto inicial do mundo e os deltas relevantes. A meta e eliminar o efeito visual de rollback em tiles fora da tela e reduzir dependencia de snapshot para cenarios estaticos.

---

## Problema Atual

Hoje o cliente usa o estado do mundo misturado com snapshots/interpolacao para desenhar elementos do mapa e entidades. Isso funciona para entidades dinamicas, mas cria confusao visual quando o mapa ou a camera mudam, especialmente em bordas do viewport.

O efeito percebido como "rollback" nao vem do tile em si, mas da estrategia de render baseada em estado sincronizado por rede em vez de um mapa local fixo e deterministico.

---

## Proposta

### 1. Cliente recebe o mundo uma vez no connect

No handshake inicial, o servidor envia um `WorldManifest` com:

- lista de mapas
- ordem/layer dos mapas no layout do mundo
- seed, se o gerador for deterministico
- pontos de spawn, portais e conexoes entre mapas
- estado inicial de elementos estaticos por mapa

### 2. Cliente reconstrói os mapas localmente

O cliente passa a:

- carregar os tiles a partir do manifesto ou seed
- montar o `MapManager` localmente
- renderizar tiles sem depender de snapshot de rede
- usar snapshots apenas para entidades dinamicas e eventos

### 3. Servidor envia deltas, nao mapa inteiro a cada tick

O servidor continua autoritativo para:

- players
- inimigos
- NPCs dinamicos
- baus abertos/fechados
- estruturas destruidas
- eventos de combate e quest

Mudancas permanentes no mapa devem ir como eventos/deltas, nao como parte do fluxo de render continuo.

---

## Arquitetura Desejada

```text
Servidor
  - WorldManifest no handshake
  - WorldSnapshot com entidades dinamicas
  - Eventos/deltas de alteracao do mapa

Cliente
  - World local fixo para tiles
  - Render de camera/viewport local
  - Snapshot apenas para entidades dinamicas
```

---

## Formato Proposto do Manifesto

Exemplo conceitual:

```json
{
  "worldId": "default-world",
  "seed": 123456,
  "maps": [
    {
      "id": "village",
      "order": 0,
      "tileSource": "maps/village.txt",
      "spawnX": 558,
      "spawnY": 217,
      "portals": [
        { "x": 10, "y": 12, "targetMapId": "neutral_1" }
      ]
    }
  ]
}
```

Se o gerador nao for deterministico, o servidor pode enviar o layout completo dos tiles uma unica vez, em vez de seed.

---

## Plano de Implementacao

### Fase 1 - Definir o contrato do manifesto

- Criar um DTO de manifesto compartilhado entre cliente e servidor.
- Definir quais campos sao obrigatorios para reproduzir o mundo.
- Separar claramente manifesto inicial de snapshot por tick.

### Fase 2 - Ajustar o handshake

- Atualizar o fluxo de conexao para enviar o manifesto logo apos o handshake.
- Garantir que o cliente so avance para a tela de jogo apos receber esse bloco inicial.
- Preservar compatibilidade temporaria com o modo atual, se necessario.

### Fase 3 - Montagem local do mapa no cliente

- Fazer o cliente popular `MapManager` e `TileMap` a partir do manifesto.
- Garantir que a ordem dos mapas seja a mesma definida pelo servidor.
- Evitar reconstruir tiles a cada frame ou a cada snapshot.

### Fase 4 - Separar tiles de entidades dinamicas

- Renderizar tiles, fog of war e camera a partir do estado local.
- Renderizar players, inimigos, NPCs e projeteis a partir de `WorldSnapshot`.
- Manter interpolacao apenas onde houver movimento real.

### Fase 5 - Enviar deltas de mapa

- Criar eventos para mudancas permanentes: porta aberta, bau aberto, estrutura destruida.
- Aplicar esses eventos no estado local do cliente.
- Evitar reenviar o mesmo estado estatico em cada snapshot.

### Fase 6 - Validar visualmente

- Confirmar que tiles fora da tela nao causam salto visual.
- Confirmar que o mapa permanece estavel durante troca de camera.
- Validar que reconexao recompõe o mesmo mundo.

---

## Riscos

- Se o mapa nao for deterministico, seed so nao basta; sera necessario transmitir layout completo.
- Se o cliente reconstruir mapas com ordem diferente do servidor, portais e transicoes podem quebrar.
- Se mudancas permanentes continuarem vindo por snapshot, o efeito visual de rollback pode persistir em estruturas e baus.
- Se o manifesto inicial vier incompleto, o cliente pode renderizar um mundo diferente do servidor.

---

## Critérios de Aceite

- O cliente monta o mapa sem depender de snapshot para tiles.
- O servidor envia o layout inicial apenas uma vez por conexao.
- Mudancas de mapa sao refletidas por eventos/deltas, nao por redraw completo.
- O efeito visual de rollback em tiles fora da tela deixa de acontecer.
- A ordem dos mapas no cliente bate com a ordem definida pelo servidor.

---

## Ordem Recomendada de Execucao

1. Definir o `WorldManifest`.
2. Ajustar handshake para transportar o manifesto.
3. Fazer o cliente reconstruir o mundo localmente.
4. Reduzir o snapshot para entidades dinamicas.
5. Adicionar eventos de delta para alteracoes permanentes.
6. Testar uma sessao completa com troca de mapa e reconexao.

---

## Resultado Esperado

Com esse modelo, o cliente passa a ser responsavel por desenhar o mundo estatico, enquanto o servidor continua dono da simulacao. Isso reduz trafego, melhora estabilidade visual e elimina o rollback associado a renderizacao de tiles por snapshot.