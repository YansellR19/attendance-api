package com.attendance.api.dto.request;

import jakarta.validation.constraints.Size;

public record AttendanceMarkRequest(
        @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
        String notes
) {
}
