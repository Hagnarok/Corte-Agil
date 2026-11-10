# Corte Ágil

## Instalação

Requisitos: Node 24, JDK 17 ou superior e Git. O Maven Wrapper do projeto baixa o Maven na primeira execução.

```sh
git clone https://github.com/Hagnarok/Corte-Agil.git
cd Corte-Agil
npm ci
npm run setup
npm run build
npm start
```

Abra http://localhost:3000. O setup informa a senha inicial do usuário `admin` e guarda a configuração em `.env`. Não publique esse arquivo. O banco H2 relacional fica em `data/corte-agil.mv.db`; pare a aplicação antes de copiar esse arquivo para backup. Alterar `ADMIN_PASSWORD` depois da criação do banco não muda a senha de uma conta existente: use a redefinição na interface.

Para desenvolver a interface com recarga automática, mantenha `npm start` em um terminal e execute `npm run dev` em outro. A porta 4200 encaminha `/api` para a porta 3000.

## Perfis

| Ação                                                            | Administrador | Atendente | Público |
| --------------------------------------------------------------- | ------------- | --------- | ------- |
| Operar chamada, chegada, conclusão, ausência, pausa e histórico | Sim           | Sim       | Não     |
| CRUD de usuários, serviços e balcões                            | Sim           | Não       | Não     |
| Editar ou cancelar senha aguardando                             | Sim           | Não       | Não     |
| Gerar link de redefinição de usuário                            | Sim           | Não       | Não     |
| Retirar e acompanhar a própria senha                            | Sim           | Sim       | Sim     |
| Consultar painel de chamada                                     | Sim           | Sim       | Sim     |

O bloqueio existe no backend e nas rotas da interface. Clientes usam o token privado da própria senha; não precisam de uma conta de funcionário.

## Cadastros

Entre como administrador e abra **Cadastros**. Selecione Serviços, Balcões, Usuários ou Senhas. Use pesquisa, situação e os botões Anterior/Próxima. A página mostra dez registros por vez.

Use **Novo cadastro**, preencha os campos e salve. Em Usuários, selecione Administrador ou Atendente, informe uma senha de pelo menos dez caracteres e, se houver, o e-mail de recuperação. **Editar** altera os dados; deixar a senha vazia preserva a senha atual. **Excluir** remove registros sem vínculos. Se houver histórico, desative o cadastro: a API impede a exclusão que quebraria relacionamentos. A própria conta administradora não pode ser excluída ou perder seu perfil.

Senhas são criadas em Cliente/Totem. É possível alterar o serviço ou excluir uma senha enquanto estiver aguardando. Senhas chamadas, em atendimento, concluídas e ausentes mantêm o histórico. Os números excluídos não são reutilizados.

## Atendimento

1. Abra `/cliente` no celular ou `/totem` no terminal de emissão. Escolha um serviço ativo e retire a senha.
2. No totem, escolha papel 58 ou 80 mm. O cupom compacto sempre inclui QR Code com acesso ao acompanhamento da senha.
3. No cliente, ative som e notificações. O navegador pede permissão. Acompanhe a página durante a espera.
4. Em `/funcionario`, use **Chamar próximo**. O painel `/painel` acompanha a chamada automaticamente.
5. Confirme a chegada para iniciar, depois conclua. Para ausência, aguarde os trinta segundos de chamada. O cliente ausente pode retornar ao fim da fila com o mesmo número.
6. Pausar senhas bloqueia novas emissões e retornos; permite terminar os atendimentos existentes.

A fila segue a ordem de chegada. Existe um atendimento ativo por vez. O balcão ativo disponível é vinculado automaticamente à senha; o cadastro de vários balcões não habilita filas paralelas. O vínculo do funcionário é registrado na chamada e no início do atendimento.

O dashboard mostra quatro indicadores: aguardando agora, atendidos hoje, tempo médio e ausentes. O tempo médio usa os últimos vinte atendimentos concluídos no dia; sem dados, usa quatro minutos. O dia considera America/Sao_Paulo. **Histórico** mostra os últimos cem eventos, data, hora e responsável. Registros anteriores à implantação da auditoria de usuários podem não ter responsável identificado.

## Recuperação de senha

Configure `PUBLIC_BASE_URL` em `.env` com a URL pública HTTPS, ou `http://localhost:3000` no computador local. Em Codespaces, use o endereço HTTPS encaminhado da porta 3000.

Sem SMTP, o administrador pode abrir Usuários e clicar em **Redefinir senha**. Entregue o link apenas ao titular da conta. O link abre a tela de nova senha, expira em quinze minutos e funciona uma vez. A alteração invalida as sessões anteriores. Se nenhum administrador conseguir entrar, é necessário recuperar acesso com o responsável pelo banco; não existe senha de emergência pública.

