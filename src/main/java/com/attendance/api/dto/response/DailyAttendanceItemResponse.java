package com.attendance.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalTime;

public record DailyAttendanceItemResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String department,
        String shiftName,
        String status,
        boolean open,
        Long attendanceId,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        int lateMinutes,
        Integer workedMinutes,
        int overtimeMinutes,
        String notes
) {
    @JsonIgnore
    public String formatWorkedDuration() {
        if ("ABSENT".equals(status)) {
            return "—";
        }
        if (workedMinutes == null) {
            return "En curso";
        }
        return AttendanceResponse.formatMinutes(workedMinutes);
    }

    @JsonIgnore
    public String formatOvertimeDuration() {
        return overtimeMinutes > 0 ? AttendanceResponse.formatMinutes(overtimeMinutes) : "—";
    }
}
