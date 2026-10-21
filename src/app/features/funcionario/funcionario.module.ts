import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { FuncionarioComponent } from './funcionario.component';
import { StatCardComponent } from './components/stat-card.component';
import { AtendimentoAtualComponent } from './components/atendimento-atual.component';
@NgModule({
  declarations: [FuncionarioComponent, StatCardComponent, AtendimentoAtualComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: FuncionarioComponent }])],
})
export class FuncionarioModule {}
