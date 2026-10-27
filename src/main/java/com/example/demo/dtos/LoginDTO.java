package com.example.demo.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;

// aceita "password" (como nos exemplos da atividade) e tambem "senha"
@Getter
@Setter
public class LoginDTO {
    private String email;

    @JsonAlias("senha")
    private String password;
}
