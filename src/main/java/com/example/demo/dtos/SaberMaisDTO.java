package com.example.demo.dtos;

import lombok.Getter;
import lombok.Setter;

// dados que o cliente preenche no formulario "saber mais" de um imovel
@Getter
@Setter
public class SaberMaisDTO {
    private String nome;
    private String telefone;
    private String email;
    private String mensagem;
}
