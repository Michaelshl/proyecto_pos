package com.pos.config;

import com.pos.rol.Rol;
import com.pos.usuario.Usuario;
import com.pos.rol.RolRepository;
import com.pos.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminCedula;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           @Value("${ADMIN_CEDULA:}") String adminCedula,
                           @Value("${ADMIN_EMAIL:admin@pos.com}") String adminEmail,
                           @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminCedula = adminCedula;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        asegurarRol("ADMIN", "Administrador del sistema",
                List.of("USUARIOS", "ROLES", "CATEGORIAS"));
        asegurarRol("CAJERO", "Cajero de punto de venta",
                List.of("CATEGORIAS_LEER"));
        crearPrimerAdmin();
    }

    private void asegurarRol(String nombre, String descripcion, List<String> permisos) {
        Rol rol = rolRepository.findById(nombre).orElseGet(() -> {
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

    private void crearPrimerAdmin() {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (adminPassword.isBlank() || adminCedula.isBlank()) {
            log.warn("No hay usuarios y ADMIN_CEDULA o ADMIN_PASSWORD no están definidas: no se creó el "
                    + "administrador inicial. Defínalas en .env y reinicie la aplicación.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellido("Sistema");
        admin.setCedula(adminCedula);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setActivo(true);
        admin.setRol(rolRepository.findById("ADMIN").orElseThrow());
        usuarioRepository.save(admin);
        log.info("Administrador inicial creado: cédula '{}'", adminCedula);
    }
}
