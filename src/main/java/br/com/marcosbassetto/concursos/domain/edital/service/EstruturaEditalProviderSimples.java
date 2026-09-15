package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ⚠️ Mock de desenvolvimento — está na lista para ser removido
 * após a migração completa para ExtratorEditalTemplate + Strategy.
 *
 * Mantido apenas para compatibilidade até o ExtratorEditalFactory
 * estar plugado no ConcursoUploadController.
 */
@Component
public class EstruturaEditalProviderSimples implements EstruturaEditalProvider {

    private static final String CARGO_GENERICO = "Cargo Exemplo";

    @Override
    public EstruturaEditalDTO extrairEstrutura(String texto) {

        DadosConcurso dados = new DadosConcurso(
                "Concurso Exemplo",
                "Órgão Exemplo",
                "Cargo Exemplo",
                "Banca Exemplo",
                2026
        );

        List<MateriaExtraida> materias = List.of(
                new MateriaExtraida("Língua Portuguesa", List.of(
                        new TopicoExtraido("Interpretação de Texto", null, null),
                        new TopicoExtraido("Concordância", null, null),
                        new TopicoExtraido("Regência", null, null),
                        new TopicoExtraido("Pontuação", null, null)
                )),
                new MateriaExtraida("Direito Constitucional", List.of(
                        new TopicoExtraido("Princípios Fundamentais", null, null),
                        new TopicoExtraido("Direitos Fundamentais", null, null),
                        new TopicoExtraido("Organização do Estado", null, null)
                )),
                new MateriaExtraida("Informática", List.of(
                        new TopicoExtraido("Sistemas Operacionais", null, null),
                        new TopicoExtraido("Redes", null, null),
                        new TopicoExtraido("Segurança da Informação", null, null)
                )),
                new MateriaExtraida("Direito Administrativo", List.of(
                        new TopicoExtraido("Atos Administrativos", null, null),
                        new TopicoExtraido("Poderes Administrativos", null, null)
                ))
        );

        List<CursoExtraido> cargos = List.of(
                new CursoExtraido(CARGO_GENERICO, null, materias)
        );

        return new EstruturaEditalDTO(dados, materias, cargos);
    }
}
