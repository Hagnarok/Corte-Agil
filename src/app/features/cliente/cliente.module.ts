import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ClienteComponent } from './cliente.component';
@NgModule({declarations:[ClienteComponent],imports:[SharedModule,RouterModule.forChild([{path:'',component:ClienteComponent}])]})export class ClienteModule {}
