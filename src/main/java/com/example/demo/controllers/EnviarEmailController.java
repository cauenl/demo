package com.example.demo.controllers;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.services.EmailService;

@RestController
@RequestMapping ("/email")
public class EnviarEmailController {

    private static final Logger log = LoggerFactory.getLogger(EnviarEmailController.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmailService service;

    public EnviarEmailController(EmailService service) {
        this.service = service;
    }

    // e-mail de teste; POST porque envia algo (nao deve disparar com um simples F5 ou link)
    @PostMapping
    public ResponseEntity<String> enviar(@RequestParam("para") String para) {
        if (para == null || !EMAIL.matcher(para.trim()).matches()) {
            return ResponseEntity.badRequest().body("Informe um e-mail válido em ?para=");
        }

        try{
            service.enviarEmailComTemplate(
            para.trim(),
            "Bem-vindo!",
            "email/template",
            Map.of(
                "nome", "Caue",
                "itens", List.of("Plano Pro", "Suporte 24h"),
                "link", "https://meusite.com"
            ));

            return ResponseEntity.ok("Enviado");
        }
        catch(Exception e){
            log.error("Falha ao enviar o e-mail de teste para {}", para, e);
            return ResponseEntity.internalServerError().body("Não foi possível enviar o e-mail.");
        }
    }

}
