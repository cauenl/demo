package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Regras de acesso. Para mudar quem pode o que, e so mexer aqui.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // publico: login, formulario "saber mais" e consulta de imoveis
                .requestMatchers(HttpMethod.POST,
                    "/public/login", "/public/register", "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/imoveis/sabermais/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/imoveis/**", "/bairros/**", "/tipos-imoveis/**", "/fotos/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // alterar o status de um imovel: qualquer usuario autenticado (ADMIN ou USER)
                .requestMatchers(HttpMethod.PATCH, "/imoveis/*/status").authenticated()
                // o que sobrou dessas rotas (escrita, usuarios, e-mail de teste): so ADMIN
                .requestMatchers("/imoveis/**", "/bairros/**", "/tipos-imoveis/**",
                    "/fotos/**", "/users/**", "/email/**").hasRole("ADMIN")
                // qualquer outra rota (ex.: /auth/me): precisa estar logado
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor())));

        return http.build();
    }

    // transforma o claim "role" do token (ex.: ADMIN) na autoridade ROLE_ADMIN
    private JwtAuthenticationConverter conversor() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("role");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
