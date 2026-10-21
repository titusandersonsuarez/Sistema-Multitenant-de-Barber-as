import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApi } from '../../core/api';
import { apiError } from '../../core/format';
import { Barber, DAY_NAMES, HoursBlock } from '../../core/models';

interface DayRow { day: number; open: boolean; start: string; end: string; }

@Component({
  selector: 'app-barbers',
  imports: [FormsModule],
  template: `
    <h2>Barberos</h2>
    @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
    @if (saved()) { <p class="alert alert-ok" role="status">{{ saved() }}</p> }

    <div class="layout">
      <div class="list">
        @for (b of barbers(); track b.id) {
          <button class="pick" [class.on]="selected()?.id === b.id" (click)="select(b)">
            <strong>{{ b.name }}</strong>
            <span class="small muted">{{ b.active ? 'Recibe reservas' : 'Oculto' }}{{ b.hasAccount ? ' · tiene acceso' : '' }}</span>
          </button>
        }
        <form class="new" (ngSubmit)="create()">
          <input name="newName" [(ngModel)]="newName" placeholder="Nombre del barbero" aria-label="Nombre del nuevo barbero">
          <button class="btn btn-small" [disabled]="!newName.trim()">Agregar</button>
        </form>
      </div>

      @if (selected(); as b) {
        <div class="panel stack">
          <div class="field">
            <label for="bn">Nombre</label>
            <input id="bn" name="bn" [(ngModel)]="edit.name">
          </div>
          <div class="field">
            <label for="bb">Descripción corta</label>
            <input id="bb" name="bb" [(ngModel)]="edit.bio" placeholder="Especialista en degradados">
          </div>
          <label class="check"><input type="checkbox" [(ngModel)]="edit.active" name="ba"> Recibe reservas en línea</label>
          <button class="btn btn-small" (click)="saveBarber(b)">Guardar datos</button>

          <h3>Horario semanal</h3>
          <div class="week">
            @for (d of week(); track d.day) {
              <div class="dayrow">
                <label class="check"><input type="checkbox" [(ngModel)]="d.open" [name]="'o' + d.day"> {{ dayName(d.day) }}</label>
                <input type="time" step="900" [(ngModel)]="d.start" [name]="'s' + d.day" [disabled]="!d.open" aria-label="Abre">
                <input type="time" step="900" [(ngModel)]="d.end" [name]="'e' + d.day" [disabled]="!d.open" aria-label="Cierra">
              </div>
            }
          </div>
          <button class="btn btn-dark" (click)="saveHours(b)">Guardar horario</button>

          @if (!b.hasAccount) {
            <h3>Acceso al panel</h3>
            <p class="muted small">Con una cuenta, el barbero ve solo su propia agenda.</p>
            <div class="field"><label for="ae">Correo</label><input id="ae" name="ae" type="email" [(ngModel)]="account.email"></div>
            <div class="field"><label for="ap">Contraseña</label><input id="ap" name="ap" type="password" [(ngModel)]="account.password" autocomplete="new-password"></div>
            <button class="btn btn-small" (click)="createAccount(b)">Crear acceso</button>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .layout { display: grid; gap: 16px; margin-top: 12px; }
    .list { display: grid; gap: 8px; align-content: start; min-width: 0; }
    .layout > .panel { min-width: 0; }
    .pick {
      width: 100%; min-width: 0;
      display: grid; text-align: left; gap: 2px; padding: 12px 14px; background: var(--surface);
      border: 1px solid var(--line); border-radius: var(--radius-control); font: inherit; color: inherit; cursor: pointer;
    }
    .pick.on { border: 2px solid var(--pole-blue); }
    .new { display: flex; gap: 8px; min-width: 0; }
    .new .btn { flex-shrink: 0; }
    .new input { flex: 1; min-width: 0; min-height: 36px; padding: 0 10px; border: 1px solid #aeb7c6; border-radius: var(--radius-control); font: inherit; }
    .check { display: flex; gap: 8px; align-items: center; font-weight: 600; }
    h3 { margin-top: 12px; }
    .week { display: grid; gap: 8px; }
    .dayrow { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; align-items: center; }
    .dayrow input[type=time] { min-height: 38px; padding: 0 6px; border: 1px solid #aeb7c6; border-radius: var(--radius-control); font: inherit; }
    @media (min-width: 820px) { .layout { grid-template-columns: 260px minmax(0, 1fr); } }
  `,
})
export class BarbersPage {
  private api = inject(AdminApi);
  readonly barbers = signal<Barber[]>([]);
  readonly selected = signal<Barber | null>(null);
  readonly week = signal<DayRow[]>([]);
  readonly error = signal<string | null>(null);
  readonly saved = signal<string | null>(null);
  edit = { name: '', bio: '', active: true };
  account = { email: '', password: '' };
  newName = '';

  constructor() {
    this.reload();
  }

  private reload(selectId?: number) {
    this.api.barbers().subscribe(list => {
      this.barbers.set(list);
      const pick = list.find(b => b.id === (selectId ?? this.selected()?.id)) ?? list[0];
      if (pick) this.select(pick);
    });
  }

  select(b: Barber) {
    this.selected.set(b);
    this.edit = { name: b.name, bio: b.bio ?? '', active: b.active ?? true };
    this.account = { email: '', password: '' };
    this.api.hours(b.id).subscribe(blocks => {
      this.week.set([1, 2, 3, 4, 5, 6, 7].map(day => {
        const blk = blocks.find(x => x.dayOfWeek === day);
        return { day, open: !!blk, start: blk?.start.slice(0, 5) ?? '09:00', end: blk?.end.slice(0, 5) ?? '19:00' };
      }));
    });
  }

  create() {
    this.api.createBarber({ name: this.newName.trim() }).subscribe({
      next: b => { this.newName = ''; this.reload(b.id); },
      error: err => this.error.set(apiError(err)),
    });
  }

  saveBarber(b: Barber) {
    this.flash();
    this.api.updateBarber(b.id, this.edit).subscribe({
      next: () => { this.saved.set('Datos guardados.'); this.reload(b.id); },
      error: err => this.error.set(apiError(err)),
    });
  }

  saveHours(b: Barber) {
    this.flash();
    const blocks: HoursBlock[] = this.week().filter(d => d.open).map(d => ({ dayOfWeek: d.day, start: d.start, end: d.end }));
    this.api.setHours(b.id, blocks).subscribe({
      next: () => this.saved.set('Horario guardado.'),
      error: err => this.error.set(apiError(err)),
    });
  }

  createAccount(b: Barber) {
    this.flash();
    this.api.createAccount(b.id, this.account).subscribe({
      next: () => { this.saved.set(`${b.name} ya puede entrar con su correo.`); this.reload(b.id); },
      error: err => this.error.set(apiError(err)),
    });
  }

  private flash() { this.error.set(null); this.saved.set(null); }
  dayName(d: number) { return DAY_NAMES[d]; }
}
