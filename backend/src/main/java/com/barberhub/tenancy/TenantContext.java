package com.barberhub.tenancy;

import java.util.function.Supplier;

/**
 * Guarda la barbería (tenant) de la petición actual.
 *
 * <ul>
 *   <li>{@link #NONE}: no hay tenant. Hibernate no devuelve ninguna fila con tenant_id
 *       (falla cerrado: si a una ruta se le olvida fijar el tenant, no filtra datos).</li>
 *   <li>{@link #ROOT}: superadministrador. Ve todos los tenants. Solo se usa en endpoints
 *       de superadmin y nunca para insertar.</li>
 * </ul>
 */
public final class TenantContext {

    public static final long NONE = -1L;
    public static final long ROOT = 0L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static long get() {
        Long id = CURRENT.get();
        return id == null ? NONE : id;
    }

    public static void set(long tenantId) {
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Ejecuta un bloque como otro tenant y restaura el anterior al terminar. */
    public static <T> T callAs(long tenantId, Supplier<T> action) {
        Long previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
