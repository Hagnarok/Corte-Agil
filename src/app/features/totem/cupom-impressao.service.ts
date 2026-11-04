import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { CUPOM_ESTILOS, LarguraCupom } from './components/cupom-layout';

@Injectable({ providedIn: 'root' })
export class CupomImpressaoService {
  private readonly document = inject(DOCUMENT);

  async imprimir(cupom: HTMLElement, largura: LarguraCupom): Promise<void> {
    if (!cupom.querySelector('img.ticket-qr[src]')) {
      throw new Error('Aguarde o QR Code para imprimir a senha.');
    }
    const frame = this.document.createElement('iframe');
    frame.title = 'Impressão da senha';
    frame.setAttribute('aria-hidden', 'true');
    Object.assign(frame.style, {
      position: 'fixed',
      left: '-10000px',
      top: '0',
      width: `${largura}mm`,
      height: '200mm',
      border: '0',
    });
    this.document.body.appendChild(frame);
    try {
      const doc = frame.contentDocument;
      const win = frame.contentWindow;
      if (!doc || !win) throw new Error('Impressão indisponível neste navegador.');
      doc.title = 'Senha CorteÁgil';
      const style = doc.createElement('style');
      style.textContent = `${CUPOM_ESTILOS}
        html, body { margin: 0; padding: 0; background: #fff; }
        body { width: ${largura - 4}mm; }
        .ticket-receipt { break-inside: avoid; }
      `;
      doc.head.appendChild(style);
      const copia = cupom.cloneNode(true) as HTMLElement;
      doc.body.appendChild(copia);
      await Promise.all(
        Array.from(copia.querySelectorAll('img')).map(
          (img) =>
            new Promise<void>((resolve, reject) => {
              const pronto = () =>
                img.naturalWidth > 0 ? resolve() : reject(new Error('QR Code indisponível.'));
              if (img.complete) pronto();
              else {
                img.onload = pronto;
                img.onerror = () => reject(new Error('Não foi possível carregar o QR Code.'));
              }
            }),
        ),
      );
      await doc.fonts.ready;
      const altura = Math.ceil((copia.getBoundingClientRect().height * 25.4) / 96) + 4;
      style.textContent += `@page { size: ${largura}mm ${altura}mm; margin: 2mm; }`;
      win.addEventListener('afterprint', () => frame.remove(), { once: true });
      // Alguns navegadores não disparam afterprint ao cancelar a janela.
      setTimeout(() => frame.remove(), 300_000);
      win.focus();
      win.print();
    } catch (error) {
      frame.remove();
      throw error;
    }
  }
}
