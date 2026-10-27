package com.example.demo.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;

// corpo do registro: { "nome", "email", "password", "role" } (aceita tambem "senha" e "tipo")
@Getter
@Setter
public class RegisterDTO {
    private String nome;
    private String email;

    @JsonAlias("senha")
    private String password;

    @JsonAlias("tipo")
    private String role;
}
