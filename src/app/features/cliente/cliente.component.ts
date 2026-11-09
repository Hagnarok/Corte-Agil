import { CatalogoService } from '../../core/services/catalogo.service';
import { Component, inject, signal, effect, untracked, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FilaService } from '../../core/services/fila.service';
import { Aviso, MinhaSenha } from '../../core/models/fila.model';
import { AvisosClienteService } from '../../core/services/avisos-cliente.service';
import { AlertaService } from '../../core/services/alerta.service';
@Component({
  selector: 'app-cliente',
  standalone: false,
  templateUrl: './cliente.component.html',
})
export class ClienteComponent implements OnDestroy {
  readonly fila = inject(FilaService);
  readonly alerts = inject(AlertaService);
  readonly route = inject(ActivatedRoute);
  readonly ticket = signal<MinhaSenha | null>(null);
  readonly busy = signal(false);
  readonly notificationsBusy = signal(false);
  readonly notificationHint = signal(
    'Ative o som e os avisos para saber quando sua vez estiver chegando.',
  );
  readonly latestNotice = signal<Aviso | null>(null);
  private readonly avisos = inject(AvisosClienteService);
  readonly services = inject(CatalogoService).services;
  service = 'Não informado';
  private token = '';
  private destroyed = false;
  private readonly poll = setInterval(() => {
    if (this.token && document.visibilityState === 'visible') void this.refresh();
  }, 15000);
  private refreshing = false;
  private refreshAgain = false;
  constructor() {
    this.token =
      new URLSearchParams(this.route.snapshot.fragment ?? '').get('senha') ||
      localStorage.getItem('corteagil-ticket') ||
      '';
    if (this.token) localStorage.setItem('corteagil-ticket', this.token);
    effect(() => {
      this.fila.state();
      untracked(() => {
        if (this.token) this.refresh();
      });
    });
  }
  async refresh() {
    if (!this.token || this.destroyed) return;
    if (this.refreshing) {
      this.refreshAgain = true;
      return;
    }
    this.refreshing = true;
    const token = this.token;
    try {
      const t = await this.fila.ticket(token);
      if (this.destroyed || token !== this.token) return;
      this.ticket.set(t);
      const novos = this.avisos.novos(token, t.notifications);
      const aviso = novos.at(-1);
      if (aviso) {
        this.latestNotice.set(aviso);
        this.fila.api.message(aviso.text);
        void this.alerts.notify(
          'CorteÁgil · ' + t.label,
          aviso.text,
          location.origin + '/cliente#senha=' + token,
          'corteagil-' + t.id,
        );
      }
    } catch (e) {
      if (!this.destroyed && token === this.token) this.fila.api.error(e);
    } finally {
      this.refreshing = false;
      if (this.refreshAgain) {
        this.refreshAgain = false;
        void this.refresh();
      }
    }
  }
  async issue() {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const t = await this.fila.issue(this.service, 'app');
      this.token = t.token;
      localStorage.setItem('corteagil-ticket', t.token);
      await this.refresh();
    } catch (e) {
      this.fila.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  async rejoin() {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      await this.fila.return(this.token);
      await this.refresh();
    } catch (e) {
      this.fila.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  async notifications() {
    if (this.notificationsBusy()) return;
    this.notificationsBusy.set(true);
    try {
      // As duas permissões começam no clique, antes de qualquer espera assíncrona.
      const permission = this.alerts.permission();
      await this.alerts.enable(false);
      const result = await permission;
      const delivered = await this.alerts.notify(
        'CorteÁgil · avisos ativados',
        'Você será avisado quando sua vez estiver chegando.',
        location.origin + '/cliente#senha=' + this.token,
        'corteagil-teste',
      );
      const sound = this.alerts.enabled() ? 'Som ativado. ' : 'Som indisponível. ';
      this.notificationHint.set(
        sound +
          (delivered
            ? 'Avisos do navegador ativados. Mantenha esta página aberta.'
            : result === 'denied'
              ? 'Notificações bloqueadas. Libere a permissão nas configurações do site; os avisos continuam nesta tela.'
              : result === 'insecure'
                ? 'Os avisos do navegador precisam de HTTPS. Acompanhe também os avisos nesta tela.'
                : 'Avisos do navegador indisponíveis. Acompanhe os avisos nesta tela.'),
      );
      this.fila.api.message(this.notificationHint());
    } finally {
      this.notificationsBusy.set(false);
    }
  }
  newTicket() {
    this.token = '';
    this.ticket.set(null);
    this.latestNotice.set(null);
    localStorage.removeItem('corteagil-ticket');
    history.replaceState(null, '', location.pathname);
  }
  ngOnDestroy() {
    this.destroyed = true;
    clearInterval(this.poll);
  }
}
