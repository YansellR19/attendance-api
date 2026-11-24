package com.attendance.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record EmployeeMonthlyStatsResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String department,
        String shiftName,
        int scheduledWorkDays,
        int attendedDays,
        int absentDays,
        double attendanceRate,
        int onTimeDays,
        int lateDays,
        int totalLateMinutes,
        int openDays,
        int totalWorkedMinutes,
        int totalOvertimeMinutes,
        double averageWorkedHours
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
