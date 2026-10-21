import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import {
  AgendaItem, AuthResponse, AppointmentStatus, Availability, Barber, Dashboard, HoursBlock,
  PublicAppointment, ServiceItem, Shop, ShopSettings,
} from './models';
import { Session } from './session';

const API = '/api';

/** API pública de una barbería (lo que usa el cliente para reservar). */
@Injectable({ providedIn: 'root' })
export class PublicApi {
  private http = inject(HttpClient);

  shop(slug: string) { return this.http.get<Shop>(`${API}/public/${slug}`); }
  services(slug: string) { return this.http.get<ServiceItem[]>(`${API}/public/${slug}/services`); }
  barbers(slug: string) { return this.http.get<Barber[]>(`${API}/public/${slug}/barbers`); }
  availability(slug: string, barberId: number, serviceId: number, date: string) {
    return this.http.get<Availability>(`${API}/public/${slug}/availability`, {
      params: { barberId, serviceId, date },
    });
  }
  book(slug: string, body: { barberId: number; serviceId: number; startAt: string; customerName: string; customerPhone: string }) {
    return this.http.post<PublicAppointment>(`${API}/public/${slug}/appointments`, body);
  }
  appointment(slug: string, code: string) {
    return this.http.get<PublicAppointment>(`${API}/public/${slug}/appointments/${code}`);
  }
  cancel(slug: string, code: string) {
    return this.http.post<PublicAppointment>(`${API}/public/${slug}/appointments/${code}/cancel`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private http = inject(HttpClient);

  register(body: { shopName: string; slug: string; ownerName: string; email: string; password: string; phone?: string }) {
    return this.http.post<AuthResponse>(`${API}/auth/register`, body);
  }
  login(body: { slug: string; email: string; password: string }) {
    return this.http.post<AuthResponse>(`${API}/auth/login`, body);
  }
}

/** API del panel de la barbería (dueño y barberos). */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private http = inject(HttpClient);

  shop() { return this.http.get<ShopSettings>(`${API}/admin/shop`); }
  updateShop(body: { name: string; phone?: string; address?: string }) {
    return this.http.put<ShopSettings>(`${API}/admin/shop`, body);
  }

  services() { return this.http.get<ServiceItem[]>(`${API}/admin/services`); }
  createService(body: Omit<ServiceItem, 'id'>) { return this.http.post<ServiceItem>(`${API}/admin/services`, body); }
  updateService(id: number, body: Omit<ServiceItem, 'id'>) {
    return this.http.put<ServiceItem>(`${API}/admin/services/${id}`, body);
  }

  barbers() { return this.http.get<Barber[]>(`${API}/admin/barbers`); }
  createBarber(body: { name: string; bio?: string }) { return this.http.post<Barber>(`${API}/admin/barbers`, body); }
  updateBarber(id: number, body: { name: string; bio?: string; active?: boolean }) {
    return this.http.put<Barber>(`${API}/admin/barbers/${id}`, body);
  }
  hours(id: number) { return this.http.get<HoursBlock[]>(`${API}/admin/barbers/${id}/hours`); }
  setHours(id: number, blocks: HoursBlock[]) {
    return this.http.put<HoursBlock[]>(`${API}/admin/barbers/${id}/hours`, { blocks });
  }
  createAccount(id: number, body: { email: string; password: string }) {
    return this.http.post<Barber>(`${API}/admin/barbers/${id}/account`, body);
  }

  agenda(from: string, to?: string, barberId?: number) {
    const params: Record<string, string> = { from };
    if (to) params['to'] = to;
    if (barberId) params['barberId'] = String(barberId);
    return this.http.get<AgendaItem[]>(`${API}/admin/appointments`, { params });
  }
  walkIn(body: { barberId: number; serviceId: number; startAt: string; customerName: string; customerPhone: string }) {
    return this.http.post<AgendaItem>(`${API}/admin/appointments`, body);
  }
  setStatus(id: number, status: AppointmentStatus) {
    return this.http.patch<AgendaItem>(`${API}/admin/appointments/${id}/status`, { status });
  }
  dashboard(from: string, to: string) {
    return this.http.get<Dashboard>(`${API}/admin/dashboard`, { params: { from, to } });
  }
}

/** Agrega el token a las llamadas del panel y cierra sesión si expiró. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(Session);
  const router = inject(Router);
  const token = session.token();
  const isAdmin = req.url.startsWith(`${API}/admin`);
  const authed = token && isAdmin ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(authed).pipe(
    catchError((err: HttpErrorResponse) => {
      if (isAdmin && err.status === 401) {
        session.end();
        router.navigate(['/entrar']);
      }
      return throwError(() => err);
    }),
  );
};

export const panelGuard: CanActivateFn = () => {
  const session = inject(Session);
  return session.loggedIn() ? true : inject(Router).createUrlTree(['/entrar']);
};
