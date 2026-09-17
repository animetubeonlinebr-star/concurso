package br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;

import java.util.List;

/**
 * Estrutura interpretada, mas ainda não normalizada nem validada.
 *
 * É o contrato entre as três fases que o usuário descreveu: a estratégia
 * decide o que extrair, produz um rascunho, e só depois normalização e
 * validação decidem se ele está bom o bastante para ir à revisão.
 */
public record EstruturaRascunho(
        EditalProfile perfil,
        String perfilCodigo,
        List<MateriaRascunho> materias,
        String mensagem
) {

    public EstruturaRascunho {
        if (materias == null) materias = List.of();
        materias = List.copyOf(materias);
        if (perfilCodigo == null && perfil != null) perfilCodigo = perfil.codigo();
    }

    public static EstruturaRascunho vazia(EditalProfile perfil, String mensagem) {
        return new EstruturaRascunho(perfil, perfil == null ? null : perfil.codigo(),
                List.of(), mensagem);
    }

    public int totalTopicos() {
        return materias.stream().mapToInt(m -> m.topicos().size()).sum();
    }

    public boolean vazia() {
        return materias.isEmpty();
    }
}