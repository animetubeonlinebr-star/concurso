package br.com.marcosbassetto.concursos.ia.gateway;

import br.com.marcosbassetto.concursos.ia.dto.IARequest;
import br.com.marcosbassetto.concursos.ia.dto.IAResponse;


public interface IAGateway {

    IAResponse generate(IARequest request);
}
