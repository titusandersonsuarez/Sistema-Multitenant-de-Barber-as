import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `
    <div class="pole"></div>
    <header class="wrap topbar">
      <span class="brand">BarberHub</span>
      <a routerLink="/entrar">Entrar al panel</a>
    </header>

    <main class="wrap hero">
      <div class="hero-copy">
        <h1>Que tus clientes reserven solos</h1>
        <p class="lead">
          Tu barbería tiene su propia página de reservas. El cliente elige servicio, barbero y hora
          desde el celular, y tú ves el día completo en un solo lugar.
        </p>
        <div class="actions">
          <a class="btn btn-primary" routerLink="/registro">Crear mi barbería gratis</a>
          <a class="btn" routerLink="/barberia-demo">Ver barbería de ejemplo</a>
        </div>
      </div>

      <aside class="board" aria-label="Ejemplo de página de reservas">
        <div class="board-head">
          <span class="board-name">Barbería El Parche</span>
          <span class="muted small">tuapp.com/el-parche</span>
        </div>
        <ul class="price-list">
          <li><span class="name">Corte clásico</span><span class="dots"></span><span class="price">$25.000</span></li>
          <li><span class="name">Corte + barba</span><span class="dots"></span><span class="price">$35.000</span></li>
          <li><span class="name">Arreglo de barba</span><span class="dots"></span><span class="price">$15.000</span></li>
        </ul>
        <div class="board-slots">
          <span>9:00</span><span>9:30</span><span class="taken">10:00</span><span>10:30</span><span>11:00</span>
        </div>
      </aside>
    </main>

    <section class="wrap how">
      <h2>Cómo funciona</h2>
      <ol class="steps">
        <li><strong>Registra tu barbería.</strong> Eliges la dirección de tu página y quedas con servicios y horario de ejemplo.</li>
        <li><strong>Ajusta precios y horarios.</strong> Agrega a tus barberos y el horario de cada uno.</li>
        <li><strong>Comparte el enlace.</strong> Ponlo en tu WhatsApp e Instagram. Las citas llegan a tu agenda sin choques de horario.</li>
      </ol>
    </section>

    <section class="wrap demo">
      <div class="panel">
        <h3>Pruébalo sin registrarte</h3>
        <p>Reserva como cliente en <a routerLink="/barberia-demo">la barbería de ejemplo</a>, o entra a su panel para ver la agenda y el resumen con datos de muestra.</p>
        <a class="btn btn-dark" routerLink="/entrar" [queryParams]="{ demo: 1 }">Entrar al panel de ejemplo</a>
      </div>
    </section>

    <footer class="wrap foot muted small">Hecho en Colombia · Precios en pesos colombianos</footer>
  `,
  styles: `
    .topbar { display: flex; justify-content: space-between; align-items: center; padding-block: 18px; }
    .brand { font-family: var(--display); font-weight: 800; font-size: var(--step-2); }
    .hero { display: grid; gap: 40px; padding-block: 32px 56px; align-items: center; }
    .lead { font-size: var(--step-1); color: var(--ink-soft); }
    .actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 24px; }
    .board {
      background: var(--surface); border: 2px solid var(--ink); border-radius: var(--radius-panel);
      padding: 22px; box-shadow: 10px 10px 0 var(--ink);
    }
    .board-head { display: grid; margin-bottom: 12px; }
    .board-name { font-family: var(--display); font-weight: 800; font-size: var(--step-2); }
    .board-slots { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
    .board-slots span { padding: 6px 12px; border: 1px solid var(--line); border-radius: var(--radius-control); font-weight: 600; }
    .board-slots .taken { color: var(--ink-soft); text-decoration: line-through; background: var(--paper); }
    .how { padding-block: 24px 48px; }
    .steps { padding-left: 1.4em; display: grid; gap: 14px; max-width: 62ch; }
    .demo { padding-bottom: 32px; }
    .demo .panel { max-width: 640px; }
    .foot { padding-block: 24px 40px; }
    @media (min-width: 860px) {
      .hero { grid-template-columns: 1.15fr 1fr; padding-block: 64px 88px; }
    }
  `,
})
export class HomePage {}
