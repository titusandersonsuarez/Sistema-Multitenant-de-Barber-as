package com.barberhub.auth;

import com.barberhub.auth.AuthDtos.AuthResponse;
import com.barberhub.auth.AuthDtos.LoginRequest;
import com.barberhub.auth.AuthDtos.RegisterRequest;
import com.barberhub.auth.AuthDtos.SuperAdminLoginRequest;
import com.barberhub.auth.AuthDtos.UserView;
import com.barberhub.catalog.Barber;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.catalog.ServiceOffering;
import com.barberhub.catalog.ServiceOfferingRepository;
import com.barberhub.catalog.WorkingHours;
import com.barberhub.catalog.WorkingHoursRepository;
import com.barberhub.common.ApiException;
import com.barberhub.security.AuthPrincipal;
import com.barberhub.security.BarberHubProperties;
import com.barberhub.security.JwtService;
import com.barberhub.tenancy.Tenant;
import com.barberhub.tenancy.TenantContext;
import com.barberhub.tenancy.TenantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    /** Slugs que chocarían con rutas del frontend. */
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "app", "panel", "login", "registro", "superadmin", "www", "static", "assets");

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final BarberRepository barbers;
    private final ServiceOfferingRepository services;
    private final WorkingHoursRepository hours;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final TransactionTemplate tx;
    private final BarberHubProperties props;

    public AuthService(TenantRepository tenants, AppUserRepository users, BarberRepository barbers,
                       ServiceOfferingRepository services, WorkingHoursRepository hours, PasswordEncoder encoder,
                       JwtService jwt, TransactionTemplate tx, BarberHubProperties props) {
        this.tenants = tenants;
        this.users = users;
        this.barbers = barbers;
        this.services = services;
        this.hours = hours;
        this.encoder = encoder;
        this.jwt = jwt;
        this.tx = tx;
        this.props = props;
    }

    /**
     * Registra una barbería nueva con su dueño y datos iniciales para que pueda recibir
     * reservas desde el primer minuto. Se hace en dos pasos porque la sesión de Hibernate
     * fija el tenant al abrirse: primero se crea la barbería, luego se abre otra transacción
     * "dentro" de ella para crear lo demás.
     */
    public AuthResponse register(RegisterRequest req) {
        String slug = req.slug().toLowerCase(Locale.ROOT);
        if (RESERVED_SLUGS.contains(slug)) {
            throw ApiException.conflict("Esa dirección no está disponible.");
        }
        Tenant tenant;
        try {
            tenant = tx.execute(s -> {
                if (tenants.existsBySlug(slug)) {
                    throw ApiException.conflict("Ya existe una barbería con esa dirección.");
                }
                Tenant t = new Tenant(req.shopName().trim(), slug);
                t.setPhone(req.phone());
                return tenants.save(t);
            });
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("Ya existe una barbería con esa dirección.");
        }

        AppUser owner = TenantContext.callAs(tenant.getId(), () -> tx.execute(s -> {
            Barber barber = barbers.save(new Barber(req.ownerName().trim(), "Dueño"));
            for (int day = 1; day <= 6; day++) {
                hours.save(new WorkingHours(barber.getId(), day, LocalTime.of(9, 0), LocalTime.of(19, 0)));
            }
            services.save(new ServiceOffering("Corte clásico", 30, 25_000));
            services.save(new ServiceOffering("Corte + barba", 45, 35_000));
            services.save(new ServiceOffering("Arreglo de barba", 20, 15_000));
            return users.save(new AppUser(req.email().toLowerCase(Locale.ROOT), encoder.encode(req.password()),
                    req.ownerName().trim(), Role.OWNER, barber.getId()));
        }));
        return respond(owner, tenant);
    }

    public AuthResponse login(LoginRequest req) {
        String slug = req.slug().toLowerCase(Locale.ROOT).trim();
        Tenant tenant = tenants.findBySlug(slug).filter(Tenant::isActive)
                .orElseThrow(AuthService::badCredentials);
        AppUser user = TenantContext.callAs(tenant.getId(), () -> tx.execute(s ->
                users.findByEmailIgnoreCase(req.email().trim()).orElse(null)));
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw badCredentials();
        }
        return respond(user, tenant);
    }

    public AuthResponse superAdminLogin(SuperAdminLoginRequest req) {
        var sa = props.superadmin();
        if (!sa.enabled() || !constantTimeEquals(sa.email(), req.email()) || !constantTimeEquals(sa.password(), req.password())) {
            throw badCredentials();
        }
        AuthPrincipal p = new AuthPrincipal(0L, TenantContext.ROOT, null, Role.SUPERADMIN, null, "Superadmin");
        return new AuthResponse(jwt.issue(p), jwt.ttlSeconds(),
                new UserView(0L, p.name(), Role.SUPERADMIN.name(), null, null, null, null));
    }

    private AuthResponse respond(AppUser user, Tenant tenant) {
        AuthPrincipal p = new AuthPrincipal(user.getId(), tenant.getId(), tenant.getSlug(), user.getRole(),
                user.getBarberId(), user.getFullName());
        return new AuthResponse(jwt.issue(p), jwt.ttlSeconds(), new UserView(user.getId(), user.getFullName(),
                user.getRole().name(), tenant.getId(), tenant.getSlug(), tenant.getName(), user.getBarberId()));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), String.valueOf(b).getBytes(StandardCharsets.UTF_8));
    }

    private static ApiException badCredentials() {
        return ApiException.unauthorized("Barbería, correo o contraseña incorrectos.");
    }
}
