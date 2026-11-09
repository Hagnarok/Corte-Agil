import { Component, computed, inject, signal } from '@angular/core';
import { CatalogoService } from '../../core/services/catalogo.service';
import { FilaService } from '../../core/services/fila.service';
import { Evento } from '../../core/models/fila.model';
@Component({
  selector: 'app-funcionario',
  standalone: false,
  templateUrl: './funcionario.component.html',
})
export class FuncionarioComponent {
  readonly fila = inject(FilaService);
  readonly catalogo = inject(CatalogoService);
  readonly filters = computed(() => [
    ...new Set([
      'Todos',
      ...this.catalogo.services(),
      ...this.fila.state().waiting.map((t) => t.service),
    ]),
  ]);
  readonly filter = signal('Todos');
  readonly history = signal<Evento[]>([]);
  readonly showHistory = signal(false);
  readonly date = new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date());
  readonly filtered = computed(() =>
    this.fila
      .state()
      .waiting.filter((t) => this.filter() === 'Todos' || t.service === this.filter()),
  );
  readonly counts = computed(() => {
    const list = this.fila.state().waiting;
    return [...new Set(list.map((t) => t.service))].map((name) => ({
      name,
      icon: name === 'Moído' ? 'grain' : name === 'Bife rápido' ? 'bolt' : 'restaurant',
      count: list.filter((t) => t.service === name).length,
    }));
  });
  async toggleHistory() {
    this.showHistory.set(!this.showHistory());
    if (this.showHistory())
      try {
        this.history.set(await this.fila.audit());
      } catch (e) {
        this.fila.api.error(e);
      }
  }
}
