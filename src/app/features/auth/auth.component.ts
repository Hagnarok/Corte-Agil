import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
@Component({
  selector: 'app-auth',
  standalone: false,
  templateUrl: './auth.component.html',
})
export class AuthComponent {
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  readonly fb = inject(FormBuilder);
  readonly busy = signal(false);
  readonly hide = signal(true);
  readonly form = this.fb.nonNullable.group({
    username: ['admin', Validators.required],
    password: ['', Validators.required],
  });
  async login() {
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    try {
      const { username, password } = this.form.getRawValue();
      await this.auth.login(username, password);
      await this.router.navigateByUrl('/funcionario');
    } catch (e) {
      this.auth.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
}
