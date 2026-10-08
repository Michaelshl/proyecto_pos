package com.pos.usuario;

import com.pos.usuario.dto.InactivarRequest;
import com.pos.usuario.dto.UsuarioRequest;
import com.pos.usuario.dto.UsuarioResponse;
import com.pos.usuario.dto.UsuarioUpdateRequest;
import java.util.List;
import java.util.stream.Collectors;
import com.pos.rol.Rol;
import com.pos.rol.RolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UsuarioResponse crear(UsuarioRequest request) {
        // Validaciones de unicidad ANTES de crear el objeto Usuario.
        // Si lo hiciéramos después, la BD lanzaría una excepción de constraint
        // más difícil de manejar y con mensajes de error menos claros al cliente.
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El username ya está registrado");
        }

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol no existe"));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        // La contraseña se encripta aquí: NUNCA se guarda texto plano en la BD.
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuario.setRol(rol);

        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!request.getEmail().equals(usuario.getEmail())
                && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        // Solo actualizamos los campos permitidos (nombre, apellido, email).
        // No tocamos username, password ni rol porque tienen restricciones explicadas en el DTO.
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());

        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    public String inactivar(Long id, InactivarRequest request) {
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }

        String motivo = request.getMotivo().trim();
        if (motivo.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo no puede superar 500 caracteres");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Soft delete: no eliminamos el registro, solo lo marcamos como inactivo.
        // Esto preserva el historial y evita problemas de integridad referencial.
        usuario.setActivo(false);
        usuario.setMotivoInactivacion(motivo);
        usuarioRepository.save(usuario);
        return "Usuario inactivado exitosamente";
    }

    public UsuarioResponse buscar(String username, String email) {
        boolean hayUsername = username != null && !username.isBlank();
        boolean hayEmail = email != null && !email.isBlank();
        if (!hayUsername && !hayEmail) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe indicar username o email");
        }

        Usuario usuario = (hayUsername
                ? usuarioRepository.findByUsername(username)
                : usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sin resultados para la búsqueda"));
        return toResponse(usuario);
    }

    public UsuarioResponse asignarRol(Long id, Long rolId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol no existe"));

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .activo(usuario.getActivo())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }
}
