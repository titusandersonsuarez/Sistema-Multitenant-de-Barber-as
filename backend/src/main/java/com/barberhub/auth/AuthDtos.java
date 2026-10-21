package com.barberhub.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Escribe el nombre de la barbería.") @Size(max = 120) String shopName,
            @NotBlank(message = "Elige la dirección de tu página.")
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Solo minúsculas, números y guiones.")
            @Size(min = 3, max = 40, message = "Entre 3 y 40 caracteres.") String slug,
            @NotBlank(message = "Escribe tu nombre.") @Size(max = 120) String ownerName,
            @NotBlank @Email(message = "Correo inválido.") @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres.") String password,
            @Size(max = 30) String phone) {
    }

    public record LoginRequest(
            @NotBlank(message = "Indica la barbería.") String slug,
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record SuperAdminLoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, long expiresIn, UserView user) {
    }

    public record UserView(Long id, String name, String role, Long tenantId, String tenantSlug, String tenantName,
                           Long barberId) {
    }
}
