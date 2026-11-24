package com.attendance.api.controller.web;

import com.attendance.api.dto.response.DailyReportResponse;
import com.attendance.api.dto.response.EmployeeMonthlyStatsResponse;
import com.attendance.api.dto.response.MonthlyStatsResponse;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.service.EmployeeService;
import com.attendance.api.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
public class ReportWebController {

    private final ReportService reportService;
    private final EmployeeService employeeService;
    private final Clock clock;

    public ReportWebController(ReportService reportService, EmployeeService employeeService, Clock clock) {
        this.reportService = reportService;
        this.employeeService = employeeService;
        this.clock = clock;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public String dailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            Model model
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now(clock);
        DailyReportResponse report = reportService.getDailyReport(targetDate, department);

        model.addAttribute("report", report);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("departments", employeeService.findDistinctDepartments());
        return "reports/daily";
    }

    @GetMapping("/monthly")
    public String monthlyReport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String department,
            Model model
    ) {
        LocalDate today = LocalDate.now(clock);
        int targetYear = year != null ? year : today.getYear();
        int targetMonth = month != null ? month : today.getMonthValue();

        model.addAttribute("selectedYear", targetYear);
        model.addAttribute("selectedMonth", targetMonth);

        if (currentUser.isAdmin()) {
            MonthlyStatsResponse monthly = reportService.getMonthlyStats(targetYear, targetMonth, department);
            model.addAttribute("monthly", monthly);
            model.addAttribute("selectedDepartment", department);
            model.addAttribute("departments", employeeService.findDistinctDepartments());
        } else {
            EmployeeMonthlyStatsResponse myStats = reportService.getEmployeeMonthlyStats(
                    currentUser.getId(), targetYear, targetMonth
            );
            model.addAttribute("myStats", myStats);
        }

        return "reports/monthly";
    }
}
