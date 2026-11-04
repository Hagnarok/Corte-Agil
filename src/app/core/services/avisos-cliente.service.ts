import { Injectable } from '@angular/core';
import { Aviso } from '../models/fila.model';

@Injectable({ providedIn: 'root' })
export class AvisosClienteService {
  private readonly lidos = new Map<string, Set<string>>();

  novos(token: string, avisos: Aviso[]): Aviso[] {
    let lidos = this.lidos.get(token);
    if (!lidos) {
      try {
        const saved: unknown = JSON.parse(sessionStorage.getItem(this.chave(token)) || '[]');
        lidos = new Set(Array.isArray(saved) ? saved.filter((id) => typeof id === 'string') : []);
      } catch {
        lidos = new Set();
      }
      this.lidos.set(token, lidos);
    }
    const novos = avisos.filter((aviso) => !lidos.has(aviso.id));
    const atuais = avisos.map((aviso) => aviso.id);
    this.lidos.set(token, new Set(atuais));
    try {
      sessionStorage.setItem(this.chave(token), JSON.stringify(atuais));
    } catch {
      // A deduplicação em memória também funciona quando o armazenamento está bloqueado.
    }
    return novos;
  }

  private chave(token: string) {
    return `corteagil-avisos:${token}`;
  }
}
