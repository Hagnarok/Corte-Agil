import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { CatalogoService } from '../../core/services/catalogo.service';
interface Registro {
  id: string;
  nome?: string;
  username?: string;
  email?: string;
  perfil?: string;
  ativo?: boolean;
  label?: string;
  service?: string;
  status?: string;
  categoria?: Registro;
}
@Component({
  selector: 'app-cadastros',
  standalone: false,
  templateUrl: './cadastros.component.html',
})
export class CadastrosComponent {
  readonly api = inject(ApiService);
  readonly catalogo = inject(CatalogoService);
  private readonly fb = inject(FormBuilder);
  readonly items = signal<Registro[]>([]);
  readonly total = signal(0);
  readonly busy = signal(false);
  readonly editing = signal(false);
  readonly resetLink = signal('');
  readonly categorias = signal<Registro[]>([]);
  tipo = 'servicos';
  q = '';
  status = '';
  ativo = '';
  page = 0;
  size = 10;
  id = '';
  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(80)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    senha: ['', [Validators.minLength(10), Validators.maxLength(72)]],
    perfil: ['ATENDENTE'],
    ativo: [true],
    categoriaId: [''],
  });
  constructor() {
    void this.load();
  }
  get senhaMode() {
    return this.tipo === 'senhas';
  }
  async load() {
    this.busy.set(true);
    try {
      const prefix = this.senhaMode ? '/gestao/senhas' : '/admin/' + this.tipo;
      const d = await this.api.request<{ items: Registro[]; total: number }>(
        prefix +
          '?q=' +
          encodeURIComponent(this.q) +
          '&status=' +
          this.status +
          '&ativo=' +
          this.ativo +
          '&page=' +
          this.page +
          '&size=' +
          this.size,
      );
      this.items.set(d.items);
      if (this.tipo === 'servicos') {
        const c = await this.api.request<{ categorias: Registro[] }>('/catalogo');
        this.categorias.set(c.categorias);
      }
      this.total.set(d.total);
    } catch (e) {
      this.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  change() {
    this.page = 0;
    this.editing.set(false);
    this.resetLink.set('');
    void this.load();
  }
  open(row?: Registro) {
    this.id = row?.id || '';
    this.resetLink.set('');
    this.form.reset({
      nome: row?.nome || row?.username || row?.service || '',
      email: row?.email || '',
      senha: '',
      perfil: row?.perfil || 'ATENDENTE',
      ativo: row?.ativo ?? true,
      categoriaId: row?.categoria?.id || '',
    });
    this.editing.set(true);
  }
  async save() {
    if (this.form.invalid || this.busy()) return;
    const d = this.form.getRawValue();
    if (this.tipo === 'usuarios' && !this.id && !d.senha) {
      this.api.message('Informe uma senha de pelo menos 10 caracteres.');
      return;
    }
    this.busy.set(true);
    try {
      if (this.senhaMode)
        await this.api.send('PUT', '/gestao/senhas/' + this.id, { service: d.nome });
      else
        await this.api.send(
          this.id ? 'PUT' : 'POST',
          '/admin/' + this.tipo + (this.id ? '/' + this.id : ''),
          d,
        );
      this.editing.set(false);
      this.api.message('Cadastro salvo.');
      await this.catalogo.load();
      await this.load();
    } catch (e) {
      this.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  async remove(row: Registro) {
    if (
      !confirm(
        'Excluir ' +
          (row.nome || row.username || row.label) +
          '? Registros com histórico devem ser desativados.',
      )
    )
      return;
    this.busy.set(true);
    try {
      await this.api.send(
        'DELETE',
        (this.senhaMode ? '/gestao/senhas' : '/admin/' + this.tipo) + '/' + row.id,
      );
      this.page = 0;
      await this.catalogo.load();
      await this.load();
    } catch (e) {
      this.api.error(e);
    } finally {
      this.busy.set(false);
    }
  }
  async reset(row: Registro) {
    try {
      const d = await this.api.request<{ url: string }>(
        '/admin/usuarios/' + row.id + '/redefinicao',
        {},
      );
      this.resetLink.set(d.url);
    } catch (e) {
      this.api.error(e);
    }
  }
}
