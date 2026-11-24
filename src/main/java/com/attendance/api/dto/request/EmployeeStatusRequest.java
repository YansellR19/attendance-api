package com.attendance.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
}
