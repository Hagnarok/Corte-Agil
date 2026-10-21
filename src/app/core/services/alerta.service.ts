import { Injectable, signal } from '@angular/core';
@Injectable({providedIn:'root'})
export class AlertaService {
 readonly enabled=signal(false);private context?:AudioContext;
 async enable(){try{this.context??=new AudioContext();await this.context.resume();this.enabled.set(true);this.play();}catch{this.enabled.set(false);}}
 play(){if(!this.enabled()||!this.context)return;const ctx=this.context;[660,880,1046].forEach((frequency,i)=>{const oscillator=ctx.createOscillator(),gain=ctx.createGain(),start=ctx.currentTime+i*.18;oscillator.frequency.value=frequency;gain.gain.setValueAtTime(.0001,start);gain.gain.exponentialRampToValueAtTime(.15,start+.02);gain.gain.exponentialRampToValueAtTime(.0001,start+.4);oscillator.connect(gain);gain.connect(ctx.destination);oscillator.start(start);oscillator.stop(start+.45);});}
 async permission(){if(!('Notification'in window))return 'unsupported';if(!window.isSecureContext)return 'insecure';return Notification.requestPermission();}
 notify(title:string,body:string){if('Notification'in window&&Notification.permission==='granted')new Notification(title,{body,icon:'/favicon.svg'});}
}
