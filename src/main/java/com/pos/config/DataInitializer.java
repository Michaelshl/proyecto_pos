package com.pos.config;

import com.pos.model.Rol;
import com.pos.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    public DataInitializer(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        asegurarRol("ADMIN", "Administrador del sistema",
                List.of("USUARIOS", "ROLES", "CATEGORIAS"));
        asegurarRol("CAJERO", "Cajero de punto de venta",
                List.of("CATEGORIAS_LEER"));
    }

    private void asegurarRol(String nombre, String descripcion, List<String> permisos) {
        Rol rol = rolRepository.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setNombre(nombre);
            return nuevo;
        });
        if (rol.getDescripcion() == null) {
            rol.setDescripcion(descripcion);
        }
        if (rol.getPermisos().isEmpty()) {
            rol.setPermisos(new ArrayList<>(permisos));
        }
        rolRepository.save(rol);
    }
}
