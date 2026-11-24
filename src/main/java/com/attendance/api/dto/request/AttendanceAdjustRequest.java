package com.attendance.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record AttendanceAdjustRequest(
        LocalTime checkInTime,
        LocalTime checkOutTime,

        @NotBlank(message = "Debe indicar un motivo para el ajuste manual")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String notes
) {
}
