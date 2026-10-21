# Como funciona o Corte Ágil

O Corte Ágil organiza o atendimento do açougue em uma fila única. Cliente e totem
emitem senhas; o funcionário chama, confirma a chegada e finaliza o atendimento.
O painel exibe as chamadas e todos recebem atualizações em tempo real.

## Tecnologias

- Angular com NgModules, componentes não standalone e Angular Material.
- Spring Boot, Spring Web, Spring Data JPA e Hibernate no backend.
- Java 17 ou superior e Maven Wrapper incluído.
- H2 persistente em arquivo por padrão; perfil MySQL disponível.
- SSE para atualizar as telas; sessão HTTP para autenticar funcionários.

## Organização

O backend segue a divisão em entidades e repositórios usada no projeto
E-Commerce-Sergiao, acrescentando as camadas necessárias para a API e as regras da fila.

| Pasta                                                                 | Responsabilidade                                   |
| --------------------------------------------------------------------- | -------------------------------------------------- |
| `src/app/core`                                                        | Modelos, serviços Angular e proteção das rotas     |
| `src/app/shared`                                                      | Componentes compartilhados e módulo do Material    |
| `src/app/features/auth`                                               | Formulário de login                                |
| `src/app/features/funcionario`                                        | Gestão e operação da fila                          |
| `src/app/features/cliente`                                            | Emissão e acompanhamento de senha                  |
| `src/app/features/painel`                                             | Chamadas para TV                                   |
| `src/app/features/totem`                                              | Emissão e impressão no totem                       |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/entidades`     | Classes mapeadas nas tabelas                       |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/repositorios`  | Interfaces que estendem JpaRepository              |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/servicos`      | Regras da fila, autenticação e eventos             |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/controladores` | Endpoints REST e rotas das páginas                 |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/dto`           | Dados de entrada e respostas públicas              |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes` | Banco, origem, limites de requisições e cabeçalhos |
| `backend/src/main/java/br/edu/unifio/corteagil_backend/excecoes`      | Erros de negócio e respostas HTTP                  |
| `backend/src/main/resources`                                          | Configuração do Spring e dos bancos                |
| `backend/src/test`                                                    | Testes de integração                               |
| `scripts`                                                             | Preparação, inicialização e migração local         |

## Módulos e componentes Angular

Um módulo agrupa componentes, importações e rotas de uma área. O `AppModule`
inicializa a aplicação e carrega os módulos das funcionalidades sob demanda.
O `SharedModule` exporta os recursos usados em várias telas. O `MaterialModule`
concentra as importações dos componentes Material.

Cada componente possui classe TypeScript, template HTML e, quando necessário,
estilos SCSS. Propriedades e signals guardam os dados; eventos como `(click)`
chamam métodos; interpolação `{{ valor }}` mostra os valores no HTML.
Os componentes permanecem com `standalone: false`.

O `ApiService` faz requisições JSON usando `fetch`. O `FilaService` concentra
os dados reativos da fila em signals. Um `computed` calcula o tempo restante.
O `EventSource` recebe snapshots do servidor e atualiza as telas sem recarregar.
O relógio é ajustado pelo horário informado pelo backend.

## Fluxo de uma ação

1. O cliente clica em retirar senha no componente Angular.
2. O serviço envia `POST /api/tickets`.
3. O controlador recebe e valida o DTO.
4. O serviço bloqueia o registro de controle da fila dentro de uma transação.
5. O repositório salva a entidade `Senha`, os avisos e a auditoria.
6. A transação confirma a operação no banco.
7. O serviço de eventos transmite o novo estado por SSE.
8. Os componentes exibem os dados atualizados.

O bloqueio pessimista na entidade `EstadoFila` e as restrições únicas de número e
token impedem que requisições concorrentes emitam números repetidos.

## Entidades e banco

`@Entity` indica uma tabela; `@Id` identifica a chave primária; `@Column`
configura colunas e restrições. Lombok gera getters, setters e construtores.
As interfaces de repositório herdam métodos como `save`, `findById`, `findAll`
e `deleteById` do `JpaRepository`, como no e-commerce.

- `Senha`: número, token, serviço, origem, posição e horários do atendimento.
- `EstadoFila`: sequência numérica, ordem de chegada e pausa de emissão.
- `EventoAuditoria`: ação realizada, senha envolvida e horário.
- `Aviso`: valor embutido relacionado à senha, armazenado por `@ElementCollection`.

O H2 cria `data/corte-agil.mv.db`. Os dados sobrevivem ao reinício. Não é um
banco em memória. Hibernate mantém o esquema com `ddl-auto=update`, preservando
os registros. As senhas atuais não são reiniciadas a cada execução.

O Spring Boot é o backend. O banco é H2 ou MySQL, acessado pelo Spring Data JPA.

## Regras da fila

As senhas seguem a ordem de emissão. Existe somente um atendimento ativo.

- `waiting`: aguardando na fila.
- `called`: chamada, com 30 segundos para comparecer.
- `serving`: chegada confirmada e atendimento iniciado.
- `done`: atendimento concluído.
- `absent`: ausência registrada após o prazo.

Não é possível concluir sem confirmar a chegada. Repetir a chamada reinicia o
prazo. A pessoa ausente pode retornar ao final mantendo o número da senha.
Pausar impede novas emissões e retornos, mas permite concluir o atendimento atual.
Os filtros da gestão não alteram a ordem de atendimento.

## Avisos e estimativa

O cliente recebe avisos quando faltam duas senhas, uma senha e quando é chamado.
Os avisos ficam relacionados à senha e evitam repetições na mesma passagem pela
fila. O retorno inicia um novo ciclo de avisos.

