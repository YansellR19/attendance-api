package com.attendance.api.controller.web;

import com.attendance.api.dto.request.EmployeeCreateRequest;
import com.attendance.api.dto.request.ShiftRequest;
import com.attendance.api.entity.Role;
import com.attendance.api.service.EmployeeService;
import com.attendance.api.service.ShiftService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {

    private final EmployeeService employeeService;
    private final ShiftService shiftService;

    public AdminWebController(EmployeeService employeeService, ShiftService shiftService) {
        this.employeeService = employeeService;
        this.shiftService = shiftService;
    }

    @GetMapping("/employees")
    public String employeesPage(Model model) {
        model.addAttribute("employees", employeeService.findAllList());
        model.addAttribute("shifts", shiftService.findActive());
        model.addAttribute("roles", Role.values());
        return "admin/employees";
    }

    @PostMapping("/employees")
    public String createEmployee(
            @RequestParam String employeeCode,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam String department,
            @RequestParam Long shiftId,
            RedirectAttributes redirectAttributes
    ) {
        employeeService.create(new EmployeeCreateRequest(
                employeeCode, fullName, email, password, role, department, shiftId
        ));
        redirectAttributes.addFlashAttribute("successMessage", "Empleado '" + fullName + "' creado exitosamente.");
        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/{id}/status")
    public String toggleEmployeeStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            RedirectAttributes redirectAttributes
    ) {
        employeeService.updateStatus(id, active);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                active ? "Empleado activado correctamente." : "Empleado desactivado correctamente."
        );
        return "redirect:/admin/employees";
    }

    @GetMapping("/shifts")
    public String shiftsPage(Model model) {
        model.addAttribute("shifts", shiftService.findAll());
        model.addAttribute("allDays", DayOfWeek.values());
        return "admin/shifts";
    }

    @PostMapping("/shifts")
    public String createShift(
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam Integer graceMinutes,
            @RequestParam Integer breakMinutes,
            @RequestParam Integer expectedWorkMinutes,
            @RequestParam(required = false) List<DayOfWeek> workDays,
            RedirectAttributes redirectAttributes
    ) {
        Set<DayOfWeek> daySet = (workDays == null || workDays.isEmpty())
                ? EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                : EnumSet.copyOf(workDays);

        shiftService.create(new ShiftRequest(
                name, startTime, endTime, graceMinutes, breakMinutes, expectedWorkMinutes, daySet, true
        ));
        redirectAttributes.addFlashAttribute("successMessage", "Turno '" + name + "' creado exitosamente.");
        return "redirect:/admin/shifts";
    }
}
