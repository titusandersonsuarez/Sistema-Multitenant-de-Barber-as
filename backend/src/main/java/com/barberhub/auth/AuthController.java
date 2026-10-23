package com.barberhub.auth;

import com.barberhub.auth.AuthDtos.AuthResponse;
import com.barberhub.auth.AuthDtos.LoginRequest;
import com.barberhub.auth.AuthDtos.RegisterRequest;
import com.barberhub.auth.AuthDtos.SuperAdminLoginRequest;
import com.barberhub.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final RateLimiter limiter;

    public AuthController(AuthService auth, RateLimiter limiter) {
        this.auth = auth;
        this.limiter = limiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        limiter.check("register:" + RateLimiter.clientIp(http), 5, Duration.ofHours(1));
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String ip = RateLimiter.clientIp(http);
        limiter.check("login-ip:" + ip, 30, Duration.ofMinutes(5));
        limiter.check("login-acct:" + RateLimiter.norm(req.slug()) + "|" + RateLimiter.norm(req.email()), 10, Duration.ofMinutes(15));
        return auth.login(req);
    }

    @PostMapping("/superadmin")
    public AuthResponse superAdmin(@Valid @RequestBody SuperAdminLoginRequest req, HttpServletRequest http) {
        limiter.check("superadmin-ip:" + RateLimiter.clientIp(http), 5, Duration.ofMinutes(15));
        limiter.check("superadmin", 20, Duration.ofMinutes(15));
        return auth.superAdminLogin(req);
    }
}
