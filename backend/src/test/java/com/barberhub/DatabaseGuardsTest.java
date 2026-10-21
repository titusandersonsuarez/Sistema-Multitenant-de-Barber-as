package com.barberhub;

import com.barberhub.booking.Appointment;
import com.barberhub.booking.AppointmentRepository;
import com.barberhub.catalog.Barber;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.tenancy.TenantContext;
import com.barberhub.tenancy.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Defensas a nivel de base de datos y de Hibernate, sin pasar por los servicios. */
class DatabaseGuardsTest extends ApiTestSupport {

    @Autowired AppointmentRepository appointments;
    @Autowired BarberRepository barbers;
    @Autowired TenantRepository tenants;
    @Autowired TransactionTemplate tx;

    @Test
    void laBaseDeDatosRechazaCitasCruzadasAunqueElCodigoFalle() throws Exception {
        Shop shop = registerShop();
        long tenantId = tenants.findBySlug(shop.slug()).orElseThrow().getId();
        LocalDateTime start = LocalDateTime.of(2026, 10, 23, 10, 0);

        TenantContext.callAs(tenantId, () -> tx.execute(s -> appointments.saveAndFlush(new Appointment(
                shop.barberId(), shop.serviceId30(), "GUARD001", "A", "3000000000", start, start.plusMinutes(30), 1))));

        assertThatThrownBy(() -> TenantContext.callAs(tenantId, () -> tx.execute(s -> appointments.saveAndFlush(
                new Appointment(shop.barberId(), shop.serviceId30(), "GUARD002", "B", "3000000000",
                        start.plusMinutes(15), start.plusMinutes(45), 1)))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("no_overlap_per_barber");
    }

    @Test
    void sinTenantNoSeVeNada() throws Exception {
        Shop shop = registerShop();
        // Contexto NONE (por ejemplo, una ruta que olvidó fijar el tenant): Hibernate no devuelve filas.
        List<Barber> all = TenantContext.callAs(TenantContext.NONE, () -> tx.execute(s -> barbers.findAll()));
        Optional<Barber> byId = TenantContext.callAs(TenantContext.NONE, () -> tx.execute(s -> barbers.findById(shop.barberId())));
        assertThat(all).isEmpty();
        assertThat(byId).isEmpty();
    }
}
