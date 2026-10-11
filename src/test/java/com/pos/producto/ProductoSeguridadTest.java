package com.pos.producto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Quién puede qué en /api/productos: el cajero solo consulta y lista; el administrador hace todo.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoSeguridadTest {

    private static final String JSON_VACIO = "{}";

    @Autowired private MockMvc mockMvc;

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/productos/P001")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/productos/sugerencias").param("q", "co"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeListarConsultarYUsarSugerencias() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/sugerencias").param("q", "co")).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/NOEXISTE")).andExpect(status().isNotFound());   // pasó la seguridad
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeCrearActualizarInactivarNiAjustar() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VACIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/productos/P001").contentType(MediaType.APPLICATION_JSON).content(JSON_VACIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/productos/P001/inactivar").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VACIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/productos/P001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VACIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeConsultarElStock() throws Exception {
        mockMvc.perform(get("/api/productos/P001/stock")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeListarYConsultarElStock() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/NOEXISTE/stock")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConJsonInvalidoDa400() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VACIO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajusteConJsonInvalidoDa400() throws Exception {
        mockMvc.perform(post("/api/productos/P001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VACIO))
                .andExpect(status().isBadRequest());
    }
}
