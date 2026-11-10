import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
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
  private streamConnected = false;
  private syncing = false;
  private version = 0;
  private destroyed = false;
  constructor() {
    this.connect();
    const clock = setInterval(() => this.now.set(Date.now() + this.offset), 250);
    const fallback = setInterval(() => {
      if (document.visibilityState === 'visible' || !this.streamConnected) void this.syncState();
    }, 5000);
    const resume = () => {
      if (document.visibilityState === 'visible') void this.syncState();
    };
    document.addEventListener('visibilitychange', resume);
    window.addEventListener('online', resume);
    inject(DestroyRef).onDestroy(() => {
      this.destroyed = true;
      this.events?.close();
      clearInterval(clock);
      clearInterval(fallback);
      document.removeEventListener('visibilitychange', resume);
      window.removeEventListener('online', resume);
    });
  }
  private apply(state: EstadoFila) {
    if (this.destroyed) return;
    this.offset = state.serverTime - Date.now();
    this.now.set(state.serverTime);
    this.state.set(state);
    this.connected.set(true);
  }
  private connect() {
    try {
      this.events = new EventSource('/api/events');
      this.events.onmessage = (e) => {
        try {
          const state = JSON.parse(e.data) as EstadoFila;
          this.version++;
          this.streamConnected = true;
          this.apply(state);
        } catch {
          this.streamConnected = false;
          void this.syncState();
        }
      };
      this.events.onerror = () => {
        this.streamConnected = false;
        void this.syncState();
      };
    } catch {
      void this.syncState();
    }
  }
  private async syncState() {
    if (this.syncing || this.destroyed) return;
    this.syncing = true;
    const version = this.version;
    try {
      const state = await this.api.request<EstadoFila>('/state');
      if (version === this.version) this.apply(state);
    } catch {
      if (!this.streamConnected && !this.destroyed) this.connected.set(false);
    } finally {
      this.syncing = false;
    }
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
