import { Routes } from '@angular/router';
import { panelGuard } from './core/api';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/public/home').then(m => m.HomePage), title: 'BarberHub' },
  { path: 'registro', loadComponent: () => import('./pages/auth/register').then(m => m.RegisterPage), title: 'Registra tu barbería' },
  { path: 'entrar', loadComponent: () => import('./pages/auth/login').then(m => m.LoginPage), title: 'Entrar al panel' },
  {
    path: 'panel',
    canActivate: [panelGuard],
    loadComponent: () => import('./pages/panel/layout').then(m => m.PanelLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/panel/agenda').then(m => m.AgendaPage), title: 'Agenda' },
      { path: 'servicios', loadComponent: () => import('./pages/panel/services').then(m => m.ServicesPage), title: 'Servicios' },
      { path: 'barberos', loadComponent: () => import('./pages/panel/barbers').then(m => m.BarbersPage), title: 'Barberos' },
      { path: 'resumen', loadComponent: () => import('./pages/panel/summary').then(m => m.SummaryPage), title: 'Resumen' },
      { path: 'ajustes', loadComponent: () => import('./pages/panel/settings').then(m => m.SettingsPage), title: 'Ajustes' },
    ],
  },
  { path: ':slug', loadComponent: () => import('./pages/public/booking').then(m => m.BookingPage) },
  { path: ':slug/cita/:code', loadComponent: () => import('./pages/public/appointment').then(m => m.AppointmentPage) },
  { path: '**', redirectTo: '' },
];