A média usa os últimos 20 atendimentos concluídos no dia de São Paulo, com mínimo
de um minuto. Sem histórico, o sistema considera quatro minutos. É uma estimativa,
não uma garantia de horário. O atendimento ativo entra no cálculo das pessoas à frente.

## Endpoints

| Método | Caminho                       | Uso                       |
| ------ | ----------------------------- | ------------------------- |
| GET    | `/api/state`                  | Estado público da fila    |
| GET    | `/api/events`                 | Atualizações SSE          |
| POST   | `/api/tickets`                | Emitir senha              |
| GET    | `/api/tickets/{token}`        | Consultar a própria senha |
| POST   | `/api/tickets/{token}/return` | Retornar ao final         |
| POST   | `/api/login`                  | Autenticar funcionário    |
| GET    | `/api/session`                | Consultar sessão          |
| POST   | `/api/logout`                 | Encerrar sessão           |
| POST   | `/api/queue/call`             | Chamar próxima            |
| POST   | `/api/queue/start`            | Confirmar chegada         |
| POST   | `/api/queue/finish`           | Finalizar                 |
| POST   | `/api/queue/absent`           | Registrar ausência        |
| POST   | `/api/queue/recall`           | Repetir chamada           |
| POST   | `/api/queue/pause`            | Alternar pausa            |
| GET    | `/api/audit`                  | Consultar auditoria       |

Operações da gestão e auditoria exigem sessão. Tokens e notificações privadas não
aparecem no estado público. Guarde o token da senha como um identificador privado.

## Login e configuração

`npm run setup` cria o `.env` caso ele não exista e gera uma senha. Se já existir,
a configuração é preservada. O usuário é `admin`. A senha deve ter no mínimo dez
caracteres. O backend verifica a senha com BCrypt e usa sessão com cookie HttpOnly
e SameSite Strict. Reiniciar o backend encerra as sessões.

```dotenv
PORT=3000
HOST=0.0.0.0
ADMIN_PASSWORD=sua_senha_com_pelo_menos_10_caracteres
```

Não envie o `.env` ao GitHub. A orientação de configuração fica na documentação,
não na tela de login.

## Executar

É necessário Node 24, JDK 17 ou superior e acesso à internet na primeira compilação.
O Maven Wrapper baixa a versão do Maven; não é preciso instalá-lo separadamente.

```bash
npm ci
npm run setup
npm run build
npm start
```

O `npm start` prepara a migração se houver um SQLite antigo, compila o backend e
inicia o JAR Spring Boot. Acesse a porta 3000. O Spring serve o Angular compilado
e a API na mesma origem. No Windows, o script usa `mvnw.cmd`.

Para desenvolver, mantenha o backend rodando e abra outro terminal:

```bash
npm run dev
```

Abra a porta 4200. O proxy Angular encaminha `/api` para o backend na porta 3000.
Alterações Java exigem reiniciar `npm start`; alterações Angular no modo dev são
recompiladas automaticamente. Para servir uma nova versão pela porta 3000, execute
`npm run build` novamente.

O ambiente do Codespaces está configurado com Java e Node. Em um Codespace antigo
sem Java, use o comando **Dev Containers: Rebuild Container** na paleta do VS Code.

## Codespaces e origem

O filtro aceita as portas 3000 e 4200 do próprio Codespace e a origem local HTTPS
reescrita pelo encaminhamento. Outros domínios podem ser declarados em
`ALLOWED_ORIGINS`, separados por vírgula e sem barra final. Não é usado curinga para
liberar qualquer origem.

## Usar MySQL

O perfil MySQL usa o mesmo padrão de `application.properties` do e-commerce.
Configure um servidor MySQL e um usuário com permissão no banco `corte_agil`.
No `.env`, adicione:

```dotenv
SPRING_PROFILES_ACTIVE=mysql
DATABASE_URL=jdbc:mysql://localhost:3306/corte_agil?createDatabaseIfNotExist=true&serverTimezone=America/Sao_Paulo
DATABASE_USERNAME=corte_agil
DATABASE_PASSWORD=sua_senha_do_banco
```

Reinicie o backend. O perfil H2 é o padrão. Alternar para MySQL não copia
automaticamente registros de um H2 existente; mantenha backup e planeje a transferência.
Não use `ddl-auto=create` em um banco com dados que deseja manter.

## Migração da versão SQLite

Ao iniciar pela primeira vez, o script lê `data/corte-agil.sqlite` sem alterar esse
arquivo e exporta `data/legacy-queue.json`. O inicializador do Spring importa o estado,
as senhas, os avisos e a auditoria dentro de uma transação. Uma marca no banco impede
reimportação após reiniciar. Se o destino já contiver senhas, a importação é cancelada
para evitar misturar filas. Preserve o SQLite original como backup.

## Totem, painel e notificações

O totem emite pela mesma API e gera QR Code com o token para abrir a senha no celular.
A impressão usa o diálogo do navegador e estilos de papel 80 mm. O painel mostra
as chamadas e permite tela cheia. É necessário ativar o som no navegador.

Notificações do navegador exigem permissão e contexto seguro. Com a página fechada
ou o telefone bloqueado, o recebimento não é garantido: não há serviço Web Push.

## Tema e formatação

O tema cyan do Angular Material fica em `src/styles.scss`. O tema usa
`mat.$cyan-palette` e os componentes usam os tokens de cores do Material.

```bash
npm run format
npm run format:check
npm run backend:format
npm test
npm run build
```

Prettier formata os arquivos web. Spotless com Google Java Format formata o Java.
Os testes verificam autenticação, origens, ciclo da fila, ausência, retorno,
validação, privacidade e concorrência. O README permanece vazio.
