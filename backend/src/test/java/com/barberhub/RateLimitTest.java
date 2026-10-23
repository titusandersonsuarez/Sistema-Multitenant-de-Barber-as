package com.barberhub;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "barberhub.rate-limit.enabled=true")
class RateLimitTest extends ApiTestSupport {

    @Test
    void elLoginSeBloqueaTrasVariosIntentosFallidos() throws Exception {
        Shop a = registerShop();
        Map<String, String> wrong = Map.of("slug", a.slug(), "email", "owner@" + a.slug() + ".co", "password", "mala-clave-x");
        for (int i = 0; i < 10; i++) {
            assertThat(call(POST("/api/auth/login"), null, wrong).status()).isEqualTo(401);
        }
        assertThat(call(POST("/api/auth/login"), null, wrong).status()).isEqualTo(429);
    }

    @Test
    void elSuperadminSeBloqueaPronto() throws Exception {
        for (int i = 0; i < 5; i++) {
            call(POST("/api/auth/superadmin"), null, Map.of("email", "root@barberhub.test", "password", "intento-" + i));
        }
        assertThat(call(POST("/api/auth/superadmin"), null,
                Map.of("email", "root@barberhub.test", "password", "super-secret-123")).status()).isEqualTo(429);
    }

    @Test
    void unCelularNoAcaparaLaAgenda() throws Exception {
        Shop a = registerShop();
        for (int h = 10; h < 13; h++) {
            assertThat(book(a, a.barberId(), a.serviceId30(), "2026-10-27T" + h + ":00:00").status()).isEqualTo(201);
        }
        assertThat(book(a, a.barberId(), a.serviceId30(), "2026-10-27T14:00:00").status()).isEqualTo(409);
    }
}
