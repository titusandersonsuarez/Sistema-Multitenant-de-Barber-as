package com.barberhub;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "barberhub.demo.enabled=true")
class DemoDataSeederTest extends ApiTestSupport {

    @Test
    void laBarberiaDemoQuedaListaParaMostrar() throws Exception {
        assertThat(call(GET("/api/public/barberia-demo"), null, null).status()).isEqualTo(200);
        assertThat(call(GET("/api/public/barberia-demo/barbers"), null, null).body()).hasSize(2);

        Response login = call(POST("/api/auth/login"), null,
                Map.of("slug", "barberia-demo", "email", "demo@barberhub.co", "password", "demo12345"));
        assertThat(login.status()).isEqualTo(200);
        String token = login.body().get("token").asText();

        JsonNode dash = call(GET("/api/admin/dashboard?from=2026-10-05&to=2026-10-19"), token, null).body();
        assertThat(dash.get("completed").asLong()).isPositive();
        assertThat(dash.get("revenueCop").asLong()).isPositive();
        assertThat(call(GET("/api/admin/appointments?from=2026-10-20"), token, null).body()).isNotEmpty();
    }
}
