package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class ExtratorEditalBase implements ExtratorEdital {

    protected final TextoPreProcessador preProcessador;

    @Override
    public final EstruturaEditalDTO extrair(String texto) {
        String limpo = preProcessador.limpar(texto);
        DadosConcurso dados = extrairDadosConcurso(limpo);
        List<CursoExtraido> cursos = extrairCursos(limpo);
        return new EstruturaEditalDTO(dados, List.of(), cursos);
    }

    protected List<CursoExtraido> extrairCursos(String texto) {
        List<MateriaExtraida> materias = extrairMaterias(texto);
        List<MateriaExtraida> enriquecidas = enriquecerComTopicos(materias, texto);
        return List.of(new CursoExtraido("Geral", null, enriquecidas));
    }

    protected abstract DadosConcurso extrairDadosConcurso(String texto);

    protected abstract List<MateriaExtraida> extrairMaterias(String texto);

    protected abstract List<TopicoExtraido> extrairTopicos(String blocoMateria);

    protected List<MateriaExtraida> enriquecerComTopicos(List<MateriaExtraida> materias,
                                                         String texto) {
        return materias.stream()
                .map(m -> {
                    String bloco = extrairBlocoMateria(texto, m.nome());
                    return new MateriaExtraida(m.nome(), extrairTopicos(bloco));
                })
                .toList();
    }

    protected String extrairBlocoMateria(String texto, String nomeMateria) {
        return texto;
    }
}
