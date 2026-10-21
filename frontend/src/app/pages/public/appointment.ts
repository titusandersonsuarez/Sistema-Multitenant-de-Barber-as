import { Component, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PublicApi } from '../../core/api';
import { apiError, formatCop, formatDay, formatTime } from '../../core/format';
import { PublicAppointment, STATUS_LABEL } from '../../core/models';

/** Comprobante de la cita: /:slug/cita/:code */
@Component({
  selector: 'app-appointment',
  imports: [RouterLink],
  template: `
    <div class="pole"></div>
    <main class="wrap page">
      @if (appt(); as a) {
        @if (justBooked && a.status === 'BOOKED') {
          <p class="alert alert-ok" role="status">Listo, tu cita quedó reservada.</p>
        }
        <article class="ticket">
          <div class="ticket-top">
            <span class="muted small">Tu código</span>
            <strong class="code">{{ a.code }}</strong>
            <span class="status status-{{ a.status }}">{{ label[a.status] }}</span>
          </div>
          <dl class="details">
            <div><dt>Día</dt><dd class="cap">{{ day(a.startAt) }}</dd></div>
            <div><dt>Hora</dt><dd>{{ time(a.startAt) }} – {{ time(a.endAt) }}</dd></div>
            <div><dt>Servicio</dt><dd>{{ a.serviceName }}</dd></div>
            <div><dt>Barbero</dt><dd>{{ a.barberName }}</dd></div>
            <div><dt>Valor</dt><dd>{{ cop(a.priceCop) }}</dd></div>
            <div><dt>A nombre de</dt><dd>{{ a.customerName }}</dd></div>
          </dl>
        </article>

        <p class="muted small">Guarda este enlace o el código para consultar o cancelar tu cita.</p>

        @if (a.status === 'BOOKED') {
          @if (!confirming()) {
            <button class="btn" (click)="confirming.set(true)">Cancelar mi cita</button>
          } @else {
            <div class="panel stack">
              <p><strong>¿Seguro que quieres cancelarla?</strong> El horario quedará libre para otra persona.</p>
              <div class="row">
                <button class="btn btn-primary" (click)="cancel()" [disabled]="busy()">Sí, cancelar cita</button>
                <button class="btn" (click)="confirming.set(false)">No, conservarla</button>
              </div>
            </div>
          }
        }
        @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
        <p><a [routerLink]="['/', slug()]">Hacer otra reserva</a></p>
      } @else if (error()) {
        <h1>Cita no encontrada</h1>
        <p>{{ error() }}</p>
        <a [routerLink]="['/', slug()]">Volver a la barbería</a>
      } @else {
        <p class="muted">Cargando…</p>
      }
    </main>
  `,
  styles: `
    .page { max-width: 560px; padding-block: 32px 56px; display: grid; gap: 18px; }
    .ticket { background: var(--surface); border: 2px solid var(--ink); border-radius: var(--radius-panel); overflow: hidden; }
    .ticket-top {
      display: grid; justify-items: start; gap: 6px; padding: 20px 22px;
      border-bottom: 2px dashed var(--ink);
    }
    .code { font-family: var(--display); font-weight: 800; font-size: var(--step-4); line-height: 0.9; letter-spacing: 0.06em; }
    .details { margin: 0; padding: 14px 22px 20px; display: grid; gap: 10px; }
    .details div { display: flex; justify-content: space-between; gap: 16px; }
    .details dt { color: var(--ink-soft); }
    .details dd { margin: 0; font-weight: 600; text-align: right; }
    .cap::first-letter { text-transform: uppercase; }
    .row { display: flex; flex-wrap: wrap; gap: 10px; }
  `,
})
export class AppointmentPage {
  private api = inject(PublicApi);

  readonly slug = input.required<string>();
  readonly code = input.required<string>();

  readonly appt = signal<PublicAppointment | null>(null);
  readonly error = signal<string | null>(null);
  readonly confirming = signal(false);
  readonly busy = signal(false);
  readonly justBooked = !!inject(Router).currentNavigation()?.extras.state?.['justBooked'];

  readonly label = STATUS_LABEL;

  constructor() {
    effect(() => {
      this.api.appointment(this.slug(), this.code()).subscribe({
        next: a => this.appt.set(a),
        error: err => this.error.set(apiError(err)),
      });
    });
  }

  cancel() {
    this.busy.set(true);
    this.api.cancel(this.slug(), this.code()).subscribe({
      next: a => { this.appt.set(a); this.confirming.set(false); this.busy.set(false); },
      error: err => { this.error.set(apiError(err)); this.busy.set(false); },
    });
  }

  cop = formatCop;
  day = formatDay;
  time = formatTime;
}
