package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.extractor.ExtratorEdital;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ExtratorPadrao implements ExtratorEdital {

    @Override
    public EstruturaEditalDTO extrair(String texto) {
        log.info("Iniciando extração com o Extrator Padrão (Edital Estruturado)...");

        if (texto == null || texto.isBlank()) {
            return new EstruturaEditalDTO(null, List.of(), List.of());
        }

        // TODO: Implementar a lógica genérica baseada em leitura de tabelas ou Markdown
        // (A sua lógica original de extração quando o documento é bem formatado vai aqui)

        return new EstruturaEditalDTO(null, List.of(), List.of());
    }
}
