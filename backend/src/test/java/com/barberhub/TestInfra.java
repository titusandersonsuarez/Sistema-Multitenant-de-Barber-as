package com.barberhub;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Infraestructura de pruebas: un PostgreSQL 16 real embebido (para probar la restricción
 * EXCLUDE y los bloqueos de verdad) y un reloj fijo.
 * "Ahora" = lunes 19 de octubre de 2026, 8:00 a. m. en Bogotá.
 */
@TestConfiguration
public class TestInfra {

    public static final Instant NOW = Instant.parse("2026-10-19T13:00:00Z");

    private static EmbeddedPostgres postgres;

    private static synchronized EmbeddedPostgres postgres() {
        if (postgres == null) {
            try {
                postgres = EmbeddedPostgres.builder().start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return postgres;
    }

    @Bean
    @Primary
    public DataSource testDataSource() {
        return postgres().getPostgresDatabase();
    }

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
