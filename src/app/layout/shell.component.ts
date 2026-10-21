import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';import { FilaService } from '../core/services/fila.service';import { AuthService } from '../core/services/auth.service';
@Component({selector:'app-shell',standalone:false,templateUrl:'./shell.component.html'})
export class ShellComponent {readonly fila=inject(FilaService);readonly auth=inject(AuthService);readonly router=inject(Router);readonly menu=signal(false);async logout(){await this.auth.logout();await this.router.navigateByUrl('/entrar');}}
