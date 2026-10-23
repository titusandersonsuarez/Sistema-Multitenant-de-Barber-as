package com.barberhub.security;

import com.barberhub.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos por ventana fija, en memoria (una instancia de la API). Frena fuerza bruta
 * de contraseñas, registro masivo de barberías y spam de reservas. Si se escala a varias
 * instancias, reemplazar por un almacén compartido (Redis, bucket4j...).
 */
@Component
public class RateLimiter {

    private static final int MAX_KEYS = 50_000;

    private record Window(long startMillis, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();
    private final boolean enabled;

    public RateLimiter(@Value("${barberhub.rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    /** Lanza 429 si {@code key} superó {@code limit} usos en la ventana actual. */
    public void check(String key, int limit, Duration window) {
        if (!enabled) {
            return;
        }
        long now = clock.millis();
        if (windows.size() > MAX_KEYS) {
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis() > Duration.ofHours(1).toMillis());
        }
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.startMillis() >= window.toMillis() ? new Window(now, 1)
                        : new Window(old.startMillis(), old.count() + 1));
        if (w.count() > limit) {
            throw ApiException.tooManyRequests("Demasiados intentos. Espera unos minutos y vuelve a intentar.");
        }
    }

    /**
     * IP del cliente. Con {@code server.forward-headers-strategy=native} Tomcat ya la toma de
     * X-Forwarded-For solo cuando la conexión viene de un proxy interno de confianza (nginx).
     */
    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    public static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
