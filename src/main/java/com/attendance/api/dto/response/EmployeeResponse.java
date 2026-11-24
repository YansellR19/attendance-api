package com.attendance.api.dto.response;

import com.attendance.api.entity.Role;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        Role role,
        String department,
        ShiftResponse shift,
        boolean active
) {
}
