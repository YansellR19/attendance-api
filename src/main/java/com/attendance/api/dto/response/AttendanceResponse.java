package com.attendance.api.dto.response;

import com.attendance.api.entity.ArrivalStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String department,
        Long shiftId,
        String shiftName,
        LocalDate workDate,
        Instant checkInAt,
        Instant checkOutAt,
        LocalTime localCheckInTime,
        LocalTime localCheckOutTime,
        ArrivalStatus arrivalStatus,
        int lateMinutes,
        Integer workedMinutes,
        int overtimeMinutes,
        boolean open,
        String notes,
        String adjustedByName
) {
    @JsonIgnore
    public String formatWorkedDuration() {
        if (workedMinutes == null) {
            return "En curso";
        }
        return formatMinutes(workedMinutes);
    }

    @JsonIgnore
    public String formatOvertimeDuration() {
        return formatMinutes(overtimeMinutes);
    }

    public static String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        return String.format("%dh %02dm", hours, mins);
    }
}
