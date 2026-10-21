import { Component, Input } from '@angular/core';
@Component({
  selector: 'app-status-badge',
  standalone: false,
  template: `<span
    class="badge"
    [class.waiting]="status === 'waiting'"
    [class.absent]="status === 'absent'"
    >{{ labels[status] || status }}</span
  >`,
})
export class StatusBadgeComponent {
  @Input() status = 'waiting';
  labels: Record<string, string> = {
    waiting: 'Aguardando',
    called: 'Chamando',
    serving: 'Em atendimento',
    done: 'Concluído',
    absent: 'Ausente',
  };
}
