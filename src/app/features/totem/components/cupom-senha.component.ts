import { Component, Input } from '@angular/core';
import { CUPOM_ESTILOS, LarguraCupom, SenhaCupom } from './cupom-layout';

@Component({
  selector: 'app-cupom-senha',
  standalone: false,
  templateUrl: './cupom-senha.component.html',
  styles: [CUPOM_ESTILOS],
})
export class CupomSenhaComponent {
  @Input({ required: true }) senha!: SenhaCupom;
  @Input() qr = '';
  @Input() largura: LarguraCupom = 58;
}
