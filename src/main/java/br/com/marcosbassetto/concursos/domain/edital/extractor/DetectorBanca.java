package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectorBanca {

    private static final int JANELA_CABECALHO = 3000;

    public Banca detectar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Banca.DESCONHECIDA;
        }

        String cabecalho = texto.length() > JANELA_CABECALHO
                ? texto.substring(0, JANELA_CABECALHO)
                : texto;

        Banca banca = detectarPorAssinatura(cabecalho.toLowerCase());
        log.debug("Detecção de banca | resultado={}", banca);
        return banca;
    }

    private Banca detectarPorAssinatura(String c) {
        if (c.contains("cebraspe") || c.contains("cespe")) {
            return Banca.CEBRASPE;
        }
        if (c.contains("fundação carlos chagas")
                || c.contains("fundacao carlos chagas")) {
            return Banca.FCC;
        }
        if (c.contains("fundação getulio vargas")
                || c.contains("fundação getúlio vargas")) {
            return Banca.FGV;
        }
        if (c.contains("vunesp")) {
            return Banca.VUNESP;
        }
        if (c.contains("unesp")) {
            return Banca.UNESP;
        }
        return Banca.DESCONHECIDA;
    }
}
