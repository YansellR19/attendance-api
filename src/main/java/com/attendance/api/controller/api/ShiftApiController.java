package com.attendance.api.controller.api;

import com.attendance.api.dto.request.ShiftRequest;
import com.attendance.api.dto.response.ShiftResponse;
import com.attendance.api.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts")
@Tag(name = "Turnos API")
public class ShiftApiController {

    private final ShiftService shiftService;

    public ShiftApiController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los turnos registrados")
    public List<ShiftResponse> listShifts() {
        return shiftService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta el detalle de un turno")
    public ShiftResponse getShift(@PathVariable Long id) {
        return shiftService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un nuevo turno (solo ADMIN)")
    public ShiftResponse createShift(@Valid @RequestBody ShiftRequest request) {
        return shiftService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un turno existente (solo ADMIN)")
    public ShiftResponse updateShift(@PathVariable Long id, @Valid @RequestBody ShiftRequest request) {
        return shiftService.update(id, request);
    }
}
