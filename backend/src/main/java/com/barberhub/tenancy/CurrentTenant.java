package com.barberhub.tenancy;

import com.barberhub.common.ApiException;
import org.springframework.stereotype.Component;

/** Acceso cómodo a la barbería de la petición actual. */
@Component
public class CurrentTenant {

    private final TenantRepository tenants;

    public CurrentTenant(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public long id() {
        long id = TenantContext.get();
        if (id <= 0) {
            throw ApiException.forbidden("Esta operación requiere una barbería.");
        }
        return id;
    }

    public Tenant get() {
        return tenants.findById(id()).orElseThrow(() -> ApiException.notFound("Barbería no encontrada."));
    }
}