Para habilitar **Esqueci minha senha** por e-mail, configure:

```dotenv
SPRING_PROFILES_ACTIVE=email
PUBLIC_BASE_URL=https://seu-endereco-publico
SMTP_HOST=seu-servidor-smtp
SMTP_PORT=587
SMTP_USERNAME=seu-usuario
SMTP_PASSWORD=sua-credencial
MAIL_FROM=seu-remetente-autorizado
```

Use as credenciais nas variáveis protegidas da hospedagem. Sem o perfil/configuração SMTP, a interface informa a indisponibilidade e orienta solicitar um link ao administrador. Os testes usam um remetente simulado; entrega em uma caixa real precisa ser validada com a conta configurada.

## Banco e atualização

O backend é Spring Boot com JPA. H2 é um banco relacional persistente, não uma lista em memória. Para MySQL, configure `SPRING_PROFILES_ACTIVE=mysql`, `DATABASE_URL` no formato JDBC, `DATABASE_USERNAME` e `DATABASE_PASSWORD`. Para SMTP junto com MySQL, use `mysql,email`.

Antes de atualizar uma instalação existente, pare o servidor e faça backup de `data` e `.env`. Depois execute `git pull --ff-only`, `npm ci`, `npm run build` e `npm start`. A atualização acrescenta tabelas e relacionamentos com `ddl-auto=update`, sem limpar as senhas existentes. Usuários de versões anteriores recebem o admin inicial de `ADMIN_PASSWORD` na primeira abertura. Na importação de SQLite legado, o script mantém a exportação em `data/legacy-queue.json`; não apague o banco antigo antes de conferir os dados.

## Publicação

O repositório inclui um Dockerfile que compila Angular e Spring Boot e entrega ambos na mesma origem. Para verificar localmente:

```sh
docker build -t corte-agil .
docker volume create corte-agil-data
docker run --rm -p 3000:3000 --env-file .env -v corte-agil-data:/app/data corte-agil
```

Na hospedagem compatível com Docker/Java, conecte este repositório e use o Dockerfile. Configure `PORT`, `ADMIN_PASSWORD`, `PUBLIC_BASE_URL` e `ALLOWED_ORIGINS` com a URL HTTPS real. Use disco persistente em `/app/data` para H2 ou um MySQL externo com backup. Sem persistência, os dados serão perdidos em uma recriação do container. Mantenha uma única instância para a fila e suas conexões SSE. O proxy deve permitir conexões SSE longas sem buffering. A verificação HTTP é `GET /api/state`.

A aplicação foi publicada em https://corte-agil-rose.vercel.app. O acesso da equipe fica em `/entrar`, com usuário `admin` e a senha configurada pelo responsável na primeira inicialização. As credenciais de avaliação devem ser entregues pelo AVA, sem publicação neste repositório. A publicação atual usa envio de código pela API da Vercel; a integração GitHub–Vercel ainda precisa ser habilitada para que novos pushes gerem deploy automaticamente.

As notificações dependem de HTTPS (localhost é aceito), permissão do usuário e suporte do navegador. O service worker permite exibir avisos nos navegadores compatíveis; o transporte é SSE/polling enquanto a aplicação está aberta, sem Web Push para navegador encerrado. No iPhone, a permissão pode exigir instalar o site na tela inicial. Confirme também a saída física nas impressoras 58 e 80 mm: a prévia de impressão não substitui o teste do equipamento.

## Verificações

```sh
npm run check
npm run backend:format
```

`npm run check` compila o Angular e executa testes de service worker e testes Spring/H2 de fila, avisos, concorrência, perfis, CRUD, auditoria e redefinição de senha. O workflow do GitHub executa a compilação e os testes a cada push e pull request.

## Vercel

Use o preset Container, com `PORT=3000` e `SPRING_PROFILES_ACTIVE=vercel`. Vincule o Neon ao ambiente Production; o perfil utiliza `PGHOST`, `PGPORT` (padrão 5432), `PGDATABASE`, `PGUSER` e `PGPASSWORD`, sem expor credenciais ao Angular. Configure `ADMIN_PASSWORD`, `PUBLIC_BASE_URL` e `ALLOWED_ORIGINS` nas variáveis do projeto. As sessões ficam em tabelas Spring Session no PostgreSQL. A sincronização HTTP complementa SSE para atender múltiplas instâncias. O H2 local continua disponível sem o perfil vercel. A publicação não importa automaticamente dados do computador.
