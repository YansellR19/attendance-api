package com.attendance.api.controller.web;

import com.attendance.api.dto.request.AttendanceAdjustRequest;
import com.attendance.api.dto.response.AttendanceResponse;
import com.attendance.api.dto.response.PageResponse;
import com.attendance.api.entity.ArrivalStatus;
import com.attendance.api.security.AuthenticatedUser;
import com.attendance.api.service.AttendanceService;
import com.attendance.api.service.EmployeeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/attendance")
public class AttendanceWebController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;

    public AttendanceWebController(AttendanceService attendanceService, EmployeeService employeeService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
    }

    @GetMapping("/history")
    public String history(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ArrivalStatus status,
            @RequestParam(required = false) Boolean openOnly,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                15,
                Sort.by(Sort.Order.desc("workDate"), Sort.Order.desc("checkInAt"))
        );

        PageResponse<AttendanceResponse> historyPage;
        if (currentUser.isAdmin()) {
            historyPage = attendanceService.searchHistory(employeeId, department, from, to, status, openOnly, pageable);
            model.addAttribute("employees", employeeService.findAllList());
            model.addAttribute("departments", employeeService.findDistinctDepartments());
        } else {
            historyPage = attendanceService.getMyHistory(currentUser.getId(), from, to, status, pageable);
        }

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("openOnly", Boolean.TRUE.equals(openOnly));
        return "attendance/history";
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public String adjustRecord(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime checkInTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime checkOutTime,
            @RequestParam String notes,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        attendanceService.adjustAttendance(id, admin.getId(), new AttendanceAdjustRequest(checkInTime, checkOutTime, notes));
        redirectAttributes.addFlashAttribute("successMessage", "Registro #" + id + " ajustado correctamente.");
        if (returnTo != null && returnTo.startsWith("/")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/attendance/history";
    }
}
