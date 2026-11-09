import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly api = inject(ApiService);
  readonly logged = signal(false);
  readonly perfil = signal('');
  readonly username = signal('');
  async check() {
    try {
      const u = await this.api.request<{ username: string; perfil: string }>('/session');
      this.perfil.set(u.perfil);
      this.username.set(u.username);
      this.logged.set(true);
      return true;
    } catch {
      this.logged.set(false);
      this.perfil.set('');
      this.username.set('');
      return false;
    }
  }
  async login(username: string, password: string) {
    await this.api.request('/login', { username, password });
    await this.check();
  }
  async logout() {
    try {
      await this.api.request('/logout', {});
    } catch {
      /* A sessão pode ter expirado no servidor. */
    } finally {
      this.logged.set(false);
      this.perfil.set('');
      this.username.set('');
    }
  }
}
