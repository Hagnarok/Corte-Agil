export type StatusSenha = 'waiting' | 'called' | 'serving' | 'done' | 'absent';
export interface Aviso {id:string;kind:string;text:string;at:number;}
export interface Senha {id:string;label:string;service:string;origin:'app'|'totem';status:StatusSenha;createdAt:number;order:number;returns:number;calledAt?:number;deadline?:number;startedAt?:number;finishedAt?:number;}
export interface MinhaSenha extends Senha {token:string;ahead:number;position:number;estimatedMinutes:number;notifications:Aviso[];}
export interface EstadoFila {current:Senha|null;waiting:Senha[];paused:boolean;stats:{completed:number;absent:number;averageMinutes:number;estimated:boolean};serverTime:number;}
export interface Evento {id:string;action:string;label:string;at:number;}
export const SERVICOS = ['Não informado','Bife rápido','Peça inteira','Moído','Outros cortes'];
