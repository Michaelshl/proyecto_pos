package com.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pos.service.TokenRevocadoService;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final RestSecurityHandlers securityHandlers;
    private final TokenRevocadoService tokenRevocadoService;

    public SecurityConfig(JwtUtil jwtUtil, RestSecurityHandlers securityHandlers,
                          TokenRevocadoService tokenRevocadoService) {
        this.jwtUtil = jwtUtil;
        this.securityHandlers = securityHandlers;
        this.tokenRevocadoService = tokenRevocadoService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityHandlers)
                .accessDeniedHandler(securityHandlers))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/*.js",
                    "/*.css",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias").hasAnyRole("ADMIN", "CAJERO")
                .requestMatchers("/api/categorias/**").hasRole("ADMIN")
                .requestMatchers("/api/usuarios/**", "/api/roles/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthFilter(jwtUtil, tokenRevocadoService),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtUtil jwtUtil;
        private final TokenRevocadoService tokenRevocadoService;

        JwtAuthFilter(JwtUtil jwtUtil, TokenRevocadoService tokenRevocadoService) {
            this.jwtUtil = jwtUtil;
            this.tokenRevocadoService = tokenRevocadoService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.tokenValido(token) && !estaRevocado(token)) {
                    String username = jwtUtil.obtenerUsername(token);
                    String rol = jwtUtil.obtenerRol(token);
                    List<SimpleGrantedAuthority> autoridades = rol == null
                            ? List.of()
                            : List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, autoridades);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            filterChain.doFilter(request, response);
        }

        // Un token sin jti (emitido antes de la revocación) se trata como revocado.
        private boolean estaRevocado(String token) {
            String jti = jwtUtil.obtenerJti(token);
            return jti == null || tokenRevocadoService.estaRevocado(jti);
        }
    }
}
