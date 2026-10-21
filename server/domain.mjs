import { randomUUID, randomBytes } from 'node:crypto';
export const services = ['Não informado', 'Bife rápido', 'Peça inteira', 'Moído', 'Outros cortes'];
export const initialState = () => ({sequence:0,tickets:[],audit:[],paused:false});
export function fail(message, status=409) { throw Object.assign(new Error(message), {status}); }
export function current(s) { return s.tickets.find(t => ['called','serving'].includes(t.status)); }
export function waiting(s) { return s.tickets.filter(t => t.status==='waiting').sort((a,b)=>a.order-b.order); }
export function audit(s, action, t, now) { s.audit.push({id:randomUUID(),action,label:t?.label ?? 'Fila',at:now}); s.audit=s.audit.slice(-2000); }
function notify(t,kind,text,now) { t.notifications.push({id:randomUUID(),kind,text,at:now}); t.notifications=t.notifications.slice(-20); }
export function updateNotifications(s,now) {
  const active=current(s)?1:0;
  waiting(s).forEach((t,i)=>{const ahead=i+active; const key=`${t.returns}:${ahead}`;
    if ([1,2].includes(ahead)&&!t.notified.includes(key)) {t.notified.push(key);notify(t,`ahead-${ahead}`,ahead===2?'Faltam duas senhas para a sua vez. Fique por perto.':'Falta uma senha para a sua vez. Volte ao açougue.',now);}
  });
}
export function issue(s, service, origin, now=Date.now()) {
  if(s.paused) fail('A retirada de senhas está pausada.');
  if(!services.includes(service)||!['app','totem'].includes(origin)) fail('Serviço ou origem inválidos.',400);
  const n=++s.sequence;
  const t={id:randomUUID(),token:randomBytes(24).toString('hex'),label:`A${String(n).padStart(3,'0')}`,service,origin,status:'waiting',createdAt:now,order:Math.max(0,...s.tickets.map(t=>t.order))+1,returns:0,notifications:[],notified:[]};
  s.tickets.push(t);audit(s,'Senha emitida',t,now); updateNotifications(s,now);return t;
}
export function callNext(s,now=Date.now()) {
  if(current(s)) fail('Finalize o atendimento atual antes de chamar o próximo.');
  const t=waiting(s)[0];if(!t) fail('Não há clientes aguardando.');
  t.status='called';t.calledAt=now;t.deadline=now+30000;notify(t,'called','É a sua vez! Apresente-se ao balcão do açougue.',now);audit(s,'Senha chamada',t,now);updateNotifications(s,now);return t;
}
export function act(s,action,now=Date.now()) {
  const t=current(s);if(!t) fail('Não há atendimento ativo.');
  if(action==='recall') { if(t.status!=='called') fail('Cliente já está em atendimento.');t.calledAt=now;t.deadline=now+30000;notify(t,'called','Sua senha está sendo chamada novamente. Vá ao balcão.',now); }
  else if(action==='start') {if(t.status!=='called') fail('Atendimento já iniciado.');t.status='serving';t.startedAt=now;}
  else if(action==='finish') {if(t.status!=='serving') fail('Confirme a chegada do cliente primeiro.');t.status='done';t.finishedAt=now;notify(t,'done','Atendimento concluído. Obrigado e boas compras!',now);}
  else if(action==='absent') {if(t.status!=='called'||now<t.deadline) fail('Aguarde os 30 segundos da chamada para registrar ausência.');t.status='absent';notify(t,'absent','Você perdeu sua vez. Deseja retornar à fila?',now);}
  else fail('Ação inválida.',400);
  audit(s,({recall:'Chamada repetida',start:'Atendimento iniciado',finish:'Atendimento concluído',absent:'Cliente ausente'})[action],t,now); updateNotifications(s,now);return t;
}
export function rejoin(s,token,now=Date.now()) {
  const t=s.tickets.find(t=>t.token===token);if(!t) fail('Senha não encontrada.',404);
  if(t.status!=='absent') fail('Somente senhas ausentes podem retornar.');
  if(s.paused) fail('A fila está pausada. Tente novamente em instantes.');
  t.status='waiting';t.returns++;t.order=Math.max(0,...s.tickets.map(t=>t.order))+1;
  notify(t,'returned','Você retornou ao final da fila com a mesma senha.',now);audit(s,'Retorno ao final da fila',t,now);updateNotifications(s,now);return t;
}
export function publicTicket(t) { if(!t)return null;const {token,notifications,notified,...safe}=t; return safe; }
const day = time => new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo'}).format(time);
export function snapshot(s,now=Date.now()) {
  const done=s.tickets.filter(t=>t.status==='done'&&day(t.finishedAt)===day(now));
  const recent=done.filter(t=>t.startedAt).slice(-20);
  const averageMinutes=recent.length?Math.max(1,Math.round(recent.reduce((sum,t)=>sum+(t.finishedAt-t.startedAt)/60000,0)/recent.length)):4;
  return {current:publicTicket(current(s)),waiting:waiting(s).map(publicTicket),paused:s.paused,stats:{completed:done.length,absent:s.tickets.filter(t=>t.status==='absent').length,averageMinutes,estimated:recent.length===0},serverTime:now};
}
export function ownTicket(s, token, now=Date.now()) {
  const t=s.tickets.find(t=>t.token===token); if(!t)fail('Senha não encontrada.',404);
  const snap=snapshot(s,now);const index=snap.waiting.findIndex(w=>w.id===t.id);
  return {...t,ahead:index<0?0:index+(snap.current?1:0),position:index<0?0:index+1+(snap.current?1:0),estimatedMinutes:index<0?0:(index+(snap.current?1:0))*snap.stats.averageMinutes};
}
