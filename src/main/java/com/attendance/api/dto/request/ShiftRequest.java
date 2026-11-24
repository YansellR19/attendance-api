package com.attendance.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record ShiftRequest(
        @NotBlank(message = "El nombre del turno es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime endTime,

        @NotNull(message = "La tolerancia es obligatoria")
        @Min(value = 0, message = "La tolerancia no puede ser negativa")
        @Max(value = 180, message = "La tolerancia no puede superar 180 minutos")
        Integer graceMinutes,

        @NotNull(message = "El tiempo de descanso es obligatorio")
        @Min(value = 0, message = "El descanso no puede ser negativo")
        @Max(value = 240, message = "El descanso no puede superar 240 minutos")
        Integer breakMinutes,

        @NotNull(message = "Los minutos esperados son obligatorios")
        @Min(value = 60, message = "La jornada esperada debe ser al menos de 60 minutos")
        @Max(value = 960, message = "La jornada esperada no puede superar 960 minutos")
        Integer expectedWorkMinutes,

        @NotEmpty(message = "Debe seleccionar al menos un dia laboral")
        Set<DayOfWeek> workDays,

        Boolean active
) {
}
