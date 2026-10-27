package com.example.demo.controllers;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.demo.dtos.SaberMaisDTO;
import com.example.demo.dtos.StatusDTO;
import com.example.demo.models.ImovelModel;
import com.example.demo.services.EmailService;
import com.example.demo.services.ImovelService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/imoveis")
public class ImovelController {

    private static final Logger log = LoggerFactory.getLogger(ImovelController.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ImovelService service;
    private final EmailService emailService;
    private final String nomeImobiliaria;
    private final String mensagemImobiliaria;

    public ImovelController(ImovelService service,
                            EmailService emailService,
                            @Value("${app.imobiliaria.nome}") String nomeImobiliaria,
                            @Value("${app.imobiliaria.mensagem}") String mensagemImobiliaria) {
        this.service = service;
        this.emailService = emailService;
        this.nomeImobiliaria = nomeImobiliaria;
        this.mensagemImobiliaria = mensagemImobiliaria;
    }

     @GetMapping()
    public ResponseEntity<List<ImovelModel>> getAll() {
        List<ImovelModel> list = service.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @GetMapping(value = "/{id}")
        public ResponseEntity<ImovelModel> find(@PathVariable Integer id) {
        ImovelModel model = service.find(id);
        if(model!=null){
            return ResponseEntity.status(HttpStatus.OK).body(model);
        }
        else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> insert(@RequestBody ImovelModel model) {
        model = service.insert(model);
        URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(model.getId()).toUri();
        return ResponseEntity.created(uri).build();
        }

    @PutMapping(value = "/{id}")
    public ResponseEntity<Void> update(@RequestBody ImovelModel model, @PathVariable Integer id) {
        if (service.find(id) == null) {
            return ResponseEntity.notFound().build();
        }
        model.setId(id);
        service.update(model);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (service.find(id) == null) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // qualquer usuario autenticado (ADMIN ou USER) pode alterar o status de um imovel
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> alterarStatus(@PathVariable Integer id, @RequestBody StatusDTO dto) {
        if (vazio(dto.getStatus())) {
            return ResponseEntity.badRequest().body("Informe o novo status.");
        }

        ImovelModel imovel = service.atualizarStatus(id, dto.getStatus().trim());
        if (imovel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(imovel);
    }

    // o cliente preenche o formulario "saber mais" e recebe por e-mail os dados do imovel
    @PostMapping("/sabermais/{id}")
    public ResponseEntity<String> saberMais(@PathVariable Integer id, @RequestBody SaberMaisDTO dto) {
        ImovelModel imovel = service.find(id);
        if (imovel == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Imóvel não encontrado.");
        }

        if (vazio(dto.getNome()) || vazio(dto.getTelefone()) || vazio(dto.getMensagem())) {
            return ResponseEntity.badRequest().body("Informe nome, telefone, e-mail e mensagem.");
        }
        if (vazio(dto.getEmail()) || !EMAIL.matcher(dto.getEmail().trim()).matches()) {
            return ResponseEntity.badRequest().body("Informe um e-mail válido.");
        }

        Map<String, Object> variaveis = new HashMap<>();
        variaveis.put("nomeCliente", dto.getNome().trim());
        variaveis.put("telefoneCliente", dto.getTelefone().trim());
        variaveis.put("emailCliente", dto.getEmail().trim());
        variaveis.put("mensagemCliente", dto.getMensagem().trim());

        variaveis.put("nomeImobiliaria", nomeImobiliaria);
        variaveis.put("mensagemImobiliaria", mensagemImobiliaria);

        variaveis.put("titulo", imovel.getTitulo());
        variaveis.put("descricao", imovel.getDescricao());
        variaveis.put("finalidade", imovel.getFinalidade());
        variaveis.put("status", imovel.getStatus());
        variaveis.put("precoVenda", imovel.getPrecoVenda());
        variaveis.put("precoAluguel", imovel.getPrecoAluguel());
        variaveis.put("dormitorios", imovel.getDormitorios());
        variaveis.put("banheiros", imovel.getBanheiros());
        variaveis.put("garagem", imovel.getGaragem());
        variaveis.put("areaTotal", imovel.getAreaTotal());
        variaveis.put("areaConstruida", imovel.getAreaConstruida());
        variaveis.put("caracteristicas", imovel.getCaracteristicas());
        variaveis.put("endereco", enderecoCompleto(imovel));
        variaveis.put("bairro", imovel.getBairro() == null ? null : imovel.getBairro().getNome());
        variaveis.put("tipoImovel", imovel.getTipoImovel() == null ? null : imovel.getTipoImovel().getNome());

        try {
            emailService.enviarEmailComTemplate(
                dto.getEmail().trim(),
                "Informações sobre o imóvel: " + imovel.getTitulo(),
                "email/sabermais",
                variaveis);
            return ResponseEntity.ok("E-mail enviado.");
        } catch (Exception e) {
            log.error("Falha ao enviar o e-mail 'saber mais' do imóvel {} para {}", id, dto.getEmail(), e);
            return ResponseEntity.internalServerError().body("Não foi possível enviar o e-mail.");
        }
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private String enderecoCompleto(ImovelModel imovel) {
        StringBuilder sb = new StringBuilder();
        if (!vazio(imovel.getEndereco())) sb.append(imovel.getEndereco().trim());
        if (!vazio(imovel.getNumero())) sb.append(", ").append(imovel.getNumero().trim());
        if (!vazio(imovel.getComplemento())) sb.append(" - ").append(imovel.getComplemento().trim());
        if (!vazio(imovel.getCep())) sb.append(" (CEP ").append(imovel.getCep().trim()).append(")");
        return sb.toString();
    }

}
