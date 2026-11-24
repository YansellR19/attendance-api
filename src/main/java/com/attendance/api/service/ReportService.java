package com.attendance.api.service;

import com.attendance.api.dto.response.DailyAttendanceItemResponse;
import com.attendance.api.dto.response.DailyReportResponse;
import com.attendance.api.dto.response.EmployeeMonthlyStatsResponse;
import com.attendance.api.dto.response.MonthlyStatsResponse;
import com.attendance.api.entity.ArrivalStatus;
import com.attendance.api.entity.AttendanceRecord;
import com.attendance.api.entity.Employee;
import com.attendance.api.entity.Shift;
import com.attendance.api.exception.BusinessRuleException;
import com.attendance.api.exception.ResourceNotFoundException;
import com.attendance.api.repository.AttendanceRecordRepository;
import com.attendance.api.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final Clock clock;
    private final ZoneId zoneId;

    public ReportService(
            EmployeeRepository employeeRepository,
            AttendanceRecordRepository attendanceRepository,
            Clock clock,
            ZoneId zoneId
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(LocalDate requestedDate, String department) {
        LocalDate date = requestedDate != null ? requestedDate : LocalDate.now(clock);
        String cleanDept = (department != null && !department.isBlank()) ? department.trim() : null;

        List<Employee> activeEmployees = cleanDept != null
                ? employeeRepository.findByActiveTrueAndDepartmentIgnoreCaseOrderByFullNameAsc(cleanDept)
                : employeeRepository.findByActiveTrueOrderByFullNameAsc();

        Map<Long, AttendanceRecord> recordsByEmployee = attendanceRepository.findByWorkDateOrderByCheckInAtAsc(date)
                .stream()
                .collect(Collectors.toMap(r -> r.getEmployee().getId(), Function.identity(), (a, b) -> a));

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DailyAttendanceItemResponse> items = new ArrayList<>();

        int scheduledEmployees = 0;
        int presentCount = 0;
        int onTimeCount = 0;
        int lateCount = 0;
        int absentCount = 0;
        int openCount = 0;
        int totalWorkedMinutes = 0;
        int totalOvertimeMinutes = 0;

        for (Employee employee : activeEmployees) {
            AttendanceRecord record = recordsByEmployee.get(employee.getId());
            boolean scheduled = employee.getShift().isWorkDay(dayOfWeek);

            if (!scheduled && record == null) {
                continue;
            }
            if (scheduled) {
                scheduledEmployees++;
            }

            if (record != null) {
                presentCount++;
                if (record.getArrivalStatus() == ArrivalStatus.ON_TIME) {
                    onTimeCount++;
                } else {
                    lateCount++;
                }
                if (record.isOpen()) {
                    openCount++;
                }
                if (record.getWorkedMinutes() != null) {
                    totalWorkedMinutes += record.getWorkedMinutes();
                }
                totalOvertimeMinutes += record.getOvertimeMinutes();

                LocalTime localIn = record.getCheckInAt().atZone(zoneId).toLocalTime().withNano(0);
                LocalTime localOut = record.getCheckOutAt() != null
                        ? record.getCheckOutAt().atZone(zoneId).toLocalTime().withNano(0)
                        : null;

                items.add(new DailyAttendanceItemResponse(
                        employee.getId(),
                        employee.getEmployeeCode(),
                        employee.getFullName(),
                        employee.getDepartment(),
                        record.getShift().getName(),
                        record.getArrivalStatus().name(),
                        record.isOpen(),
                        record.getId(),
                        localIn,
                        localOut,
                        record.getLateMinutes(),
                        record.getWorkedMinutes(),
                        record.getOvertimeMinutes(),
                        record.getNotes()
                ));
            } else {
                absentCount++;
                items.add(new DailyAttendanceItemResponse(
                        employee.getId(),
                        employee.getEmployeeCode(),
                        employee.getFullName(),
                        employee.getDepartment(),
                        employee.getShift().getName(),
                        "ABSENT",
                        false,
                        null,
                        null,
                        null,
                        0,
                        null,
                        0,
                        null
                ));
            }
        }

        return new DailyReportResponse(
                date,
                cleanDept,
                scheduledEmployees,
                presentCount,
                onTimeCount,
                lateCount,
                absentCount,
                openCount,
                totalWorkedMinutes,
                totalOvertimeMinutes,
                items
        );
    }

    @Transactional(readOnly = true)
    public MonthlyStatsResponse getMonthlyStats(int year, int month, String department) {
        validateYearMonth(year, month);
        String cleanDept = (department != null && !department.isBlank()) ? department.trim() : null;

        YearMonth ym = YearMonth.of(year, month);
        LocalDate periodStart = ym.atDay(1);
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveEnd = resolveEffectiveEnd(ym, today);

        List<Employee> employees = cleanDept != null
                ? employeeRepository.findByActiveTrueAndDepartmentIgnoreCaseOrderByFullNameAsc(cleanDept)
                : employeeRepository.findByActiveTrueOrderByFullNameAsc();

        Map<Long, List<AttendanceRecord>> recordsByEmployee = attendanceRepository
                .findByWorkDateBetweenOrderByWorkDateAsc(periodStart, ym.atEndOfMonth())
                .stream()
                .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));

        List<EmployeeMonthlyStatsResponse> employeeStats = new ArrayList<>();
        int totalScheduledDays = 0;
        int totalAttendedDays = 0;
        int totalAbsentDays = 0;
        int totalLateCount = 0;
        int totalWorkedMinutes = 0;
        int totalOvertimeMinutes = 0;

        for (Employee employee : employees) {
            List<AttendanceRecord> empRecords = recordsByEmployee.getOrDefault(employee.getId(), List.of());
            EmployeeMonthlyStatsResponse stats = computeEmployeeMonthlyStats(employee, periodStart, effectiveEnd, empRecords);
            employeeStats.add(stats);

            totalScheduledDays += stats.scheduledWorkDays();
            totalAttendedDays += stats.attendedDays();
            totalAbsentDays += stats.absentDays();
            totalLateCount += stats.lateDays();
            totalWorkedMinutes += stats.totalWorkedMinutes();
            totalOvertimeMinutes += stats.totalOvertimeMinutes();
        }

        double overallRate = totalScheduledDays > 0
                ? roundTwoDecimals((Math.min(totalAttendedDays, totalScheduledDays) * 100.0) / totalScheduledDays)
                : 0.0;

        return new MonthlyStatsResponse(
                year,
                month,
                periodStart,
                effectiveEnd.isBefore(periodStart) ? periodStart : effectiveEnd,
                cleanDept,
                employees.size(),
                totalScheduledDays,
                totalAttendedDays,
                totalAbsentDays,
                overallRate,
                totalLateCount,
                totalWorkedMinutes,
                totalOvertimeMinutes,
                employeeStats
        );
    }

    @Transactional(readOnly = true)
    public EmployeeMonthlyStatsResponse getEmployeeMonthlyStats(Long employeeId, int year, int month) {
        validateYearMonth(year, month);
        Employee employee = employeeRepository.findWithShiftById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + employeeId));

        YearMonth ym = YearMonth.of(year, month);
        LocalDate periodStart = ym.atDay(1);
        LocalDate effectiveEnd = resolveEffectiveEnd(ym, LocalDate.now(clock));

        List<AttendanceRecord> records = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, periodStart, ym.atEndOfMonth());

        return computeEmployeeMonthlyStats(employee, periodStart, effectiveEnd, records);
    }

    private EmployeeMonthlyStatsResponse computeEmployeeMonthlyStats(
            Employee employee,
            LocalDate periodStart,
            LocalDate effectiveEnd,
            List<AttendanceRecord> records
    ) {
        Shift shift = employee.getShift();
        Set<DayOfWeek> workDays = shift.getWorkDaySet();
        int scheduledWorkDays = countScheduledDays(periodStart, effectiveEnd, workDays);

        int attendedDays = records.size();
        int attendedOnScheduledDays = (int) records.stream()
                .filter(r -> !r.getWorkDate().isAfter(effectiveEnd) && workDays.contains(r.getWorkDate().getDayOfWeek()))
                .count();
        int absentDays = Math.max(0, scheduledWorkDays - attendedOnScheduledDays);

        double attendanceRate = scheduledWorkDays > 0
                ? roundTwoDecimals((attendedOnScheduledDays * 100.0) / scheduledWorkDays)
                : 0.0;

        int onTimeDays = 0;
        int lateDays = 0;
        int totalLateMinutes = 0;
        int openDays = 0;
        int closedDays = 0;
        int totalWorkedMinutes = 0;
        int totalOvertimeMinutes = 0;

        for (AttendanceRecord r : records) {
            if (r.getArrivalStatus() == ArrivalStatus.ON_TIME) {
                onTimeDays++;
            } else {
                lateDays++;
                totalLateMinutes += r.getLateMinutes();
            }
            if (r.isOpen()) {
                openDays++;
            } else if (r.getWorkedMinutes() != null) {
                closedDays++;
                totalWorkedMinutes += r.getWorkedMinutes();
            }
            totalOvertimeMinutes += r.getOvertimeMinutes();
        }

        double averageWorkedHours = closedDays > 0
                ? roundTwoDecimals((totalWorkedMinutes / 60.0) / closedDays)
                : 0.0;

        return new EmployeeMonthlyStatsResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getDepartment(),
                shift.getName(),
                scheduledWorkDays,
                attendedDays,
                absentDays,
                attendanceRate,
                onTimeDays,
                lateDays,
                totalLateMinutes,
                openDays,
                totalWorkedMinutes,
                totalOvertimeMinutes,
                averageWorkedHours
        );
    }

    private int countScheduledDays(LocalDate start, LocalDate end, Set<DayOfWeek> workDays) {
        if (end.isBefore(start) || workDays.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (workDays.contains(d.getDayOfWeek())) {
                count++;
            }
        }
        return count;
    }

    private LocalDate resolveEffectiveEnd(YearMonth ym, LocalDate today) {
        YearMonth currentYm = YearMonth.from(today);
        if (ym.equals(currentYm)) {
            return today;
        }
        if (ym.isAfter(currentYm)) {
            return ym.atDay(1).minusDays(1);
        }
        return ym.atEndOfMonth();
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new BusinessRuleException("El ano debe estar entre 2000 y 2100.");
        }
        if (month < 1 || month > 12) {
            throw new BusinessRuleException("El mes debe estar entre 1 y 12.");
        }
    }

    private double roundTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
