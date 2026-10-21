package com.barberhub.security;

import com.barberhub.auth.Role;
import com.barberhub.tenancy.Tenant;
import com.barberhub.tenancy.TenantContext;
import com.barberhub.tenancy.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decide a qué barbería pertenece cada petición y lo deja en {@link TenantContext}:
 * <ul>
 *   <li>Rutas públicas {@code /api/public/{slug}/...}: por el slug de la URL.</li>
 *   <li>Rutas autenticadas: por el claim {@code tid} del JWT (nunca por algo que mande el cliente).</li>
 * </ul>
 * Si no se puede resolver, el contexto queda en NONE y Hibernate no devuelve datos de nadie.
 */
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final Pattern PUBLIC_PATH = Pattern.compile("^/api/public/([a-z0-9-]+)(/.*)?$");

    private final JwtService jwt;
    private final TenantRepository tenants;

    public TenantResolutionFilter(JwtService jwt, TenantRepository tenants) {
        this.jwt = jwt;
        this.tenants = tenants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Matcher m = PUBLIC_PATH.matcher(request.getRequestURI());
            if (m.matches()) {
                Optional<Tenant> tenant = tenants.findBySlug(m.group(1)).filter(Tenant::isActive);
                if (tenant.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"mensaje\":\"Barbería no encontrada.\"}");
                    return;
                }
                TenantContext.set(tenant.get().getId());
            } else {
                bearerToken(request).flatMap(jwt::parse).ifPresent(this::authenticate);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(AuthPrincipal principal) {
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.set(principal.role() == Role.SUPERADMIN ? TenantContext.ROOT : principal.tenantId());
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
