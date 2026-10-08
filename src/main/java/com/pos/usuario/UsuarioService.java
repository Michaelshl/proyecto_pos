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

        if (usuarioRepository.existsById(request.getCedula())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La cédula ya está registrada");
        }

        Rol rol = rolRepository.findById(request.getRol().trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol no existe"));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCedula(request.getCedula());
        usuario.setEmail(request.getEmail());
        // La contraseña se encripta aquí: NUNCA se guarda texto plano en la BD.
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuario.setRol(rol);

        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    public UsuarioResponse actualizar(String cedula, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!request.getEmail().equals(usuario.getEmail())
                && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        // Solo actualizamos los campos permitidos (nombre, apellido, email).
        // No tocamos cédula, password ni rol porque tienen restricciones explicadas en el DTO.
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());

        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    public String inactivar(String cedula, InactivarRequest request) {
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }

        String motivo = request.getMotivo().trim();
        if (motivo.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo no puede superar 500 caracteres");
        }

        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Soft delete: no eliminamos el registro, solo lo marcamos como inactivo.
        // Esto preserva el historial y evita problemas de integridad referencial.
        usuario.setActivo(false);
        usuario.setMotivoInactivacion(motivo);
        usuarioRepository.save(usuario);
        return "Usuario inactivado exitosamente";
    }

    public UsuarioResponse buscar(String cedula, String email) {
        boolean hayCedula = cedula != null && !cedula.isBlank();
        boolean hayEmail = email != null && !email.isBlank();
        if (!hayCedula && !hayEmail) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe indicar cédula o email");
        }

        Usuario usuario = (hayCedula
                ? usuarioRepository.findById(cedula.trim())
                : usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sin resultados para la búsqueda"));
        return toResponse(usuario);
    }

    public UsuarioResponse asignarRol(String cedula, String nombreRol) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Rol rol = rolRepository.findById(nombreRol.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol no existe"));

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .cedula(usuario.getCedula())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .activo(usuario.getActivo())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }
}
