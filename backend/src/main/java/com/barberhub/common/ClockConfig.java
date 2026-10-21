package com.barberhub.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Reloj inyectable: en los tests se reemplaza por uno fijo para que "ahora" sea predecible. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
