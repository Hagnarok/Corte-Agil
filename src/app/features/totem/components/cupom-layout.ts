export type LarguraCupom = 58 | 80;

export interface SenhaCupom {
  label: string;
  url: string;
  time: number;
  service: string;
}

export const CUPOM_ESTILOS = `
  .ticket-receipt {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 48mm;
    max-width: 100%;
    margin: 0 auto;
    padding: 1mm 0;
    background: #fff;
    color: #000;
    font-family: Arial, Helvetica, sans-serif;
    text-align: center;
    line-height: 1.15;
    overflow-wrap: anywhere;
  }
  .ticket-receipt.wide { width: 72mm; }
  .ticket-receipt * { box-sizing: border-box; margin: 0; }
  .ticket-receipt .ticket-brand { font-size: 10pt; font-weight: 700; }
  .ticket-receipt .ticket-counter { margin-top: 0.6mm; font-size: 7pt; }
  .ticket-receipt .ticket-number {
    max-width: 100%;
    margin: 1mm 0;
    font-size: 32pt;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.5pt;
  }
  .ticket-receipt .ticket-number.long { font-size: 23pt; }
  .ticket-receipt .ticket-service { margin-bottom: 1mm; font-size: 8pt; }
  .ticket-receipt .ticket-qr {
    display: block;
    flex: none;
    width: 28mm;
    height: 28mm;
    margin: 0.5mm 0;
    image-rendering: pixelated;
  }
  .ticket-receipt.wide .ticket-qr { width: 30mm; height: 30mm; }
  .ticket-receipt .ticket-qr-hint { font-size: 7pt; }
  .ticket-receipt .ticket-time { margin-top: 1mm; font-size: 7pt; }
  .ticket-receipt .ticket-footer {
    width: 100%;
    margin-top: 1.5mm;
    padding-top: 1mm;
    border-top: 0.2mm dashed #000;
    font-size: 7pt;
  }
`;
