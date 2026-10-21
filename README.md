# CorteÁgil — Angular Material

Sistema de gestão da fila do açougue, baseado nos dois PDFs fornecidos. Aplicação Angular 22.2.3 com Angular Material 22.2.3 e módulos NgModule carregados sob demanda. API Node.js 24 com Express, SQLite e atualização em tempo real por Server-Sent Events (SSE).

## Começar no Windows

1. Instale **Node.js 24 LTS (24.15 ou superior)** (https://nodejs.org). Confirme `node -v` e `npm -v` no terminal.
2. Clone este repositório ou baixe e extraia o ZIP do GitHub. Abra a pasta no VS Code. O terminal deve estar no diretório que contém `package.json`; não há uma segunda pasta `corte-agil` dentro do projeto.
3. Execute:

```powershell
npm ci
npm run setup
npm run build
npm start
```

O comando `setup` cria `.env` com uma senha aleatória e mostra as credenciais. Usuário: **admin**. Guarde a senha exibida; ela fica no campo `ADMIN_PASSWORD` do `.env`. O comando não sobrescreve uma configuração existente.

Abra **http://localhost:3000**. A compilação gera `dist/`, que não é enviada ao GitHub. A primeira execução começa com **fila vazia e dados reais de uso**; os exemplos mostrados na tela de login são apenas ilustrativos.

### Usar no GitHub Codespaces

Abra **Code → Codespaces → Create codespace on main**. No terminal, execute os quatro comandos acima na raiz do projeto. Após `npm start`, abra a aba **PORTS/PORTAS** e clique no endereço da porta **3000**. Mantenha a visibilidade da porta como **Private**. Os arquivos do projeto são públicos, mas isso não exige publicar o servidor ou seus dados.

Para desenvolver com recarga automática, execute `npm run dev` em outro terminal e abra a porta **4200**. A API deve continuar rodando na porta 3000.

Para trocar a senha, altere `ADMIN_PASSWORD` no `.env` (mínimo 10 caracteres), salve e reinicie o servidor. Não publique esse arquivo. As sessões expiram em 8 horas e são encerradas quando o servidor reinicia.

Se o PowerShell bloquear `npm.ps1`, use `npm.cmd` nos comandos ou abra o terminal Prompt de Comando. Não é necessário desativar políticas de segurança.

## Endereços

| Endereço | Interface | Acesso |
| --- | --- | --- |
| `/entrar` | Login da equipe | Público |
| `/funcionario` | Gestão, chamadas, presença, ausência, histórico e filtros | Login obrigatório |
| `/cliente` | Retirada de senha e acompanhamento | Sem cadastro |
| `/painel` | TV, senha atual, próximas, som e tela cheia | Público |
| `/totem` | Retirada, ticket e QR Code | Público |

## Teste completo em poucos minutos

1. Em `/cliente`, escolha ou não um serviço e retire a senha. Mantenha a página aberta.
2. Em outra aba, abra `/totem`, retire outras senhas e veja que todas entram na mesma fila.
3. Entre em `/funcionario` com `admin` e sua senha. Clique em **Chamar próxima senha**.
4. Observe a mudança em `/painel` e na página do cliente. No painel, clique em **Ativar som** uma vez. O navegador exige interação antes de liberar áudio.
5. Clique em **Cliente chegou · iniciar**, depois em **Concluir atendimento**.
6. Para testar ausência, chame outra senha e aguarde os **30 segundos reais**. Clique em **Marcar ausente**. Na página dessa senha, clique em **Quero retornar à fila**.
7. O retorno mantém o número e coloca a senha no **final da fila**. A entrada original continua registrada.
8. Atualize as páginas e reinicie o servidor: a fila e o histórico continuam salvos no SQLite.

O navegador do cliente lembra a senha mais recente. Para representar clientes diferentes, use celulares ou perfis/janelas anônimas diferentes, ou os links individuais dos tickets do totem.

## Alterar o código e ver as mudanças

Dois terminais:

```powershell
# Terminal 1: API e banco de dados
npm start
```

```powershell
# Terminal 2: Angular com recarga automática
npm run dev
```

Abra **http://localhost:4200** durante o desenvolvimento. O proxy encaminha `/api` ao servidor da porta 3000. Use sempre o mesmo endereço para todas as interfaces de um teste; uma sessão iniciada em `localhost` não é uma sessão em `127.0.0.1`.

Para atualizar a versão servida na porta 3000:

```powershell
npm run build
npm start
```

Se o servidor já estiver rodando, encerre com Ctrl+C antes de iniciá-lo novamente.

## Usar no celular e na TV na rede local

No computador servidor, execute `ipconfig` e localize o IPv4 da conexão, por exemplo `192.168.1.50`. Conecte o celular e a TV à mesma rede e abra:

```text
http://192.168.1.50:3000/cliente
http://192.168.1.50:3000/painel
http://192.168.1.50:3000/totem
```

No Firewall do Windows, permita a porta TCP 3000 **somente na rede privada**. Mantenha o computador servidor ligado. Configure reserva de IP no roteador se quiser endereços fixos. Redes Wi-Fi com isolamento de clientes precisam permitir o acesso ao servidor. Não use encaminhamento de portas para a internet.

**Abra o totem pelo IP ou nome do servidor usado pelos celulares**, pois o QR Code usa o endereço do navegador. Um ticket emitido em `localhost` gera um link `localhost`, que não serve para outro dispositivo.

O sistema não depende de internet durante o uso, depois da instalação. Ícones, estilos, bibliotecas e QR Code são locais. A primeira instalação por `npm ci` precisa de internet.

## Organização e criação de módulos/componentes

Consulte [docs/GUIA_MODULOS.md](docs/GUIA_MODULOS.md), com árvore, responsabilidades, comandos Angular CLI, exemplos de componentes, serviços e fluxo de dados.

## Implementação dos requisitos e decisões

Consulte [docs/REQUISITOS.md](docs/REQUISITOS.md) e [docs/ARQUITETURA.md](docs/ARQUITETURA.md). Algumas decisões de negócio não estavam completamente definidas nos PDFs; elas estão documentadas para revisão.

- Fila única com sequência persistente; não há reset diário que possa repetir senhas ainda ativas.
- FIFO: o tipo de serviço ajuda a visualizar e filtrar, mas não muda automaticamente a prioridade.
- Ausência é uma ação manual do funcionário após 30 segundos, validada também no servidor.
- Retorno solicitado pelo cliente entra no final da fila, mantendo a mesma senha.
- Pausar bloqueia novas emissões e retornos, mas permite concluir e chamar atendimentos já existentes.
- Estimativa inicial: 4 minutos por atendimento. Após conclusões, usa a média dos últimos 20 atendimentos concluídos no dia, com mínimo de 1 minuto. É estimativa, não promessa.
- Datas dos indicadores diários usam `America/Sao_Paulo`.

## Notificações, som e impressora

**Avisos dentro da página** funcionam pela rede local: com duas e uma senhas à frente, chamada, ausência, retorno e conclusão. Mantenha a página aberta. Os eventos ficam persistidos e podem ser vistos novamente.

**Notificações do sistema operacional** dependem de permissão do navegador e contexto seguro, geralmente HTTPS. `localhost` pode ser tratado como seguro, mas `http://192.168...` não. Não há Web Push ou Service Worker para página fechada/tela bloqueada. Ao retornar à página, o estado é atualizado; para operação real com tela bloqueada, será necessária uma integração adicional, por exemplo Web Push em HTTPS. A fila continua funcionando em HTTP local com avisos visuais.

**Som do painel:** clique em Ativar som antes de começar. O alerta é sintetizado localmente com Web Audio; pode ser testado no mesmo botão. Clique em tela cheia ou pressione F11.

**Impressão:** o ticket contém QR Code real e folha de estilo para 80 mm. A impressão abre o diálogo do navegador; selecione a impressora, tamanho do papel e desative cabeçalhos/rodapés. Não há driver ESC/POS, envio USB direto ou impressão silenciosa implementados. Para 58 mm, ajuste `@media print` em `src/styles.scss` e valide no equipamento real. No totem compartilhado, finalize a impressão e clique em Próximo cliente para limpar a tela.

## Dados, backup e segurança

O banco fica em `data/corte-agil.sqlite`. Para backup simples, encerre o servidor com Ctrl+C e copie **a pasta `data` inteira**, junto com a configuração `.env`, para um local seguro. Para restaurar, pare o servidor, substitua a pasta `data` pelo backup e inicie novamente. Nunca restaure um banco enquanto estiver em uso.

- A autenticação é verificada no servidor, além da proteção de rota Angular. Cookies de sessão HttpOnly/SameSite; senha conferida com scrypt e comparação em tempo constante.
- Os links de acompanhamento usam tokens aleatórios; o token não aparece no estado público da fila nem nos logs de auditoria.
- O link é uma credencial de acompanhamento. Quem o possui pode consultar e solicitar retorno daquela senha. Não o divulgue.
- O sistema não solicita nome, telefone, CPF ou outros dados pessoais do cliente.
- O histórico registra emissão, chamada, presença, conclusão, ausência, retorno e pausa; mostra os últimos 100 e mantém até 2.000 eventos.
- Requisições de escrita com origem externa são bloqueadas; há limite de tentativas de login, emissões e retornos por IP.
- Os tickets permanecem no banco para recuperação. Não há expurgo automático implementado; para uso prolongado é preciso definir política de retenção.
- É um projeto funcional de referência para estudo e piloto local. Para implantação definitiva, valide equipamentos, carga, HTTPS, gestão de usuários, backups automatizados e monitoramento no ambiente do estabelecimento.

## Validação

```powershell
npm test
npm run build
# Ou ambos:
npm run check
```

Os testes cobrem domínio e API real: unicidade, concorrência, login, autorização, notificações, SSE, 30 segundos, retorno com penalização e recuperação depois do reinício. SQLite é fornecido pelo próprio Node 24; uma eventual mensagem de API experimental do Node não significa falha do sistema.

Projeto preparado para o grupo: Guilherme Perino Feitosa, Gustavo Henrique de Araujo Alves, Matheus Boletti Fortes e Luiz Felipe Gaspar Fernandes da Silva.
