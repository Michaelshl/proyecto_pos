package com.pos.categoria;

import com.pos.categoria.dto.CategoriaListaResponse;
import com.pos.categoria.dto.CategoriaRequest;
import com.pos.categoria.dto.CategoriaResponse;
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
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(categoriaRepository);
    }

    private CategoriaRequest solicitud(String nombre) {
        CategoriaRequest r = new CategoriaRequest();
        r.setNombre(nombre);
        r.setDescripcion("Gaseosas y jugos");
        return r;
    }

    @Test
    void crearGuardaElNombreSinEspaciosComoLlave() {
        when(categoriaRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(false);

        CategoriaResponse resp = service.crear(solicitud("  Bebidas "));

        assertEquals("Bebidas", resp.getNombre());
        verify(categoriaRepository).save(argThat(c -> "Bebidas".equals(c.getNombre())));
    }

    @Test
    void crearConNombreRepetidoDa400() {
        when(categoriaRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud("Bebidas")));

        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(e.getStatusCode().value()));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void listarDevuelveElTotalYLasCategorias() {
        Categoria c = new Categoria();
        c.setNombre("Lacteos");
        c.setDescripcion("Leche y derivados");
        when(categoriaRepository.findAll()).thenReturn(List.of(c));

        CategoriaListaResponse resp = service.listar();

        assertEquals(1, resp.getTotal());
        assertEquals("Lacteos", resp.getCategorias().get(0).getNombre());
    }
}
