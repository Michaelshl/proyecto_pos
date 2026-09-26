package com.pos.service;

import com.pos.dto.RolRequest;
import com.pos.dto.RolResponse;
import com.pos.model.Rol;
import com.pos.repository.RolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

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

        return RolResponse.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .permisos(rol.getPermisos())
                .build();
    }
}
