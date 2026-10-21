# Arquitetura e contrato da API

## Fluxo principal

O navegador executa Angular e Angular Material. O mesmo servidor entrega a versão compilada e os endpoints `/api`. Cada interface abre um canal SSE que recebe o estado público da fila a cada alteração. Não há pooling manual nem dependência de um serviço na nuvem.

A interface do cliente, ao receber uma mudança, consulta sua senha pelo token secreto e obtém posição e notificações específicas. Os demais clientes não recebem esses tokens pelo SSE.

Em desenvolvimento, `ng serve` usa um proxy. Na versão compilada, tudo opera na porta 3000 e na mesma origem.

## Persistência e concorrência

`server/domain.mjs` contém regras independentes de HTTP e banco. `server/index.mjs` abre SQLite com WAL e sincronização FULL. A fila é armazenada como um documento JSON em uma linha transacional, decisão simples para o escopo acadêmico e piloto local.

Cada mutação executa `BEGIN IMMEDIATE`, lê, aplica a regra, grava e confirma a transação. As operações são síncronas dentro do processo e não contêm awaits entre leitura e gravação. O SQLite serializa escritores. Falhas de regra geram rollback. O número sequencial faz parte do mesmo estado persistido, portanto emissões simultâneas e reinícios não reutilizam o identificador.

**Implantação suportada:** uma instância do servidor Node para o banco. Não iniciar dois servidores apontando ao mesmo arquivo: as escritas seriam serializadas pelo SQLite, mas os canais SSE e as sessões em memória não seriam compartilhados. Para múltiplas instâncias seria necessário transporte compartilhado de eventos e armazenamento compartilhado de sessões.

Esta implementação privilegia clareza. Para filas muito grandes, convém normalizar tickets, notificações e auditoria em tabelas, adicionar índices e definir retenção. Os testes cobrem concorrência funcional, não constituem benchmark de carga de supermercado.

## Estados

| Estado | Tradução | Transição permitida |
| --- | --- | --- |
| `waiting` | Aguardando | `called` pela próxima chamada |
| `called` | Chamando | `serving` pela confirmação ou `absent` após 30 segundos |
| `serving` | Em atendimento | `done` pela conclusão |
| `absent` | Ausente | `waiting` por retorno explícito ao final |
| `done` | Concluído | Final; nova visita gera outra senha |

Repetir chamada mantém `called`, registra evento e reinicia o prazo de 30 segundos. Existe um balcão operacional. O filtro por serviço não muda a ordem da chamada, que usa sempre a primeira senha de toda a fila.

## Endpoints

Todas as respostas são JSON, exceto SSE. Erros usam `{ "message": "..." }` e status 400, 401, 403, 404, 409 ou 429 conforme a situação.

| Método e rota | Corpo | Acesso/efeito |
| --- | --- | --- |
| `GET /api/state` | — | Estado público, sem tokens ou avisos privados |
| `GET /api/events` | — | Canal SSE com o mesmo estado público |
| `POST /api/tickets` | `{service, origin}` | Emite senha; `origin` é `app` ou `totem` |
| `GET /api/tickets/:token` | — | Senha individual, posição e avisos |
| `POST /api/tickets/:token/return` | `{}` | Retorna uma senha ausente ao final |
| `POST /api/login` | `{username, password}` | Cria sessão HttpOnly de 8 horas |
| `GET /api/session` | — | Exige sessão válida |
| `POST /api/logout` | `{}` | Encerra sessão |
| `POST /api/queue/call` | `{}` | Funcionário: chama a próxima senha |
| `POST /api/queue/start` | `{}` | Funcionário: confirma chegada |
| `POST /api/queue/finish` | `{}` | Funcionário: conclui atendimento iniciado |
| `POST /api/queue/recall` | `{}` | Funcionário: repete chamada |
| `POST /api/queue/absent` | `{}` | Funcionário: ausência, somente após o prazo |
| `POST /api/queue/pause` | `{}` | Funcionário: alterna pausa de novas entradas |
| `GET /api/audit` | — | Funcionário: últimos 100 eventos |

## Notificações

A quantidade à frente soma as senhas aguardando antes do cliente e o atendimento ativo. O servidor registra eventos com duas e uma senhas à frente. Cada limiar é notificado uma vez por ciclo de retorno; uma senha nova que entra já com uma ou duas à frente recebe o aviso correspondente imediatamente. Senha que entra em fila vazia é mostrada como próxima.

Chamada, ausência, conclusão e retorno também geram eventos. Esses eventos são persistidos com a senha, limitados aos últimos 20 por senha. A lista de limiares já enviados evita repetição a cada snapshot. Ao reconectar o cliente consulta novamente seu histórico; os últimos avisos aparecem na página.

No painel, o horário da chamada determina o disparo sonoro. Repetir a chamada altera esse horário. O som precisa ser previamente habilitado por interação do usuário; não há como contornar a política de autoplay do navegador.

## O que não é prometido

- Identificação física de Wi-Fi: o acesso é condicionado à disponibilidade do servidor na rede; o Angular não identifica o SSID. A restrição de rede depende do roteador/firewall.
- Notificações em página fechada ou com celular bloqueado: exigem solução adicional de push e HTTPS.
- Impressão silenciosa/ESC-POS: é usado o diálogo de impressão do navegador e um QR real.
- Login individual por funcionário, recuperação de senha por e-mail, múltiplos balcões, alta disponibilidade e backups automáticos: fora desta versão.
- Totem travado em modo quiosque: configurar no sistema operacional/navegador do equipamento. A página não bloqueia o usuário de navegar para outros endereços.

## Testes e manutenção

`domain.test.mjs` testa regras com relógio controlado, sem aguardar 30 segundos de verdade. `integration.test.mjs` inicia a API em porta aleatória, usa um banco temporário e verifica sessões, SSE, emissões e chamadas simultâneas, proteção contra origem externa e recuperação após reinício. O diretório temporário é removido no final.

Nunca substitua a validação de prazo no servidor pelo temporizador visual. O front ajusta o relógio com `serverTime` para mostrar a contagem; a autorização para ausência sempre é calculada pelo Node.
