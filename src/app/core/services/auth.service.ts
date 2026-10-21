import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
@Injectable({providedIn:'root'})
export class AuthService {
 readonly api=inject(ApiService);readonly logged=signal(false);
 async check(){try{await this.api.request('/session');this.logged.set(true);return true;}catch{this.logged.set(false);return false;}}
 async login(username:string,password:string){await this.api.request('/login',{username,password});this.logged.set(true);}
 async logout(){try{await this.api.request('/logout',{});}catch{ /* A sessão pode ter expirado no servidor. */ }finally{this.logged.set(false);}}
}
