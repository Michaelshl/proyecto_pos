package com.pos.rol;

import com.pos.rol.dto.RolRequest;
import com.pos.rol.dto.RolResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock private RolRepository rolRepository;

    private RolService service;

    @BeforeEach
    void setUp() {
        service = new RolService(rolRepository);
    }

    private RolRequest solicitud(String nombre) {
        RolRequest r = new RolRequest();
        r.setNombre(nombre);
        r.setDescripcion("Supervisa turnos");
        r.setPermisos(List.of("VENTAS_VER"));
        return r;
    }

    @Test
    void crearGuardaElNombreEnMayusculasComoLlave() {
        when(rolRepository.existsById("SUPERVISOR")).thenReturn(false);

        RolResponse resp = service.crear(solicitud("  supervisor "));

        assertEquals("SUPERVISOR", resp.getNombre());
        verify(rolRepository).save(argThat(r -> "SUPERVISOR".equals(r.getNombre())));
    }

    @Test
    void crearConNombreRepetidoDa400() {
        when(rolRepository.existsById("ADMIN")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud("admin")));

        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(e.getStatusCode().value()));
        verify(rolRepository, never()).save(any());
    }

    @Test
    void listarDevuelveTodosLosRoles() {
        Rol rol = new Rol();
        rol.setNombre("CAJERO");
        rol.setPermisos(List.of("CATEGORIAS_LEER"));
        when(rolRepository.findAll()).thenReturn(List.of(rol));

        List<RolResponse> roles = service.listar();

        assertEquals(1, roles.size());
        assertEquals("CAJERO", roles.get(0).getNombre());
        assertEquals(List.of("CATEGORIAS_LEER"), roles.get(0).getPermisos());
    }
}
