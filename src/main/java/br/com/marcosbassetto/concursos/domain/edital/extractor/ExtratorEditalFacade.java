package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtratorEditalFacade {

    private final DetectorBanca detectorBanca;
    private final DetectorPadraoEdital detectorPadrao;
    private final ExtratorEditalFactory factory;

    public EstruturaEditalDTO extrair(String texto,
                                      Banca bancaInformada,
                                      PadraoEdital padraoInformado) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Texto do edital não pode ser vazio");
        }

        Banca banca = (bancaInformada != null && bancaInformada.isConhecida())
                ? bancaInformada
                : detectorBanca.detectar(texto);

        PadraoEdital padrao = (padraoInformado != null)
                ? padraoInformado
                : detectorPadrao.detectar(texto);

        ExtratorEdital extrator = factory.criar(banca, padrao);

        log.info("Extraindo edital | banca={} | padrão={} | extrator={}",
                banca, padrao, extrator.getClass().getSimpleName());

        return extrator.extrair(texto);
    }

    public EstruturaEditalDTO extrair(String texto) {
        return extrair(texto, null, null);
    }
}
