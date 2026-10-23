package com.barberhub.booking;

import com.barberhub.catalog.Barber;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.catalog.ServiceOffering;
import com.barberhub.catalog.ServiceOfferingRepository;
import com.barberhub.catalog.WorkingHours;
import com.barberhub.catalog.WorkingHoursRepository;
import com.barberhub.common.ApiException;
import com.barberhub.security.BarberHubProperties;
import com.barberhub.tenancy.CurrentTenant;
import com.barberhub.tenancy.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula horarios libres y crea/cancela citas. Todo corre dentro del tenant actual,
 * así que un barbero o servicio de otra barbería simplemente "no existe" aquí.
 */
@Service
public class BookingService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Citas futuras activas que un mismo celular puede tener a la vez en una barbería (reserva pública). */
    private static final int MAX_ACTIVE_PER_PHONE = 3;

    private final BarberRepository barbers;
    private final ServiceOfferingRepository services;
    private final WorkingHoursRepository hours;
    private final AppointmentRepository appointments;
    private final CurrentTenant currentTenant;
    private final Clock clock;
    private final int stepMinutes;
    private final int maxDaysAhead;

    public BookingService(BarberRepository barbers, ServiceOfferingRepository services, WorkingHoursRepository hours,
                          AppointmentRepository appointments, CurrentTenant currentTenant, Clock clock,
                          BarberHubProperties props) {
        this.barbers = barbers;
        this.services = services;
        this.hours = hours;
        this.appointments = appointments;
        this.currentTenant = currentTenant;
        this.clock = clock;
        this.stepMinutes = props.booking().slotStepMinutes();
        this.maxDaysAhead = props.booking().maxDaysAhead();
    }

    /** Horas de inicio disponibles para un barbero, un servicio y un día. */
    @Transactional(readOnly = true)
    public List<LocalTime> availableSlots(Long barberId, Long serviceId, LocalDate date) {
        Tenant tenant = currentTenant.get();
        Barber barber = activeBarber(barberId);
        ServiceOffering service = activeService(serviceId);
        LocalDateTime now = LocalDateTime.now(clock.withZone(tenant.zone()));
        if (date.isBefore(now.toLocalDate()) || date.isAfter(now.toLocalDate().plusDays(maxDaysAhead))) {
            return List.of();
        }
        return computeSlots(barber.getId(), service.getDurationMinutes(), date, now);
    }

    /** Reserva desde la página pública: además limita cuántas citas activas puede acaparar un celular. */
    @Transactional
    public Appointment bookPublic(Long barberId, Long serviceId, LocalDateTime startAt, String customerName, String customerPhone) {
        LocalDateTime now = LocalDateTime.now(clock.withZone(currentTenant.get().zone()));
        if (appointments.countActiveByPhone(customerPhone.trim(), now) >= MAX_ACTIVE_PER_PHONE) {
            throw ApiException.conflict("Ya tienes " + MAX_ACTIVE_PER_PHONE + " citas pendientes. Cancela alguna o llama a la barbería.");
        }
        return book(barberId, serviceId, startAt, customerName, customerPhone);
    }

    @Transactional
    public Appointment book(Long barberId, Long serviceId, LocalDateTime startAt, String customerName, String customerPhone) {
        Tenant tenant = currentTenant.get();
        ServiceOffering service = activeService(serviceId);
        // Bloqueo por barbero: dos clientes reservando al mismo barbero se atienden en fila,
        // y el segundo ya ve la cita del primero. La restricción EXCLUDE de la BD es la red de seguridad.
        Barber barber = barbers.findByIdForUpdate(barberId)
                .filter(Barber::isActive)
                .orElseThrow(() -> ApiException.notFound("Barbero no encontrado."));

        LocalDateTime now = LocalDateTime.now(clock.withZone(tenant.zone()));
        LocalDate date = startAt.toLocalDate();
        if (!startAt.isAfter(now)) {
            throw ApiException.badRequest("No puedes reservar en el pasado.");
        }
        if (date.isAfter(now.toLocalDate().plusDays(maxDaysAhead))) {
            throw ApiException.badRequest("Solo se puede reservar con hasta " + maxDaysAhead + " días de anticipación.");
        }
        List<LocalTime> free = computeSlots(barber.getId(), service.getDurationMinutes(), date, now);
        if (!free.contains(startAt.toLocalTime().withSecond(0).withNano(0))) {
            throw ApiException.conflict("Ese horario no está disponible. Elige otro.");
        }

        Appointment appt = new Appointment(barber.getId(), service.getId(), newCode(), customerName.trim(),
                customerPhone.trim(), startAt, startAt.plusMinutes(service.getDurationMinutes()), service.getPriceCop());
        return appointments.saveAndFlush(appt);
    }

    @Transactional
    public Appointment cancelByCustomer(String code) {
        Appointment appt = appointments.findByCode(code.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("No encontramos esa cita."));
        if (appt.getStatus() != AppointmentStatus.BOOKED) {
            throw ApiException.conflict("Esta cita ya no se puede cancelar.");
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(currentTenant.get().zone()));
        if (!appt.getStartAt().isAfter(now)) {
            throw ApiException.conflict("La cita ya empezó o ya pasó.");
        }
        appt.setStatus(AppointmentStatus.CANCELLED);
        return appt;
    }

    private List<LocalTime> computeSlots(Long barberId, int durationMinutes, LocalDate date, LocalDateTime now) {
        List<WorkingHours> blocks = hours.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(
                barberId, (short) date.getDayOfWeek().getValue());
        if (blocks.isEmpty()) {
            return List.of();
        }
        List<Appointment> busy = appointments.findBlocking(barberId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        List<LocalTime> slots = new ArrayList<>();
        for (WorkingHours block : blocks) {
            LocalDateTime cursor = date.atTime(block.getStartTime());
            LocalDateTime blockEnd = date.atTime(block.getEndTime());
            while (!cursor.plusMinutes(durationMinutes).isAfter(blockEnd)) {
                LocalDateTime slotEnd = cursor.plusMinutes(durationMinutes);
                LocalDateTime slotStart = cursor;
                boolean overlaps = busy.stream().anyMatch(a ->
                        a.getStartAt().isBefore(slotEnd) && a.getEndAt().isAfter(slotStart));
                if (slotStart.isAfter(now) && !overlaps) {
                    slots.add(slotStart.toLocalTime());
                }
                cursor = cursor.plusMinutes(stepMinutes);
            }
        }
        return slots;
    }

    private Barber activeBarber(Long id) {
        return barbers.findById(id).filter(Barber::isActive)
                .orElseThrow(() -> ApiException.notFound("Barbero no encontrado."));
    }

    private ServiceOffering activeService(Long id) {
        return services.findById(id).filter(ServiceOffering::isActive)
                .orElseThrow(() -> ApiException.notFound("Servicio no encontrado."));
    }

    private static String newCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
