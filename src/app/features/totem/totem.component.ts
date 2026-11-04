import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import QRCode from 'qrcode';
import { FilaService } from '../../core/services/fila.service';
import { CupomImpressaoService } from './cupom-impressao.service';
import { LarguraCupom, SenhaCupom } from './components/cupom-layout';
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
  private readonly impressao = inject(CupomImpressaoService);
  @ViewChild('cupom', { read: ElementRef }) private cupom?: ElementRef<HTMLElement>;
  readonly receipt = signal<SenhaCupom | null>(null);
  readonly printing = signal(false);
  largura: LarguraCupom = localStorage.getItem('corteagil-papel') === '80' ? 80 : 58;
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
          width: 512,
          margin: 4,
          errorCorrectionLevel: 'M',
        }),
      );
    } catch (e) {
      this.fila.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  salvarImpressao() {
    localStorage.setItem('corteagil-papel', String(this.largura));
  }
  async print() {
    const cupom = this.cupom?.nativeElement.querySelector<HTMLElement>('.ticket-receipt');
    if (!cupom || !this.qr() || this.printing()) return;
    this.printError.set('');
    this.printing.set(true);
    try {
      await this.impressao.imprimir(cupom, this.largura);
    } catch {
      this.printError.set('Não foi possível abrir a impressão. Verifique a impressora.');
    } finally {
      this.printing.set(false);
    }
  }
  next() {
    this.receipt.set(null);
    this.qr.set('');
    this.service = 'Não informado';
    this.printError.set('');
  }
}
