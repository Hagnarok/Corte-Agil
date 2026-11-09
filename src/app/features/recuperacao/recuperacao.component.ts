import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
@Component({
  selector: 'app-recuperacao',
  standalone: false,
  templateUrl: './recuperacao.component.html',
})
export class RecuperacaoComponent {
  readonly api = inject(ApiService);
  readonly token =
    new URLSearchParams(inject(ActivatedRoute).snapshot.fragment || '').get('token') || '';
  readonly busy = signal(false);
  readonly message = signal('');
  readonly done = signal(false);
  private fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    email: [
      '',
      this.token ? [] : [Validators.required, Validators.email, Validators.maxLength(150)],
    ],
    senha: [
      '',
      this.token ? [Validators.required, Validators.minLength(10), Validators.maxLength(72)] : [],
    ],
    confirmacao: [''],
  });
  async submit() {
    if (this.form.invalid || this.busy()) return;
    const d = this.form.getRawValue();
    if (this.token && d.senha !== d.confirmacao) {
      this.message.set('As senhas precisam ser iguais.');
      return;
    }
    this.busy.set(true);
    try {
      const r = await this.api.request<{ message: string }>(
        '/password-reset/' + (this.token ? 'confirm' : 'request'),
        this.token ? { token: this.token, senha: d.senha } : { email: d.email },
      );
      this.message.set(r.message);
      this.done.set(true);
    } catch (e) {
      this.message.set(e instanceof Error ? e.message : 'Não foi possível concluir.');
    } finally {
      this.busy.set(false);
    }
  }
}
