import { Injectable, computed, signal } from '@angular/core';
import { AuthResponse, UserView } from './models';

const KEY = 'barberhub.session';

interface StoredSession {
  token: string;
  user: UserView;
  expiresAt: number;
}

/** Sesión del panel. Se guarda en localStorage para sobrevivir recargas. */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly state = signal<StoredSession | null>(this.load());

  readonly user = computed(() => this.state()?.user ?? null);
  readonly token = computed(() => this.state()?.token ?? null);
  readonly isOwner = computed(() => this.user()?.role === 'OWNER');
  readonly loggedIn = computed(() => this.state() !== null);

  start(res: AuthResponse): void {
    const s: StoredSession = { token: res.token, user: res.user, expiresAt: Date.now() + res.expiresIn * 1000 };
    this.state.set(s);
    try {
      localStorage.setItem(KEY, JSON.stringify(s));
    } catch {
      /* modo privado: la sesión vive solo en memoria */
    }
  }

  end(): void {
    this.state.set(null);
    try {
      localStorage.removeItem(KEY);
    } catch {
      /* ignorar */
    }
  }

  private load(): StoredSession | null {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return null;
      const s = JSON.parse(raw) as StoredSession;
      return s.expiresAt > Date.now() ? s : null;
    } catch {
      return null;
    }
  }
}
