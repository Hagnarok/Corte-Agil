import { Component, inject, signal, effect, untracked, OnDestroy } from '@angular/core';
import { FilaService } from '../../core/services/fila.service';
import { AlertaService } from '../../core/services/alerta.service';
@Component({
  selector: 'app-painel',
  standalone: false,
  templateUrl: './painel.component.html',
})
export class PainelComponent implements OnDestroy {
  readonly String = String;
  readonly fila = inject(FilaService);
  readonly alerts = inject(AlertaService);
  readonly clock = signal(new Date());
  readonly fullscreenError = signal('');
  private timer = setInterval(() => this.clock.set(new Date()), 1000);
  private lastCall = 0;
  constructor() {
    effect(() => {
      const t = this.fila.state().current;
      if (t?.status === 'called' && t.calledAt !== this.lastCall) {
        this.lastCall = t.calledAt ?? 0;
        untracked(() => this.alerts.play());
      }
    });
  }
  async fullscreen() {
    try {
      if (document.fullscreenElement) await document.exitFullscreen();
      else await document.documentElement.requestFullscreen();
    } catch {
      this.fullscreenError.set('Tela cheia indisponível. Use F11 no computador.');
    }
  }
  ngOnDestroy() {
    clearInterval(this.timer);
  }
}
