package com.barberhub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "barberhub")
public record BarberHubProperties(Jwt jwt, SuperAdmin superadmin, Cors cors, Booking booking) {

    public record Jwt(String secret, int expirationHours) {
    }

    public record SuperAdmin(String email, String password) {
        public boolean enabled() {
            return email != null && !email.isBlank() && password != null && password.length() >= 8;
        }
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Booking(int slotStepMinutes, int maxDaysAhead) {
    }
}
