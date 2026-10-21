package com.barberhub.booking;

import com.barberhub.catalog.Barber;
import com.barberhub.catalog.BarberRepository;
import com.barberhub.catalog.ServiceOffering;
import com.barberhub.catalog.ServiceOfferingRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/** Arma vistas de citas con nombres de barbero y servicio sin consultas N+1. */
@Component
public class AppointmentViews {

    private final BarberRepository barbers;
    private final ServiceOfferingRepository services;

    public AppointmentViews(BarberRepository barbers, ServiceOfferingRepository services) {
        this.barbers = barbers;
        this.services = services;
    }

    public record Names(Map<Long, String> barbers, Map<Long, String> services) {
        public String barber(Long id) { return barbers.getOrDefault(id, "—"); }
        public String service(Long id) { return services.getOrDefault(id, "—"); }
    }

    public Names namesFor(Collection<Appointment> list) {
        var barberIds = list.stream().map(Appointment::getBarberId).collect(Collectors.toSet());
        var serviceIds = list.stream().map(Appointment::getServiceId).collect(Collectors.toSet());
        Map<Long, String> b = barbers.findAllById(barberIds).stream()
                .collect(Collectors.toMap(Barber::getId, Barber::getName));
        Map<Long, String> s = services.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(ServiceOffering::getId, ServiceOffering::getName));
        return new Names(b, s);
    }

    public PublicDtos.AppointmentView publicView(Appointment a) {
        Names n = namesFor(List.of(a));
        return new PublicDtos.AppointmentView(a.getCode(), a.getStatus().name(), n.barber(a.getBarberId()),
                n.service(a.getServiceId()), a.getStartAt(), a.getEndAt(), a.getPriceCop(), a.getCustomerName());
    }

    public <T> List<T> map(List<Appointment> list, BiFunction<Appointment, Names, T> fn) {
        Names n = namesFor(list);
        return list.stream().map(a -> fn.apply(a, n)).toList();
    }
}
