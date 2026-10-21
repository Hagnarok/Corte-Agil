import { NgModule, Component, provideZoneChangeDetection, LOCALE_ID } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { RouterModule } from '@angular/router';
import { SharedModule } from './shared/shared.module';
import { ShellComponent } from './layout/shell.component';
import { authGuard } from './core/guards/auth.guard';
registerLocaleData(pt);
@Component({
  selector: 'app-root',
  standalone: false,
  template: '<router-outlet />',
})
export class AppComponent {}
@NgModule({
  declarations: [AppComponent, ShellComponent],
  imports: [
    BrowserModule,
    SharedModule,
    RouterModule.forRoot([
      {
        path: 'entrar',
        loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
      },
      {
        path: 'painel',
        loadChildren: () => import('./features/painel/painel.module').then((m) => m.PainelModule),
      },
      {
        path: 'totem',
        loadChildren: () => import('./features/totem/totem.module').then((m) => m.TotemModule),
      },
      {
        path: '',
        component: ShellComponent,
        children: [
          {
            path: 'funcionario',
            canActivate: [authGuard],
            loadChildren: () =>
              import('./features/funcionario/funcionario.module').then((m) => m.FuncionarioModule),
          },
          {
            path: 'cliente',
            loadChildren: () =>
              import('./features/cliente/cliente.module').then((m) => m.ClienteModule),
          },
          { path: '', pathMatch: 'full', redirectTo: 'funcionario' },
        ],
      },
      { path: '**', redirectTo: 'cliente' },
    ]),
  ],
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
