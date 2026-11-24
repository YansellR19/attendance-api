package com.attendance.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.List;

public record MonthlyStatsResponse(
        int year,
        int month,
        LocalDate periodStart,
        LocalDate periodEnd,
        String departmentFilter,
        int totalEmployees,
        int totalScheduledEmployeeDays,
        int totalAttendedDays,
        int totalAbsentDays,
        double overallAttendanceRate,
        int totalLateCount,
        int totalWorkedMinutes,
        int totalOvertimeMinutes,
        List<EmployeeMonthlyStatsResponse> employees
) {
    @JsonIgnore
    public String formatTotalWorked() {
        return AttendanceResponse.formatMinutes(totalWorkedMinutes);
    }

    @JsonIgnore
    public String formatTotalOvertime() {
        return AttendanceResponse.formatMinutes(totalOvertimeMinutes);
    }
}
