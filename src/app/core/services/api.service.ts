import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
@Injectable({ providedIn: 'root' })
export class ApiService {
  private snack = inject(MatSnackBar);
  async request<T>(path: string, body?: unknown): Promise<T> {
    return this.send<T>(body === undefined ? 'GET' : 'POST', path, body);
  }
  async send<T>(method: string, path: string, body?: unknown): Promise<T> {
    let response: Response;
    try {
      response = await fetch('/api' + path, {
        method,
        credentials: 'same-origin',
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw Error('Sem conexão com o servidor. Verifique a rede local.');
    }
    if (response.status === 204) return undefined as T;
    const data = await response.json();
    if (!response.ok) throw Error(data.message || 'Não foi possível concluir a operação.');
    return data;
  }
  error(error: unknown) {
    this.snack.open(error instanceof Error ? error.message : 'Ocorreu um erro.', 'Fechar', {
      duration: 6000,
    });
  }
  message(text: string) {
    this.snack.open(text, 'Fechar', { duration: 4500 });
  }
}
