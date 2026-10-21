import { Injectable, inject, signal, computed } from '@angular/core';
import { ApiService } from './api.service';
import { EstadoFila, MinhaSenha, Evento } from '../models/fila.model';
@Injectable({ providedIn: 'root' })
export class FilaService {
  readonly api = inject(ApiService);
  readonly state = signal<EstadoFila>({
    current: null,
    waiting: [],
    paused: false,
    stats: { completed: 0, absent: 0, averageMinutes: 4, estimated: true },
    serverTime: Date.now(),
  });
  readonly connected = signal(false);
  readonly busy = signal(false);
  readonly now = signal(Date.now());
  readonly remaining = computed(() =>
    Math.max(0, Math.ceil(((this.state().current?.deadline ?? 0) - this.now()) / 1000)),
  );
  private offset = 0;
  private events?: EventSource;
  constructor() {
    this.connect();
    setInterval(() => this.now.set(Date.now() + this.offset), 250);
  }
  private connect() {
    this.events = new EventSource('/api/events');
    this.events.onmessage = (e) => {
      const state = JSON.parse(e.data) as EstadoFila;
      this.offset = state.serverTime - Date.now();
      this.now.set(state.serverTime);
      this.state.set(state);
      this.connected.set(true);
    };
    this.events.onerror = () => this.connected.set(false);
  }
  async run(action: string) {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      await this.api.request('/queue/' + action, {});
    } catch (e) {
      this.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  async issue(service: string, origin: 'app' | 'totem') {
    return this.api.request<{ token: string; label: string }>('/tickets', {
      service,
      origin,
    });
  }
  ticket(token: string) {
    return this.api.request<MinhaSenha>('/tickets/' + encodeURIComponent(token));
  }
  return(token: string) {
    return this.api.request('/tickets/' + encodeURIComponent(token) + '/return', {});
  }
  audit() {
    return this.api.request<Evento[]>('/audit');
  }
}
