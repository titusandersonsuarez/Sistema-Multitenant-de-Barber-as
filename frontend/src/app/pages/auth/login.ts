import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi } from '../../core/api';
import { apiError } from '../../core/format';
import { Session } from '../../core/session';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pole"></div>
    <main class="wrap auth">
      <a routerLink="/" class="brand">BarberHub</a>
      <h2>Entrar al panel</h2>
      <form class="panel stack" (ngSubmit)="submit()">
        <div class="field">
          <label for="slug">Dirección de tu barbería</label>
          <input id="slug" name="slug" [(ngModel)]="slug" required autocapitalize="off" spellcheck="false" placeholder="el-parche">
        </div>
        <div class="field">
          <label for="email">Correo</label>
          <input id="email" name="email" type="email" [(ngModel)]="email" autocomplete="email" required>
        </div>
        <div class="field">
          <label for="pass">Contraseña</label>
          <input id="pass" name="pass" type="password" [(ngModel)]="password" autocomplete="current-password" required>
        </div>
        @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
        <button class="btn btn-dark btn-block" [disabled]="busy()">{{ busy() ? 'Entrando…' : 'Entrar' }}</button>
      </form>
      <p class="small">¿Aún no tienes cuenta? <a routerLink="/registro">Registra tu barbería</a></p>
      <p class="small muted">¿Solo quieres mirar? <button type="button" class="link" (click)="fillDemo()">Usar la cuenta de ejemplo</button></p>
    </main>
  `,
  styles: `
    .auth { max-width: 420px; padding-block: 28px 56px; }
    .link { background: none; border: 0; padding: 0; color: var(--pole-blue); text-decoration: underline; font: inherit; cursor: pointer; }
    .brand { display: inline-block; font-family: var(--display); font-weight: 800; font-size: var(--step-2); color: var(--ink); text-decoration: none; margin-bottom: 20px; }
  `,
})
export class LoginPage implements OnInit {
  private api = inject(AuthApi);
  private session = inject(Session);
  private router = inject(Router);

  slug = '';
  email = '';
  password = '';
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly demo = input<string>();

  ngOnInit() {
    if (this.demo()) this.fillDemo();
  }

  fillDemo() {
    this.slug = 'barberia-demo';
    this.email = 'demo@barberhub.co';
    this.password = 'demo12345';
  }

  submit() {
    this.busy.set(true);
    this.error.set(null);
    this.api.login({ slug: this.slug.trim().toLowerCase(), email: this.email, password: this.password }).subscribe({
      next: res => { this.session.start(res); this.router.navigate(['/panel']); },
      error: err => { this.error.set(apiError(err)); this.busy.set(false); },
    });
  }
}
