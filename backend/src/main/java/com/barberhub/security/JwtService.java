package com.barberhub.security;

import com.barberhub.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(BarberHubProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
        if (secret.contains("change-me") || secret.contains("dev-only")) {
            throw new IllegalStateException("JWT_SECRET usa un valor de ejemplo publicado; genera uno con: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(props.jwt().expirationHours());
    }

    public String issue(AuthPrincipal p) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(p.userId()))
                .claim("tid", p.tenantId())
                .claim("slug", p.tenantSlug())
                .claim("role", p.role().name())
                .claim("name", p.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        if (p.barberId() != null) {
            builder.claim("bid", p.barberId());
        }
        return builder.signWith(key).compact();
    }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Number bid = c.get("bid", Number.class);
            long tid = c.get("tid", Number.class).longValue();
            Role role = Role.valueOf(c.get("role", String.class));
            // tid 0 es el tenant ROOT (ve todas las barberías): solo válido para SUPERADMIN.
            if (role == Role.SUPERADMIN ? tid != 0 : tid <= 0) {
                return Optional.empty();
            }
            if (role == Role.BARBER && bid == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthPrincipal(
                    Long.valueOf(c.getSubject()),
                    tid,
                    c.get("slug", String.class),
                    role,
                    bid == null ? null : bid.longValue(),
                    c.get("name", String.class)));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
