package com.barberhub.admin;

import com.barberhub.admin.AdminDtos.AgendaItem;
import com.barberhub.admin.AdminDtos.Breakdown;
import com.barberhub.admin.AdminDtos.Dashboard;
import com.barberhub.admin.AdminDtos.StatusUpdate;
import com.barberhub.admin.AdminDtos.WalkInRequest;
import com.barberhub.booking.Appointment;
import com.barberhub.booking.AppointmentRepository;
import com.barberhub.booking.AppointmentStatus;
import com.barberhub.booking.AppointmentViews;
import com.barberhub.booking.AppointmentViews.Names;
import com.barberhub.booking.BookingService;
import com.barberhub.common.ApiException;
import com.barberhub.security.AuthPrincipal;
import com.barberhub.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Agenda y resumen de ventas. El dueño ve todo; un barbero solo lo suyo. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('OWNER','BARBER')")
public class AdminAppointmentController {

    private final AppointmentRepository appointments;
    private final AppointmentViews views;
    private final BookingService booking;

    public AdminAppointmentController(AppointmentRepository appointments, AppointmentViews views, BookingService booking) {
        this.appointments = appointments;
        this.views = views;
        this.booking = booking;
    }

    @GetMapping("/appointments")
    @Transactional(readOnly = true)
    public List<AgendaItem> agenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) Long barberId) {
        LocalDate end = to == null ? from : to;
        if (end.isBefore(from) || end.isAfter(from.plusDays(31))) {
            throw ApiException.badRequest("Rango de fechas inválido (máximo 31 días).");
        }
        Long effectiveBarber = scopeBarber(barberId);
        List<Appointment> list = appointments.findAgenda(from.atStartOfDay(), end.plusDays(1).atStartOfDay(), effectiveBarber);
        return views.map(list, AdminAppointmentController::toItem);
    }

    /** Cita registrada desde el panel (cliente que llama o llega sin reserva). */
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaItem walkIn(@Valid @RequestBody WalkInRequest req) {
        Long barberId = scopeBarber(req.barberId());
        Appointment a = booking.book(barberId, req.serviceId(), req.startAt(), req.customerName(), req.customerPhone());
        return views.map(List.of(a), AdminAppointmentController::toItem).getFirst();
    }

    @PatchMapping("/appointments/{id}/status")
    @Transactional
    public AgendaItem updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate req) {
        Appointment a = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Cita no encontrada."));
        scopeBarber(a.getBarberId());
        AppointmentStatus next = AppointmentStatus.valueOf(req.status());
        if (a.getStatus() == AppointmentStatus.CANCELLED && next != AppointmentStatus.CANCELLED) {
            throw ApiException.conflict("Una cita cancelada no se puede reactivar; crea una nueva.");
        }
        a.setStatus(next);
        appointments.flush();
        return views.map(List.of(a), AdminAppointmentController::toItem).getFirst();
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Dashboard dashboard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || to.isAfter(from.plusDays(366))) {
            throw ApiException.badRequest("Rango de fechas inválido.");
        }
        Long barberScope = scopeBarber(null);
        List<Appointment> list = appointments.findInRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .filter(a -> barberScope == null || a.getBarberId().equals(barberScope))
                .toList();
        Names names = views.namesFor(list);
        List<Appointment> done = list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList();

        return new Dashboard(from, to,
                done.size(),
                count(list, AppointmentStatus.BOOKED),
                count(list, AppointmentStatus.CANCELLED),
                count(list, AppointmentStatus.NO_SHOW),
                done.stream().mapToLong(Appointment::getPriceCop).sum(),
                breakdown(done, a -> names.service(a.getServiceId())),
                breakdown(done, a -> names.barber(a.getBarberId())));
    }

    /**
     * Un barbero solo puede operar sobre su propia agenda; el dueño, sobre cualquiera.
     * Devuelve el barbero al que se limita la consulta (null = todos).
     */
    private static Long scopeBarber(Long requested) {
        AuthPrincipal user = CurrentUser.get();
        if (user.isOwner()) {
            return requested;
        }
        if (requested != null && !requested.equals(user.barberId())) {
            throw ApiException.forbidden("Solo puedes gestionar tu propia agenda.");
        }
        return user.barberId();
    }

    private static long count(List<Appointment> list, AppointmentStatus status) {
        return list.stream().filter(a -> a.getStatus() == status).count();
    }

    private static List<Breakdown> breakdown(List<Appointment> done, Function<Appointment, String> key) {
        Map<String, List<Appointment>> groups = done.stream().collect(Collectors.groupingBy(key));
        return groups.entrySet().stream()
                .map(e -> new Breakdown(e.getKey(), e.getValue().size(),
                        e.getValue().stream().mapToLong(Appointment::getPriceCop).sum()))
                .sorted(Comparator.comparingLong(Breakdown::revenueCop).reversed())
                .toList();
    }

    private static AgendaItem toItem(Appointment a, Names n) {
        return new AgendaItem(a.getId(), a.getCode(), a.getBarberId(), n.barber(a.getBarberId()), a.getServiceId(),
                n.service(a.getServiceId()), a.getCustomerName(), a.getCustomerPhone(), a.getStartAt(), a.getEndAt(),
                a.getPriceCop(), a.getStatus().name());
    }
}
