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
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
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
            return Optional.of(new AuthPrincipal(
                    Long.valueOf(c.getSubject()),
                    c.get("tid", Number.class).longValue(),
                    c.get("slug", String.class),
                    Role.valueOf(c.get("role", String.class)),
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
