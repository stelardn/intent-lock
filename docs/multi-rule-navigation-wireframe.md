# IntentLock UI Spec: Dashboard, Regras e Permissões

## Objetivo

Evoluir a UI atual de tela única para uma navegação orientada a tarefas, com menor carga cognitiva e melhor separação entre:

- visão geral do sistema
- configuração das regras
- permissões do Android

Esta spec também prepara o produto para suportar **múltiplas regras simultâneas**.

## Direção de UX

### Princípios

- A tela inicial deve ser leve e escaneável.
- Informações frequentes e de alto valor devem aparecer primeiro.
- Configuração detalhada não deve competir com o estado atual do sistema.
- A navegação principal deve ser explícita e sempre visível.
- Cada regra deve ser compreensível rapidamente sem exigir abertura imediata dos detalhes.

### Decisão de navegação

Usar **bottom navigation** como navegação principal, com 3 destinos visíveis e rótulos curtos:

- `Início`
- `Regras`
- `Sistema`

Não usar menu hamburguer como navegação principal nesta fase.

### Ícones da bottom navigation

Cada item da navbar deve ter ícone + texto.

Recomendação:

- `Início`: ícone de casa
- `Regras`: ícone de lista, grade ou regra
- `Sistema`: ícone de escudo, cadeado ou ajustes

### Regras dos rótulos

- Os nomes devem ter leitura imediata.
- Os rótulos devem funcionar bem em telas pequenas.
- Evitar palavras muito longas na barra inferior.
- O título interno da tela pode ser mais descritivo do que o rótulo da navbar.

Observação:

- Na navegação, usar `Sistema`.
- Dentro da tela, o título pode continuar como `Permissões` ou `Sistema e permissões`, conforme ficar mais claro visualmente.

## Estrutura de informação

### Dashboard

Responsável por responder:

- o app está pronto para funcionar?
- existem créditos ativos agora?
- quais apps estão protegidos?
- existe desafio em andamento?
- existe algo exigindo atenção?

### Regras

Responsável por responder:

- quais regras existem?
- quais estão válidas?
- quais apps estão protegidos por cada regra?
- como criar, editar, ativar/desativar ou excluir uma regra?

### Permissões

Responsável por responder:

- o que falta para o sistema funcionar corretamente?
- como abrir as telas do Android necessárias?

## Regras de produto refletidas na UI

- O app suporta **múltiplas regras simultâneas**.
- Cada `app bloqueado` pode existir em **apenas uma regra**.
- O `app de controle` pode ser reutilizado em várias regras.
- Créditos ativos são tratados por regra/app bloqueado, não como estado global único.
- A home mostra resumo; o detalhamento operacional fica em `Regras`.

## Navegação principal

```text
+--------------------------------------------------+
| [home] Início | [rules] Regras | [shield] Sistema|
+--------------------------------------------------+
```

### Comportamento

- `Início` abre por padrão.
- A aba atual deve preservar seu estado ao alternar entre tabs.
- A tela de bloqueio (`LockActivity`) continua fora da bottom nav.
- Fluxos de criar/editar regra podem abrir como tela dedicada ou sub-rota dentro de `Regras`.

## Dashboard

## Objetivo da tela

Ser um painel calmo, resumido e útil. Não deve repetir toda a configuração.

## Seções

### 1. Hero de status

Mostra o estado geral do sistema.

Estados possíveis:

- `Pronto`
- `Faltam permissões`
- `Desafio em andamento`
- `Créditos ativos`
- `Regras com problema`
- `Nenhuma regra configurada`

Conteúdo esperado:

- título curto
- descrição de uma linha
- CTA contextual quando fizer sentido

Exemplos:

- `IntentLock está pronto para proteger seus apps`
- `Algumas permissões ainda precisam ser ativadas`

### 2. Créditos ativos

Deve aparecer com destaque quando houver pelo menos um crédito ativo.

#### Regras de exibição

- Se houver apenas 1 crédito ativo:
  - mostrar nome do app bloqueado
  - mostrar tempo restante
  - mostrar horário de expiração
- Se houver múltiplos créditos ativos:
  - mostrar resumo como `3 apps com crédito ativo`
  - listar até 3 apps
  - mostrar `+N` quando houver mais

#### Exemplo

```text
+--------------------------------------------------+
| Créditos ativos                                  |
| Instagram liberado por mais 8 min                |
| Expira às 14:32                                  |
+--------------------------------------------------+
```

Ou:

