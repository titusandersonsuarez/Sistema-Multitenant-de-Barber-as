import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApi } from '../../core/api';
import { apiError, formatCop } from '../../core/format';
import { ServiceItem } from '../../core/models';

type Draft = Omit<ServiceItem, 'id'> & { id?: number };

@Component({
  selector: 'app-services',
  imports: [FormsModule],
  template: `
    <h2>Servicios</h2>
    <p class="muted">Lo que ven tus clientes al reservar. La duración define cuánto espacio ocupa en la agenda.</p>

    @if (error()) { <p class="alert" role="alert">{{ error() }}</p> }
    @if (saved()) { <p class="alert alert-ok" role="status">{{ saved() }}</p> }

    <div class="table-scroll panel">
      <table class="table">
        <thead><tr><th>Servicio</th><th>Duración (min)</th><th>Precio (COP)</th><th>Visible</th><th></th></tr></thead>
        <tbody>
          @for (s of rows(); track $index) {
            <tr>
              <td><input class="cell" [(ngModel)]="s.name" [name]="'n' + $index" aria-label="Nombre"></td>
              <td><input class="cell num" type="number" min="5" max="480" step="5" [(ngModel)]="s.durationMinutes" [name]="'d' + $index" aria-label="Duración"></td>
              <td>
                <input class="cell num" type="number" min="0" step="1000" [(ngModel)]="s.priceCop" [name]="'p' + $index" aria-label="Precio">
                <div class="small muted">{{ cop(s.priceCop || 0) }}</div>
              </td>
              <td><input type="checkbox" [(ngModel)]="s.active" [name]="'a' + $index" aria-label="Visible para clientes"></td>
              <td><button class="btn btn-small" (click)="save(s)">{{ s.id ? 'Guardar' : 'Crear' }}</button></td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    <button class="btn" (click)="addRow()">Agregar servicio</button>
  `,
  styles: `
    h2 { margin-bottom: 4px; }
    .panel { margin: 16px 0; padding: 8px 12px; }
    .cell { width: 100%; min-width: 120px; min-height: 40px; padding: 6px 8px; border: 1px solid #aeb7c6; border-radius: var(--radius-control); font: inherit; }
    .num { min-width: 90px; max-width: 130px; }
  `,
})
export class ServicesPage {
  private api = inject(AdminApi);
  readonly rows = signal<Draft[]>([]);
  readonly error = signal<string | null>(null);
  readonly saved = signal<string | null>(null);

  constructor() {
    this.api.services().subscribe(list => this.rows.set(list.map(s => ({ ...s }))));
  }

  addRow() {
    this.rows.update(r => [...r, { name: '', durationMinutes: 30, priceCop: 20000, active: true }]);
  }

  save(s: Draft) {
    this.error.set(null);
    this.saved.set(null);
    const body = { name: s.name, durationMinutes: Number(s.durationMinutes), priceCop: Number(s.priceCop), active: s.active };
    const req = s.id ? this.api.updateService(s.id, body) : this.api.createService(body);
    req.subscribe({
      next: res => { s.id = res.id; this.saved.set(`Guardado: ${res.name}.`); },
      error: err => this.error.set(apiError(err)),
    });
  }

  cop = formatCop;
}
