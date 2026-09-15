package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class DetectorPadraoEdital {

    private static final Pattern CABECALHO_DISCIPLINA = Pattern.compile(
            "^\\s*(?:\\|\\s*[A-ZÁÉÍÓÚÂÊÔÃÕÇ][^|]{3,80}\\s*\\||"
                    + "##\\s*[A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\\n]{3,80}|"
                    + "\\*\\*[A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\\n]{3,80}\\*\\*)",
            Pattern.MULTILINE);

    private static final Pattern CABECALHO_ESTRUTURAL = Pattern.compile(
            "^(CAPÍTULO|CAPITULO|CLÁUSULA|SEÇÃO|ANEXO|[0-9]{1,2}\\.)\\s+[A-ZÁÀÂÃÉÈÊÍÏÓÔÕÖÚÇÑ\\s]{4,}",
            Pattern.MULTILINE);

    private static final Pattern NUMERAL_ROMANO = Pattern.compile(
            "^\\s*[IVXLCDM]+\\.\\s+\\S", Pattern.MULTILINE);

    private static final Pattern BULLET = Pattern.compile(
            "^\\s*[▪•\\-*]\\s+\\S", Pattern.MULTILINE);

    public PadraoEdital detectar(String texto) {
        if (texto == null || texto.isBlank()) {
            return PadraoEdital.DESCONHECIDO;
        }

        long cabecalhosDisciplina = contar(CABECALHO_DISCIPLINA, texto);
        long cabecalhosEstruturais = contar(CABECALHO_ESTRUTURAL, texto);
        long totalCabecalhos = cabecalhosDisciplina + cabecalhosEstruturais;

        long romanos = contar(NUMERAL_ROMANO, texto);
        long bullets = contar(BULLET, texto);

        PadraoEdital padrao = classificar(totalCabecalhos, romanos, bullets);

        log.debug("Detecção de padrão | cabecalhos={} (disc: {}, est: {}) romanos={} bullets={} → {}",
                totalCabecalhos, cabecalhosDisciplina, cabecalhosEstruturais, romanos, bullets, padrao);

        return padrao;
    }

    private PadraoEdital classificar(long cabecalhos, long romanos, long bullets) {
        if (cabecalhos >= 2 && romanos >= 2) {
            return PadraoEdital.MULTI_NIVEL;
        }
        if (cabecalhos >= 2 && bullets >= 5) {
            return PadraoEdital.DISCIPLINA_CABECALHO;
        }
        if (cabecalhos >= 5) {
            return PadraoEdital.MULTI_NIVEL;
        }
        if (bullets >= 5 && cabecalhos < 2) {
            return PadraoEdital.LISTA_SIMPLES;
        }
        if (romanos >= 3 && cabecalhos < 2) {
            return PadraoEdital.MULTI_NIVEL;
        }
        return PadraoEdital.DESCONHECIDO;
    }

    private long contar(Pattern p, String texto) {
        var m = p.matcher(texto);
        long total = 0;
        while (m.find()) total++;
        return total;
    }
}
