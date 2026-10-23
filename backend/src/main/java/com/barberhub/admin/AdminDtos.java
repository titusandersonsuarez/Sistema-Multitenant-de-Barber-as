package com.barberhub.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    // ---- Barbería ----
    public record ShopSettings(Long id, String name, String slug, String phone, String address, String timezone) {
    }

    public record ShopUpdate(@NotBlank @Size(max = 120) String name, @Size(max = 30) String phone,
                             @Size(max = 200) String address) {
    }

    // ---- Servicios ----
    public record ServiceDto(Long id, String name, int durationMinutes, int priceCop, boolean active) {
    }

    public record ServiceUpsert(
            @NotBlank(message = "Escribe el nombre del servicio.") @Size(max = 100) String name,
            @Min(value = 5, message = "Mínimo 5 minutos.") @Max(value = 480, message = "Máximo 8 horas.") int durationMinutes,
            @Min(value = 0, message = "El precio no puede ser negativo.") int priceCop,
            Boolean active) {
    }

    // ---- Barberos ----
    public record BarberDto(Long id, String name, String bio, boolean active, boolean hasAccount) {
    }

    public record BarberUpsert(@NotBlank(message = "Escribe el nombre.") @Size(max = 100) String name,
                               @Size(max = 300) String bio, Boolean active) {
    }

    public record HoursBlock(@Min(1) @Max(7) int dayOfWeek, @NotNull LocalTime start, @NotNull LocalTime end) {
    }

    public record HoursUpdate(@NotNull @Size(max = 50) @Valid List<@NotNull HoursBlock> blocks) {
    }

    public record BarberAccountRequest(@NotBlank @Email @Size(max = 150) String email,
                                       @NotBlank @Size(min = 8, max = 72) String password) {
    }

    // ---- Agenda ----
    public record AgendaItem(Long id, String code, Long barberId, String barberName, Long serviceId, String serviceName,
                             String customerName, String customerPhone, LocalDateTime startAt, LocalDateTime endAt,
                             int priceCop, String status) {
    }

    public record StatusUpdate(@NotBlank @Pattern(regexp = "COMPLETED|CANCELLED|NO_SHOW|BOOKED") String status) {
    }

    public record WalkInRequest(@NotNull Long barberId, @NotNull Long serviceId, @NotNull LocalDateTime startAt,
                                @NotBlank @Size(max = 100) String customerName,
                                @NotBlank @Pattern(regexp = "^[+0-9 ]{7,20}$", message = "Celular inválido.") String customerPhone) {
    }

    // ---- Resumen ----
    public record Dashboard(LocalDate from, LocalDate to, long completed, long upcoming, long cancelled, long noShow,
                            long revenueCop, List<Breakdown> byService, List<Breakdown> byBarber) {
    }

    public record Breakdown(String name, long count, long revenueCop) {
    }
}
