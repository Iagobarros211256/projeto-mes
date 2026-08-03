package com.biscoitos.manutencao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * MVP usa HTTP Basic + autorização por perfil (RN04) via @PreAuthorize nos controllers.
 * Evolução natural para Fase 2+: trocar Basic por JWT, sem precisar mexer nas regras
 * de @PreAuthorize (elas continuam funcionando com qualquer mecanismo de autenticação).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // T04: handshake do SockJS/STOMP fica sem autenticação HTTP por ora.
                        // Autenticar WebSocket de verdade exigiria um ChannelInterceptor lendo
                        // credenciais do frame STOMP CONNECT (mecanismo diferente do HTTP Basic
                        // usado no resto da API) — fora de escopo deste MVP, registrado como
                        // débito técnico consciente pra Fase 2+.
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
