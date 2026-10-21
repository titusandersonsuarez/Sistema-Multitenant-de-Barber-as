package com.barberhub.superadmin;

import com.barberhub.common.ApiException;
import com.barberhub.tenancy.Tenant;
import com.barberhub.tenancy.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Vista de la plataforma completa. Corre como tenant ROOT, que ve todas las barberías. */
@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {

    public record TenantSummary(Long id, String name, String slug, boolean active, Instant createdAt, long appointments) {
    }

    public record ActiveUpdate(@NotNull Boolean active) {
    }

    private final TenantRepository tenants;
    private final EntityManager em;

    public SuperAdminController(TenantRepository tenants, EntityManager em) {
        this.tenants = tenants;
        this.em = em;
    }

    @GetMapping("/tenants")
    @Transactional(readOnly = true)
    public List<TenantSummary> list() {
        Map<Long, Long> counts = em.createQuery(
                        "select a.tenantId, count(a) from Appointment a group by a.tenantId", Object[].class)
                .getResultList().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return tenants.findAll().stream()
                .map(t -> new TenantSummary(t.getId(), t.getName(), t.getSlug(), t.isActive(), t.getCreatedAt(),
                        counts.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    @PatchMapping("/tenants/{id}")
    @Transactional
    public TenantSummary setActive(@PathVariable Long id, @RequestBody ActiveUpdate req) {
        Tenant t = tenants.findById(id).orElseThrow(() -> ApiException.notFound("Barbería no encontrada."));
        t.setActive(req.active());
        return new TenantSummary(t.getId(), t.getName(), t.getSlug(), t.isActive(), t.getCreatedAt(), 0);
    }
}
