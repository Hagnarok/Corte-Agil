import { Component, inject, signal } from '@angular/core';
import QRCode from 'qrcode';
import { FilaService } from '../../core/services/fila.service';
import { SERVICOS } from '../../core/models/fila.model';
@Component({
  selector: 'app-totem',
  standalone: false,
  templateUrl: './totem.component.html',
})
export class TotemComponent {
  readonly fila = inject(FilaService);
  readonly services = SERVICOS;
  service = 'Não informado';
  readonly busy = signal(false);
  readonly receipt = signal<{
    label: string;
    url: string;
    time: number;
    service: string;
  } | null>(null);
  readonly qr = signal('');
  readonly printError = signal('');
  async issue() {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const t = await this.fila.issue(this.service, 'totem');
      const url = location.origin + '/cliente#senha=' + t.token;
      this.receipt.set({
        label: t.label,
        url,
        time: Date.now(),
        service: this.service,
      });
      this.qr.set(
        await QRCode.toDataURL(url, {
          width: 240,
          margin: 2,
          errorCorrectionLevel: 'M',
        }),
      );
    } catch (e) {
      this.fila.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  print() {
    if (!this.qr()) return;
    try {
      window.print();
    } catch {
      this.printError.set('Não foi possível abrir a impressão. Verifique a impressora.');
    }
  }
  next() {
    this.receipt.set(null);
    this.qr.set('');
    this.service = 'Não informado';
    this.printError.set('');
  }
}
