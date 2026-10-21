import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApi } from '../../core/api';
import { apiError } from '../../core/format';

@Component({
  selector: 'app-settings',
  imports: [FormsModule],
  template: `
    <h2>Ajustes</h2>
    <form class="panel stack form" (ngSubmit)="save()">
      <div class="field">
        <label for="n">Nombre de la barbería</label>
        <input id="n" name="n" [(ngModel)]="form.name" required>
      </div>
      <div class="field">
        <label for="p">Celular</label>
        <input id="p" name="p" type="tel" [(ngModel)]="form.phone">
      </div>
      <div class="field">
        <label for="a">Dirección</label>
        <input id="a" name="a" [(ngModel)]="form.address" placeholder="Cra 27 # 45-12, Cabecera">
      </div>
      <div class="field">
        <label>Enlace de reservas</label>
        <div class="link-row">
          <code>{{ link() }}</code>
          <button type="button" class="btn btn-small" (click)="copy()">{{ copied() ? 'Copiado' : 'Copiar enlace' }}</button>
        </div>
        <span class="hint">Ponlo en la descripción de tu WhatsApp Business y en tu Instagram.</span>
      </div>
      @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
      @if (saved()) { <p class="alert alert-ok" role="status">Cambios guardados.</p> }
      <button class="btn btn-dark">Guardar cambios</button>
    </form>
  `,
  styles: `
    .form { max-width: 560px; }
    .link-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    code { padding: 8px 10px; background: var(--paper); border-radius: var(--radius-control); word-break: break-all; }
  `,
})
export class SettingsPage {
  private api = inject(AdminApi);
  form = { name: '', phone: '', address: '' };
  readonly link = signal('');
  readonly saved = signal(false);
  readonly copied = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.api.shop().subscribe(s => {
      this.form = { name: s.name, phone: s.phone ?? '', address: s.address ?? '' };
      this.link.set(`${location.origin}/${s.slug}`);
    });
  }

  save() {
    this.saved.set(false);
    this.error.set(null);
    this.api.updateShop(this.form).subscribe({
      next: () => this.saved.set(true),
      error: err => this.error.set(apiError(err)),
    });
  }

  copy() {
    navigator.clipboard?.writeText(this.link()).then(() => this.copied.set(true)).catch(() => {});
  }
}
