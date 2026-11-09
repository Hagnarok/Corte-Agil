import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CadastrosComponent } from './cadastros.component';
@NgModule({
  declarations: [CadastrosComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: CadastrosComponent }])],
})
export class CadastrosModule {}
