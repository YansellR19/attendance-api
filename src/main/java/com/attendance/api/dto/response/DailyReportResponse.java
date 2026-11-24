package com.attendance.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        LocalDate date,
        String departmentFilter,
        int scheduledEmployees,
        int presentCount,
        int onTimeCount,
        int lateCount,
        int absentCount,
        int openCount,
        int totalWorkedMinutes,
        int totalOvertimeMinutes,
        List<DailyAttendanceItemResponse> details
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
