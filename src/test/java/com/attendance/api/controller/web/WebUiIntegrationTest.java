package com.attendance.api.controller.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPage_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithUserDetails("admin@asistencia.com")
    void adminUser_canRenderAllWebPages() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        mockMvc.perform(get("/attendance/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/history"));

        mockMvc.perform(get("/reports/daily"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/daily"));

        mockMvc.perform(get("/reports/monthly"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/monthly"));

        mockMvc.perform(get("/admin/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/employees"));

        mockMvc.perform(get("/admin/shifts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/shifts"));
    }

    @Test
    @WithUserDetails("ana.garcia@asistencia.com")
    void employeeUser_canRenderOwnViewsAndIsBlockedFromAdminViews() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        mockMvc.perform(get("/attendance/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance/history"));

        mockMvc.perform(get("/reports/monthly"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/monthly"));

        mockMvc.perform(get("/reports/daily"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/employees"))
                .andExpect(status().isForbidden());
    }
}
