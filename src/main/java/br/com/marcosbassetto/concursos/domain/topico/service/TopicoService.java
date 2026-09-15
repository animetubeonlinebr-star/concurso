package br.com.marcosbassetto.concursos.domain.topico.service;

import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TopicoService {

    private final TopicoRepository topicoRepository;

    @Transactional
    public TopicoEntity criar(TopicoEntity topico) {
        return topicoRepository.save(topico);
    }

}
