package br.com.marcosbassetto.concursos.ia.gateway;

import br.com.marcosbassetto.concursos.ia.dto.IARequest;
import br.com.marcosbassetto.concursos.ia.dto.IAResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class FakeIAGateway implements IAGateway {

    @Override
    public IAResponse generate(IARequest request) {
        String respostaSimulada = """
                {
                  "questoes": [
                    {
                      "enunciado": "Qual é a capital do Brasil?",
                      "alternativas": ["São Paulo", "Brasília", "Rio de Janeiro"],
                      "respostaCorreta": "B"
                    }
                  ]
                }
                """;
        return new IAResponse(true, respostaSimulada, "fake-model", 0);
    }
}
