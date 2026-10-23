package com.barberhub.admin;

import com.barberhub.admin.AdminDtos.BarberAccountRequest;
import com.barberhub.admin.AdminDtos.BarberDto;
import com.barberhub.admin.AdminDtos.BarberUpsert;
import com.barberhub.admin.AdminDtos.HoursBlock;
import com.barberhub.admin.AdminDtos.HoursUpdate;
import com.barberhub.admin.AdminDtos.ServiceDto;
import com.barberhub.admin.AdminDtos.ServiceUpsert;
import com.barberhub.admin.AdminDtos.ShopSettings;
import com.barberhub.admin.AdminDtos.ShopUpdate;
import com.barberhub.auth.AppUser;
import com.barberhub.auth.AppUserRepository;
import com.barberhub.auth.Role;
import com.barberhub.catalog.Barber;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.catalog.ServiceOffering;
import com.barberhub.catalog.ServiceOfferingRepository;
import com.barberhub.catalog.WorkingHours;
import com.barberhub.catalog.WorkingHoursRepository;
import com.barberhub.common.ApiException;
import com.barberhub.tenancy.CurrentTenant;
import com.barberhub.tenancy.Tenant;
import com.barberhub.tenancy.TenantRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Configuración del negocio: datos de la barbería, servicios, barberos y horarios. Solo el dueño. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('OWNER')")
@Transactional
public class AdminCatalogController {

    private final CurrentTenant currentTenant;
    private final TenantRepository tenants;
    private final ServiceOfferingRepository services;
    private final BarberRepository barbers;
    private final WorkingHoursRepository hours;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AdminCatalogController(CurrentTenant currentTenant, TenantRepository tenants,
                                  ServiceOfferingRepository services, BarberRepository barbers,
                                  WorkingHoursRepository hours, AppUserRepository users, PasswordEncoder encoder) {
        this.currentTenant = currentTenant;
        this.tenants = tenants;
        this.services = services;
        this.barbers = barbers;
        this.hours = hours;
        this.users = users;
        this.encoder = encoder;
    }

    // ---------------- Barbería ----------------

    @GetMapping("/shop")
    @PreAuthorize("hasAnyRole('OWNER','BARBER')")
    public ShopSettings shop() {
        return toDto(currentTenant.get());
    }

    @PutMapping("/shop")
    public ShopSettings updateShop(@Valid @RequestBody ShopUpdate req) {
        Tenant t = currentTenant.get();
        t.setName(req.name().trim());
        t.setPhone(req.phone());
        t.setAddress(req.address());
        return toDto(tenants.save(t));
    }

    // ---------------- Servicios ----------------

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('OWNER','BARBER')")
    public List<ServiceDto> listServices() {
        return services.findAllByOrderByNameAsc().stream().map(AdminCatalogController::toDto).toList();
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceDto createService(@Valid @RequestBody ServiceUpsert req) {
        ServiceOffering s = new ServiceOffering(req.name().trim(), req.durationMinutes(), req.priceCop());
        if (req.active() != null) s.setActive(req.active());
        return toDto(services.save(s));
    }

    @PutMapping("/services/{id}")
    public ServiceDto updateService(@PathVariable Long id, @Valid @RequestBody ServiceUpsert req) {
        ServiceOffering s = services.findById(id).orElseThrow(() -> ApiException.notFound("Servicio no encontrado."));
        s.setName(req.name().trim());
        s.setDurationMinutes(req.durationMinutes());
        s.setPriceCop(req.priceCop());
        if (req.active() != null) s.setActive(req.active());
        return toDto(s);
    }

    // ---------------- Barberos ----------------

    @GetMapping("/barbers")
    @PreAuthorize("hasAnyRole('OWNER','BARBER')")
    public List<BarberDto> listBarbers() {
        return barbers.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @PostMapping("/barbers")
    @ResponseStatus(HttpStatus.CREATED)
    public BarberDto createBarber(@Valid @RequestBody BarberUpsert req) {
        Barber b = new Barber(req.name().trim(), req.bio());
        if (req.active() != null) b.setActive(req.active());
        return toDto(barbers.save(b));
    }

    @PutMapping("/barbers/{id}")
    public BarberDto updateBarber(@PathVariable Long id, @Valid @RequestBody BarberUpsert req) {
        Barber b = barber(id);
        b.setName(req.name().trim());
        b.setBio(req.bio());
        if (req.active() != null) b.setActive(req.active());
        return toDto(b);
    }

    @GetMapping("/barbers/{id}/hours")
    @PreAuthorize("hasAnyRole('OWNER','BARBER')")
    public List<HoursBlock> getHours(@PathVariable Long id) {
        barber(id);
        return hours.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(id).stream()
                .map(h -> new HoursBlock(h.getDayOfWeek(), h.getStartTime(), h.getEndTime()))
                .toList();
    }

    /** Reemplaza el horario semanal completo del barbero. */
    @PutMapping("/barbers/{id}/hours")
    public List<HoursBlock> setHours(@PathVariable Long id, @Valid @RequestBody HoursUpdate req) {
        barber(id);
        validateBlocks(req.blocks());
        hours.deleteByBarberId(id);
        hours.flush();
        req.blocks().forEach(b -> hours.save(new WorkingHours(id, b.dayOfWeek(), b.start(), b.end())));
        return getHours(id);
    }

    /** Crea el usuario con el que el barbero entra a ver su propia agenda. */
    @PostMapping("/barbers/{id}/account")
    @ResponseStatus(HttpStatus.CREATED)
    public BarberDto createAccount(@PathVariable Long id, @Valid @RequestBody BarberAccountRequest req) {
        com.barberhub.auth.AuthDtos.requireBcryptLength(req.password());
        Barber b = barber(id);
        if (users.findByBarberId(id).isPresent()) {
            throw ApiException.conflict("Este barbero ya tiene cuenta.");
        }
        String email = req.email().toLowerCase(Locale.ROOT).trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya hay un usuario con ese correo en tu barbería.");
        }
        users.save(new AppUser(email, encoder.encode(req.password()), b.getName(), Role.BARBER, b.getId()));
        return toDto(b);
    }

    // ---------------- helpers ----------------

    private Barber barber(Long id) {
        return barbers.findById(id).orElseThrow(() -> ApiException.notFound("Barbero no encontrado."));
    }

    private static void validateBlocks(List<HoursBlock> blocks) {
        for (HoursBlock b : blocks) {
            if (!b.end().isAfter(b.start())) {
                throw ApiException.badRequest("La hora de cierre debe ser posterior a la de apertura.");
            }
        }
        var sorted = blocks.stream()
                .sorted(Comparator.comparingInt(HoursBlock::dayOfWeek).thenComparing(HoursBlock::start))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            HoursBlock prev = sorted.get(i - 1);
            HoursBlock cur = sorted.get(i);
            if (prev.dayOfWeek() == cur.dayOfWeek() && cur.start().isBefore(prev.end())) {
                throw ApiException.badRequest("Hay bloques de horario que se cruzan el mismo día.");
            }
        }
    }

    private static ShopSettings toDto(Tenant t) {
        return new ShopSettings(t.getId(), t.getName(), t.getSlug(), t.getPhone(), t.getAddress(), t.getTimezone());
    }

    private static ServiceDto toDto(ServiceOffering s) {
        return new ServiceDto(s.getId(), s.getName(), s.getDurationMinutes(), s.getPriceCop(), s.isActive());
    }

    private BarberDto toDto(Barber b) {
        return new BarberDto(b.getId(), b.getName(), b.getBio(), b.isActive(), users.findByBarberId(b.getId()).isPresent());
    }
}
