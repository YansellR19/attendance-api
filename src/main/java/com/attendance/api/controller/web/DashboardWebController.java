package com.attendance.api.controller.web;

import com.attendance.api.dto.response.AttendanceResponse;
import com.attendance.api.dto.response.EmployeeMonthlyStatsResponse;
import com.attendance.api.dto.response.EmployeeResponse;
import com.attendance.api.dto.response.PageResponse;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.service.AttendanceService;
import com.attendance.api.service.AuthService;
import com.attendance.api.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
public class DashboardWebController {

    private final AuthService authService;
    private final AttendanceService attendanceService;
    private final ReportService reportService;
    private final Clock clock;
    private final ZoneId zoneId;

    public DashboardWebController(
            AuthService authService,
            AttendanceService attendanceService,
            ReportService reportService,
            Clock clock,
            ZoneId zoneId
    ) {
        this.authService = authService;
        this.attendanceService = attendanceService;
        this.reportService = reportService;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        LocalDate today = LocalDate.now(clock);
        EmployeeResponse profile = authService.getCurrentUserProfile(currentUser.getId());
        AttendanceResponse todayRecord = attendanceService.getTodayRecord(currentUser.getId()).orElse(null);
        EmployeeMonthlyStatsResponse myStats = reportService.getEmployeeMonthlyStats(
                currentUser.getId(), today.getYear(), today.getMonthValue()
        );
        PageResponse<AttendanceResponse> recentHistory = attendanceService.getMyHistory(
                currentUser.getId(),
                null,
                null,
                null,
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc("workDate"), Sort.Order.desc("checkInAt")))
        );

        model.addAttribute("profile", profile);
        model.addAttribute("todayDate", today);
        model.addAttribute("timezone", zoneId.getId());
        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("myMonthlyStats", myStats);
        model.addAttribute("recentRecords", recentHistory.content());

        if (currentUser.isAdmin()) {
            model.addAttribute("todaySummary", reportService.getDailyReport(today, null));
        }

        return "dashboard";
    }

    @PostMapping("/attendance/check-in")
    public String checkIn(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {
        AttendanceResponse record = attendanceService.checkIn(currentUser.getId(), notes);
        String msg = record.lateMinutes() > 0
                ? "Entrada registrada a las " + record.localCheckInTime() + " (tardanza de " + record.lateMinutes() + " min)."
                : "Entrada registrada puntualmente a las " + record.localCheckInTime() + ".";
        redirectAttributes.addFlashAttribute("successMessage", msg);
        return "redirect:/dashboard";
    }

    @PostMapping("/attendance/check-out")
    public String checkOut(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {
        AttendanceResponse record = attendanceService.checkOut(currentUser.getId(), notes);
        String msg = "Salida registrada a las " + record.localCheckOutTime()
                + ". Jornada neta: " + record.formatWorkedDuration()
                + (record.overtimeMinutes() > 0 ? " | Extras: " + record.formatOvertimeDuration() : "") + ".";
        redirectAttributes.addFlashAttribute("successMessage", msg);
        return "redirect:/dashboard";
    }
}
