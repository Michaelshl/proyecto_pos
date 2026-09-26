package com.pos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map((FieldError e) -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construir(status, detalle, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        String mensaje;
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            mensaje = rse.getReason();
        } else if (body instanceof ProblemDetail pd && pd.getDetail() != null) {
            mensaje = pd.getDetail();
        } else {
            mensaje = "";
        }
        if (mensaje.isBlank()) {
            mensaje = status instanceof HttpStatus hs ? hs.getReasonPhrase() : "Error";
        }
        return construir(status, mensaje, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> manejarInesperado(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request.getRequestURI());
    }

    private ResponseEntity<Object> construir(HttpStatusCode status, String mensaje, WebRequest request) {
        return construir(status, mensaje, ((ServletWebRequest) request).getRequest().getRequestURI());
    }

    private ResponseEntity<Object> construir(HttpStatusCode status, String mensaje, String path) {
        String razon = status instanceof HttpStatus hs ? hs.getReasonPhrase() : status.toString();
        return ResponseEntity.status(status).body(ApiError.of(status.value(), razon, mensaje, path));
    }
}
