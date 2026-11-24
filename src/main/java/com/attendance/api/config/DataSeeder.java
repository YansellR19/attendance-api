package com.attendance.api.config;

import com.attendance.api.entity.ArrivalStatus;
import com.attendance.api.entity.AttendanceRecord;
import com.attendance.api.entity.Employee;
import com.attendance.api.entity.Role;
import com.attendance.api.entity.Shift;
import com.attendance.api.repository.AttendanceRecordRepository;
import com.attendance.api.repository.EmployeeRepository;
import com.attendance.api.repository.ShiftRepository;
import com.attendance.api.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ZoneId zoneId;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.demo-attendance:true}")
    private boolean seedDemoAttendance;

    public DataSeeder(
            ShiftRepository shiftRepository,
            EmployeeRepository employeeRepository,
            AttendanceRecordRepository attendanceRepository,
            AttendanceService attendanceService,
            PasswordEncoder passwordEncoder,
            Clock clock,
            ZoneId zoneId
    ) {
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        List<Shift> shifts = shiftRepository.findAllByOrderByIdAsc();
        if (shifts.isEmpty()) {
            log.warn("No se encontraron turnos en la base de datos; omitiendo seed de usuarios.");
            return;
        }

        Shift standardShift = shifts.get(0);
        Shift earlyShift = shifts.size() > 1 ? shifts.get(1) : standardShift;
        Shift partTimeShift = shifts.size() > 2 ? shifts.get(2) : standardShift;

        Employee admin = ensureEmployee(
                "EMP-001", "Administrador General", "admin@asistencia.com",
                "Admin123!", Role.ADMIN, "Direccion", standardShift
        );
        Employee ana = ensureEmployee(
                "EMP-002", "Ana Garcia", "ana.garcia@asistencia.com",
                "Empleado123!", Role.EMPLOYEE, "Ingenieria", standardShift
        );
        Employee carlos = ensureEmployee(
                "EMP-003", "Carlos Lopez", "carlos.lopez@asistencia.com",
                "Empleado123!", Role.EMPLOYEE, "Operaciones", earlyShift
        );
        Employee maria = ensureEmployee(
                "EMP-004", "Maria Perez", "maria.perez@asistencia.com",
                "Empleado123!", Role.EMPLOYEE, "Ventas", partTimeShift
        );

        if (seedDemoAttendance && attendanceRepository.count() == 0) {
            seedRecentMonthAttendance(List.of(admin, ana, carlos, maria));
        }
    }

    private Employee ensureEmployee(
            String code,
            String fullName,
            String email,
            String rawPassword,
            Role role,
            String department,
            Shift shift
    ) {
        return employeeRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Employee emp = new Employee();
            emp.setEmployeeCode(code);
            emp.setFullName(fullName);
            emp.setEmail(email);
            emp.setPasswordHash(passwordEncoder.encode(rawPassword));
            emp.setRole(role);
            emp.setDepartment(department);
            emp.setShift(shift);
            emp.setActive(true);
            Employee saved = employeeRepository.save(emp);
            log.info("Usuario inicial creado: {} ({})", email, role);
            return saved;
        });
    }

    private void seedRecentMonthAttendance(List<Employee> employees) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate cursor = startOfMonth;
        int dayIndex = 0;

        while (cursor.isBefore(today)) {
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                Shift shift = emp.getShift();
                if (!shift.isWorkDay(cursor.getDayOfWeek())) {
                    continue;
                }
                // Dejar alguna ausencia realista para que los reportes muestren variedad
                if (i == 2 && dayIndex % 6 == 3) {
                    continue;
                }

                int arrivalOffsetMinutes = (i == 1 && dayIndex % 4 == 1) ? shift.getGraceMinutes() + 18
                        : (i == 2 && dayIndex % 5 == 2) ? shift.getGraceMinutes() + 9
                        : (dayIndex % 3) - 2;

                int extraMinutes = (i == 1 && dayIndex % 3 == 0) ? 75
                        : (i == 0 && dayIndex % 4 == 0) ? 45
                        : 10;

                LocalTime inLocal = shift.getStartTime().plusMinutes(Math.max(-15, arrivalOffsetMinutes));
                LocalTime outLocal = shift.getEndTime().plusMinutes(extraMinutes);

                Instant inInstant = cursor.atTime(inLocal).atZone(zoneId).toInstant();
                Instant outInstant = cursor.atTime(outLocal).atZone(zoneId).toInstant();

                AttendanceService.ArrivalEvaluation eval = attendanceService.evaluateArrival(shift, inLocal);
                AttendanceService.WorkDuration duration = attendanceService.calculateWorkDuration(inInstant, outInstant, shift);

                AttendanceRecord record = new AttendanceRecord();
                record.setEmployee(emp);
                record.setShift(shift);
                record.setWorkDate(cursor);
                record.setCheckInAt(inInstant);
                record.setCheckOutAt(outInstant);
                record.setArrivalStatus(eval.status() != null ? eval.status() : ArrivalStatus.ON_TIME);
                record.setLateMinutes(eval.lateMinutes());
                record.setWorkedMinutes(duration.workedMinutes());
                record.setOvertimeMinutes(duration.overtimeMinutes());
                if (eval.status() == ArrivalStatus.LATE) {
                    record.setNotes("Trafico en hora pico");
                }
                attendanceRepository.save(record);
            }
            cursor = cursor.plusDays(1);
            dayIndex++;
        }
        log.info("Registros de asistencia de demostracion generados para el mes actual.");
    }
}
