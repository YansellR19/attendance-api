package com.attendance.api.controller.api;

import com.attendance.api.repository.AttendanceRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendanceRecordRepository attendanceRepository;

    @BeforeEach
    void cleanAttendance() {
        attendanceRepository.deleteAll();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    void unauthenticatedApiRequest_returns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/attendances/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void employeeRole_cannotAccessAdminEndpoints() throws Exception {
        String empToken = loginAndGetToken("ana.garcia@asistencia.com", "Empleado123!");

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/reports/daily")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullAttendanceFlow_checkInCheckOutHistoryReportsAndAdminAdjustment() throws Exception {
        String empToken = loginAndGetToken("ana.garcia@asistencia.com", "Empleado123!");
        String adminToken = loginAndGetToken("admin@asistencia.com", "Admin123!");

        // 1. Check-in exitoso
        MvcResult checkInRes = mockMvc.perform(post("/api/v1/attendances/check-in")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Ingreso desde oficina\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").value("EMP-002"))
                .andExpect(jsonPath("$.open").value(true))
                .andReturn();

        long recordId = objectMapper.readTree(checkInRes.getResponse().getContentAsString()).get("id").asLong();
        assertThat(recordId).isPositive();

        // 2. Doble check-in el mismo dia debe fallar (422)
        mockMvc.perform(post("/api/v1/attendances/check-in")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        // 3. Check-out exitoso
        mockMvc.perform(post("/api/v1/attendances/check-out")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Fin de jornada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.workedMinutes").isNumber());

        // 4. Historial propio
        mockMvc.perform(get("/api/v1/attendances/me")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // 5. Ajuste manual por ADMIN para simular jornada con horas extra (09:00 a 19:30 -> 570m, 90m extras)
        mockMvc.perform(patch("/api/v1/attendances/" + recordId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checkInTime": "09:00:00",
                                  "checkOutTime": "19:30:00",
                                  "notes": "Correccion validada por jefatura"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrivalStatus").value("ON_TIME"))
                .andExpect(jsonPath("$.workedMinutes").value(570))
                .andExpect(jsonPath("$.overtimeMinutes").value(90))
                .andExpect(jsonPath("$.adjustedByName").value("Administrador General"));

        // 6. Reporte diario (ADMIN)
        mockMvc.perform(get("/api/v1/reports/daily")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.totalOvertimeMinutes").value(90));

        // 7. Estadisticas mensuales (ADMIN y empleado)
        mockMvc.perform(get("/api/v1/reports/monthly")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttendedDays").value(1))
                .andExpect(jsonPath("$.totalOvertimeMinutes").value(90));

        mockMvc.perform(get("/api/v1/reports/monthly/me")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendedDays").value(1))
                .andExpect(jsonPath("$.totalOvertimeMinutes").value(90));
    }
}
