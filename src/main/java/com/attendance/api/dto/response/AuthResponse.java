package com.attendance.api.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        EmployeeResponse user
) {
}
