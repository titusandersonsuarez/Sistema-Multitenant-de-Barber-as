package com.barberhub.auth;

import com.barberhub.auth.AuthDtos.AuthResponse;
import com.barberhub.auth.AuthDtos.LoginRequest;
import com.barberhub.auth.AuthDtos.RegisterRequest;
import com.barberhub.auth.AuthDtos.SuperAdminLoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/superadmin")
    public AuthResponse superAdmin(@Valid @RequestBody SuperAdminLoginRequest req) {
        return auth.superAdminLogin(req);
    }
}
