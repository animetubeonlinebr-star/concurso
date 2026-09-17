package br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seleciona a estratégia adequada ao perfil do edital (Factory Method).
 *
 * Existe para que a escolha de "como interpretar" seja um ponto único e
 * explícito. Antes essa decisão estava diluída em uma cadeia de
 * {@code if} dentro da factory de extratores, que escolhia por banca e
 * padrão textual em vez de por organização do documento.
 *
 * As estratégias são injetadas pelo próprio Spring: incluir um formato novo
 * é criar a classe e anotá-la, sem editar esta factory.
 */
@Slf4j
@Component
public class EstrategiaFactory {

    private static final String PERFIL_PADRAO = "GENERICO";

    private final List<EstrategiaInterpretacaoEdital> estrategias;

    public EstrategiaFactory(List<EstrategiaInterpretacaoEdital> estrategias) {
        validarPerfisUnicos(estrategias);
        this.estrategias = List.copyOf(estrategias);
        validarPerfilPadrao(this.estrategias);

        log.info("Estratégias de interpretação registradas | perfis={}",
                estrategias.stream().map(EstrategiaInterpretacaoEdital::perfilSuportado).toList());
    }

    public EstrategiaInterpretacaoEdital criar(String perfilCodigo) {
        return estrategias.stream()
                .filter(e -> e.suporta(perfilCodigo))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Perfil sem estratégia dedicada | perfil={} | usando {}",
                            perfilCodigo, PERFIL_PADRAO);
                    return porPerfilPadrao();
                });
    }

    private EstrategiaInterpretacaoEdital porPerfilPadrao() {
        return estrategias.stream()
                .filter(e -> PERFIL_PADRAO.equals(e.perfilSuportado()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhuma estratégia registrada para o perfil " + PERFIL_PADRAO));
    }

    /**
     * Dois registros para o mesmo perfil seriam resolvidos por ordem de
     * injeção — imprevisível. Falhar na subida é melhor que escolher a
     * estratégia errada em produção.
     */
    private void validarPerfisUnicos(List<EstrategiaInterpretacaoEdital> estrategias) {
        Set<String> vistos = new HashSet<>();

        for (EstrategiaInterpretacaoEdital e : estrategias) {
            if (!vistos.add(e.perfilSuportado())) {
                throw new IllegalStateException(
                        "Mais de uma estratégia declara o perfil " + e.perfilSuportado());
            }
        }
    }

    /**
     * A estratégia padrão é o destino de todo perfil sem dedicada. Sem ela, a
     * primeira extração de um perfil novo falharia em produção, em vez de na
     * subida da aplicação.
     */
    private void validarPerfilPadrao(List<EstrategiaInterpretacaoEdital> estrategias) {
        boolean temPadrao = estrategias.stream()
                .anyMatch(e -> PERFIL_PADRAO.equals(e.perfilSuportado()));

        if (!temPadrao) {
            throw new IllegalStateException(
                    "Nenhuma estratégia registrada para o perfil padrão " + PERFIL_PADRAO);
        }
    }
}