package com.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Bean del encoder de contraseñas.
    // BCrypt aplica un hash con "salt" aleatorio, lo que hace que dos hashes del mismo
    // texto sean diferentes. Es el estándar actual para contraseñas.
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos CSRF porque usamos JWT (sin cookies de sesión, CSRF no aplica)
            .csrf(csrf -> csrf.disable())

            // STATELESS: Spring NO crea sesiones de servidor. Cada petición se autentica
            // solo con el JWT que viene en el header. Esto hace la API escalable.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )

            // Insertamos nuestro filtro JWT ANTES del filtro de autenticación estándar de Spring.
            // Así, cuando llega una petición con Bearer token, nosotros la autenticamos primero.
            .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtUtil jwtUtil;

        JwtAuthFilter(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");

            // El estándar Bearer Token: el header llega como "Bearer eyJhbGci..."
            // Comprobamos que exista y tenga el prefijo correcto antes de procesar
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtil.tokenValido(token)) {
                    String username = jwtUtil.obtenerUsername(token);

                    // Tercer parámetro (authorities): lista vacía porque no implementamos
                    // control por roles en los endpoints todavía (solo autenticación básica).
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            // Independientemente de si había token o no, dejamos pasar la petición al siguiente filtro.
            // Si no se registró autenticación y la ruta requiere auth, Spring devolverá 403 solo.
            filterChain.doFilter(request, response);
        }
    }
}
