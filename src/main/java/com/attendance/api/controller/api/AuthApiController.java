package com.attendance.api.controller.api;

import com.attendance.api.dto.request.LoginRequest;
import com.attendance.api.dto.response.AuthResponse;
import com.attendance.api.dto.response.EmployeeResponse;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth API")
public class AuthApiController {

    private final AuthService service;

    public AuthApiController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Genera un token JWT para el empleado")
    public AuthResponse authenticate(@Valid @RequestBody LoginRequest payload) {
        return service.login(payload);
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve los datos del empleado de la sesion JWT")
    public EmployeeResponse currentProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.getCurrentUserProfile(principal.getId());
    }
}
