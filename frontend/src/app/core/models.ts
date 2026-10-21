export type Role = 'OWNER' | 'BARBER' | 'SUPERADMIN';
export type AppointmentStatus = 'BOOKED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface UserView {
  id: number;
  name: string;
  role: Role;
  tenantId?: number;
  tenantSlug?: string;
  tenantName?: string;
  barberId?: number;
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
  user: UserView;
}

export interface Shop {
  name: string;
  slug: string;
  phone?: string;
  address?: string;
}

export interface ServiceItem {
  id: number;
  name: string;
  durationMinutes: number;
  priceCop: number;
  active?: boolean;
}

export interface Barber {
  id: number;
  name: string;
  bio?: string;
  active?: boolean;
  hasAccount?: boolean;
}

export interface Availability {
  date: string;
  slots: string[];
}

export interface PublicAppointment {
  code: string;
  status: AppointmentStatus;
  barberName: string;
  serviceName: string;
  startAt: string;
  endAt: string;
  priceCop: number;
  customerName: string;
}

export interface AgendaItem {
  id: number;
  code: string;
  barberId: number;
  barberName: string;
  serviceId: number;
  serviceName: string;
  customerName: string;
  customerPhone: string;
  startAt: string;
  endAt: string;
  priceCop: number;
  status: AppointmentStatus;
}

export interface HoursBlock {
  dayOfWeek: number;
  start: string;
  end: string;
}

export interface ShopSettings {
  id: number;
  name: string;
  slug: string;
  phone?: string;
  address?: string;
  timezone: string;
}

export interface Breakdown {
  name: string;
  count: number;
  revenueCop: number;
}

export interface Dashboard {
  from: string;
  to: string;
  completed: number;
  upcoming: number;
  cancelled: number;
  noShow: number;
  revenueCop: number;
  byService: Breakdown[];
  byBarber: Breakdown[];
}

export const STATUS_LABEL: Record<AppointmentStatus, string> = {
  BOOKED: 'Reservada',
  COMPLETED: 'Atendida',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No llegó',
};

export const DAY_NAMES = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
