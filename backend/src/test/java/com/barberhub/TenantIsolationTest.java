package com.barberhub;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo más importante de un SaaS multitenant: una barbería nunca ve ni toca datos de otra. */
class TenantIsolationTest extends ApiTestSupport {

    @Test
    void cadaBarberiaSoloVeSusPropiosServiciosYBarberos() throws Exception {
        Shop a = registerShop();
        Shop b = registerShop();
        call(POST("/api/admin/services"), b.token(), Map.of("name", "Solo de B", "durationMinutes", 20, "priceCop", 1000));

        JsonNode servicesA = call(GET("/api/admin/services"), a.token(), null).body();
        assertThat(servicesA).hasSize(3);
        assertThat(servicesA.findValuesAsText("name")).doesNotContain("Solo de B");

        JsonNode barbersA = call(GET("/api/admin/barbers"), a.token(), null).body();
        assertThat(barbersA.findValuesAsText("id")).containsExactly(String.valueOf(a.barberId()));
    }

    @Test
    void noSePuedeLeerNiModificarRecursosDeOtraBarberiaPorId() throws Exception {
        Shop a = registerShop();
        Shop b = registerShop();

        assertThat(call(GET("/api/admin/barbers/" + b.barberId() + "/hours"), a.token(), null).status()).isEqualTo(404);
        assertThat(call(PUT("/api/admin/services/" + b.serviceId30()), a.token(),
                Map.of("name", "Hackeado", "durationMinutes", 30, "priceCop", 0)).status()).isEqualTo(404);
        assertThat(call(PUT("/api/admin/barbers/" + b.barberId() + "/hours"), a.token(),
                Map.of("blocks", List.of())).status()).isEqualTo(404);

        // El servicio de B sigue intacto.
        JsonNode servicesB = call(GET("/api/admin/services"), b.token(), null).body();
        assertThat(servicesB.findValuesAsText("name")).doesNotContain("Hackeado");
    }

    @Test
    void lasCitasDeOtraBarberiaSonInvisibles() throws Exception {
        Shop a = registerShop();
        Shop b = registerShop();
        Response booked = book(b, b.barberId(), b.serviceId30(), "2026-10-20T10:00:00");
        assertThat(booked.status()).isEqualTo(201);
        String codeB = booked.body().get("code").asText();

        JsonNode agendaA = call(GET("/api/admin/appointments?from=2026-10-20"), a.token(), null).body();
        assertThat(agendaA).isEmpty();

        // Consultar la cita de B usando la URL pública de A no la encuentra.
        assertThat(call(GET("/api/public/" + a.slug() + "/appointments/" + codeB), null, null).status()).isEqualTo(404);
        assertThat(call(POST("/api/public/" + a.slug() + "/appointments/" + codeB + "/cancel"), null, null).status()).isEqualTo(404);

        // Cambiar el estado de la cita de B con el token de A tampoco.
        long idB = call(GET("/api/admin/appointments?from=2026-10-20"), b.token(), null).body().get(0).get("id").asLong();
        assertThat(call(PATCH("/api/admin/appointments/" + idB + "/status"), a.token(), Map.of("status", "CANCELLED")).status())
                .isEqualTo(404);
    }

    @Test
    void noSePuedeReservarConElBarberoDeOtraBarberia() throws Exception {
        Shop a = registerShop();
        Shop b = registerShop();
        assertThat(book(a, b.barberId(), a.serviceId30(), "2026-10-20T10:00:00").status()).isEqualTo(404);
        assertThat(book(a, a.barberId(), b.serviceId30(), "2026-10-20T10:00:00").status()).isEqualTo(404);
        assertThat(call(GET("/api/public/" + a.slug() + "/availability?barberId=" + b.barberId()
                + "&serviceId=" + a.serviceId30() + "&date=2026-10-20"), null, null).status()).isEqualTo(404);
    }

    @Test
    void elLoginExigeLaBarberiaCorrecta() throws Exception {
        Shop a = registerShop();
        Shop b = registerShop();
        Response wrongShop = call(POST("/api/auth/login"), null, Map.of(
                "slug", b.slug(), "email", "owner@" + a.slug() + ".co", "password", "clave-segura-1"));
        assertThat(wrongShop.status()).isEqualTo(401);

        Response ok = call(POST("/api/auth/login"), null, Map.of(
                "slug", a.slug(), "email", "owner@" + a.slug() + ".co", "password", "clave-segura-1"));
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.body().get("user").get("tenantSlug").asText()).isEqualTo(a.slug());
    }

    @Test
    void sinTokenNoHayAccesoAlPanel() throws Exception {
        assertThat(call(GET("/api/admin/services"), null, null).status()).isEqualTo(401);
        assertThat(call(GET("/api/admin/services"), "token-falso", null).status()).isEqualTo(401);
        assertThat(call(GET("/api/public/no-existe-esta"), null, null).status()).isEqualTo(404);
    }

    @Test
    void elSuperadminVeTodasLasBarberiasYPuedeDesactivarUna() throws Exception {
        Shop a = registerShop();
        String token = call(POST("/api/auth/superadmin"), null,
                Map.of("email", "root@barberhub.test", "password", "super-secret-123")).body().get("token").asText();

        JsonNode list = call(GET("/api/superadmin/tenants"), token, null).body();
        assertThat(list.findValuesAsText("slug")).contains(a.slug());

        // Un dueño no puede entrar al panel de superadmin.
        assertThat(call(GET("/api/superadmin/tenants"), a.token(), null).status()).isEqualTo(403);

        long id = 0;
        for (JsonNode t : list) if (t.get("slug").asText().equals(a.slug())) id = t.get("id").asLong();
        assertThat(call(PATCH("/api/superadmin/tenants/" + id), token, Map.of("active", false)).status()).isEqualTo(200);
        assertThat(call(GET("/api/public/" + a.slug()), null, null).status()).isEqualTo(404);
    }
}
