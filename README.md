# Corte Ágil

Sistema de organização da fila de atendimento de açougues. O cliente retira uma senha no celular ou no totem, acompanha sua posição e recebe os avisos da chamada. A equipe controla chegada, atendimento, conclusão, ausência e retorno à fila.

O backend REST aplica essas regras, mantém os cadastros, registra a auditoria e calcula os indicadores da operação. Foi adaptado ao domínio do PI a partir da organização de entidades, repositórios e recursos apresentada no [projeto-base da disciplina](https://github.com/sergiodelfino/ecommerce-backend-4a/tree/main/ecommerce). A adaptação acrescenta Services, DTOs, validação, tratamento de exceções e segurança.

## Tecnologias

- Java 17 ou superior, Spring Boot 3.5, Spring MVC, Spring Data JPA/Hibernate e Maven Wrapper.
- MySQL 8.4 para execução e avaliação desta atividade.
- Spring Security com HTTP Basic e perfis ADMIN e ATENDENTE; o login por sessão da interface foi preservado.
- Bean Validation, BCrypt e JUnit/MockMvc.
- Angular com NgModules, `standalone: false` e Angular Material cyan.
- Postman Collection v2.1 e Newman para reprodução das requisições.

## Execução para a avaliação com MySQL

É necessário Docker com Docker Compose. Na raiz do repositório:

```sh
docker compose -f compose.avaliacao.yml up --build -d
```

A interface e a API ficam em http://localhost:3000. O MySQL usa um volume exclusivo de avaliação e também é acessível em `127.0.0.1:3307`. Não apague o volume se quiser preservar as evidências de atendimento.

As credenciais abaixo são **apenas contas didáticas**, criadas quando o perfil `avaliacao` está ativo:

| Usuário   | Senha de teste         | Perfil    |
| --------- | ---------------------- | --------- |
| avaliador | Avaliacao-Admin123     | ADMIN     |
| operador  | Avaliacao-Atendente123 | ATENDENTE |

No Postman, selecione **Basic Auth**. Na interface, entre em `/entrar` com uma dessas contas. O perfil de avaliação não é ativado na Vercel e essas senhas não são as credenciais de produção. Não publique senhas pessoais no repositório.

Para encerrar sem apagar os dados:

```sh
docker compose -f compose.avaliacao.yml down
```

### Executar o Java sem container

Com Node 24, JDK 17+ e o MySQL do Compose rodando:

```sh
docker compose -f compose.avaliacao.yml up -d mysql
npm ci
npm run build
```

Copie `.env.avaliacao.example` para `.env` somente numa cópia local de avaliação, preservando qualquer configuração existente. Execute `npm start`. O script carrega esse arquivo e inicia o Spring Boot com `mysql,avaliacao`. Se usar um MySQL próprio, altere `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`.

A configuração padrão local com H2 e o perfil `vercel` com PostgreSQL continuam disponíveis para as instalações anteriores. **Para comprovar o requisito MySQL desta prova, use o ambiente de avaliação acima.** O site publicado usa PostgreSQL e não substitui essa validação.

## API e evidências

1. Importe [a coleção Postman](postman/projeto-integrador.postman_collection.json).
2. Confira a variável `baseUrl`, cujo padrão é `http://localhost:3000`.
3. Execute as requisições na ordem indicada, ou utilize o Collection Runner. Os scripts atualizam os IDs e conferem os status HTTP.
4. Os exemplos já incluídos em `response` foram capturados de respostas reais de um MySQL 8.4.13. No Postman, ficam disponíveis como exemplos de resposta. Para atualizar evidências, use **Save Response** e exporte novamente em Collection v2.1, mantendo os valores das variáveis compartilhados.

As 58 requisições cobrem CRUD das cinco entidades, consultas individuais e listagens, duas consultas personalizadas, regras de negócio, erros 400/404/409, autenticação, diferença entre 401 e 403 e fluxo completo de atendimento.

```sh
npx --yes newman@6.2.1 run postman/projeto-integrador.postman_collection.json
```

Use um banco local sem fila ativa para reproduzir os testes. O coletor opcional `python postman/coletar-evidencias.py` atualiza os exemplos com respostas reais; ele recusa endereços externos e uma fila ativa. Não execute as contas didáticas em produção.

- [Documentação dos endpoints e regras](docs/documentacao-api.md)
- [Diagrama e organização das classes](docs/arquitetura.md)
- [Roteiro de avaliação e resultados](docs/avaliacao-spring.md)
- [Manual da interface](docs/manual.md)

## Testes automatizados

```sh
npm ci
npm run check
```

Os testes Java usam bancos H2 isolados e verificam fila, concorrência, avisos, cadastros, HTTP Basic, permissões, integridade e sessões. Os testes HTTP da coleção também foram reproduzidos contra MySQL real. A interface foi validada com MySQL, incluindo login por sessão, categorias, exclusão 204, emissão, atendimento, atualização do cliente e QR Code obrigatório.

## Integrantes

| Integrante                                              | ID acadêmico |
| ------------------------------------------------------- | ------------ |
| Gustavo Henrique de Araujo Alves                        | A informar   |
| Demais integrantes do grupo — confirmar nomes completos | A informar   |

Antes da entrega, complete os nomes e IDs acadêmicos de todos os participantes.

## Entrega

O enunciado desta atividade pede o link deste repositório no Ambiente Conhecer, na atividade **Projeto Integrador**. A coleção Postman e a documentação estão incluídas aqui. A demonstração web está em https://corte-agil-rose.vercel.app; as credenciais de produção devem ser informadas separadamente ao professor.
