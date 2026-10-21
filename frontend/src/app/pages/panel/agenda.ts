import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApi } from '../../core/api';
import { addDays, apiError, formatCop, formatDay, formatTime, isoDate } from '../../core/format';
import { AgendaItem, AppointmentStatus, Barber, STATUS_LABEL, ServiceItem } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-agenda',
  imports: [FormsModule],
  template: `
    <div class="head">
      <h2 class="cap">{{ dayLabel() }}</h2>
      <div class="nav">
        <button class="btn btn-small" (click)="shift(-1)" aria-label="Día anterior">‹</button>
        <button class="btn btn-small" (click)="date.set(today)" [disabled]="date() === today">Hoy</button>
        <button class="btn btn-small" (click)="shift(1)" aria-label="Día siguiente">›</button>
        <input class="date-input" type="date" [ngModel]="date()" (ngModelChange)="$event && date.set($event)" aria-label="Elegir fecha">
      </div>
    </div>

    <div class="toolbar">
      @if (session.isOwner() && barbers().length > 1) {
        <select [ngModel]="barberFilter()" (ngModelChange)="barberFilter.set($event)" aria-label="Filtrar por barbero">
          <option [ngValue]="null">Todos los barberos</option>
          @for (b of barbers(); track b.id) { <option [ngValue]="b.id">{{ b.name }}</option> }
        </select>
      }
      <span class="muted">{{ countLabel() }}</span>
      <button class="btn btn-dark btn-small add" (click)="showForm.set(!showForm())">
        {{ showForm() ? 'Cerrar' : 'Agregar cita' }}
      </button>
    </div>

    @if (showForm()) {
      <form class="panel walkin" (ngSubmit)="addWalkIn()">
        <div class="field">
          <label for="wb">Barbero</label>
          <select id="wb" name="wb" [(ngModel)]="form.barberId" required>
            @for (b of barbersForForm(); track b.id) { <option [ngValue]="b.id">{{ b.name }}</option> }
          </select>
        </div>
        <div class="field">
          <label for="ws">Servicio</label>
          <select id="ws" name="ws" [(ngModel)]="form.serviceId" required>
            @for (s of services(); track s.id) { <option [ngValue]="s.id">{{ s.name }}</option> }
          </select>
        </div>
        <div class="field">
          <label for="wt">Hora</label>
          <input id="wt" name="wt" type="time" step="900" [(ngModel)]="form.time" required>
        </div>
        <div class="field">
          <label for="wn">Cliente</label>
          <input id="wn" name="wn" [(ngModel)]="form.customerName" required>
        </div>
        <div class="field">
          <label for="wp">Celular</label>
          <input id="wp" name="wp" type="tel" [(ngModel)]="form.customerPhone" required>
        </div>
        <button class="btn btn-primary" [disabled]="busy()">Guardar cita</button>
      </form>
    }

    @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }

    @if (loading()) {
      <p class="muted">Cargando agenda…</p>
    } @else if (items().length === 0) {
      <div class="panel empty">
        <p><strong>No hay citas este día.</strong></p>
        <p class="muted">Comparte tu página de reservas para que lleguen solas, o agrega una cita a mano.</p>
      </div>
    } @else {
      <ol class="timeline">
        @for (a of items(); track a.id) {
          <li class="item" [class.dim]="a.status === 'CANCELLED'">
            <div class="when">
              <strong>{{ time(a.startAt) }}</strong>
              <span class="small muted">hasta {{ time(a.endAt) }}</span>
            </div>
            <div class="what">
              <div class="line1">
                <strong>{{ a.customerName }}</strong>
                <span class="status status-{{ a.status }}">{{ label[a.status] }}</span>
              </div>
              <div class="small muted">
                {{ a.serviceName }} · {{ cop(a.priceCop) }}
                @if (session.isOwner()) { · {{ a.barberName }} }
              </div>
              <a class="small" [href]="whatsapp(a)" target="_blank" rel="noopener">Escribir por WhatsApp</a>
            </div>
            @if (a.status === 'BOOKED') {
              <div class="actions">
                <button class="btn btn-small" (click)="set(a, 'COMPLETED')">Atendida</button>
                <button class="btn btn-small" (click)="set(a, 'NO_SHOW')">No llegó</button>
                <button class="btn btn-small" (click)="set(a, 'CANCELLED')">Cancelar</button>
              </div>
            } @else if (a.status !== 'CANCELLED') {
              <div class="actions">
                <button class="btn btn-small" (click)="set(a, 'BOOKED')">Deshacer</button>
              </div>
            }
          </li>
        }
      </ol>
    }
  `,
  styles: `
    .head { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: end; gap: 12px; margin-bottom: 12px; }
    .head h2 { margin: 0; }
    .cap::first-letter { text-transform: uppercase; }
    .nav { display: flex; gap: 6px; align-items: center; }
    .date-input { min-height: 36px; padding: 0 8px; border: 1px solid #aeb7c6; border-radius: var(--radius-control); font: inherit; }
    .toolbar { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 16px; }
    .toolbar select { min-height: 36px; border-radius: var(--radius-control); border: 1px solid #aeb7c6; padding: 0 8px; font: inherit; }
    .add { margin-left: auto; }
    .walkin { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); align-items: end; margin-bottom: 16px; }
    .timeline { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
    .item {
      display: grid; grid-template-columns: 104px minmax(0, 1fr); gap: 6px 16px; padding: 14px 16px;
      background: var(--surface); border: 1px solid var(--line); border-left: 5px solid var(--pole-blue); border-radius: var(--radius-control);
    }
    .item.dim { opacity: 0.6; border-left-color: var(--line); }
    .when { display: grid; white-space: nowrap; }
    .when strong { font-family: var(--display); font-size: var(--step-1); }
    .line1 { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    .actions { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: 6px; }
    @media (min-width: 720px) {
      .item { grid-template-columns: 112px minmax(0, 1fr) auto; align-items: center; }
      .actions { grid-column: auto; }
    }
  `,
})
export class AgendaPage {
  private api = inject(AdminApi);
  readonly session = inject(Session);

