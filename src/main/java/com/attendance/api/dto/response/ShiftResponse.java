package com.attendance.api.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record ShiftResponse(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        int graceMinutes,
        int breakMinutes,
        int expectedWorkMinutes,
        List<DayOfWeek> workDays,
        boolean active
) {
}
