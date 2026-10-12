package com.pos.cliente;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /api/clientes lo usan el cajero y el administrador; sin token, 401. También prueba las validaciones.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteSeguridadTest {

    @Autowired private MockMvc mockMvc;

    private String cuerpo(String tipo, String numero, String extra) {
        return "{\"tipoDocumento\":\"" + tipo + "\",\"numeroDocumento\":\"" + numero
                + "\",\"nombre\":\"Ana\",\"apellido\":\"Lopez\"" + extra + "}";
    }

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/clientes/1001")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/clientes/sugerencias").param("q", "an")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeCrearConsultarActualizarYUsarSugerencias() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("CC", "7000001", ",\"email\":\"ana@correo.com\",\"telefono\":\"3001234567\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento").value("7000001"));

        mockMvc.perform(get("/api/clientes/7000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));

        mockMvc.perform(put("/api/clientes/7000001").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"CE\",\"nombre\":\"Ana Maria\",\"apellido\":\"Lopez\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("CE"))
                .andExpect(jsonPath("$.nombre").value("Ana Maria"));

        // Se busca el documento completo: otros tests de esta clase también crean clientes 70000xx.
        mockMvc.perform(get("/api/clientes/sugerencias").param("q", "7000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroDocumento").value("7000001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTambienPuedeConsultar() throws Exception {
        mockMvc.perform(get("/api/clientes/NO-EXISTE")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void documentoRepetidoDa400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("CC", "7000002", ""))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("CC", "7000002", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe un cliente con ese número de documento"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void unNitSeCreaConRazonSocialYSinApellido() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"NIT\",\"numeroDocumento\":\"900700700-1\","
                                + "\"nombre\":\"Distribuciones El Sol S.A.S.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Distribuciones El Sol S.A.S."))
                .andExpect(jsonPath("$.apellido").doesNotExist());

        mockMvc.perform(get("/api/clientes/900700700-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("NIT"))
                .andExpect(jsonPath("$.apellido").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void unaPersonaSinApellidoDa400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"7000009\",\"nombre\":\"Ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El apellido es obligatorio"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void crearConJsonVacioDa400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void tipoDeDocumentoInvalidoDa400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("XX", "7000003", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("CC, CE, TI, NIT o PP")));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void emailTelefonoYDocumentoInvalidosDan400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("CC", "7000004", ",\"email\":\"no-es-un-correo\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("CC", "7000004", ",\"telefono\":\"abc\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("CC", "12", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void actualizarInexistenteDa404() throws Exception {
        mockMvc.perform(put("/api/clientes/NO-EXISTE").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"CC\",\"nombre\":\"A\",\"apellido\":\"B\"}"))
                .andExpect(status().isNotFound());
    }
}