```text
+--------------------------------------------------+
| Créditos ativos                                  |
| 3 apps com crédito ativo                         |
| Instagram • YouTube • X • +2                     |
+--------------------------------------------------+
```

### 3. Apps protegidos

Resumo visual dos apps atualmente cobertos por regras válidas.

#### Objetivo

Comunicar valor rapidamente sem entrar em detalhes de configuração.

#### Regras de exibição

- Mostrar apenas apps bloqueados
- Preferir chips compactos ou lista curta
- Mostrar `+N` se houver muitos
- Tocar no bloco leva para `Regras`

#### Exemplo

```text
+--------------------------------------------------+
| Apps protegidos                                  |
| Instagram | YouTube | X | +4                     |
+--------------------------------------------------+
```

### 4. Desafio em andamento

Aparece somente quando houver sessão ativa.

Conteúdo:

- app de controle
- progresso atual
- status resumido

Exemplo:

```text
+--------------------------------------------------+
| Desafio em andamento                             |
| Duolingo                                         |
| 18s de 30s                                       |
+--------------------------------------------------+
```

### 5. Alertas e atenção

Bloco para estados que exigem ação do usuário.

Pode incluir:

- permissões ausentes
- regra inválida
- app removido do dispositivo
- conflito de configuração detectado

#### Exemplo

```text
+--------------------------------------------------+
| Atenção                                          |
| 1 regra precisa ser revisada                     |
| Usage Access ainda não está ativo                |
+--------------------------------------------------+
```

## Wireframe do Dashboard

```text
+--------------------------------------------------+
| IntentLock                                       |
| [ Pronto ]                                       |
| Seus apps protegidos estão configurados          |
+--------------------------------------------------+

+--------------------------------------------------+
| Créditos ativos                                  |
| Instagram liberado por mais 8 min                |
| Expira às 14:32                                  |
+--------------------------------------------------+

+--------------------------------------------------+
| Apps protegidos                                  |
| Instagram | YouTube | X | +4                     |
+--------------------------------------------------+

+--------------------------------------------------+
| Desafio em andamento                             |
| Duolingo                                         |
| 18s de 30s                                       |
+--------------------------------------------------+

+--------------------------------------------------+
| Atenção                                          |
| Usage Access ainda não está ativo                |
+--------------------------------------------------+

[ Início ] [ Regras ] [ Sistema ]
```

## Regras

## Objetivo da tela

Ser o centro da configuração do produto.

## Estrutura da tela

### 1. Cabeçalho

Conteúdo:

- título `Regras`
- subtítulo curto
- ação principal `Nova regra`

### 2. Lista de regras

Cada regra deve aparecer como um card resumido.

### Conteúdo mínimo de cada card

- nome do app bloqueado
- nome do app de controle
- tempo do desafio
- janela de desbloqueio
- status da regra

### Status possíveis

- `Ativa`
- `Inativa`
- `Inválida`
- `Permissões incompletas`

### Ações por regra

- `Editar`
- `Ativar/Desativar`
- `Excluir`

### Regras visuais

- O app bloqueado é o identificador principal do card
- O app de controle aparece como metadado secundário
- O status precisa ser visível rapidamente
- Cards inválidos devem ter tratamento visual distinto

## Wireframe da lista de regras

```text
+--------------------------------------------------+
| Regras                               [Nova regra]|
| Configure quais apps exigem intenção antes do uso|
+--------------------------------------------------+

+--------------------------------------------------+
| Instagram                                        |
| Controle: Duolingo                               |
| Desafio: 30s   Janela: 10 min                    |
| [Ativa]                   [Editar] [Desativar]   |
+--------------------------------------------------+

+--------------------------------------------------+
| YouTube                                          |
| Controle: Forest                                 |
| Desafio: 45s   Janela: 15 min                    |
| [Inválida]                [Editar] [Excluir]     |
+--------------------------------------------------+

[ Início ] [ Regras ] [ Sistema ]
```

## Criar/editar regra

### Abordagem

Recomendado usar **tela dedicada** para criar/editar, em vez de expandir inline na lista.

Motivos:

- reduz poluição visual
- melhora foco
- escala melhor para validação e estados de erro

### Campos

- seletor de `App bloqueado`
- seletor de `App de controle`
- campo de `Duração do desafio`
- campo de `Janela de desbloqueio`
- toggle `Regra ativa`

### Validações

- app bloqueado é obrigatório
- app de controle é obrigatório
- apps devem ser diferentes
- app bloqueado não pode já estar em uso por outra regra
- duração deve respeitar limites de produto
- janela deve respeitar limites de produto

