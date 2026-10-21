import { Component, inject } from '@angular/core';
import { FilaService } from '../../../core/services/fila.service';
@Component({
  selector: 'app-atendimento-atual',
  standalone: false,
  templateUrl: './atendimento-atual.component.html',
})
export class AtendimentoAtualComponent {
  readonly fila = inject(FilaService);
}
