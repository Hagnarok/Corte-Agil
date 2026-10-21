import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { PainelComponent } from './painel.component';
@NgModule({declarations:[PainelComponent],imports:[SharedModule,RouterModule.forChild([{path:'',component:PainelComponent}])]})export class PainelModule {}
