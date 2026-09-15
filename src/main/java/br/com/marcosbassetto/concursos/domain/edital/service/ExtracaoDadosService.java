package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.DadosExtraidos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ⚠️ Service LEGADO — pipeline antigo (regex puro).
 *
 * Será deletado quando o ExtratorEditalFactory + Strategy estiver
 * plugado no ConcursoUploadController. Mantido apenas para build verde
 * durante a migração.
 *
 * @deprecated substituído por ExtratorEditalFactory + ExtratorCebraspe
 */
@Deprecated(since = "migração Template+Strategy")
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtracaoDadosService {

    private final ExtratorDadosConcurso extratorConcurso;
    private final ExtratorMaterias extratorMaterias;

    public DadosExtraidos extrair(String texto) {
        if (texto == null || texto.isBlank()) {
            log.warn("📄 Texto vazio ou nulo para extração");
            return new DadosExtraidos();
        }

        String textoNorm = texto.replaceAll("\\s+", " ").trim();
        log.debug("📄 Texto normalizado: {} caracteres", textoNorm.length());

        DadosExtraidos dados = new DadosExtraidos();
        dados.setNome(extratorConcurso.extrairNome(textoNorm));
        dados.setBanca(extratorConcurso.extrairBanca(textoNorm));
        dados.setOrgao(extratorConcurso.extrairOrgao(textoNorm));
        dados.setAno(extratorConcurso.extrairAno(textoNorm));

        dados.setMaterias(extratorMaterias.extrair(texto));

        logResumo(dados);
        return dados;
    }

    private void logResumo(DadosExtraidos dados) {
        log.info("📊 DADOS EXTRAÍDOS:");
        log.info("   Nome: {}", dados.getNome());
        log.info("   Banca: {}", dados.getBanca());
        log.info("   Órgão: {}", dados.getOrgao());
        log.info("   Ano: {}", dados.getAno());
        log.info("   Matérias: {}",
                dados.getMaterias() != null ? dados.getMaterias().size() : 0);
        if (dados.getMaterias() != null) {
            dados.getMaterias().stream()
                    .limit(5)
                    .forEach(m -> log.info("      - {} ({} tópicos)",
                            m.nome(),
                            m.topicos() != null ? m.topicos().size() : 0));
        }
    }
}
