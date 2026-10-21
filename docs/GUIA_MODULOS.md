# Guia de criação e separação — Angular Material

## 1. Como o projeto está organizado

```text
src/app/
  app.module.ts                         # Bootstrap e rotas principais
  core/
    models/fila.model.ts                # Interfaces: Senha, MinhaSenha, EstadoFila
    guards/auth.guard.ts                # Proteção da rota de funcionário
    services/
      api.service.ts                    # HTTP, erros e MatSnackBar
      auth.service.ts                   # Login, sessão, logout
      fila.service.ts                   # Estado compartilhado com signals e SSE
      alerta.service.ts                 # Som e notificações do navegador
  shared/
    material/material.module.ts         # Agrupa os imports do Angular Material
    shared.module.ts                    # Exporta recursos comuns
    components/
      status-badge.component.ts         # Situação da senha
      queue-list.component.ts           # Lista de senhas reutilizável
      queue-list.component.html
  layout/
    shell.component.ts                 # Menu lateral, cabeçalho, rodapé
    shell.component.html
  features/
    auth/
      auth.module.ts
      auth.component.ts
      auth.component.html
    funcionario/
      funcionario.module.ts
      funcionario.component.ts
      funcionario.component.html
      components/
        stat-card.component.ts          # Card de indicador reutilizado 4 vezes
        atendimento-atual.component.ts  # Botões e temporizador do balcão
        atendimento-atual.component.html
    cliente/
      cliente.module.ts
      cliente.component.ts
      cliente.component.html
    painel/
      painel.module.ts
      painel.component.ts
      painel.component.html
    totem/
      totem.module.ts
      totem.component.ts
      totem.component.html
src/styles.scss                          # Tema Material e estilos responsivos
server/
  domain.mjs                             # Regras puras da fila
  index.mjs                              # API, autenticação, SSE e SQLite
  setup.mjs                              # Cria a senha e configuração inicial
  domain.test.mjs
  integration.test.mjs
```

**Módulo organiza; componente apresenta; serviço compartilha comportamento; servidor aplica regras e salva dados.**

- Um módulo de funcionalidade importa `SharedModule` e declara os seus componentes.
- `SharedModule` exporta formulários, roteamento, Material e componentes comuns.
- Serviços em `core/services` usam `providedIn: 'root'`: uma instância para toda a aplicação. Não os repita em `providers` dos módulos, pois isso pode criar estados de fila separados.
- As regras de concorrência, numeração e ausência estão no servidor. Uma desabilitação de botão no HTML é apenas parte da experiência; o backend precisa validar novamente.
- Componentes com template curto, como badge e indicador, usam template inline. As páginas têm arquivo HTML separado.

## 2. Por que NgModule neste projeto?

O Angular também oferece componentes standalone e os recomenda para novos projetos. Aqui foram usados **NgModules explícitos** para atender ao pedido de separação em módulos e facilitar o estudo dessa organização. Isso não impede o uso de signals, `@if`, `@for`, formulários reativos, lazy loading e Angular Material.

Não misture a declaração de um componente standalone no array `declarations`. Neste projeto os componentes usam `standalone: false` e são declarados em exatamente um módulo.

Documentação oficial: https://angular.dev/guide/ngmodules/overview

## 3. Comandos para reconstruir a estrutura do zero

**O ZIP já está pronto. Não execute estes comandos nele para recriar arquivos existentes.** Use-os em um projeto novo para estudar o processo. Dentro do projeto entregue, use apenas comandos para funcionalidades novas.

```powershell
npx @angular/cli@22.2.3 new corte-agil --no-standalone --routing --style=scss --skip-tests
cd corte-agil
npx ng add @angular/material@22.2.3
```

A versão do ZIP é fixada em 22.2.3 no `package-lock.json`; o assistente `ng add` pode produzir diferenças no tema e nos arquivos de bootstrap. O projeto entregue já contém um tema personalizado.

```powershell
npx ng generate module shared/material
npx ng generate module shared

npx ng generate module features/auth --routing
npx ng generate module features/funcionario --routing
npx ng generate module features/cliente --routing
npx ng generate module features/painel --routing
npx ng generate module features/totem --routing
```

Crie os componentes, informando explicitamente o módulo:

```powershell
npx ng generate component features/auth/auth --flat --standalone=false --type=component --module=features/auth/auth.module --skip-tests
npx ng generate component features/funcionario/funcionario --flat --standalone=false --type=component --module=features/funcionario/funcionario.module --skip-tests
npx ng generate component features/cliente/cliente --flat --standalone=false --type=component --module=features/cliente/cliente.module --skip-tests
npx ng generate component features/painel/painel --flat --standalone=false --type=component --module=features/painel/painel.module --skip-tests
npx ng generate component features/totem/totem --flat --standalone=false --type=component --module=features/totem/totem.module --skip-tests
npx ng generate component shared/components/queue-list --flat --standalone=false --type=component --module=shared/shared.module --skip-tests
npx ng generate component shared/components/status-badge --flat --standalone=false --type=component --module=shared/shared.module --skip-tests
npx ng generate component features/funcionario/components/stat-card --flat --standalone=false --type=component --module=features/funcionario/funcionario.module --skip-tests
npx ng generate component features/funcionario/components/atendimento-atual --flat --standalone=false --type=component --module=features/funcionario/funcionario.module --skip-tests
npx ng generate component layout/shell --flat --standalone=false --type=component --module=app.module --skip-tests
```

