import { Component, effect, inject, signal } from '@angular/core';
import { AdminApi } from '../../core/api';
import { addDays, apiError, formatCop, isoDate } from '../../core/format';
import { Dashboard } from '../../core/models';

type Range = 'week' | 'month' | 'last30';

@Component({
  selector: 'app-summary',
  template: `
    <h2>Resumen</h2>
    <div class="ranges" role="group" aria-label="Periodo">
      <button class="btn btn-small" [class.btn-dark]="range() === 'week'" (click)="range.set('week')">Esta semana</button>
      <button class="btn btn-small" [class.btn-dark]="range() === 'month'" (click)="range.set('month')">Este mes</button>
      <button class="btn btn-small" [class.btn-dark]="range() === 'last30'" (click)="range.set('last30')">Últimos 30 días</button>
    </div>

    @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }

    @if (data(); as d) {
      <div class="figures">
        <div class="figure main">
          <span class="value">{{ cop(d.revenueCop) }}</span>
          <span class="muted">cobrado en citas atendidas</span>
        </div>
        <div class="figure"><span class="value">{{ d.completed }}</span><span class="muted">atendidas</span></div>
        <div class="figure"><span class="value">{{ d.upcoming }}</span><span class="muted">por atender</span></div>
        <div class="figure"><span class="value">{{ d.noShow }}</span><span class="muted">no llegaron</span></div>
        <div class="figure"><span class="value">{{ d.cancelled }}</span><span class="muted">canceladas</span></div>
      </div>

      <div class="cols">
        <section class="panel">
          <h3>Por servicio</h3>
          @if (d.byService.length === 0) { <p class="muted">Aún no hay citas atendidas en este periodo.</p> }
          @for (b of d.byService; track b.name) {
            <div class="bar-row">
              <div class="bar-label"><span>{{ b.name }} ({{ b.count }})</span><strong>{{ cop(b.revenueCop) }}</strong></div>
              <div class="bar"><span [style.width.%]="pct(b.revenueCop, d.revenueCop)"></span></div>
            </div>
          }
        </section>
        <section class="panel">
          <h3>Por barbero</h3>
          @if (d.byBarber.length === 0) { <p class="muted">Aún no hay citas atendidas en este periodo.</p> }
          @for (b of d.byBarber; track b.name) {
            <div class="bar-row">
              <div class="bar-label"><span>{{ b.name }} ({{ b.count }})</span><strong>{{ cop(b.revenueCop) }}</strong></div>
              <div class="bar"><span [style.width.%]="pct(b.revenueCop, d.revenueCop)"></span></div>
            </div>
          }
        </section>
      </div>
    }
  `,
  styles: `
    .ranges { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }
    .figures { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 20px; }
    .figure { display: grid; padding: 14px 16px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-control); }
    .figure .value { font-family: var(--display); font-weight: 800; font-size: var(--step-2); line-height: 1.1; }
    .figure.main { grid-column: 1 / -1; background: var(--ink); color: #fff; border-color: var(--ink); }
    .figure.main .value { font-size: var(--step-3); }
    .figure.main .muted { color: #b9c9ec; }
    .cols { display: grid; gap: 16px; }
    .bar-row { margin-bottom: 12px; }
    .bar-label { display: flex; justify-content: space-between; gap: 8px; font-size: var(--step--1); margin-bottom: 4px; }
    .bar { height: 10px; background: var(--paper); border-radius: 999px; overflow: hidden; }
    .bar span { display: block; height: 100%; background: var(--pole-blue); }
    @media (min-width: 760px) {
      .figures { grid-template-columns: 2fr repeat(4, 1fr); }
      .figure.main { grid-column: auto; }
      .cols { grid-template-columns: 1fr 1fr; }
    }
  `,
})
export class SummaryPage {
  private api = inject(AdminApi);
  readonly range = signal<Range>('week');
  readonly data = signal<Dashboard | null>(null);
  readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const [from, to] = this.bounds(this.range());
      this.api.dashboard(from, to).subscribe({
        next: d => this.data.set(d),
        error: err => this.error.set(apiError(err)),
      });
    });
  }

  private bounds(r: Range): [string, string] {
    const now = new Date();
    const today = isoDate(now);
    if (r === 'week') {
      const monday = addDays(today, -((now.getDay() + 6) % 7));
      return [monday, addDays(monday, 6)];
    }
    if (r === 'month') {
      const first = isoDate(new Date(now.getFullYear(), now.getMonth(), 1));
      const last = isoDate(new Date(now.getFullYear(), now.getMonth() + 1, 0));
      return [first, last];
    }
    return [addDays(today, -29), today];
  }

  pct(part: number, total: number) { return total ? Math.max(2, Math.round((part / total) * 100)) : 0; }
  cop = formatCop;
}
