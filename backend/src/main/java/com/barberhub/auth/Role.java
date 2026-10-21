package com.barberhub.auth;

public enum Role {
    /** Dueño de la barbería: administra todo su negocio. */
    OWNER,
    /** Barbero: solo ve y gestiona su propia agenda. */
    BARBER,
    /** Administrador de la plataforma (no pertenece a ninguna barbería). */
    SUPERADMIN
}
