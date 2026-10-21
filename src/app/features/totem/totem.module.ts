import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { TotemComponent } from './totem.component';
@NgModule({
  declarations: [TotemComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: TotemComponent }])],
})
export class TotemModule {}
