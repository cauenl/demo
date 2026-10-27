package com.example.demo.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.models.FotoImovelModel;
import com.example.demo.models.ImovelModel;
import com.example.demo.repositories.FotoImovelRepository;
import com.example.demo.repositories.ImovelRepository;


@Service
public class FotoImovelService {

    private final FotoImovelRepository fotoImovelRepository;
    private final ImovelRepository imovelRepository;
    private final String uploadDir;

    public FotoImovelService(FotoImovelRepository fotoImovelRepository,
                             ImovelRepository imovelRepository,
                             @Value("${app.upload-dir}") String uploadDir) {
        this.fotoImovelRepository = fotoImovelRepository;
        this.imovelRepository = imovelRepository;
        this.uploadDir = uploadDir;
    }

    private Path pastaDoImovel(Integer imovelId) {
        return Paths.get(uploadDir, String.valueOf(imovelId));
    }

    // o arquivo e localizado pela pasta de uploads + nome, nao pelo caminho gravado no banco
    private Path arquivoDe(FotoImovelModel foto) {
        return pastaDoImovel(foto.getImovel().getId()).resolve(foto.getNomeArquivo());
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        String tipo = arquivo.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("O arquivo precisa ser uma imagem");
        }
    }

    // grava o arquivo na pasta do imovel e devolve o nome final
    private String gravar(MultipartFile arquivo, Integer imovelId) throws IOException {
        Path pasta = pastaDoImovel(imovelId);
        Files.createDirectories(pasta);

        String nomeOriginal = arquivo.getOriginalFilename() == null
            ? "foto"
            : StringUtils.cleanPath(arquivo.getOriginalFilename());
        String nomeFinal = UUID.randomUUID() + "_" + nomeOriginal;
        Path destino = pasta.resolve(nomeFinal).normalize();

        if (!destino.startsWith(pasta.normalize())) {
            throw new IllegalArgumentException("Nome de arquivo inválido");
        }

        arquivo.transferTo(destino);
        return nomeFinal;
    }

    public FotoImovelModel salvar(MultipartFile arquivo, Integer imovelId) {
        ImovelModel imovel = imovelRepository.findById(imovelId)
            .orElseThrow(() -> new NoSuchElementException("Imóvel não encontrado: " + imovelId));

        validar(arquivo);

        try {
            String nomeFinal = gravar(arquivo, imovelId);

            FotoImovelModel model = new FotoImovelModel();
            model.setCaminho(pastaDoImovel(imovelId).resolve(nomeFinal).toString());
            model.setNomeArquivo(nomeFinal);
            model.setImovel(imovel);

            return fotoImovelRepository.save(model);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar o arquivo", e);
        }
    }

    public List<FotoImovelModel> listarPorImovel(Integer imovelId) {
        return fotoImovelRepository.findByImovelIdOrderByOrdemAscIdAsc(imovelId);
    }

    public FotoImovelModel buscar(Integer id) {
        return fotoImovelRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Foto não encontrada: " + id));
    }

    // atualiza a ordem e/ou troca o arquivo; o que vier nulo/vazio fica como está
    public FotoImovelModel atualizar(Integer id, Integer ordem, MultipartFile arquivo) {
        FotoImovelModel foto = buscar(id);

        if (ordem != null) {
            foto.setOrdem(ordem);
        }

        if (arquivo != null && !arquivo.isEmpty()) {
            validar(arquivo);
            try {
                Integer imovelId = foto.getImovel().getId();
                Path antigo = arquivoDe(foto);

                String nomeFinal = gravar(arquivo, imovelId);
                Files.deleteIfExists(antigo);

                foto.setCaminho(pastaDoImovel(imovelId).resolve(nomeFinal).toString());
                foto.setNomeArquivo(nomeFinal);
            }
            catch (IOException e) {
                throw new UncheckedIOException("Falha ao salvar o arquivo", e);
            }
        }

        return fotoImovelRepository.save(foto);
    }

    public void excluir(Integer id) {
        FotoImovelModel foto = buscar(id);

        try {
            Files.deleteIfExists(arquivoDe(foto));
        }
        catch (IOException e) {
            throw new UncheckedIOException("Falha ao apagar o arquivo", e);
        }

        fotoImovelRepository.delete(foto);
    }

    // usado ao apagar um imovel: remove os arquivos e os registros das fotos dele
    @Transactional
    public void excluirPorImovel(Integer imovelId) {
        List<FotoImovelModel> fotos = listarPorImovel(imovelId);

        for (FotoImovelModel foto : fotos) {
            try {
                Files.deleteIfExists(arquivoDe(foto));
            }
            catch (IOException e) {
                throw new UncheckedIOException("Falha ao apagar o arquivo", e);
            }
        }

        fotoImovelRepository.deleteAll(fotos);

        try {
            Files.deleteIfExists(pastaDoImovel(imovelId));
        }
        catch (DirectoryNotEmptyException e) {
            // ficou algum arquivo que nao era do sistema: deixa a pasta
        }
        catch (IOException e) {
            throw new UncheckedIOException("Falha ao apagar a pasta", e);
        }
    }

    // só uma foto por imóvel pode ser a capa
    @Transactional
    public FotoImovelModel definirCapa(Integer id) {
        FotoImovelModel foto = buscar(id);

        for (FotoImovelModel outra : listarPorImovel(foto.getImovel().getId())) {
            outra.setCapa(outra.getId().equals(id));
        }

        return foto;
    }

}