### Erro importante

Quando o usuário escolher um `app bloqueado` já usado em outra regra:

- impedir salvamento
- mostrar mensagem clara

Texto sugerido:

`Este app já está protegido por outra regra. Edite a regra existente ou escolha outro app bloqueado.`

## Wireframe da edição de regra

```text
+--------------------------------------------------+
| Nova regra                                       |
| Configure um app protegido e seu desafio         |
+--------------------------------------------------+

App bloqueado
[ Selecionar app ]

App de controle
[ Selecionar app ]

Duração do desafio
[ 30 ]

Janela de desbloqueio
[ 10 ]

[ Regra ativa ]

[ Salvar regra ]
[ Cancelar ]
```

## Permissões

## Objetivo da tela

Concentrar tudo que depende do Android em um lugar previsível e de baixa fricção.

## Seções

### 1. Status geral

Resumo curto:

- `Tudo pronto`
- `1 permissão pendente`
- `2 permissões pendentes`

### 2. Cartões de permissão

Permissões atuais:

- `Acessibilidade`
- `Usage Access`

Cada card deve ter:

- nome da permissão
- breve explicação
- status atual
- botão para abrir a configuração correspondente

### Wireframe

```text
+--------------------------------------------------+
| Permissões                                       |
| 1 permissão pendente                             |
+--------------------------------------------------+

+--------------------------------------------------+
| Acessibilidade                                   |
| Detecta quando um app bloqueado foi aberto       |
| [Ativa]                         [Abrir ajustes]  |
+--------------------------------------------------+

+--------------------------------------------------+
| Usage Access                                     |
| Mede o tempo gasto no app de controle            |
| [Pendente]                      [Abrir ajustes]  |
+--------------------------------------------------+

[ Início ] [ Regras ] [ Sistema ]
```

## Estados vazios

## Dashboard sem regras

```text
+--------------------------------------------------+
| IntentLock                                       |
| [ Nenhuma regra configurada ]                    |
| Crie sua primeira regra para começar             |
|                                      [Nova regra]|
+--------------------------------------------------+
```

## Regras vazia

```text
+--------------------------------------------------+
| Regras                                           |
| Você ainda não criou nenhuma regra               |
|                                      [Nova regra]|
+--------------------------------------------------+
```

## Créditos ausentes

Quando não houver crédito ativo, o card pode:

- não aparecer
- ou aparecer como estado neutro curto

Recomendação:

- esconder por padrão para manter o dashboard limpo

## Estados de erro e invalidez

### Regra inválida

Motivos possíveis:

- app bloqueado removido
- app de controle removido
- configuração corrompida

Comportamento:

- a regra continua visível na lista
- o status mostra `Inválida`
- o dashboard pode exibir alerta resumido

### Permissões pendentes

Comportamento:

- não impedir edição de regras
- reduzir prontidão do sistema
- refletir no hero do dashboard

## Comportamentos importantes

### Dashboard

- Não deve repetir lista completa de regras.
- Não deve expor detalhes excessivos de configuração.
- Deve priorizar informação operacional e valor percebido.

### Regras

- Deve ser a fonte principal de verdade da configuração.
- Deve permitir múltiplas regras simultâneas.
- Deve prevenir duplicidade de `app bloqueado`.

### Permissões

- Deve ser funcional e direta.
- Deve evitar linguagem técnica excessiva.

## Responsividade

### Mobile

- Bottom nav sempre visível
- Cards empilhados verticalmente
- Chips de apps protegidos quebram linha quando necessário

### Layout adaptativo

Se houver espaço horizontal maior no futuro:

- `Dashboard` pode migrar para grid de cards
- `Regras` pode exibir cards com ações melhor distribuídas

## Microcopy recomendada

## Navegação principal

### Bottom navigation

Usar rótulos curtos e estáveis:

- `Início`
- `Regras`
- `Sistema`

### Ícones sugeridos

- `Início`: casa
- `Regras`: lista, grade ou regra
- `Sistema`: escudo, cadeado ou ajustes

## Títulos por tela

### Tela `Início`

- título: `IntentLock`
- subtítulo quando pronto: `Seus apps protegidos estão sob controle`
- subtítulo quando sem regras: `Crie regras para começar a proteger seus apps`
- subtítulo quando houver pendências: `Alguns ajustes ainda precisam da sua atenção`

### Tela `Regras`

