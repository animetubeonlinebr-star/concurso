package br.com.marcosbassetto.concursos.domain.edital.parser;

import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚠️ Parser da ERA-IA. A extração agora é 100% determinística via
 * Template Method + Strategy, então este parser está fora do fluxo
 * principal. Mantido apenas para compatibilidade até ser removido
 * junto com SegmentadorEdital, EditalPromptBuilder e o pipeline antigo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EditalResponseParser {

    private final ObjectMapper objectMapper;

    public List<CursoExtraido> parse(String respostaIA) {
        if (respostaIA == null || respostaIA.isBlank()) {
            log.warn("Resposta da IA vazia");
            return List.of();
        }

        String json = limparCercasMarkdown(respostaIA);

        try {
            JsonNode raiz = objectMapper.readTree(json);

            if (raiz == null || !raiz.has("cargos") || !raiz.get("cargos").isArray()) {
                log.warn("JSON da IA sem campo 'cargos' válido");
                return List.of();
            }

            List<CursoExtraido> cargos = new ArrayList<>();

            for (JsonNode cargoNode : raiz.get("cargos")) {
                CursoExtraido cargo = parseCargo(cargoNode);
                if (cargo != null && !cargo.materias().isEmpty()) {
                    cargos.add(cargo);
                }
            }

            log.info("IA retornou {} cargos válidos", cargos.size());
            return cargos;

        } catch (Exception e) {
            log.error("Falha ao parsear JSON da IA: {}", e.getMessage());
            log.debug("JSON problemático: {}", json);
            return List.of();
        }
    }

    private CursoExtraido parseCargo(JsonNode node) {
        String nome = textoOuNull(node, "nome");
        if (nome == null || nome.isBlank()) {
            return null;
        }

        String codigo = textoOuNull(node, "codigo");
        // ⚠️ escolaridade foi removida do CursoExtraido — não usada no domínio.

        List<MateriaExtraida> materias = new ArrayList<>();
        if (node.has("materias") && node.get("materias").isArray()) {
            for (JsonNode m : node.get("materias")) {
                MateriaExtraida materia = parseMateria(m);
                if (materia != null) {
                    materias.add(materia);
                }
            }
        }

        return new CursoExtraido(nome.trim(), codigo, materias);
    }

    private MateriaExtraida parseMateria(JsonNode node) {
        String nome = textoOuNull(node, "nome");
        if (nome == null || nome.isBlank()) {
            return null;
        }

        List<TopicoExtraido> topicos = new ArrayList<>();
        if (node.has("topicos") && node.get("topicos").isArray()) {
            for (JsonNode t : node.get("topicos")) {
                TopicoExtraido topico = parseTopico(t);
                if (topico != null) {
                    topicos.add(topico);
                }
            }
        }

        return new MateriaExtraida(nome.trim(), topicos);
    }

    /**
     * Aceita 2 formatos de tópico:
     *   - String simples:      "Direitos Fundamentais"
     *   - Objeto estruturado:  { "nome": "...", "codigo": "1.2", "detalhe": "..." }
     */
    private TopicoExtraido parseTopico(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        // Formato 1: string simples
        if (node.isTextual()) {
            String texto = node.asText("").trim();
            return texto.isBlank() ? null : new TopicoExtraido(texto, null, null);
        }

        // Formato 2: objeto estruturado
        if (node.isObject()) {
            String nome = textoOuNull(node, "nome");
            if (nome == null || nome.isBlank()) {
                return null;
            }
            String codigo = textoOuNull(node, "codigo");
            String detalhe = textoOuNull(node, "detalhe");
            return new TopicoExtraido(nome.trim(), codigo, detalhe);
        }

        return null;
    }

    private String textoOuNull(JsonNode node, String campo) {
        JsonNode valor = node.get(campo);
        if (valor == null || valor.isNull()) return null;
        return valor.asText();
    }

    private String limparCercasMarkdown(String texto) {
        String limpo = texto.trim();
        if (limpo.startsWith("```")) {
            limpo = limpo.replaceFirst("^```(?:json)?\\s*", "");
            limpo = limpo.replaceFirst("\\s*```$", "");
        }
        return limpo.trim();
    }
}
