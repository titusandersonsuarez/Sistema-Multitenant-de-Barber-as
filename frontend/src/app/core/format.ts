const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

export function formatCop(value: number): string {
  return cop.format(value);
}

/** "2026-10-20T15:30:00" → "3:30 p. m." (la hora ya viene en hora local de la barbería). */
export function formatTime(isoLocal: string): string {
  const [h, m] = isoLocal.slice(11, 16).split(':').map(Number);
  return formatHour(h, m);
}

/** "15:30" → "3:30 p. m." */
export function formatSlot(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number);
  return formatHour(h, m);
}

function formatHour(h: number, m: number): string {
  const suffix = h < 12 ? 'a. m.' : 'p. m.';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')} ${suffix}`;
}

/** "2026-10-20" → "martes 20 de octubre" */
export function formatDay(isoDate: string): string {
  const [y, mo, d] = isoDate.slice(0, 10).split('-').map(Number);
  return new Date(y, mo - 1, d).toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
}

/** Fecha local del navegador en formato yyyy-mm-dd. */
export function isoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return isoDate(new Date(y, m - 1, d + days));
}

/** Extrae el mensaje de error que manda la API ({ mensaje, campos }). */
export function apiError(err: unknown, fallback = 'Algo salió mal. Intenta de nuevo.'): string {
  const e = err as { error?: { mensaje?: string; campos?: Record<string, string> }; status?: number };
  if (e?.status === 0) return 'No hay conexión con el servidor.';
  const campos = e?.error?.campos;
  if (campos && Object.keys(campos).length) return Object.values(campos)[0];
  return e?.error?.mensaje ?? fallback;
}
