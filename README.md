# Corte Ágil — Projeto Integrador

Sistema de organização da fila de atendimento de açougues. O cliente retira uma senha pelo celular ou pelo totem, acompanha a posição e recebe avisos da chamada. A equipe chama o próximo cliente, confirma a chegada e conclui o atendimento, com registro de ausências, retornos e auditoria.

O **backend avaliado está na pasta [backend/](backend/)**. É um projeto **Java 17 com Spring Boot 3.5.16 e Maven**, separado do frontend Angular. A API REST concentra as regras de negócio, a autenticação, os cadastros, a persistência e os indicadores da operação.

| Acesso rápido para avaliação              | Local                                                                                                            |
| ----------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Projeto Java/Spring Boot e dependências   | [backend/pom.xml](backend/pom.xml)                                                                               |
| Classe principal para iniciar a aplicação | [CorteAgilApplication.java](backend/src/main/java/br/edu/unifio/corteagil_backend/CorteAgilApplication.java)     |
| Código-fonte Java                         | [backend/src/main/java/br/edu/unifio/corteagil_backend/](backend/src/main/java/br/edu/unifio/corteagil_backend/) |
| Testes Java                               | [backend/src/test/java/br/edu/unifio/corteagil_backend/](backend/src/test/java/br/edu/unifio/corteagil_backend/) |
| Ambiente de avaliação com MySQL           | [compose.avaliacao.yml](compose.avaliacao.yml)                                                                   |
| Coleção Postman com respostas salvas      | [postman/projeto-integrador.postman_collection.json](postman/projeto-integrador.postman_collection.json)         |
| Diagrama de classes                       | [docs/arquitetura.md — Classes de negócio](docs/arquitetura.md#classes-de-negócio)                               |
| Documentação da API                       | [docs/documentacao-api.md](docs/documentacao-api.md)                                                             |
| Execuções dos testes no GitHub            | [Actions — Verificar projeto](https://github.com/Hagnarok/Corte-Agil/actions/workflows/verificar.yml)            |

## Integrantes

| Nome completo                         | ID acadêmico |
| ------------------------------------- | ------------ |
| Luiz Felipe Gaspar Fernandes da Silva | 273435       |
| Guilherme Perino Feitosa              | 272668       |
| Matheus Boletti Fortes                | 272518       |
| Gustavo Henrique de Araujo Alves      | 274075       |

## Guia de leitura

1. [Organização do projeto e tecnologias](#organização-do-projeto-e-tecnologias)
2. [Como executar com MySQL](#como-executar-com-mysql)
3. [Como localizar e executar o backend Java](#como-localizar-e-executar-o-backend-java)
4. [Onde conferir cada requisito](#onde-conferir-cada-requisito)
5. [Entidades, relacionamentos e CRUD](#entidades-relacionamentos-e-crud)
6. [Autenticação e regras de negócio](#autenticação-e-regras-de-negócio)
7. [Testes com Postman e MySQL](#testes-com-postman-e-mysql)
8. [Testes automatizados e relatórios](#testes-automatizados-e-relatórios)
9. [Roteiro para demonstração e entrega](#roteiro-para-demonstração-e-entrega)

## Organização do projeto e tecnologias

O projeto adapta o domínio do [exemplo de e-commerce da disciplina](https://github.com/sergiodelfino/ecommerce-backend-4a/tree/main/ecommerce) para a fila de um açougue. A relação Categoria → Produto corresponde a CategoriaServico → Servico, agrupando os cortes e preparos oferecidos. A aplicação inclui a camada Service, validações, permissões e regras específicas de atendimento.

| Parte                          | Tecnologias e localização                                                                            |
| ------------------------------ | ---------------------------------------------------------------------------------------------------- |
| Backend                        | Java 17, Spring Boot 3.5.16, Spring MVC, Spring Data JPA/Hibernate e Maven; [backend/](backend/)     |
| Banco da avaliação             | MySQL 8.4, driver MySQL Connector/J e configuração do perfil `mysql`                                 |
| Segurança                      | Spring Security, HTTP Basic, BCrypt e perfis ADMIN/ATENDENTE                                         |
| Validação e testes Java        | Bean Validation, JUnit 5, Spring Boot Test, MockMvc e H2 isolado para testes                         |
| Frontend                       | Angular 22, NgModules, `standalone: false`, roteamento e Angular Material cyan; [src/app/](src/app/) |
| Testes HTTP                    | Postman Collection v2.1 e Newman 6.2.1; [postman/](postman/)                                         |
| Execução e integração contínua | [Dockerfile](Dockerfile), Docker Compose e [GitHub Actions](.github/workflows/verificar.yml)         |

O frontend Angular fica em `src/`, na raiz. O código Java fica em `backend/src/`. O arquivo `package.json` reúne os comandos da interface e scripts que iniciam o Java; as dependências Spring Boot estão em `backend/pom.xml`.

## Como executar com MySQL

### 1. Preparar o ambiente

Instale Git e Docker com Docker Compose. No Windows, utilize o Docker Desktop em execução. O Dockerfile compila a interface e o backend dentro dos containers.

```sh
git clone https://github.com/Hagnarok/Corte-Agil.git
cd Corte-Agil
docker compose -f compose.avaliacao.yml up --build -d
```

Os comandos deste README partem da raiz `Corte-Agil/`. A primeira execução baixa as imagens e dependências e compila a aplicação. Para acompanhar a inicialização:

```sh
docker compose -f compose.avaliacao.yml ps
docker compose -f compose.avaliacao.yml logs -f aplicacao
```

### 2. Acessar a aplicação

| Recurso                                    | Endereço                                                           |
| ------------------------------------------ | ------------------------------------------------------------------ |
| Interface e login da equipe                | [http://localhost:3000/entrar](http://localhost:3000/entrar)       |
| Área do cliente                            | [http://localhost:3000/cliente](http://localhost:3000/cliente)     |
| Totem de emissão                           | [http://localhost:3000/totem](http://localhost:3000/totem)         |
| Painel público                             | [http://localhost:3000/painel](http://localhost:3000/painel)       |
| API pública para verificar a inicialização | [http://localhost:3000/api/state](http://localhost:3000/api/state) |
| Base da API no Postman                     | `http://localhost:3000/api`                                        |
| MySQL pelo computador local                | `127.0.0.1:3307`, banco `corte_agil_avaliacao`                     |

As contas abaixo são criadas pelo perfil `avaliacao` e servem exclusivamente para os testes da atividade:

| Usuário   | Senha de teste         | Perfil    | Permissão                                  |
| --------- | ---------------------- | --------- | ------------------------------------------ |
| avaliador | Avaliacao-Admin123     | ADMIN     | Mantém os cadastros e opera a fila         |
| operador  | Avaliacao-Atendente123 | ATENDENTE | Consulta dados da operação e atende a fila |

Acesse `/entrar` com uma dessas contas para usar a interface. No Postman, utilize **Basic Auth**. As credenciais do banco local também estão no [Compose](compose.avaliacao.yml): usuário `corte_avaliacao` e senha didática `MySQL-Avaliacao123`.

A inicialização cadastra os serviços e o balcão usados na demonstração. As classes responsáveis estão em [CadastrosIniciais.java](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/CadastrosIniciais.java), [CategoriasIniciais.java](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/CategoriasIniciais.java) e [DadosAvaliacao.java](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/DadosAvaliacao.java).

### 3. Encerrar e preservar os dados

```sh
docker compose -f compose.avaliacao.yml down
```

O MySQL mantém os dados no volume `mysql_avaliacao`. Iniciar novamente utiliza esse mesmo banco.

### Configurações de banco disponíveis

| Ambiente                  | Banco e configuração                                                                                                               | Uso                                                        |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| Avaliação desta atividade | MySQL; perfis `mysql,avaliacao`; [compose.avaliacao.yml](compose.avaliacao.yml) e [.env.avaliacao.example](.env.avaliacao.example) | Demonstração dos requisitos da prova e execução da coleção |
| Testes Java               | H2 em memória configurado nas próprias classes de teste                                                                            | Testes isolados e repetíveis                               |
| Execução local padrão     | H2 persistente; [application.properties](backend/src/main/resources/application.properties)                                        | Desenvolvimento local                                      |
| Site publicado            | PostgreSQL; perfil `vercel`; [application-vercel.properties](backend/src/main/resources/application-vercel.properties)             | Instalação na nuvem já existente                           |

**Para avaliar o requisito MySQL, utilize o ambiente da atividade descrito acima.** A publicação em nuvem utiliza PostgreSQL. As contas didáticas do perfil `avaliacao` não ficam ativas nessa publicação.

## Como localizar e executar o backend Java

### Abrir na IDE

Importe [backend/pom.xml](backend/pom.xml) como projeto Maven, com JDK 17 ou superior. O pacote principal é `br.edu.unifio.corteagil_backend`, e o ponto de entrada é [CorteAgilApplication.java](backend/src/main/java/br/edu/unifio/corteagil_backend/CorteAgilApplication.java), anotado com `@SpringBootApplication`.

As pastas da tabela abaixo estão dentro de `backend/src/main/java/br/edu/unifio/corteagil_backend/`:

| Pasta Java                                                                             | Responsabilidade                                        | Exemplos para leitura                                             |
| -------------------------------------------------------------------------------------- | ------------------------------------------------------- | ----------------------------------------------------------------- |
| [entidades/](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/)         | Modelagem JPA e relacionamentos com o banco             | `CategoriaServico`, `Servico`, `Balcao`, `Usuario`, `Senha`       |
| [repositorios/](backend/src/main/java/br/edu/unifio/corteagil_backend/repositorios/)   | Persistência e consultas com Spring Data JPA            | `ServicoRepositorio`, `SenhaRepositorio`                          |
| [servicos/](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/)           | Regras de negócio, transações, cadastros e autenticação | `FilaServico`, `CadastroServico`, `ConsultaServico`               |
| [controladores/](backend/src/main/java/br/edu/unifio/corteagil_backend/controladores/) | Endpoints REST, entrada de dados e códigos HTTP         | `CadastroControlador`, `SenhasControlador`, `ConsultaControlador` |
| [dto/](backend/src/main/java/br/edu/unifio/corteagil_backend/dto/)                     | Dados recebidos e respostas da API                      | `CadastroRequest`, `EmissaoRequest`, `SenhaResposta`              |
| [excecoes/](backend/src/main/java/br/edu/unifio/corteagil_backend/excecoes/)           | Exceção de negócio e tratamento centralizado            | `RegraNegocioException`, `TratadorExcecoes`                       |
| [configuracoes/](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/) | Segurança, perfis, filtros, sessões e dados iniciais    | `SegurancaApi`, `DadosAvaliacao`                                  |

As propriedades ficam em [backend/src/main/resources/](backend/src/main/resources/). O controlador recebe a requisição e chama um Service. O Service valida as regras e utiliza os repositories para acessar o banco. Os controladores não acessam repositories diretamente.

### Executar o Java no computador, usando o MySQL do Compose

São necessários JDK 17+ e Node.js 24.15 ou superior da linha 24. Suba o banco e compile a interface:

```sh
docker compose -f compose.avaliacao.yml up -d mysql
npm ci
npm run build
```

Em uma cópia local destinada à avaliação, copie `.env.avaliacao.example` para `.env`, preservando qualquer configuração já existente. Depois execute:

```sh
npm start
```

O comando carrega `.env`, compila o projeto Maven e inicia `backend/target/corte-agil.jar`. O processo que atende a API é o Spring Boot. Para acompanhar esse ponto de entrada, consulte [scripts/start.mjs](scripts/start.mjs).

Para iniciar diretamente pelo Maven ou pela IDE, configure as variáveis de ambiente da tabela abaixo. **O Maven e a IDE não carregam `.env` automaticamente**; esse carregamento é feito pelo comando `npm start`.

| Variável                 | Valor para avaliação local                                                                                                        |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | `mysql,avaliacao`                                                                                                                 |
| `DATABASE_URL`           | `jdbc:mysql://localhost:3307/corte_agil_avaliacao?serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true&sslMode=DISABLED` |
| `DATABASE_USERNAME`      | `corte_avaliacao`                                                                                                                 |
| `DATABASE_PASSWORD`      | `MySQL-Avaliacao123`                                                                                                              |
| `ADMIN_PASSWORD`         | `Avaliacao-Admin123`                                                                                                              |
| `PUBLIC_BASE_URL`        | `http://localhost:3000`                                                                                                           |
| `ALLOWED_ORIGINS`        | `http://localhost:3000`                                                                                                           |

Com essas variáveis configuradas e o MySQL em execução, inicie a classe principal na IDE ou use, na raiz do repositório:

```sh
./backend/mvnw -B -f backend/pom.xml spring-boot:run
```

No PowerShell do Windows, o comando equivalente é:

```powershell
.\backend\mvnw.cmd -B -f backend/pom.xml spring-boot:run
```

O Maven Wrapper está incluído em `backend/`, portanto não é necessário instalar uma distribuição Maven separadamente. A API pode ser avaliada diretamente pelo Postman; a interface exige o build Angular descrito acima.

## Onde conferir cada requisito

| Requisito do enunciado                          | Arquivos e evidências                                                                                                                                                                                                                                                                                                                                  |
| ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Cinco entidades JPA relacionadas                | [entidades/](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/) e [diagrama de classes](docs/arquitetura.md#classes-de-negócio)                                                                                                                                                                                                         |
| Persistência com MySQL                          | [application-mysql.properties](backend/src/main/resources/application-mysql.properties), [pom.xml](backend/pom.xml) e [Compose](compose.avaliacao.yml)                                                                                                                                                                                                 |
| CRUD, listagem e consulta individual            | [CadastroControlador.java](backend/src/main/java/br/edu/unifio/corteagil_backend/controladores/CadastroControlador.java), [SenhasControlador.java](backend/src/main/java/br/edu/unifio/corteagil_backend/controladores/SenhasControlador.java) e [documentação da API](docs/documentacao-api.md)                                                       |
| Separação Controller, Service e Repository      | [controladores/](backend/src/main/java/br/edu/unifio/corteagil_backend/controladores/), [servicos/](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/) e [repositorios/](backend/src/main/java/br/edu/unifio/corteagil_backend/repositorios/)                                                                                            |
| Regras de negócio                               | [FilaServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/FilaServico.java) e [CadastroServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/CadastroServico.java)                                                                                                                                      |
| Bean Validation e `@Valid`                      | [CadastroRequest.java](backend/src/main/java/br/edu/unifio/corteagil_backend/dto/CadastroRequest.java), [EmissaoRequest.java](backend/src/main/java/br/edu/unifio/corteagil_backend/dto/EmissaoRequest.java) e controladores correspondentes                                                                                                           |
| Exceção personalizada e `@RestControllerAdvice` | [RegraNegocioException.java](backend/src/main/java/br/edu/unifio/corteagil_backend/excecoes/RegraNegocioException.java) e [TratadorExcecoes.java](backend/src/main/java/br/edu/unifio/corteagil_backend/excecoes/TratadorExcecoes.java)                                                                                                                |
| Erros 400, 404 e 409                            | [ApiAvaliacaoTest.java](backend/src/test/java/br/edu/unifio/corteagil_backend/ApiAvaliacaoTest.java) e exemplos salvos na coleção Postman                                                                                                                                                                                                              |
| Duas consultas personalizadas JPA               | [ServicoRepositorio.java](backend/src/main/java/br/edu/unifio/corteagil_backend/repositorios/ServicoRepositorio.java), [SenhaRepositorio.java](backend/src/main/java/br/edu/unifio/corteagil_backend/repositorios/SenhaRepositorio.java) e [ConsultaServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/ConsultaServico.java) |
| Spring Security, HTTP Basic e perfis            | [SegurancaApi.java](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/SegurancaApi.java), [DetalhesUsuarioServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/DetalhesUsuarioServico.java) e [DadosAvaliacao.java](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/DadosAvaliacao.java) |
| Diferença entre 401 e 403                       | Requisições 01 a 05 da coleção e teste `distingue401De403EAutenticaDoisPerfis` em [ApiAvaliacaoTest.java](backend/src/test/java/br/edu/unifio/corteagil_backend/ApiAvaliacaoTest.java)                                                                                                                                                                 |
| Funcionalidade específica do PI                 | [FilaControlador.java](backend/src/main/java/br/edu/unifio/corteagil_backend/controladores/FilaControlador.java) e [FilaServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/FilaServico.java): emissão, chamada, chegada, conclusão, ausência e retorno                                                                       |
| Coleção com variáveis e respostas salvas        | [projeto-integrador.postman_collection.json](postman/projeto-integrador.postman_collection.json)                                                                                                                                                                                                                                                       |
| Instruções, diagrama e identificação do grupo   | Este README, [arquitetura.md](docs/arquitetura.md) e [documentacao-api.md](docs/documentacao-api.md)                                                                                                                                                                                                                                                   |

## Entidades, relacionamentos e CRUD

| Entidade         | Papel no domínio                                                           | Código Java                                                                                                    |
| ---------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| CategoriaServico | Agrupa cortes e preparos; uma categoria pode ter vários serviços           | [CategoriaServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/CategoriaServico.java) |
| Servico          | Tipo de preparo solicitado; pertence a uma categoria                       | [Servico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/Servico.java)                   |
| Balcao           | Ponto de atendimento relacionado às senhas                                 | [Balcao.java](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/Balcao.java)                     |
| Usuario          | Funcionário com perfil de acesso; relacionado ao atendimento e à auditoria | [Usuario.java](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/Usuario.java)                   |
| Senha            | Registro do cliente na fila; relaciona serviço, balcão e atendente         | [Senha.java](backend/src/main/java/br/edu/unifio/corteagil_backend/entidades/Senha.java)                       |

O [diagrama de classes](docs/arquitetura.md#classes-de-negócio) mostra os relacionamentos e suas cardinalidades. Estado da fila, avisos, auditoria e tokens de recuperação são registros de controle e histórico; sua alteração ocorre pelas operações correspondentes da aplicação.

Todos os caminhos abaixo têm o prefixo `/api`:

| Entidades/operação                                | Endpoints                                                                                     |
| ------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Categorias, serviços, balcões e usuários          | `GET` e `POST /admin/{tipo}`; `GET`, `PUT` e `DELETE /admin/{tipo}/{id}`                      |
| Valores de `{tipo}`                               | `categorias`, `servicos`, `balcoes`, `usuarios`                                               |
| Criação de senha de atendimento                   | `POST /tickets`                                                                               |
| Gestão de senhas                                  | `GET /gestao/senhas`; `GET`, `PUT` e `DELETE /gestao/senhas/{id}`                             |
| Consulta personalizada: serviços de uma categoria | `GET /consultas/servicos-por-categoria/{id}`; método `findByCategoriaIdOrderByNomeAsc`        |
| Consulta personalizada: senhas por situação       | `GET /consultas/senhas-por-situacao?status=waiting`; método `findByStatusOrderByCreatedAtAsc` |

A criação retorna **201**, consultas e atualizações retornam **200**, e a exclusão retorna **204**, sem corpo. Dados inválidos retornam **400**, registros inexistentes **404** e conflitos de regra ou integridade **409**. Os corpos JSON, filtros, paginação e permissões estão detalhados em [documentacao-api.md](docs/documentacao-api.md).

A exclusão respeita os vínculos: uma categoria com serviços não pode ser removida, por exemplo. Senhas podem ser editadas ou excluídas enquanto aguardam; após a chamada, o histórico é preservado.

## Autenticação e regras de negócio

A API utiliza Spring Security com **HTTP Basic** e senhas armazenadas em BCrypt. O ADMIN administra cadastros e opera a fila. O ATENDENTE consulta a operação e realiza chamada, chegada e conclusão. Os endpoints administrativos exigem ADMIN; a operação da equipe exige autenticação. Emissão e acompanhamento pelo token continuam públicos para o cliente.

Para demonstrar a autorização, utilize `GET /api/admin/servicos` no Postman:

| Credencial enviada            | Resposta esperada |
| ----------------------------- | ----------------- |
| Sem autenticação              | 401 Unauthorized  |
| Usuário ou senha inválidos    | 401 Unauthorized  |
| `avaliador` com senha correta | 200 OK            |
| `operador` com senha correta  | 403 Forbidden     |

O login da interface utiliza sessão e cookie HttpOnly. O HTTP Basic é uma forma adicional de autenticação para as requisições da API. A configuração das duas formas está em [SegurancaApi.java](backend/src/main/java/br/edu/unifio/corteagil_backend/configuracoes/SegurancaApi.java) e [AutenticacaoServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/AutenticacaoServico.java).

Exemplos de regras implementadas na camada Service:

- Uma chamada só pode começar quando não há outro atendimento ativo.
- A conclusão exige confirmar antes a chegada do cliente.
- A pausa bloqueia novas emissões, preservando o atendimento em andamento.
- O retorno de um cliente ausente mantém o número e o coloca ao final da fila.
- O administrador não pode excluir a própria conta nem retirar o próprio acesso administrativo.

As operações da fila usam transações e bloqueio do estado para preservar a ordem e evitar números repetidos em emissões simultâneas. A auditoria registra ação, data e responsável. Consulte [FilaServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/FilaServico.java) e [AuditoriaServico.java](backend/src/main/java/br/edu/unifio/corteagil_backend/servicos/AuditoriaServico.java).

## Testes com Postman e MySQL

### Importar e reproduzir

1. Inicie o ambiente MySQL com o Compose e aguarde a API responder em `/api/state`.
2. Importe [postman/projeto-integrador.postman_collection.json](postman/projeto-integrador.postman_collection.json) no Postman.
3. Nas variáveis da coleção, confira `baseUrl = http://localhost:3000`, **sem** `/api` no final. Os caminhos das requisições já contêm esse prefixo.
4. Confira as variáveis `adminUsuario`, `adminSenha`, `atendenteUsuario` e `atendenteSenha`, preenchidas com as contas didáticas desta atividade.
5. Execute a coleção em ordem, pelo Collection Runner, com uma iteração. Os scripts capturam os IDs criados e validam os status HTTP.
6. Abra os exemplos de resposta de cada requisição para conferir os resultados salvos.

Use o banco de avaliação sem senha de atendimento ativa e com a emissão liberada. A coleção cria dados de teste, exercita os vínculos e conclui um atendimento; o histórico concluído permanece no banco. Os IDs são atualizados durante a execução, permitindo reproduzir a sequência.

| Requisições | O que demonstram                                                                                      |
| ----------- | ----------------------------------------------------------------------------------------------------- |
| 01–05       | Falta de autenticação, credenciais inválidas, dois perfis válidos e acesso administrativo negado      |
| 06–17       | Categoria e serviço, relacionamento, consulta por categoria, validação, duplicidade e integridade     |
| 18–23       | CRUD de balcão e confirmação de exclusão                                                              |
| 24–30       | CRUD de usuário e validação de e-mail                                                                 |
| 31–37       | Emissão, consulta, edição e exclusão de senha enquanto aguarda                                        |
| 38–52       | Atendimento completo, consulta por situação, pausa e conflitos de regras                              |
| 53–58       | Indicadores, auditoria, erros de consulta e exclusão dos cadastros de teste sem vínculo               |
| 59–65       | Listagem de serviços, parâmetros obrigatórios, campos inválidos, JSON e formatos de conteúdo/resposta |

As requisições 60 e 61 demonstram o tratamento de parâmetro obrigatório ausente (400) e Content-Type incompatível (415). As seguintes conferem mensagens por campo, JSON malformado, paginação inválida e formato de resposta incompatível (406). O campo `message` resume o problema; `errors`, quando presente, relaciona os nomes dos campos às orientações de correção.

### Respostas salvas e exportação

A coleção contém **65 requisições com respostas reais salvas em `response`**, capturadas em um MySQL 8.4.13. Elas aparecem como exemplos no Postman. As variáveis necessárias estão incluídas no JSON.

Para atualizar as evidências pelo Postman, execute as requisições, use **Save Response** e exporte a coleção em **Collection v2.1**, mantendo a opção **Share** das variáveis ativada. As credenciais exportadas devem continuar sendo apenas as contas didáticas da avaliação.

### Executar a coleção pelo terminal

Com Node.js e a API de avaliação em execução:

```sh
npx --yes newman@6.2.1 run postman/projeto-integrador.postman_collection.json
```

O Newman executa a coleção e os scripts de verificação de status. Para gerar um relatório JSON em um diretório ignorado pelo Git:

```sh
npx --yes newman@6.2.1 run postman/projeto-integrador.postman_collection.json --reporters cli,json --reporter-json-export backend/target/newman-avaliacao.json
```

O script opcional [postman/coletar-evidencias.py](postman/coletar-evidencias.py) executa as requisições e atualiza o JSON com as respostas recebidas. Ele exige Python 3, aceita somente destino local e verifica se a fila está disponível para os testes. A importação e execução no Postman não dependem de Python.

## Testes automatizados e relatórios

### Testes Java/Spring Boot

Os testes ficam em **[backend/src/test/java/br/edu/unifio/corteagil_backend/](backend/src/test/java/br/edu/unifio/corteagil_backend/)**. Utilizam Spring Boot Test, JUnit e MockMvc, com bancos H2 isolados. A execução HTTP com MySQL complementa esses testes e está na coleção Postman e no workflow. Execute os testes Java em um terminal ou configuração de IDE sem os perfis `mysql` e `vercel` ativos, para utilizar as configurações H2 definidas nas classes de teste.

| Arquivo                                                                                                            | Quantidade | Cobertura principal                                                                              |
| ------------------------------------------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------ |
| [ApiAvaliacaoTest.java](backend/src/test/java/br/edu/unifio/corteagil_backend/ApiAvaliacaoTest.java)               | 9 testes   | HTTP Basic, 401/403, categoria/serviço, consultas personalizadas, validações, integridade e fila |
| [CadastrosIntegracaoTest.java](backend/src/test/java/br/edu/unifio/corteagil_backend/CadastrosIntegracaoTest.java) | 5 testes   | CRUD, perfis, pesquisa/paginação, vínculos, preservação do histórico e recuperação de acesso     |
| [FilaIntegracaoTest.java](backend/src/test/java/br/edu/unifio/corteagil_backend/FilaIntegracaoTest.java)           | 7 testes   | Ordem da fila, ausência/retorno, pausa, emissão concorrente, avisos e eventos SSE                |
| [SessoesNuvemTest.java](backend/src/test/java/br/edu/unifio/corteagil_backend/SessoesNuvemTest.java)               | 1 teste    | Persistência e encerramento de sessão                                                            |

Para executar apenas os **22 testes Java**, com JDK 17+:

```sh
./backend/mvnw -B -f backend/pom.xml test
```

No PowerShell:

```powershell
.\backend\mvnw.cmd -B -f backend/pom.xml test
```

Para conferir a formatação Java, executar testes e gerar o JAR:

```sh
./backend/mvnw -B -f backend/pom.xml spotless:check verify
```

Os relatórios Maven são gerados em `backend/target/surefire-reports/`, em arquivos XML e texto por classe. O JAR executável é `backend/target/corte-agil.jar`. A pasta `target/` é gerada localmente e não é versionada.

### Interface e notificações

Os três testes do service worker ficam em [scripts/notificacoes-sw.test.mjs](scripts/notificacoes-sw.test.mjs), e o código correspondente em [public/notificacoes-sw.js](public/notificacoes-sw.js).

```sh
node --test scripts/notificacoes-sw.test.mjs
```

Para executar os testes Java, os testes do service worker e o build Angular:

```sh
npm ci
npm run check
```

O build é gerado em `dist/corte-agil/browser/`. A interface também foi conferida em computador e celular, incluindo login por sessão, cadastros, exclusão com retorno 204, emissão, atendimento, atualização do cliente e QR Code do cupom. Essa conferência no navegador é distinta dos testes automatizados listados acima.

### GitHub Actions

O workflow [.github/workflows/verificar.yml](.github/workflows/verificar.yml) executa, a cada push e pull request:

1. Instalação das dependências e build Angular.
2. Três testes do service worker.
3. Verificação de formatação e os 22 testes Java com Maven.
4. Inicialização do backend com os perfis `mysql,avaliacao` e um serviço MySQL real.
5. Coleta das 65 respostas HTTP e execução da coleção pelo Newman.
6. Publicação do artefato `avaliacao-mysql-postman`, com `aplicacao.log`, `colecao.json` e `newman.json`.

Para conferir, abra [Actions — Verificar projeto](https://github.com/Hagnarok/Corte-Agil/actions/workflows/verificar.yml), selecione uma execução e examine as etapas e o artefato. Selecione a execução correspondente ao commit entregue para conferir os resultados daquela versão.

## Roteiro para demonstração e entrega

1. Abra `backend/pom.xml` e a classe principal para identificar o projeto Spring Boot.
2. Apresente as cinco entidades e seus relacionamentos pelo [diagrama](docs/arquitetura.md#classes-de-negócio).
3. Inicie a aplicação com MySQL e mostre o CRUD na coleção Postman ou na interface.
4. Demonstre 401 sem autenticação, 200 com ADMIN e 403 com ATENDENTE em uma operação administrativa.
5. Mostre as duas consultas personalizadas, um erro de validação e um conflito de integridade.
6. Execute emissão → chamada → chegada → conclusão e consulte a auditoria.
7. Apresente os testes Java, as respostas salvas e o relatório de uma execução do GitHub Actions.

Materiais complementares: [documentação dos endpoints](docs/documentacao-api.md), [arquitetura e diagrama](docs/arquitetura.md), [manual da interface](docs/manual.md) e [roteiro de avaliação Spring Boot](docs/avaliacao-spring.md).

A demonstração publicada está em [corte-agil-rose.vercel.app](https://corte-agil-rose.vercel.app). As credenciais de produção devem ser fornecidas separadamente; as contas de avaliação são as indicadas neste README e usadas no ambiente MySQL local.

Conforme o [enunciado](https://hackmd.io/@srdelfino/SyMXGnkAzg), um integrante deve entregar o link deste repositório no Ambiente Conhecer, na atividade **Projeto Integrador**. O código, o diagrama, a documentação e a coleção Postman estão incluídos no repositório. Todos os integrantes devem compreender a implementação e estar preparados para explicar as decisões e as regras do sistema.
