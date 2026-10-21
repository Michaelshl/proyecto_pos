package com.pos.security;

import com.pos.auditoria.Auditoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        Auditoria.alerta("Acceso sin sesión válida (401)", detalle(request));
        escribir(request, response, HttpStatus.UNAUTHORIZED, "Token ausente, inválido o expirado");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        Auditoria.alerta("Acceso denegado (403)", "usuario " + Auditoria.actorActual() + ", " + detalle(request));
        escribir(request, response, HttpStatus.FORBIDDEN, "No tiene permisos para esta operación");
    }

    private static String detalle(HttpServletRequest request) {
        return request.getMethod() + " " + Auditoria.limpiar(request.getRequestURI())
                + " desde " + request.getRemoteAddr();
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiError.of(status.value(), status.getReasonPhrase(), mensaje, request.getRequestURI()));
    }
}
