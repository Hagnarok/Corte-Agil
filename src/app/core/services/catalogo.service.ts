import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { SERVICOS } from '../models/fila.model';
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly api = inject(ApiService);
  readonly services = signal(SERVICOS);
  constructor() {
    void this.load();
  }
  async load() {
    try {
      const d = await this.api.request<{ servicos: { nome: string }[] }>('/catalogo');
      this.services.set(d.servicos.map((s) => s.nome));
    } catch {
      /* Mantém o catálogo conhecido até a conexão voltar. */
    }
  }
}
