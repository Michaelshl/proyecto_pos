package com.pos.auth;

import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.LoginResponse;
import com.pos.usuario.Usuario;
import com.pos.usuario.UsuarioRepository;
import com.pos.security.JwtUtil;
import com.pos.security.TokenRevocadoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    // Hash BCrypt válido de una clave aleatoria; solo sirve para igualar tiempos de respuesta.
    private static final String HASH_FALSO =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BUk0IDXnS4bQ6xS7mQ0pQ5vC1Wku";

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenRevocadoService tokenRevocadoService;

    public AuthService(UsuarioRepository usuarioRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       TokenRevocadoService tokenRevocadoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenRevocadoService = tokenRevocadoService;
    }

    public LoginResponse login(LoginRequest request) {
        // Usamos UNAUTHORIZED (no NOT_FOUND) deliberadamente para no revelar
        // si la cédula existe o no (evita ataques de enumeración de usuarios).
        Usuario usuario = usuarioRepository.findById(request.getCedula().trim()).orElse(null);

        // Si el usuario no existe se compara contra un hash falso, para que la
        // respuesta tarde lo mismo y no se pueda enumerar usuarios por tiempo.
        String hash = usuario != null ? usuario.getPassword() : HASH_FALSO;

        // BCrypt.matches() compara el texto plano con el hash guardado en BD.
        // Nunca desencriptamos la contraseña — BCrypt es un hash unidireccional.
        boolean claveCorrecta = passwordEncoder.matches(request.getPassword(), hash);
        if (usuario == null || !claveCorrecta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // Solo se revela que la cuenta está inactiva cuando la contraseña es correcta.
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            String motivo = usuario.getMotivoInactivacion();
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    motivo == null || motivo.isBlank()
                            ? "Usuario inactivo"
                            : "Usuario inactivo. Motivo: " + motivo);
        }

        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        String token = jwtUtil.generarToken(usuario.getCedula(), rol);
        return new LoginResponse(token);
    }

    // Un JWT no se puede "destruir", así que se guarda su identificador (jti) en una lista
    // de revocación que el filtro consulta en cada petición.
    public String logout(String token) {
        tokenRevocadoService.revocar(token);
        return "Sesión cerrada exitosamente";
    }
}
