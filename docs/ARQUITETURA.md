# Arquitetura e contrato da API

## Fluxo principal

O navegador executa Angular e Angular Material. O mesmo servidor entrega a versão compilada e os endpoints `/api`. Cada interface abre um canal SSE que recebe o estado público da fila a cada alteração. Não há polling manual nem dependência de um serviço na nuvem.

A interface do cliente, ao receber uma mudança, consulta sua senha pelo token secreto e obtém posição e notificações específicas. Os demais clientes não recebem esses tokens pelo SSE.

Em desenvolvimento, `ng serve` usa um proxy. Na versão compilada, tudo opera na porta 3000 e na mesma origem.

## Persistência e concorrência

O backend Spring Boot separa controladores, serviços, repositórios e entidades JPA.
O banco padrão é H2 em arquivo; o perfil MySQL permite usar um servidor dedicado.
A fila não é armazenada como documento JSON: senhas, controle da fila, notificações
e auditoria possuem tabelas próprias.

Cada mutação bloqueia a linha de controle com `PESSIMISTIC_WRITE`, aplica as regras
e confirma a transação. O próximo número e a ordem de chegada são atualizados na
mesma transação. Tokens e números possuem restrições únicas. Falhas provocam rollback.
Os eventos SSE são publicados após a confirmação no banco.

A implantação usa uma instância Spring Boot. Para múltiplas instâncias, seria
necessário compartilhar transporte de eventos e sessões. Os testes de concorrência
verificam integridade funcional e não representam um benchmark de carga.

## Estados

| Estado    | Tradução       | Transição permitida                                     |
| --------- | -------------- | ------------------------------------------------------- |
| `waiting` | Aguardando     | `called` pela próxima chamada                           |
| `called`  | Chamando       | `serving` pela confirmação ou `absent` após 30 segundos |
| `serving` | Em atendimento | `done` pela conclusão                                   |
| `absent`  | Ausente        | `waiting` por retorno explícito ao final                |
| `done`    | Concluído      | Final; nova visita gera outra senha                     |

Repetir chamada mantém `called`, registra evento e reinicia o prazo de 30 segundos. Existe um balcão operacional. O filtro por serviço não muda a ordem da chamada, que usa sempre a primeira senha de toda a fila.

## Endpoints

Todas as respostas são JSON, exceto SSE. Erros usam `{ "message": "..." }` e status 400, 401, 403, 404, 409 ou 429 conforme a situação.

| Método e rota                     | Corpo                  | Acesso/efeito                                 |
| --------------------------------- | ---------------------- | --------------------------------------------- |
| `GET /api/state`                  | —                      | Estado público, sem tokens ou avisos privados |
| `GET /api/events`                 | —                      | Canal SSE com o mesmo estado público          |
| `POST /api/tickets`               | `{service, origin}`    | Emite senha; `origin` é `app` ou `totem`      |
| `GET /api/tickets/:token`         | —                      | Senha individual, posição e avisos            |
| `POST /api/tickets/:token/return` | `{}`                   | Retorna uma senha ausente ao final            |
| `POST /api/login`                 | `{username, password}` | Cria sessão HttpOnly de 8 horas               |
| `GET /api/session`                | —                      | Exige sessão válida                           |
| `POST /api/logout`                | `{}`                   | Encerra sessão                                |
| `POST /api/queue/call`            | `{}`                   | Funcionário: chama a próxima senha            |
| `POST /api/queue/start`           | `{}`                   | Funcionário: confirma chegada                 |
| `POST /api/queue/finish`          | `{}`                   | Funcionário: conclui atendimento iniciado     |
| `POST /api/queue/recall`          | `{}`                   | Funcionário: repete chamada                   |
| `POST /api/queue/absent`          | `{}`                   | Funcionário: ausência, somente após o prazo   |
| `POST /api/queue/pause`           | `{}`                   | Funcionário: alterna pausa de novas entradas  |
| `GET /api/audit`                  | —                      | Funcionário: últimos 100 eventos              |

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

`FilaIntegracaoTest` usa Spring Boot, MockMvc e H2 isolado. Verifica autenticação,
origens encaminhadas pelo Codespaces, privacidade, emissão concorrente, chamada,
confirmação, conclusão, ausência, retorno e pausa. Execute com `npm test`.

O temporizador do Angular é apenas visual. A validação de prazo é realizada pelo
serviço Java. O processo de atualização e migração do SQLite está documentado em
[Como funciona](COMO_FUNCIONA.md).
