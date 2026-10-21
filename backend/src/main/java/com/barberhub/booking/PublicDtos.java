package com.barberhub.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PublicDtos {

    private PublicDtos() {
    }

    public record ShopView(String name, String slug, String phone, String address) {
    }

    public record ServiceView(Long id, String name, int durationMinutes, int priceCop) {
    }

    public record BarberView(Long id, String name, String bio) {
    }

    /** slots en formato HH:mm, hora local de la barbería. */
    public record AvailabilityView(LocalDate date, List<String> slots) {
    }

    public record BookingRequest(
            @NotNull Long barberId,
            @NotNull Long serviceId,
            @NotNull LocalDateTime startAt,
            @NotBlank(message = "Escribe tu nombre.") @Size(max = 100) String customerName,
            @NotBlank(message = "Escribe tu celular.")
            @Pattern(regexp = "^[+0-9 ]{7,20}$", message = "Celular inválido.") String customerPhone) {
    }

    public record AppointmentView(String code, String status, String barberName, String serviceName,
                                  LocalDateTime startAt, LocalDateTime endAt, int priceCop, String customerName) {
    }
}
