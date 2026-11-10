# API REST do Corte Ágil

Base local de avaliação: `http://localhost:3000/api`. Formato de dados: JSON. As credenciais de teste e o comando para iniciar com MySQL estão no README.

## Segurança

A API usa Spring Security e HTTP Basic com usuários persistidos em `Usuario`. ADMIN mantém cadastros; ATENDENTE consulta e opera a fila. O perfil `avaliacao` cria duas contas didáticas para a atividade. O envio de Basic Auth é independente do login por cookie da interface; cada requisição Basic deve enviar suas credenciais. Em serviços publicados, utilize HTTPS.

A interface continua utilizando `POST /login`, `GET /session` e `POST /logout`. O filtro de segurança reconhece sessões válidas e o contexto autenticado por Basic. Desativação de conta e mudança de senha invalidam sessões; senhas ficam em BCrypt. Pedidos de escrita vindos do navegador passam também pelo filtro de origem. Tentativas Basic inválidas são limitadas por endereço.

| Código | Significado                               |
| ------ | ----------------------------------------- |
| 200    | Consulta ou atualização concluída         |
| 201    | Cadastro ou senha criada                  |
| 204    | Exclusão concluída, sem corpo             |
| 400    | Dados, parâmetros ou serviço inválidos    |
| 401    | Sem autenticação ou credenciais inválidas |
| 403    | Autenticado sem o perfil exigido          |
| 404    | Registro não encontrado                   |
| 409    | Conflito de regra ou integridade          |
| 429    | Limite de tentativas atingido             |

Os erros usam `{ "message": "descrição do problema" }`. `RegraNegocioException` representa falhas de domínio; `TratadorExcecoes` centraliza os retornos com `@RestControllerAdvice`.

## Cadastros administrativos

Substitua `{tipo}` por `categorias`, `servicos`, `balcoes` ou `usuarios`. Todos os endpoints desta tabela exigem ADMIN.

| Método | Caminho              | Uso                                     |
| ------ | -------------------- | --------------------------------------- |
| GET    | `/admin/{tipo}`      | Lista com `q`, `ativo`, `page` e `size` |
| GET    | `/admin/{tipo}/{id}` | Consulta individual                     |
| POST   | `/admin/{tipo}`      | Criação; retorna 201 e o registro       |
| PUT    | `/admin/{tipo}/{id}` | Atualização; retorna 200                |
| DELETE | `/admin/{tipo}/{id}` | Exclusão sem vínculos; retorna 204      |

Página começa em zero; tamanho aceito de 1 a 50. `ativo` aceita `true`, `false` ou vazio. Nome é obrigatório e tem até 80 caracteres. Usuário recebe e-mail válido quando informado, perfil ADMIN/ATENDENTE e senha de pelo menos dez caracteres; ao editar, senha vazia mantém a atual.

Categoria ou balcão:

```json
{ "nome": "Cortes bovinos", "ativo": true }
```

Serviço relacionado a uma categoria:

```json
{ "nome": "Cubos para panela", "ativo": true, "categoriaId": "id-da-categoria" }
```

A categoria é retornada junto com o serviço. Quando não informada em pedidos antigos, a aplicação mantém a categoria existente ou utiliza a categoria inicial. Isso preserva a compatibilidade da interface e de instalações anteriores.

Usuário de teste:

```json
{
  "nome": "auxiliar",
  "email": "auxiliar@example.com",
  "senha": "Auxiliar-Test123",
  "perfil": "ATENDENTE",
  "ativo": true
}
```

Nenhuma resposta expõe hash de senha ou versão da sessão.

## Senhas de atendimento

| Método | Caminho                   | Acesso e resultado                                      |
| ------ | ------------------------- | ------------------------------------------------------- |
| POST   | `/tickets`                | Público; emite senha, retorna 201 com `token` e `label` |
| GET    | `/tickets/{token}`        | Público com token privado; acompanha posição e avisos   |
| GET    | `/gestao/senhas`          | Equipe; lista por `q`, `status`, `page`, `size`         |
| GET    | `/gestao/senhas/{id}`     | Equipe; consulta individual sem token privado           |
| PUT    | `/gestao/senhas/{id}`     | ADMIN; altera serviço de senha aguardando               |
| DELETE | `/gestao/senhas/{id}`     | ADMIN; cancela senha aguardando, retorna 204            |
| POST   | `/tickets/{token}/return` | Cliente ausente volta ao final da fila                  |

Emissão:

```json
{ "service": "Moído", "origin": "app" }
```

`origin` aceita `app` ou `totem`. Edição recebe apenas `{ "service": "Peça inteira" }`. As situações são `waiting`, `called`, `serving`, `done` e `absent`. O token de acompanhamento é diferente do ID público usado na gestão.

## Consultas personalizadas Spring Data JPA

Exigem equipe autenticada:

| Endpoint                                            | Método no repository              | Finalidade                                          |
| --------------------------------------------------- | --------------------------------- | --------------------------------------------------- |
| GET `/consultas/servicos-por-categoria/{id}`        | `findByCategoriaIdOrderByNomeAsc` | Identificar serviços de um grupo de cortes/preparos |
| GET `/consultas/senhas-por-situacao?status=waiting` | `findByStatusOrderByCreatedAtAsc` | Consultar senhas por etapa do atendimento           |

Os controllers chamam Services; os Services fazem a validação e consultam os repositories. Categoria inexistente gera 404 e situação desconhecida gera 400. Outros métodos personalizados já usados incluem busca de usuário por nome/e-mail e bloqueio pessimista do estado da fila.

## Operação específica: fila

A equipe utiliza `POST /queue/{acao}`. As ações são `call`, `recall`, `start`, `finish`, `absent` e `pause`. A chamada seleciona a próxima senha pela ordem da fila. `GET /state` fornece o painel público; `GET /events` fornece SSE; `GET /audit` exige equipe e retorna os últimos cem eventos com responsável e data.

Regras implementadas nos Services:

1. Há um único atendimento ativo: uma nova chamada antes de terminar o atual retorna 409.
2. Só é possível concluir após confirmar a chegada; concluir diretamente uma senha chamada retorna 409.
3. Ausência só é permitida depois dos trinta segundos da chamada; o retorno mantém o número e vai ao fim da fila.
4. Pausa impede novas emissões e retornos, sem impedir concluir atendimentos existentes.
5. Não é permitido excluir categoria ligada a serviços, serviço ligado a senhas ou usuário ligado ao histórico. A chave estrangeira impede a quebra dos vínculos e a API retorna 409; use desativação quando aplicável.
6. Senhas chamadas, em atendimento, concluídas ou ausentes não podem ser excluídas. A exclusão é admitida somente enquanto aguardam, sem reaproveitar o número.
7. O administrador não pode excluir a própria conta nem retirar o próprio perfil administrativo.

A auditoria, o estado da fila e os tokens de redefinição são registros de infraestrutura e histórico. Não são cadastros de negócio e não possuem edição ou exclusão pública arbitrária. As cinco entidades de negócio gerenciadas por CRUD são CategoriaServico, Servico, Balcao, Usuario e Senha.

## Evidências reproduzíveis

A coleção JSON contém 58 requisições com exemplos reais em `response`, variáveis e captura automática de IDs. A execução não exige alterar código JavaScript: importe, ajuste `baseUrl` e rode na ordem. As contas e senhas incluídas são exclusivamente didáticas. O coletor registra respostas reais; não inventa resultados esperados. Depois de executar pelo Postman, é possível usar Save Response para atualizar os exemplos e exportar a coleção.
