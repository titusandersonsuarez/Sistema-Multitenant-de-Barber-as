package com.barberhub.demo;

import com.barberhub.auth.AuthDtos.AuthResponse;
import com.barberhub.auth.AuthDtos.RegisterRequest;
import com.barberhub.auth.AuthService;
import com.barberhub.booking.Appointment;
import com.barberhub.booking.AppointmentRepository;
import com.barberhub.booking.AppointmentStatus;
import com.barberhub.catalog.Barber;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.catalog.ServiceOffering;
import com.barberhub.catalog.ServiceOfferingRepository;
import com.barberhub.catalog.WorkingHours;
import com.barberhub.catalog.WorkingHoursRepository;
import com.barberhub.tenancy.Tenant;
import com.barberhub.tenancy.TenantContext;
import com.barberhub.tenancy.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/**
 * Crea una barbería de demostración con barberos, servicios y citas de ejemplo para que
 * quien visite la demo pública vea la agenda y el resumen con datos. Solo corre si
 * {@code barberhub.demo.enabled=true} y la barbería todavía no existe.
 */
@Component
@ConditionalOnProperty(name = "barberhub.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String SLUG = "barberia-demo";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String[] CUSTOMERS = {
            "Juan Pérez", "Carlos Gómez", "Andrés Rojas", "Felipe Díaz", "Santiago Ruiz", "Mateo Vargas",
            "Daniel Torres", "Sebastián Castro", "Camilo Herrera", "Luis Moreno", "Diego Ortiz", "Tomás Medina"
    };

    private final AuthService auth;
    private final TenantRepository tenants;
    private final BarberRepository barbers;
    private final ServiceOfferingRepository services;
    private final WorkingHoursRepository hours;
    private final AppointmentRepository appointments;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final String email;
    private final String password;

    public DemoDataSeeder(AuthService auth, TenantRepository tenants, BarberRepository barbers,
                          ServiceOfferingRepository services, WorkingHoursRepository hours,
                          AppointmentRepository appointments, TransactionTemplate tx, Clock clock,
                          @Value("${barberhub.demo.email:demo@barberhub.co}") String email,
                          @Value("${barberhub.demo.password:demo12345}") String password) {
        this.auth = auth;
        this.tenants = tenants;
        this.barbers = barbers;
        this.services = services;
        this.hours = hours;
        this.appointments = appointments;
        this.tx = tx;
        this.clock = clock;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tenants.existsBySlug(SLUG)) {
            return;
        }
        AuthResponse res = auth.register(new RegisterRequest(
                "Barbería Demo", SLUG, "Andrés Barbero", email, password, "3001234567"));
        long tenantId = res.user().tenantId();

        TenantContext.callAs(tenantId, () -> tx.execute(s -> {
            Tenant t = tenants.findById(tenantId).orElseThrow();
            t.setAddress("Cra 33 # 48-20, Cabecera, Bucaramanga");
            tenants.save(t);

            Barber second = barbers.save(new Barber("Camilo Rueda", "Degradados y diseño de barba"));
            for (int day = 1; day <= 6; day++) {
                hours.save(new WorkingHours(second.getId(), day, LocalTime.of(10, 0), LocalTime.of(18, 0)));
            }
            services.save(new ServiceOffering("Cejas y perfilado", 15, 10_000));

            List<Barber> team = barbers.findByActiveTrueOrderByNameAsc();
            List<ServiceOffering> menu = services.findByActiveTrueOrderByNameAsc();
            seedAppointments(team, menu);
            return null;
        }));
        log.info("Barbería de demostración creada en /{} (usuario {})", SLUG, email);
    }

    /** Citas de las últimas dos semanas (atendidas, algunas canceladas o sin asistir) y de los próximos días. */
    private void seedAppointments(List<Barber> team, List<ServiceOffering> menu) {
        Random random = new Random(42);
        LocalDate today = LocalDate.now(clock.withZone(java.time.ZoneId.of("America/Bogota")));
        int code = 0;
        for (int offset = -14; offset <= 5; offset++) {
            LocalDate date = today.plusDays(offset);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (Barber barber : team) {
                LocalTime time = LocalTime.of(10, 0);
                int perDay = 3 + random.nextInt(3);
                for (int i = 0; i < perDay; i++) {
                    ServiceOffering service = menu.get(random.nextInt(menu.size()));
                    Appointment a = new Appointment(barber.getId(), service.getId(), "DEMO%04d".formatted(++code),
                            CUSTOMERS[random.nextInt(CUSTOMERS.length)], "300" + (1000000 + random.nextInt(8999999)),
                            date.atTime(time), date.atTime(time).plusMinutes(service.getDurationMinutes()),
                            service.getPriceCop());
                    if (offset < 0) {
                        int roll = random.nextInt(10);
                        a.setStatus(roll == 0 ? AppointmentStatus.NO_SHOW
                                : roll == 1 ? AppointmentStatus.CANCELLED : AppointmentStatus.COMPLETED);
                    }
                    appointments.save(a);
                    time = time.plusMinutes(service.getDurationMinutes() + 15L * (1 + random.nextInt(4)));
                    if (time.isAfter(LocalTime.of(16, 30))) break;
                }
            }
        }
    }
}
