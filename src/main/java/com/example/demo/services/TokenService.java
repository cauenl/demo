package com.example.demo.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.example.demo.models.UserModel;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long minutos;

    public TokenService(JwtEncoder encoder,
                        @Value("${app.jwt.expiration-minutes:120}") long minutos) {
        this.encoder = encoder;
        this.minutos = minutos;
    }

    public long segundosDeValidade() {
        return minutos * 60;
    }

    // "admin" no campo tipo do usuario vira ADMIN; qualquer outro valor vira USER
    public static String papel(UserModel user) {
        return "admin".equalsIgnoreCase(user.getTipo()) ? "ADMIN" : "USER";
    }

    public String gerar(UserModel user) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("demo-imobiliaria")
            .issuedAt(agora)
            .expiresAt(agora.plus(minutos, ChronoUnit.MINUTES))
            .subject(user.getEmail())
            .claim("id", user.getId())
            .claim("role", papel(user))
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
