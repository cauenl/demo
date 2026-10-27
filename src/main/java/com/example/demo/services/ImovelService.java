package com.example.demo.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.ImovelModel;
import com.example.demo.repositories.ImovelRepository;

@Service
public class ImovelService {
    private final ImovelRepository repository;
    private final FotoImovelService fotoService;

    public ImovelService(ImovelRepository repository, FotoImovelService fotoService) {
        this.repository = repository;
        this.fotoService = fotoService;
    }

    public List<ImovelModel> getAll() {
        List<ImovelModel> list = repository.findAll();
        return list;
    }

    public ImovelModel find(Integer id) {
        Optional<ImovelModel> model = repository.findById(id);
        return model.orElse(null);
    }

    public ImovelModel insert(ImovelModel model) {
        return repository.save(model);
    }

    // o controller confere se o imovel existe antes de chamar
    public ImovelModel update(ImovelModel model) {
        return repository.save(model);
    }

    // altera so o status; devolve null se o imovel nao existe
    public ImovelModel atualizarStatus(Integer id, String status) {
        ImovelModel imovel = find(id);
        if (imovel == null) {
            return null;
        }
        imovel.setStatus(status);
        return repository.save(imovel);
    }

    // apaga tambem as fotos (arquivos e registros), que dependem do imovel
    @Transactional
    public void delete(Integer id) {
        fotoService.excluirPorImovel(id);
        repository.deleteById(id);
    }
}
