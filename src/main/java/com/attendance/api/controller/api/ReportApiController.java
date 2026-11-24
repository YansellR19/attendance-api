package com.attendance.api.controller.api;

import com.attendance.api.dto.response.DailyReportResponse;
import com.attendance.api.dto.response.EmployeeMonthlyStatsResponse;
import com.attendance.api.dto.response.MonthlyStatsResponse;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes y Estadisticas API")
public class ReportApiController {

    private final ReportService reportService;
    private final Clock clock;

    public ReportApiController(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @GetMapping("/daily")
    @Operation(summary = "Genera el reporte diario de asistencia (solo ADMIN)")
    public DailyReportResponse dailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department
    ) {
        return reportService.getDailyReport(date, department);
    }

    @GetMapping("/monthly")
    @Operation(summary = "Genera estadisticas mensuales globales y por empleado (solo ADMIN)")
    public MonthlyStatsResponse monthlyStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String department
    ) {
        LocalDate today = LocalDate.now(clock);
        int targetYear = year != null ? year : today.getYear();
        int targetMonth = month != null ? month : today.getMonthValue();
        return reportService.getMonthlyStats(targetYear, targetMonth, department);
    }

    @GetMapping("/monthly/me")
    @Operation(summary = "Genera estadisticas mensuales del empleado autenticado")
    public EmployeeMonthlyStatsResponse myMonthlyStats(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate today = LocalDate.now(clock);
        int targetYear = year != null ? year : today.getYear();
        int targetMonth = month != null ? month : today.getMonthValue();
        return reportService.getEmployeeMonthlyStats(principal.getId(), targetYear, targetMonth);
    }
}
