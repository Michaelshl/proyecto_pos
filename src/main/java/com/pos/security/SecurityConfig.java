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

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final RestSecurityHandlers securityHandlers;

    public SecurityConfig(JwtUtil jwtUtil, RestSecurityHandlers securityHandlers) {
        this.jwtUtil = jwtUtil;
        this.securityHandlers = securityHandlers;
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
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias").hasAnyRole("ADMIN", "CAJERO")
                .requestMatchers("/api/categorias/**").hasRole("ADMIN")
                .requestMatchers("/api/usuarios/**", "/api/roles/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class
            );

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
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.tokenValido(token)) {
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
    }
}
