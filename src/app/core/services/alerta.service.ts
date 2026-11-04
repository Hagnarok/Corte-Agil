import { Injectable, signal } from '@angular/core';

type PermissaoAvisos = NotificationPermission | 'unsupported' | 'insecure' | 'error';

@Injectable({ providedIn: 'root' })
export class AlertaService {
  readonly enabled = signal(false);
  readonly systemEnabled = signal(false);
  private context?: AudioContext;
  private registration?: Promise<ServiceWorkerRegistration>;

  async enable(tocarTeste = true) {
    try {
      this.context ??= new AudioContext();
      await this.context.resume();
      this.enabled.set(this.context.state === 'running');
      if (tocarTeste) this.play();
    } catch {
      this.enabled.set(false);
    }
  }

  play() {
    if (!this.enabled() || !this.context) return;
    try {
      const ctx = this.context;
      if (ctx.state !== 'running') {
        this.enabled.set(false);
        return;
      }
      [660, 880, 1046].forEach((frequency, i) => {
        const oscillator = ctx.createOscillator();
        const gain = ctx.createGain();
        const start = ctx.currentTime + i * 0.18;
        oscillator.frequency.value = frequency;
        gain.gain.setValueAtTime(0.0001, start);
        gain.gain.exponentialRampToValueAtTime(0.15, start + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.4);
        oscillator.connect(gain);
        gain.connect(ctx.destination);
        oscillator.start(start);
        oscillator.stop(start + 0.45);
      });
    } catch {
      this.enabled.set(false);
    }
  }

  async permission(): Promise<PermissaoAvisos> {
    if (typeof Notification === 'undefined') return 'unsupported';
    if (!window.isSecureContext) return 'insecure';
    try {
      return Notification.permission === 'default'
        ? await Notification.requestPermission()
        : Notification.permission;
    } catch {
      return 'error';
    }
  }

  private worker(): Promise<ServiceWorkerRegistration> {
    this.registration ??= navigator.serviceWorker
      .register('/notificacoes-sw.js')
      .then(async (registration) => {
        if (registration.active) return registration;
        const worker = registration.installing || registration.waiting;
        if (!worker) throw new Error('Avisos indisponíveis.');
        await new Promise<void>((resolve, reject) => {
          const finish = (error?: Error) => {
            clearTimeout(timer);
            worker.removeEventListener('statechange', changed);
            if (error) reject(error);
            else resolve();
          };
          const changed = () => {
            if (worker.state === 'activated') finish();
            else if (worker.state === 'redundant') finish(new Error('Avisos indisponíveis.'));
          };
          const timer = setTimeout(() => finish(new Error('Avisos indisponíveis.')), 5000);
          worker.addEventListener('statechange', changed);
          changed();
        });
        return registration;
      })
      .catch((error) => {
        this.registration = undefined;
        throw error;
      });
    return this.registration;
  }

  async notify(title: string, body: string, url = location.href, tag = 'corteagil') {
    this.play();
    if (
      !window.isSecureContext ||
      typeof Notification === 'undefined' ||
      Notification.permission !== 'granted'
    ) {
      this.systemEnabled.set(false);
      return false;
    }
    const options: NotificationOptions & { renotify: boolean } = {
      body,
      icon: '/favicon.svg',
      tag,
      renotify: true,
      data: { url },
    };
    if ('serviceWorker' in navigator) {
      try {
        const registration = await this.worker();
        await registration.showNotification(title, options);
        this.systemEnabled.set(true);
        return true;
      } catch {
        // O aviso na tela e o som continuam disponíveis se o navegador negar a entrega.
      }
    }
    try {
      const notification = new Notification(title, options);
      notification.onclick = () => {
        window.focus();
        notification.close();
      };
      this.systemEnabled.set(true);
      return true;
    } catch {
      this.systemEnabled.set(false);
      return false;
    }
  }
}
