package com.barberhub.booking;

import com.barberhub.booking.PublicDtos.AppointmentView;
import com.barberhub.booking.PublicDtos.AvailabilityView;
import com.barberhub.booking.PublicDtos.BarberView;
import com.barberhub.booking.PublicDtos.BookingRequest;
import com.barberhub.booking.PublicDtos.ServiceView;
import com.barberhub.booking.PublicDtos.ShopView;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.catalog.ServiceOfferingRepository;
import com.barberhub.common.ApiException;
import com.barberhub.tenancy.CurrentTenant;
import com.barberhub.tenancy.Tenant;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API pública de cada barbería. El {@code slug} de la URL ya fue resuelto a un tenant
 * por {@code TenantResolutionFilter}, así que aquí todo está aislado a esa barbería.
 */
@RestController
@RequestMapping("/api/public/{slug}")
public class PublicController {

    private final CurrentTenant currentTenant;
    private final ServiceOfferingRepository services;
    private final BarberRepository barbers;
    private final BookingService booking;
    private final AppointmentRepository appointments;
    private final AppointmentViews views;

    public PublicController(CurrentTenant currentTenant, ServiceOfferingRepository services, BarberRepository barbers,
                            BookingService booking, AppointmentRepository appointments, AppointmentViews views) {
        this.currentTenant = currentTenant;
        this.services = services;
        this.barbers = barbers;
        this.booking = booking;
        this.appointments = appointments;
        this.views = views;
    }

    @GetMapping
    public ShopView shop(@PathVariable String slug) {
        Tenant t = currentTenant.get();
        return new ShopView(t.getName(), t.getSlug(), t.getPhone(), t.getAddress());
    }

    @GetMapping("/services")
    @Transactional(readOnly = true)
    public List<ServiceView> services(@PathVariable String slug) {
        return services.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new ServiceView(s.getId(), s.getName(), s.getDurationMinutes(), s.getPriceCop()))
                .toList();
    }

    @GetMapping("/barbers")
    @Transactional(readOnly = true)
    public List<BarberView> barbers(@PathVariable String slug) {
        return barbers.findByActiveTrueOrderByNameAsc().stream()
                .map(b -> new BarberView(b.getId(), b.getName(), b.getBio()))
                .toList();
    }

    @GetMapping("/availability")
    public AvailabilityView availability(@PathVariable String slug,
                                         @RequestParam Long barberId,
                                         @RequestParam Long serviceId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var slots = booking.availableSlots(barberId, serviceId, date).stream()
                .map(t -> t.toString().substring(0, 5))
                .toList();
        return new AvailabilityView(date, slots);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentView book(@PathVariable String slug, @Valid @RequestBody BookingRequest req) {
        Appointment a = booking.book(req.barberId(), req.serviceId(), req.startAt(), req.customerName(), req.customerPhone());
        return views.publicView(a);
    }

    @GetMapping("/appointments/{code}")
    @Transactional(readOnly = true)
    public AppointmentView find(@PathVariable String slug, @PathVariable String code) {
        return appointments.findByCode(code.toUpperCase())
                .map(views::publicView)
                .orElseThrow(() -> ApiException.notFound("No encontramos esa cita."));
    }

    @PostMapping("/appointments/{code}/cancel")
    public AppointmentView cancel(@PathVariable String slug, @PathVariable String code) {
        return views.publicView(booking.cancelByCustomer(code));
    }
}
