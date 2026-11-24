package com.attendance.api.service;

import com.attendance.api.dto.request.LoginRequest;
import com.attendance.api.dto.response.AuthResponse;
import com.attendance.api.dto.response.EmployeeResponse;
import com.attendance.api.entity.Employee;
import com.attendance.api.exception.ResourceNotFoundException;
import com.attendance.api.mapper.AttendanceMapper;
import com.attendance.api.repository.EmployeeRepository;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;
    private final AttendanceMapper mapper;

    public AuthService(
            AuthenticationManager authenticationManager,
            EmployeeRepository employeeRepository,
            JwtService jwtService,
            AttendanceMapper mapper
    ) {
        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password())
        );

        Employee employee = employeeRepository.findWithShiftByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        AuthenticatedUser principal = new AuthenticatedUser(employee);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                mapper.toEmployeeResponse(employee)
        );
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getCurrentUserProfile(Long employeeId) {
        Employee employee = employeeRepository.findWithShiftById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + employeeId));
        return mapper.toEmployeeResponse(employee);
    }
}
