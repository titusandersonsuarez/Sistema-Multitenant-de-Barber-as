import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Session } from '../../core/session';

@Component({
  selector: 'app-panel-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="rail">
        <div class="pole"></div>
        <div class="rail-inner">
          <div class="shop">
            <strong>{{ session.user()?.tenantName }}</strong>
            <a class="small" [href]="'/' + session.user()?.tenantSlug" target="_blank" rel="noopener">Ver mi página de reservas</a>
          </div>
          <nav aria-label="Panel">
            <a routerLink="/panel" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }">Agenda</a>
            @if (session.isOwner()) {
              <a routerLink="/panel/barberos" routerLinkActive="on">Barberos</a>
              <a routerLink="/panel/servicios" routerLinkActive="on">Servicios</a>
            }
            <a routerLink="/panel/resumen" routerLinkActive="on">Resumen</a>
            @if (session.isOwner()) {
              <a routerLink="/panel/ajustes" routerLinkActive="on">Ajustes</a>
            }
          </nav>
          <div class="who small muted">
            {{ session.user()?.name }}
            <button class="link" (click)="logout()">Cerrar sesión</button>
          </div>
        </div>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .shell { min-height: 100dvh; }
    .rail { background: var(--ink); color: #fff; }
    .rail-inner { padding: 14px var(--gutter); display: grid; gap: 12px; }
    .shop { display: grid; gap: 2px; }
    .shop strong { font-family: var(--display); font-size: var(--step-2); font-weight: 800; line-height: 1.05; }
    .shop a { color: #b9c9ec; }
    nav { display: flex; gap: 4px; overflow-x: auto; }
    nav a {
      padding: 10px 12px; border-radius: var(--radius-control); color: #d6deee; text-decoration: none; font-weight: 600; white-space: nowrap;
    }
    nav a:hover { background: rgba(255, 255, 255, 0.08); }
    nav a.on { background: #fff; color: var(--ink); }
    .who { display: flex; gap: 12px; align-items: center; color: #b9c9ec; }
    .link { background: none; border: 0; padding: 0; color: #fff; text-decoration: underline; font: inherit; cursor: pointer; }
    .content { padding: 24px var(--gutter) 56px; max-width: 1000px; }
    @media (min-width: 900px) {
      .shell { display: grid; grid-template-columns: 250px 1fr; }
      .rail { position: sticky; top: 0; height: 100dvh; }
      .rail-inner { padding: 20px; grid-template-rows: auto 1fr auto; height: calc(100% - 10px); }
      nav { flex-direction: column; overflow: visible; }
      .who { flex-direction: column; align-items: flex-start; gap: 4px; }
      .content { padding: 36px 40px 64px; }
    }
  `,
})
export class PanelLayout {
  readonly session = inject(Session);
  private router = inject(Router);

  logout() {
    this.session.end();
    this.router.navigate(['/entrar']);
  }
}
