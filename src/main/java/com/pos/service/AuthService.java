package com.pos.service;

import com.pos.dto.LoginRequest;
import com.pos.dto.LoginResponse;
import com.pos.model.Usuario;
import com.pos.repository.UsuarioRepository;
import com.pos.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

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
        // si el username existe o no (evita ataques de enumeración de usuarios).
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        // Se distingue del 401 para que el cliente pueda mostrar un mensaje diferente
        // ("cuenta bloqueada" vs "credenciales incorrectas").
        if (!usuario.getActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        // BCrypt.matches() compara el texto plano con el hash guardado en BD.
        // Nunca desencriptamos la contraseña — BCrypt es un hash unidireccional.
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        String token = jwtUtil.generarToken(usuario.getUsername(), rol);
        return new LoginResponse(token);
    }

    // Un JWT no se puede "destruir", así que se guarda su identificador (jti) en una lista
    // de revocación que el filtro consulta en cada petición.
    public String logout(String token) {
        tokenRevocadoService.revocar(token);
        return "Sesión cerrada exitosamente";
    }
}