- `--flat`: mantém os arquivos na pasta informada, sem criar uma subpasta extra.
- `--standalone=false`: cria um componente pertencente a um NgModule.
- `--type=component`: mantém o nome `nome.component.ts` mesmo em versões cujo padrão de nomes seja diferente.
- `--module`: indica o módulo que recebe a declaração.
- `--skip-tests`: evita arquivos de testes vazios gerados automaticamente. Testes reais podem ser criados conforme o comportamento.

Serviços e guard:

```powershell
npx ng generate service core/services/fila --type=service --skip-tests
npx ng generate service core/services/api --type=service --skip-tests
npx ng generate service core/services/auth --type=service --skip-tests
npx ng generate service core/services/alerta --type=service --skip-tests
npx ng generate guard core/guards/auth --functional --implements=CanActivate --type-separator=. --skip-tests
```

Não é obrigatório criar `CoreModule` para os serviços: `providedIn: 'root'` já resolve o escopo global. A pasta `core` é uma divisão de responsabilidade.

## 4. Como o lazy loading funciona

No módulo principal:

```typescript
{
  path: 'funcionario',
  canActivate: [authGuard],
  loadChildren: () => import('./features/funcionario/funcionario.module')
    .then(m => m.FuncionarioModule)
}
```

O Angular baixa o módulo quando a rota é acessada. Os módulos de funcionalidades não são importados diretamente em `AppModule`.

Neste projeto as rotas filhas ficam no próprio módulo, reduzindo arquivos de configuração:

```typescript
@NgModule({
  declarations: [FuncionarioComponent, StatCardComponent, AtendimentoAtualComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: FuncionarioComponent }])
  ]
})
export class FuncionarioModule {}
```

O CLI com `--routing` cria um arquivo de routing separado. Isso também é válido; nesse caso coloque o `RouterModule.forChild(...)` no arquivo gerado e importe seu módulo de rotas. Use apenas uma dessas formas para não duplicar rotas.

## 5. Como usar Angular Material

`MaterialModule` centraliza os módulos utilizados, como `MatButtonModule`, `MatCardModule`, `MatFormFieldModule`, `MatInputModule`, `MatSelectModule`, `MatSnackBarModule`, `MatIconModule`, `MatProgressBarModule` e `MatTooltipModule`.

Ao adicionar outro recurso, por exemplo `MatDialog`:

```typescript
import { MatDialogModule } from '@angular/material/dialog';
```

Adicione `MatDialogModule` à constante `modules` de `material.module.ts`; essa constante alimenta os arrays `imports` e `exports`. Os módulos de funcionalidades que importam `SharedModule` passam a poder usar o recurso.

O tema está em `src/styles.scss` e usa `mat.theme`. Cores, espaçamento, estados e estilos de impressão ficam ali. Os ícones são empacotados com `@fontsource/material-icons`, para funcionar sem Google Fonts ou internet.

## 6. Como um componente recebe dados

O indicador não busca dados nem calcula a fila; apenas mostra os valores recebidos:

```typescript
@Component({
  selector: 'app-stat-card',
  standalone: false,
  template: `<mat-card><h3>{{ label }}</h3><strong>{{ value }}</strong></mat-card>`
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value = 0;
}
```

No componente pai:

```html
<app-stat-card
  label="Aguardando"
  [value]="fila.state().waiting.length" />
```

Evite fazer uma requisição de rede em cada card. O `FilaService` mantém o estado central, atualizado por SSE, e os componentes leem esse estado.

## 7. Como acrescentar um módulo novo

Exemplo: uma área exclusiva de relatórios para funcionários.

```powershell
npx ng generate module features/relatorios --routing
npx ng generate component features/relatorios/relatorios --flat --standalone=false --type=component --module=features/relatorios/relatorios.module --skip-tests
```

Depois:

1. Importe `SharedModule` no módulo novo.
2. Configure a rota filha vazia para `RelatoriosComponent`.
3. Adicione a rota `relatorios` com `loadChildren` e `canActivate: [authGuard]` dentro dos filhos do `ShellComponent` em `app.module.ts`.
4. Adicione o link ao menu em `layout/shell.component.html`.
5. Se precisar de dados novos, crie endpoint autenticado no servidor e método de serviço; não faça o relatório calculando valores fictícios no HTML.
6. Teste com `npm run check`.

## 8. Divisão sugerida para o grupo

| Integrante | Responsabilidade principal | Arquivos |
| --- | --- | --- |
| Guilherme Perino Feitosa | Experiência do cliente e avisos | `features/cliente`, `alerta.service.ts` |
| Gustavo Henrique de Araujo Alves | Servidor, autenticação e integração | `server`, `core/services`, `core/guards` |
| Matheus Boletti Fortes | Operação do funcionário e indicadores | `features/funcionario` |
| Luiz Felipe Gaspar Fernandes da Silva | Painel de TV, totem e impressão | `features/painel`, `features/totem` |

Essa é uma sugestão de organização, sem atribuição de autoria do código. O grupo deve validar os fluxos em conjunto. Alterações no modelo da fila precisam ser combinadas antes, pois afetam as quatro interfaces.
