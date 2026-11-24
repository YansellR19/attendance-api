package com.attendance.api.controller.api;

import com.attendance.api.dto.request.EmployeeCreateRequest;
import com.attendance.api.dto.request.EmployeeStatusRequest;
import com.attendance.api.dto.request.EmployeeUpdateRequest;
import com.attendance.api.dto.response.EmployeeResponse;
import com.attendance.api.dto.response.PageResponse;
import com.attendance.api.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Empleados API")
public class EmployeeApiController {

    private final EmployeeService employeeService;

    public EmployeeApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Lista empleados paginados con filtros opcionales (solo ADMIN)")
    public PageResponse<EmployeeResponse> listEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by("fullName").ascending());
        return employeeService.findAll(department, active, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un empleado por ID (solo ADMIN)")
    public EmployeeResponse getEmployee(@PathVariable Long id) {
        return employeeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un nuevo empleado (solo ADMIN)")
    public EmployeeResponse createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de un empleado (solo ADMIN)")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa o desactiva un empleado (solo ADMIN)")
    public EmployeeResponse changeStatus(@PathVariable Long id, @Valid @RequestBody EmployeeStatusRequest request) {
        return employeeService.updateStatus(id, request.active());
    }
}
