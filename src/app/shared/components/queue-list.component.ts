import { Component, Input } from '@angular/core';
import { Senha } from '../../core/models/fila.model';
@Component({selector:'app-queue-list',standalone:false,templateUrl:'./queue-list.component.html'})
export class QueueListComponent {@Input()tickets:Senha[]=[];@Input()compact=false;}
