package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.MateriaSugeridaEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.TopicoSugeridoEntity;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.InterpretadorEditalFacade;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.edital.service.DetectorDuplicidade;
import br.com.marcosbassetto.concursos.domain.edital.service.NormalizadorEstrutura;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fronteira transacional do processamento de edital.
 *
 * Vive em bean separado de {@link ProcessarEditalUseCase} porque anotações
 * {@code @Transactional} (inclusive {@code REQUIRES_NEW} para gravar o erro)
 * não são aplicadas em chamadas de um método para outro dentro do mesmo
 * objeto: o proxy do Spring só intercepta chamadas vindas de fora.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EditalStagingService {

    private final ConcursoRepository concursoRepository;
    private final EditalImportacaoRepository importacaoRepository;
    private final MateriaSugeridaRepository materiaSugeridaRepository;
    private final InterpretadorEditalFacade interpretador;
    private final NormalizadorEstrutura normalizador;
    private final DetectorDuplicidade detectorDuplicidade;
    private final MateriaRepository materiaRepository;
    private final TopicoRepository topicoRepository;

    @Transactional
    public void processar(Long concursoId) {
        ConcursoEntity concurso = concursoRepository.findById(concursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", concursoId));

        EditalImportacaoEntity importacao = importacaoRepository.findByConcurso_Id(concursoId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.IMPORTACAO_NAO_ENCONTRADA,
                        "Nenhuma importação encontrada para o concurso " + concursoId + "."));

        marcarProcessando(concurso, importacao);

        String texto = concurso.getTextoExtraido();
        if (texto == null || texto.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "O texto do edital não está disponível para processamento.");
        }

        EstruturaEditalDTO estrutura = normalizador.normalizar(interpretador.interpretar(texto));
        List<MateriaExtraida> materias = estrutura.materiasDoPrimeiroCurso();

        // Reprocessar não pode acumular o staging do processamento anterior.
        materiaSugeridaRepository.deleteByImportacao_Id(importacao.getId());
        materiaSugeridaRepository.flush();

        gravarStaging(importacao, materias, concursoId);
        aplicarDadosDoConcurso(concurso, estrutura.dadosConcurso());

        // A qualidade da extração é registrada separadamente: sem isso, um
        // edital sem conteúdo programático viraria "sucesso" com zero matérias.
        StatusExtracao statusExtracao = estrutura.status() != null
                ? estrutura.status()
                : StatusExtracao.PROCESSADO;

        importacao.setStatusExtracao(statusExtracao);
        importacao.setExtraidoEm(LocalDateTime.now());
        importacao.setMensagemErro(null);
        importacao.setStatus(StatusProcessamento.AGUARDANDO_REVISAO);
        importacaoRepository.save(importacao);

        concurso.definirStatusProcessamento(StatusProcessamento.AGUARDANDO_REVISAO);
        concursoRepository.save(concurso);

        log.info("Edital processado | concursoId={} | materias={} | topicos={} | extracao={}",
                concursoId, materias.size(),
                materias.stream().mapToInt(m -> m.topicos().size()).sum(),
                statusExtracao);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarErro(Long concursoId, String mensagem) {
        importacaoRepository.findByConcurso_Id(concursoId).ifPresent(importacao -> {
            importacao.setStatus(StatusProcessamento.ERRO);
            importacao.setMensagemErro(mensagem);
            importacaoRepository.save(importacao);
        });

        concursoRepository.findById(concursoId).ifPresent(concurso -> {
            concurso.definirStatusProcessamento(StatusProcessamento.ERRO);
            concursoRepository.save(concurso);
        });
    }

    private void marcarProcessando(ConcursoEntity concurso, EditalImportacaoEntity importacao) {
        importacao.setStatus(StatusProcessamento.PROCESSANDO);
        importacao.setMensagemErro(null);
        importacaoRepository.save(importacao);

        concurso.definirStatusProcessamento(StatusProcessamento.PROCESSANDO);
        concursoRepository.save(concurso);
    }

    /**
     * Grava matérias/tópicos sugeridos marcando as possíveis duplicidades.
     * Nada é mesclado ou removido: as flags apenas orientam a revisão.
     */
    private void gravarStaging(EditalImportacaoEntity importacao,
                               List<MateriaExtraida> materias,
                               Long concursoId) {
        List<MateriaSugeridaEntity> persistidas = new ArrayList<>();

        int ordem = 1;
        for (MateriaExtraida m : materias) {
            MateriaSugeridaEntity materia = MateriaSugeridaEntity.builder()
                    .importacao(importacao)
                    .nome(m.nome())
                    .ordem(ordem++)
                    .selecionada(true)
                    .build();

            int ordemTopico = 1;
            for (TopicoExtraido t : m.topicos()) {
                materia.adicionarTopico(TopicoSugeridoEntity.builder()
                        .nome(t.nome())
                        .ordem(ordemTopico++)
                        .selecionado(true)
                        .build());
            }

            persistidas.add(materiaSugeridaRepository.save(materia));
        }

        sinalizarDuplicidades(persistidas);
        sinalizarColisaoComConteudoConfirmado(persistidas, concursoId);
        materiaSugeridaRepository.saveAll(persistidas);
    }

    private void sinalizarDuplicidades(List<MateriaSugeridaEntity> materias) {
        List<String> nomes = materias.stream().map(MateriaSugeridaEntity::getNome).toList();

        Map<Integer, Integer> referencias = detectorDuplicidade.detectar(nomes).stream()
                .collect(Collectors.toMap(
                        DetectorDuplicidade.OcorrenciaDuplicada::indiceDuplicado,
                        DetectorDuplicidade.OcorrenciaDuplicada::indiceReferencia,
                        // Mais de uma referência para o mesmo índice: fica a primeira.
                        (a, b) -> a));

        referencias.forEach((indice, referencia) -> {
            MateriaSugeridaEntity duplicada = materias.get(indice);
            duplicada.setPossivelDuplicidade(true);
            duplicada.setSimilarA(materias.get(referencia));
        });

        sinalizarTopicosDeCadaMateria(materias);

        log.debug("Staging de matérias | total={} | com duplicidade={}",
                materias.size(), referencias.size());
    }

    /**
     * Marca sugestões cujo nome já existe em materia/topico do concurso.
     *
     * Diferente da duplicidade interna: aqui não há o que mesclar no staging,
     * o item simplesmente já está gravado. Reimportar um edital já
     * confirmado cai exatamente neste caso.
     */
    private void sinalizarColisaoComConteudoConfirmado(
            List<MateriaSugeridaEntity> materias, Long concursoId) {

        Set<String> materiasGravadas = new HashSet<>(
                materiaRepository.findNomeNormalizadoByConcursoId(concursoId));

        Set<String> topicosGravados = new HashSet<>(
                topicoRepository.findNomeNormalizadoByConcursoId(concursoId));

        if (materiasGravadas.isEmpty() && topicosGravados.isEmpty()) {
            return;
        }

        int colisoes = 0;

        for (MateriaSugeridaEntity materia : materias) {
            if (materiasGravadas.contains(materia.getNomeNormalizado())) {
                materia.setJaExisteConfirmada(true);
                colisoes++;
            }

            for (TopicoSugeridoEntity topico : materia.getTopicos()) {
                if (topicosGravados.contains(topico.getNomeNormalizado())) {
                    topico.setJaExisteConfirmado(true);
                    colisoes++;
                }
            }
        }

        log.debug("Colisão com conteúdo confirmado | concursoId={} | itens={}",
                concursoId, colisoes);
    }

    private void sinalizarTopicosDeCadaMateria(List<MateriaSugeridaEntity> materias) {
        for (MateriaSugeridaEntity materia : materias) {
            List<TopicoSugeridoEntity> topicos = materia.getTopicos();
            List<String> nomes = topicos.stream().map(TopicoSugeridoEntity::getNome).toList();

            for (DetectorDuplicidade.OcorrenciaDuplicada ocorrencia
                    : detectorDuplicidade.detectar(nomes)) {
                TopicoSugeridoEntity duplicado = topicos.get(ocorrencia.indiceDuplicado());
                duplicado.setPossivelDuplicidade(true);
                duplicado.setSimilarA(topicos.get(ocorrencia.indiceReferencia()));
            }
        }
    }

    /**
     * Completa os dados cadastrais extraídos sem sobrescrever o que o
     * usuário já informou (o nome dado no upload, por exemplo).
     */
    private void aplicarDadosDoConcurso(ConcursoEntity concurso, DadosConcurso dados) {
        if (dados == null) return;

        if (concurso.getOrgao() == null && dados.orgao() != null) {
            concurso.setOrgao(truncar(dados.orgao(), 200));
        }
        if (concurso.getCargo() == null && dados.cargo() != null) {
            concurso.setCargo(truncar(dados.cargo(), 100));
        }
        if (concurso.getBanca() == null && dados.banca() != null) {
            concurso.setBanca(truncar(dados.banca(), 100));
        }
        if (concurso.getAno() == null && dados.ano() != null) {
            concurso.setAno(dados.ano());
        }
    }

    private String truncar(String valor, int max) {
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
