package com.barberhub.security;

import com.barberhub.auth.Role;

/** Usuario autenticado, reconstruido desde el JWT en cada petición. */
public record AuthPrincipal(Long userId, long tenantId, String tenantSlug, Role role, Long barberId, String name) {

    public boolean isOwner() {
        return role == Role.OWNER;
    }
}