- título: `Regras`
- subtítulo padrão: `Defina quais apps exigem intenção antes do uso`
- subtítulo com lista vazia: `Você ainda não criou nenhuma regra`

### Tela `Sistema`

- título: `Permissões`
- subtítulo quando pronto: `Tudo pronto para o IntentLock funcionar`
- subtítulo quando houver pendências: `Algumas permissões ainda precisam ser ativadas`

## Títulos de seções e cards

### Tela `Início`

- hero pronto: `IntentLock está pronto`
- hero sem regras: `Nenhuma regra configurada`
- hero com pendências: `Faltam ajustes no sistema`
- hero com crédito ativo: `Há crédito ativo agora`
- card de crédito: `Créditos ativos`
- card de apps protegidos: `Apps protegidos`
- card de desafio: `Desafio em andamento`
- card de alerta: `Atenção`

### Tela `Regras`

- card válido: usar o nome do app bloqueado como título principal
- linha secundária: `Controle: <nome do app>`
- métrica curta: `Desafio: 30s`
- métrica curta: `Janela: 10 min`

### Tela `Sistema`

- card: `Acessibilidade`
- descrição: `Detecta quando um app protegido foi aberto`
- card: `Usage Access`
- descrição: `Mede o tempo no app de controle`

## Status curtos

Usar palavras curtas e consistentes:

- `Pronto`
- `Ativa`
- `Inativa`
- `Inválida`
- `Pendente`
- `Protegido`
- `Sem crédito`

Evitar:

- frases longas dentro de chips
- termos técnicos excessivos
- variações desnecessárias do mesmo estado

## CTAs principais

### Globais

- `Nova regra`
- `Salvar regra`
- `Cancelar`
- `Voltar`
- `Atualizar`

### Tela `Regras`

- `Editar`
- `Excluir`
- `Ativar`
- `Desativar`

### Tela `Sistema`

- `Abrir ajustes`
- `Abrir acessibilidade`
- `Abrir Usage Access`

### Tela `Início`

- `Ver regras`
- `Nova regra`
- `Ver sistema`

## Mensagens de apoio

### Créditos ativos

- `Instagram liberado por mais 8 min`
- `Expira às 14:32`
- `3 apps com crédito ativo`

### Apps protegidos

- `Apps com regra ativa no momento`
- `Toque para ver ou editar`

### Desafio em andamento

- `Duolingo em andamento`
- `18s de 30s`
- `Continue até completar o tempo mínimo`

### Sistema com pendência

- `Usage Access ainda não está ativo`
- `A acessibilidade precisa ser ativada`
- `1 regra precisa ser revisada`

## Mensagens de estado vazio

### Início

- título: `Nenhuma regra configurada`
- apoio: `Crie sua primeira regra para começar`

### Regras

- título: `Você ainda não criou nenhuma regra`
- apoio: `Adicione um app bloqueado e um app de controle`

### Créditos

- recomendação: ocultar o card quando não houver crédito
- fallback opcional: `Sem créditos ativos agora`

## Mensagens de erro e validação

### Regra duplicada por app bloqueado

- `Este app já está protegido por outra regra.`
- `Edite a regra existente ou escolha outro app bloqueado.`

### Campos obrigatórios

- `Selecione o app bloqueado.`
- `Selecione o app de controle.`

### Conflito entre apps

- `Os dois apps precisam ser diferentes.`

### Faixas válidas

- `A duração do desafio deve ficar entre 10 e 300 segundos.`
- `A janela de desbloqueio deve ficar entre 1 e 60 minutos.`

### Apps não instalados

- `Este app não está mais instalado.`
- `Revise a regra para continuar usando a proteção.`

## Notas para implementação futura

- A antiga `MainActivity` de tela única deve ser substituída por um shell com navegação principal.
- `Início`, `Regras` e `Sistema` devem virar destinos separados.
- A lista de regras precisa de uma nova modelagem de estado, já que a UI atual assume regra única.
- Créditos ativos e desafios em andamento devem ser agregados por regra no dashboard.
- A edição de regra deve reutilizar o picker de apps já existente, adaptado para o contexto de múltiplas regras.

## Critérios de aceite de UX

- A home é claramente mais leve que a versão de tela única.
- O usuário consegue entender quais apps estão protegidos em poucos segundos.
- O usuário consegue encontrar onde editar regras sem ambiguidade.
- O usuário consegue identificar rapidamente se há créditos ativos.
- O usuário consegue localizar permissões pendentes sem misturar isso com configuração.
