package com.attendance.api.mapper;

import com.attendance.api.dto.response.AttendanceResponse;
import com.attendance.api.dto.response.EmployeeResponse;
import com.attendance.api.dto.response.ShiftResponse;
import com.attendance.api.entity.AttendanceRecord;
import com.attendance.api.entity.Employee;
import com.attendance.api.entity.Shift;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Component
public class AttendanceMapper {

    public ShiftResponse toShiftResponse(Shift shift) {
        List<DayOfWeek> sortedDays = shift.getWorkDaySet().stream()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .toList();

        return new ShiftResponse(
                shift.getId(),
                shift.getName(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getGraceMinutes(),
                shift.getBreakMinutes(),
                shift.getExpectedWorkMinutes(),
                sortedDays,
                shift.isActive()
        );
    }

    public EmployeeResponse toEmployeeResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getDepartment(),
                toShiftResponse(employee.getShift()),
                employee.isActive()
        );
    }

    public AttendanceResponse toAttendanceResponse(AttendanceRecord record, ZoneId zoneId) {
        LocalTime localCheckIn = record.getCheckInAt() != null
                ? record.getCheckInAt().atZone(zoneId).toLocalTime().withNano(0)
                : null;
        LocalTime localCheckOut = record.getCheckOutAt() != null
                ? record.getCheckOutAt().atZone(zoneId).toLocalTime().withNano(0)
                : null;

        return new AttendanceResponse(
                record.getId(),
                record.getEmployee().getId(),
                record.getEmployee().getEmployeeCode(),
                record.getEmployee().getFullName(),
                record.getEmployee().getDepartment(),
                record.getShift().getId(),
                record.getShift().getName(),
                record.getWorkDate(),
                record.getCheckInAt(),
                record.getCheckOutAt(),
                localCheckIn,
                localCheckOut,
                record.getArrivalStatus(),
                record.getLateMinutes(),
                record.getWorkedMinutes(),
                record.getOvertimeMinutes(),
                record.isOpen(),
                record.getNotes(),
                record.getAdjustedBy() != null ? record.getAdjustedBy().getFullName() : null
        );
    }
}
