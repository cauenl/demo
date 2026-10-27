package com.example.demo.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.models.BairroModel;
import com.example.demo.repositories.BairroRepository;

@Service
public class BairroService {

    private final BairroRepository repository;

    public BairroService(BairroRepository repository) {
        this.repository = repository;
    }

    public List<BairroModel> getAll() {
        return repository.findAll();
    }

    public Page<BairroModel> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public BairroModel find(Integer id) {
        Optional<BairroModel> model = repository.findById(id);
        return model.orElse(null);
    }

    public BairroModel insert(BairroModel model) {
        return repository.save(model);
    }

    // o controller confere se o bairro existe antes de chamar
    public BairroModel update(BairroModel model) {
        return repository.save(model);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }

}
