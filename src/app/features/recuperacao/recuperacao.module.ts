import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RecuperacaoComponent } from './recuperacao.component';
@NgModule({
  declarations: [RecuperacaoComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: RecuperacaoComponent }])],
})
export class RecuperacaoModule {}