  readonly today = isoDate(new Date());
  readonly date = signal(this.today);
  readonly barberFilter = signal<number | null>(null);
  readonly items = signal<AgendaItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly showForm = signal(false);
  readonly barbers = signal<Barber[]>([]);
  readonly services = signal<ServiceItem[]>([]);

  readonly label = STATUS_LABEL;
  form = { barberId: 0, serviceId: 0, time: '', customerName: '', customerPhone: '' };

  readonly dayLabel = computed(() => (this.date() === this.today ? 'Hoy, ' : '') + formatDay(this.date()));
  readonly countLabel = computed(() => {
    const active = this.items().filter(a => a.status !== 'CANCELLED').length;
    return active === 1 ? '1 cita' : `${active} citas`;
  });
  readonly barbersForForm = computed(() => {
    const own = this.session.user()?.barberId;
    return this.session.isOwner() ? this.barbers().filter(b => b.active) : this.barbers().filter(b => b.id === own);
  });

  constructor() {
    this.api.barbers().subscribe(list => {
      this.barbers.set(list);
      this.form.barberId = this.barbersForForm()[0]?.id ?? 0;
    });
    this.api.services().subscribe(list => {
      this.services.set(list.filter(s => s.active));
      this.form.serviceId = this.services()[0]?.id ?? 0;
    });
    effect(() => this.load(this.date(), this.barberFilter()));
  }

  private load(date: string, barberId: number | null) {
    this.loading.set(true);
    this.api.agenda(date, undefined, barberId ?? undefined).subscribe({
      next: list => { this.items.set(list); this.loading.set(false); },
      error: err => { this.error.set(apiError(err)); this.loading.set(false); },
    });
  }

  shift(days: number) { this.date.set(addDays(this.date(), days)); }

  set(a: AgendaItem, status: AppointmentStatus) {
    this.error.set(null);
    this.api.setStatus(a.id, status).subscribe({
      next: updated => this.items.update(list => list.map(x => (x.id === updated.id ? updated : x))),
      error: err => this.error.set(apiError(err)),
    });
  }

  addWalkIn() {
    this.busy.set(true);
    this.error.set(null);
    this.api.walkIn({
      barberId: this.form.barberId, serviceId: this.form.serviceId, startAt: `${this.date()}T${this.form.time}:00`,
      customerName: this.form.customerName, customerPhone: this.form.customerPhone,
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.showForm.set(false);
        this.form = { ...this.form, time: '', customerName: '', customerPhone: '' };
        this.load(this.date(), this.barberFilter());
      },
      error: err => { this.busy.set(false); this.error.set(apiError(err)); },
    });
  }

  whatsapp(a: AgendaItem) {
    const digits = a.customerPhone.replace(/\D/g, '');
    const number = digits.length === 10 ? '57' + digits : digits;
    const text = `Hola ${a.customerName}, te escribimos por tu cita de ${a.serviceName} a las ${formatTime(a.startAt)}.`;
    return `https://wa.me/${number}?text=${encodeURIComponent(text)}`;
  }

  time = formatTime;
  cop = formatCop;
}
