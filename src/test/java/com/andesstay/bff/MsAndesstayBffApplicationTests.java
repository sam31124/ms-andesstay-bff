package com.andesstay.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MsAndesstayBffApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // --- 1. PRUEBA SIN AUTENTICACIÓN (Debe dar 401 Unauthorized) ---
    @Test
    void unauthenticatedUser_Returns401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    // --- 2. PRUEBA ROL HUÉSPED ---
    @Test
    @WithMockUser(username = "huesped_user", roles = {"Huesped"})
    void huesped_CanReadReservations() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "huesped_user", roles = {"Huesped"})
    void huesped_CannotUpdateStatus_Returns403() throws Exception {
        // Huésped NO puede cambiar estados de reserva
        mockMvc.perform(put("/api/reservations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isForbidden());
    }

    // --- 3. PRUEBA ROL AUDITOR ---
    @Test
    @WithMockUser(username = "auditor_user", roles = {"Auditor"})
    void auditor_CannotUpdateStatus_Returns403() throws Exception {
        // Auditor es solo lectura, no puede operar estados
        mockMvc.perform(put("/api/reservations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isForbidden());
    }

    // --- 4. PRUEBA ROL RECEPCIONISTA ---
    @Test
    @WithMockUser(username = "recepcionista_user", roles = {"Recepcionista"})
    void recepcionista_CanUpdateStatus() throws Exception {
        // Recepcionista SÍ tiene permiso para operar
        mockMvc.perform(put("/api/reservations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isOk());
    }

    // --- 5. PRUEBA ROL ADMIN ---
    @Test
    @WithMockUser(username = "admin_user", roles = {"Admin"})
    void admin_CanDoEverything() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/reservations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isOk());
    }
}