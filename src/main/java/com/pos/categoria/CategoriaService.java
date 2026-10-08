package com.pos.categoria;

import com.pos.categoria.dto.CategoriaListaResponse;
import com.pos.categoria.dto.CategoriaRequest;
import com.pos.categoria.dto.CategoriaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una categoría con ese nombre");
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());
        categoriaRepository.save(categoria);
        return toResponse(categoria);
    }

    public CategoriaListaResponse listar() {
        List<CategoriaResponse> categorias = categoriaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return new CategoriaListaResponse(categorias.size(), categorias);
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .build();
    }
}
