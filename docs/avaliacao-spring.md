# Conferência da atividade de Spring Boot

Enunciado: https://hackmd.io/@srdelfino/SyMXGnkAzg

Esta atividade acrescenta critérios à entrega de interface anterior. A conferência abaixo considera especificamente o backend solicitado pelo professor.

| Item do enunciado                             | Onde conferir                                                                  |
| --------------------------------------------- | ------------------------------------------------------------------------------ |
| Cinco entidades JPA coerentes                 | CategoriaServico, Servico, Balcao, Usuario e Senha; diagrama em arquitetura.md |
| MySQL                                         | compose.avaliacao.yml, perfil mysql e evidências reais da coleção              |
| CRUD completo, listagem e consulta individual | /api/admin/{tipo}, /api/admin/{tipo}/{id} e /api/gestao/senhas                 |
| Camadas e nenhum repository em controller     | pacotes entidades, repositorios, servicos e controladores                      |
| Pelo menos duas regras de negócio             | FilaServico e CadastroServico; documentação da API                             |
| Bean Validation e @Valid                      | DTOs e controladores de cadastro, autenticação e senhas                        |
| Exceção própria e tratamento centralizado     | RegraNegocioException e TratadorExcecoes                                       |
| 400, 404 e 409                                | requisições 10, 15, 17, 23, 28 e demais exemplos da coleção                    |
| Duas consultas personalizadas                 | ConsultaServico, ServicoRepositorio e SenhaRepositorio                         |
| Spring Security, HTTP Basic e dois perfis     | SegurancaApi, DetalhesUsuarioServico e DadosAvaliacao                          |
| Demonstração 401 e 403                        | requisições 01 a 05 da coleção                                                 |
| Funcionalidade específica do PI               | emissão, chamada, chegada, conclusão, ausência e retorno da fila               |
| Postman com respostas, variáveis e exportação | postman/projeto-integrador.postman_collection.json                             |
| README, endpoints e diagrama                  | README.md, documentacao-api.md e arquitetura.md                                |
| Nomes completos e IDs acadêmicos              | Pendente de informação pelo grupo; completar a tabela do README                |

## Base utilizada e adaptação

O exemplo do professor separa entidades JPA, repositories Spring Data e recursos REST. No Corte Ágil, a relação Categoria → Produto foi adaptada para CategoriaServico → Servico: agrupa os tipos de cortes e preparos oferecidos pelo açougue. O backend mantém a mesma base de conceitos e acrescenta camada Service, segurança, validações e regras próprias da fila. Não foi alterada a versão do Spring Boot apenas para igualar o exemplo, pois o enunciado não exige uma versão específica.

## Como demonstrar

1. Inicie o ambiente MySQL pelo Compose e importe a coleção.
2. Execute os exemplos de ausência de autenticação (401), ADMIN válido, ATENDENTE válido e operação administrativa negada (403).
3. Cadastre categoria, serviço, balcão e usuário; consulte individualmente e altere/exclua os registros sem histórico.
4. Demonstre a consulta de serviços por categoria e o conflito ao tentar excluir uma categoria vinculada.
5. Emita uma senha. Consulte-a, altere o serviço e cancele enquanto aguarda.
6. Emita outra senha e execute chamada, confirmação de chegada e conclusão. Demonstre o bloqueio de uma segunda chamada e a preservação do histórico.
7. Consulte as senhas concluídas e a auditoria identificando o operador.

## Explicação para apresentação

- `@Entity` mapeia a classe para o banco; `@Id` identifica um registro. Os IDs deste projeto são UUIDs gerados na aplicação.
- `@ManyToOne` relaciona vários serviços a uma categoria e várias senhas a serviço, balcão e atendente.
- Repository cuida de persistência e consultas; Service valida regras e transações; Controller traduz requisições e respostas HTTP.
- `@Valid` aciona Bean Validation. Conflitos de fluxo usam exceção própria e status 409; registros inexistentes usam 404.
- HTTP Basic transporta usuário e senha no cabeçalho Authorization. Base64 não é criptografia: use HTTPS fora do computador local.
- Autenticação verifica quem é o usuário; autorização verifica o que seu perfil pode fazer. Por isso 401 e 403 representam situações diferentes.
- A transação e o bloqueio do estado da fila preservam ordem e numeração durante emissões simultâneas.
- O perfil avaliacao cria contas de teste apenas no ambiente local. O perfil vercel continua usando PostgreSQL e as contas reais já existentes.

Os exemplos da coleção foram capturados em requisições HTTP contra MySQL 8.4.13 e reproduzidos pelo Newman, o executor de coleções do Postman. A interface Angular também foi testada nesse banco. Não foram utilizados dados de clientes nem credenciais de produção nas evidências.

## Verificação realizada

- 17 testes Java aprovados, incluindo permissões, integridade e regras da fila.
- 58 requisições HTTP executadas contra MySQL 8.4.13 e reproduzidas pelo Newman sem falhas.
- Build Angular e três testes do service worker aprovados.
- Interface conferida em computador e celular, com login por sessão, categorias, edição, exclusão e atendimento.

O workflow `verificar.yml` repete os testes Java, a compilação Angular e a coleção com um serviço MySQL. O relatório e as respostas dessa execução ficam no artefato `avaliacao-mysql-postman` do GitHub Actions.
