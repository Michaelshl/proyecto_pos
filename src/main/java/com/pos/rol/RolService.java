package com.pos.rol;

import com.pos.rol.dto.RolRequest;
import com.pos.rol.dto.RolResponse;
import com.pos.rol.Rol;
import com.pos.rol.RolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public RolResponse crear(RolRequest request) {
        String nombre = request.getNombre().trim().toUpperCase();
        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un rol con ese nombre");
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(request.getDescripcion());
        rol.setPermisos(new ArrayList<>(request.getPermisos()));
        rolRepository.save(rol);

        return toResponse(rol);
    }

    public List<RolResponse> listar() {
        return rolRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private RolResponse toResponse(Rol rol) {
        return RolResponse.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .permisos(rol.getPermisos())
                .build();
    }
}
