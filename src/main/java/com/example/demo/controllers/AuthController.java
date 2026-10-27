package com.example.demo.controllers;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.dtos.TokenDTO;
import com.example.demo.dtos.UserDTO;
import com.example.demo.models.UserModel;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.TokenService;
import com.example.demo.services.UserService;

@RestController
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final boolean permitirAdminNoRegistro;

    public AuthController(UserRepository userRepository,
                          UserService userService,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          @Value("${app.register.permitir-admin:false}") boolean permitirAdminNoRegistro) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.permitirAdminNoRegistro = permitirAdminNoRegistro;
    }

    // cadastro aberto (como no exemplo da atividade). O papel vem do campo "role".
    @PostMapping("/public/register")
    public ResponseEntity<String> register(@RequestBody RegisterDTO dto) {
        if (vazio(dto.getNome()) || vazio(dto.getEmail()) || vazio(dto.getPassword())) {
            return ResponseEntity.badRequest().body("Informe nome, email e password.");
        }
        if (!EMAIL.matcher(dto.getEmail().trim()).matches()) {
            return ResponseEntity.badRequest().body("Informe um e-mail válido.");
        }
        if (userRepository.existsByEmail(dto.getEmail().trim())) {
            return ResponseEntity.status(409).body("E-mail já cadastrado.");
        }

        // so vira ADMIN se o papel pedido for ADMIN e isso estiver liberado na configuracao
        boolean admin = permitirAdminNoRegistro && "ADMIN".equalsIgnoreCase(dto.getRole());

        UserDTO novo = new UserDTO();
        novo.setNome(dto.getNome().trim());
        novo.setEmail(dto.getEmail().trim());
        novo.setSenha(dto.getPassword());
        novo.setTipo(admin ? "admin" : "user");
        userService.insert(novo);

        return ResponseEntity.ok().build();
    }

    // /auth/login e /public/login fazem a mesma coisa
    @PostMapping({"/public/login", "/auth/login"})
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {
        if (vazio(dto.getEmail()) || vazio(dto.getPassword())) {
            return ResponseEntity.badRequest().body("Informe email e password.");
        }

        UserModel user = userRepository.findFirstByEmail(dto.getEmail().trim()).orElse(null);

        // mesma resposta para e-mail inexistente e senha errada (nao revela qual deles falhou)
        if (user == null || user.getSenha() == null
                || !passwordEncoder.matches(dto.getPassword(), user.getSenha())) {
            return ResponseEntity.status(401).body("Credenciais inválidas.");
        }

        return ResponseEntity.ok(new TokenDTO(
            tokenService.gerar(user),
            "Bearer",
            TokenService.papel(user),
            tokenService.segundosDeValidade()));
    }

    // devolve quem e o dono do token (util para conferir o login)
    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "email", jwt.getSubject(),
            "id", jwt.getClaim("id"),
            "role", jwt.getClaimAsString("role"),
            "expiraEm", jwt.getExpiresAt().toString()));
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
