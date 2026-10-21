import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PublicApi } from '../../core/api';
import { addDays, apiError, formatCop, formatDay, formatSlot, isoDate } from '../../core/format';
import { Barber, ServiceItem, Shop } from '../../core/models';

/** Página pública de reservas de una barbería: /:slug */
@Component({
  selector: 'app-booking',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pole"></div>

    @if (notFound()) {
      <main class="wrap empty">
        <h1>No encontramos esta barbería</h1>
        <p>Revisa el enlace que te compartieron.</p>
        <a routerLink="/">Ir al inicio</a>
      </main>
    } @else if (shop(); as s) {
      <header class="wrap shop-head">
        <h1>{{ s.name }}</h1>
        <p class="muted">
          @if (s.address) { {{ s.address }} }
          @if (s.address && s.phone) { <br> }
          @if (s.phone) { <a [href]="'tel:' + s.phone">{{ s.phone }}</a> }
        </p>
      </header>

      <main class="wrap booking">
        <ol class="flow">
          <li class="panel">
            <h3>¿Qué te vas a hacer?</h3>
            <ul class="price-list choices">
              @for (sv of services(); track sv.id) {
                <li>
                  <button type="button" class="choice" [class.on]="serviceId() === sv.id" (click)="pickService(sv.id)"
                          [attr.aria-pressed]="serviceId() === sv.id">
                    <span class="name">{{ sv.name }} <span class="muted small">· {{ sv.durationMinutes }} min</span></span>
                    <span class="dots"></span>
                    <span class="price">{{ cop(sv.priceCop) }}</span>
                  </button>
                </li>
              }
            </ul>
          </li>

          <li class="panel" [class.locked]="!serviceId()">
            <h3>¿Con quién?</h3>
            <div class="chips">
              @for (b of barbers(); track b.id) {
                <button type="button" class="chip" [class.on]="barberId() === b.id" [disabled]="!serviceId()"
                        (click)="pickBarber(b.id)" [attr.aria-pressed]="barberId() === b.id">
                  {{ b.name }}
                </button>
              }
            </div>
          </li>

          <li class="panel" [class.locked]="!barberId()">
            <h3>¿Cuándo?</h3>
            <div class="days" role="group" aria-label="Día">
              @for (d of days; track d) {
                <button type="button" class="day" [class.on]="date() === d" [disabled]="!barberId()"
                        (click)="pickDate(d)" [attr.aria-pressed]="date() === d">
                  <span class="small">{{ weekday(d) }}</span>
                  <strong>{{ d.slice(8) }}</strong>
                </button>
              }
            </div>
            @if (barberId()) {
              <p class="muted small day-label">{{ dayLabel(date()) }}</p>
              @if (loadingSlots()) {
                <p class="muted">Buscando horarios…</p>
              } @else if (slots().length === 0) {
                <p class="muted">No hay horarios libres este día. Prueba otro.</p>
              } @else {
                <div class="slots">
                  @for (t of slots(); track t) {
                    <button type="button" class="slot" [class.on]="time() === t" (click)="time.set(t)"
                            [attr.aria-pressed]="time() === t">{{ slot(t) }}</button>
                  }
                </div>
              }
            }
          </li>

          <li class="panel" [class.locked]="!time()">
            <h3>Tus datos</h3>
            <form class="stack" (ngSubmit)="confirm()">
              <div class="field">
                <label for="name">Nombre</label>
                <input id="name" name="name" [(ngModel)]="customerName" autocomplete="name" required [disabled]="!time()">
              </div>
              <div class="field">
                <label for="phone">Celular</label>
                <input id="phone" name="phone" type="tel" inputmode="tel" [(ngModel)]="customerPhone"
                       autocomplete="tel" required [disabled]="!time()">
                <span class="hint">Lo usa la barbería solo para avisarte si hay un cambio.</span>
              </div>
              @if (summary(); as sum) {
                <p class="summary">{{ sum }}</p>
              }
              @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
              <button class="btn btn-primary btn-block" type="submit" [disabled]="!time() || saving()">
                {{ saving() ? 'Reservando…' : 'Reservar cita' }}
              </button>
            </form>
          </li>
        </ol>
      </main>
    } @else {
      <main class="wrap empty"><p class="muted">Cargando…</p></main>
    }
  `,
  styles: `
    .shop-head { padding-block: 28px 8px; }
    .shop-head h1 { margin-bottom: 8px; }
    .booking { padding-bottom: 56px; }
    .flow { list-style: none; padding: 0; margin: 0; display: grid; gap: 16px; max-width: 640px; counter-reset: step; }
    .flow { grid-template-columns: minmax(0, 1fr); }
    .flow > li { counter-increment: step; min-width: 0; }
    .flow h3::before { content: counter(step) '. '; color: var(--pole-red); }
    .locked { opacity: 0.55; }
    .choices li { padding: 0; }
    .choice {
      display: flex; align-items: baseline; gap: 8px; width: 100%; padding: 10px 8px; text-align: left;
      background: none; border: 2px solid transparent; border-radius: var(--radius-control); font: inherit; color: inherit; cursor: pointer;
    }
    .choice.on { border-color: var(--pole-blue); background: #e3ebf8; }
    .chips { display: flex; flex-wrap: wrap; gap: 8px; }
    .slots { display: grid; grid-template-columns: repeat(auto-fill, minmax(104px, 1fr)); gap: 8px; }
    .chip, .slot, .day {
      min-height: 44px; padding: 0 16px; border: 1px solid #aeb7c6; border-radius: var(--radius-control);
      background: var(--surface); font: 600 var(--step-0) var(--body); color: var(--ink); cursor: pointer;
    }
    .chip.on, .slot.on, .day.on { background: var(--pole-blue); border-color: var(--pole-blue); color: #fff; }
    .chip:disabled, .day:disabled { cursor: not-allowed; }
    .days { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 6px; scrollbar-width: thin; }
    .day { display: grid; place-items: center; min-width: 58px; padding: 6px 8px; line-height: 1.2; }
    .day strong { font-family: var(--display); font-size: var(--step-1); }
    .day-label::first-letter { text-transform: uppercase; }
    .slot { padding: 0 8px; }
    .summary { padding: 12px 14px; background: var(--paper); border-radius: var(--radius-control); }
    .empty { padding-block: 64px; }
  `,
})
export class BookingPage {
  private api = inject(PublicApi);
  private router = inject(Router);

  readonly slug = input.required<string>();

  readonly shop = signal<Shop | null>(null);
  readonly notFound = signal(false);
  readonly services = signal<ServiceItem[]>([]);
  readonly barbers = signal<Barber[]>([]);

  readonly serviceId = signal<number | null>(null);
  readonly barberId = signal<number | null>(null);
  readonly date = signal(isoDate(new Date()));
  readonly time = signal<string | null>(null);
  readonly slots = signal<string[]>([]);
  readonly loadingSlots = signal(false);

  customerName = '';
  customerPhone = '';
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly days = Array.from({ length: 14 }, (_, i) => addDays(isoDate(new Date()), i));

  readonly summary = computed(() => {
    const sv = this.services().find(s => s.id === this.serviceId());
    const b = this.barbers().find(x => x.id === this.barberId());
    const t = this.time();
    if (!sv || !b || !t) return null;
    return `${sv.name} con ${b.name}, ${formatDay(this.date())} a las ${formatSlot(t)} y pagas ${formatCop(sv.priceCop)} en la barbería.`;
  });

  constructor() {
    effect(() => {
      const slug = this.slug();
      this.api.shop(slug).subscribe({
        next: s => {
          this.shop.set(s);
          this.api.services(slug).subscribe(list => this.services.set(list));
          this.api.barbers(slug).subscribe(list => {
            this.barbers.set(list);
            if (list.length === 1) this.barberId.set(list[0].id);
          });
        },
        error: () => this.notFound.set(true),
      });
    });
    effect(() => {
      const barberId = this.barberId();
      const serviceId = this.serviceId();
      const date = this.date();
      this.time.set(null);
      if (!barberId || !serviceId) return;
      this.loadingSlots.set(true);
      this.api.availability(this.slug(), barberId, serviceId, date).subscribe({
        next: a => { this.slots.set(a.slots); this.loadingSlots.set(false); },
        error: () => { this.slots.set([]); this.loadingSlots.set(false); },
      });
    });
  }

  pickService(id: number) { this.serviceId.set(id); this.error.set(null); }
  pickBarber(id: number) { this.barberId.set(id); this.error.set(null); }
  pickDate(d: string) { this.date.set(d); this.error.set(null); }

  confirm() {
    const barberId = this.barberId();
    const serviceId = this.serviceId();
    const t = this.time();
    if (!barberId || !serviceId || !t) return;
    if (!this.customerName.trim() || !this.customerPhone.trim()) {
      this.error.set('Escribe tu nombre y tu celular.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.api.book(this.slug(), {
      barberId, serviceId, startAt: `${this.date()}T${t}:00`,
      customerName: this.customerName.trim(), customerPhone: this.customerPhone.trim(),
    }).subscribe({
      next: a => this.router.navigate(['/', this.slug(), 'cita', a.code], { state: { justBooked: true } }),
      error: err => {
        this.saving.set(false);
        this.error.set(apiError(err));
        if (err.status === 409) {
          // Alguien tomó el turno: refrescamos los horarios.
          this.time.set(null);
          this.api.availability(this.slug(), barberId, serviceId, this.date()).subscribe(a => this.slots.set(a.slots));
        }
      },
    });
  }

  cop = formatCop;
  slot = formatSlot;
  dayLabel = formatDay;
  weekday(d: string) {
    const [y, m, day] = d.split('-').map(Number);
    return new Date(y, m - 1, day).toLocaleDateString('es-CO', { weekday: 'short' }).replace('.', '');
  }
}
