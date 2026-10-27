package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.models.UserModel;
import com.example.demo.repositories.UserRepository;

// cria o primeiro administrador (sem ele ninguem consegue cadastrar usuarios, pois isso exige ADMIN)
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminSeeder(UserRepository repository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.senha:}") String senha) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || senha.isBlank()) {
            log.info("app.admin.email/app.admin.senha não definidos: nenhum admin foi criado.");
            return;
        }

        if (repository.existsByEmail(email)) {
            return;
        }

        UserModel admin = new UserModel();
        admin.setNome("Administrador");
        admin.setEmail(email);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setTipo("admin");
        repository.save(admin);

        log.info("Usuário administrador criado: {}", email);
    }
}
