package com.attendance.api.controller.api;

import com.attendance.api.dto.request.AttendanceAdjustRequest;
import com.attendance.api.dto.request.AttendanceMarkRequest;
import com.attendance.api.dto.response.AttendanceResponse;
import com.attendance.api.dto.response.PageResponse;
import com.attendance.api.entity.ArrivalStatus;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendances")
@Tag(name = "Asistencias API")
public class AttendanceApiController {

    private final AttendanceService attendanceService;

    public AttendanceApiController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Marca la entrada diaria del empleado autenticado")
    public AttendanceResponse checkIn(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody(required = false) AttendanceMarkRequest request
    ) {
        String notes = request != null ? request.notes() : null;
        return attendanceService.checkIn(principal.getId(), notes);
    }

    @PostMapping("/check-out")
    @Operation(summary = "Marca la salida diaria y calcula horas trabajadas y horas extra")
    public AttendanceResponse checkOut(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody(required = false) AttendanceMarkRequest request
    ) {
        String notes = request != null ? request.notes() : null;
        return attendanceService.checkOut(principal.getId(), notes);
    }

    @GetMapping("/today")
    @Operation(summary = "Consulta el registro de asistencia de hoy del empleado autenticado")
    public ResponseEntity<AttendanceResponse> today(@AuthenticationPrincipal AuthenticatedUser principal) {
        return attendanceService.getTodayRecord(principal.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta el historial propio con filtros y paginacion")
    public PageResponse<AttendanceResponse> myHistory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ArrivalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Order.desc("workDate"), Sort.Order.desc("checkInAt"))
        );
        return attendanceService.getMyHistory(principal.getId(), from, to, status, pageable);
    }

    @GetMapping
    @Operation(summary = "Consulta el historial global de asistencias (solo ADMIN)")
    public PageResponse<AttendanceResponse> allHistory(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ArrivalStatus status,
            @RequestParam(required = false) Boolean openOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Order.desc("workDate"), Sort.Order.desc("checkInAt"))
        );
        return attendanceService.searchHistory(employeeId, department, from, to, status, openOnly, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un registro de asistencia por ID (solo ADMIN)")
    public AttendanceResponse getById(@PathVariable Long id) {
        return attendanceService.findById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Realiza ajuste manual de entrada/salida con nota de auditoria (solo ADMIN)")
    public AttendanceResponse adjust(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @Valid @RequestBody AttendanceAdjustRequest request
    ) {
        return attendanceService.adjustAttendance(id, admin.getId(), request);
    }
}
