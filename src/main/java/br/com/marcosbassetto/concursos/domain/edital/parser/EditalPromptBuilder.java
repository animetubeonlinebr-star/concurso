package br.com.marcosbassetto.concursos.domain.edital.parser;

import org.springframework.stereotype.Component;

@Component
public class EditalPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            Você é um extrator de dados estruturados de editais de concurso público.
            
            REGRAS OBRIGATÓRIAS:
            1. Devolva SEMPRE e SOMENTE um JSON válido.
            2. NUNCA escreva texto antes ou depois do JSON.
            3. NUNCA invente matérias ou tópicos que não estejam no texto.
            4. Se houver múltiplos cargos/ênfases, cada um vira um item em "cargos".
            5. Se houver apenas um cargo (implícito), crie um único item com nome genérico.
            6. Tópicos devem ser atômicos — quebre listas longas em itens separados.
            7. Remova numeração ("1.", "a)", "•") dos nomes de matérias e tópicos.
            8. Preserve acentuação e capitalização originais.
            
            SCHEMA OBRIGATÓRIO:
            {
              "cargos": [
                {
                  "nome": "string",
                  "codigo": "string ou null",
                  "escolaridade": "string ou null",
                  "materias": [
                    {
                      "nome": "string",
                      "topicos": ["string", "string"]
                    }
                  ]
                }
              ]
            }
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }


    public String buildUserPrompt(String trechoConteudoProgramatico) {
        return """
                Extraia a estrutura de cargos, matérias e tópicos do trecho de edital abaixo.
                
                Devolva APENAS o JSON. Nada mais.
                
                <<<EDITAL>>>
                %s
                <<<FIM_EDITAL>>>
                """.formatted(trechoConteudoProgramatico);
    }
}
