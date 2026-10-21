import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi } from '../../core/api';
import { apiError } from '../../core/format';
import { Session } from '../../core/session';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pole"></div>
    <main class="wrap auth">
      <a routerLink="/" class="brand">BarberHub</a>
      <h2>Registra tu barbería</h2>
      <p class="muted">Quedas con tres servicios y horario de lunes a sábado de ejemplo. Los cambias cuando quieras.</p>

      <form class="panel stack" (ngSubmit)="submit()">
        <div class="field">
          <label for="shop">Nombre de la barbería</label>
          <input id="shop" name="shop" [(ngModel)]="shopName" (ngModelChange)="suggestSlug($event)" required>
        </div>
        <div class="field">
          <label for="slug">Dirección de tu página</label>
          <input id="slug" name="slug" [(ngModel)]="slug" (ngModelChange)="slugTouched = true" required
                 autocapitalize="off" spellcheck="false">
          <span class="hint">Tus clientes reservarán en {{ origin }}/{{ slug || 'tu-barberia' }}</span>
        </div>
        <div class="field">
          <label for="owner">Tu nombre</label>
          <input id="owner" name="owner" [(ngModel)]="ownerName" autocomplete="name" required>
        </div>
        <div class="field">
          <label for="phone">Celular de la barbería (opcional)</label>
          <input id="phone" name="phone" type="tel" [(ngModel)]="phone" autocomplete="tel">
        </div>
        <div class="field">
          <label for="email">Correo</label>
          <input id="email" name="email" type="email" [(ngModel)]="email" autocomplete="email" required>
        </div>
        <div class="field">
          <label for="pass">Contraseña</label>
          <input id="pass" name="pass" type="password" [(ngModel)]="password" autocomplete="new-password" required minlength="8">
          <span class="hint">Mínimo 8 caracteres.</span>
        </div>
        @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
        <button class="btn btn-primary btn-block" [disabled]="busy()">{{ busy() ? 'Creando…' : 'Crear mi barbería' }}</button>
      </form>
      <p class="small">¿Ya tienes cuenta? <a routerLink="/entrar">Entra al panel</a></p>
    </main>
  `,
  styles: `
    .auth { max-width: 480px; padding-block: 28px 56px; }
    .brand { display: inline-block; font-family: var(--display); font-weight: 800; font-size: var(--step-2); color: var(--ink); text-decoration: none; margin-bottom: 20px; }
  `,
})
export class RegisterPage {
  private api = inject(AuthApi);
  private session = inject(Session);
  private router = inject(Router);

  readonly origin = location.host;
  shopName = '';
  slug = '';
  slugTouched = false;
  ownerName = '';
  phone = '';
  email = '';
  password = '';
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  suggestSlug(name: string) {
    if (this.slugTouched) return;
    this.slug = name.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
      .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40);
  }

  submit() {
    this.busy.set(true);
    this.error.set(null);
    this.api.register({
      shopName: this.shopName, slug: this.slug.trim(), ownerName: this.ownerName,
      email: this.email, password: this.password, phone: this.phone || undefined,
    }).subscribe({
      next: res => { this.session.start(res); this.router.navigate(['/panel']); },
      error: err => { this.error.set(apiError(err)); this.busy.set(false); },
    });
  }
}
