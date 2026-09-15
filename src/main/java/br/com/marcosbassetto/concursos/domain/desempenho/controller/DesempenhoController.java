package br.com.marcosbassetto.concursos.domain.desempenho.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/desempenho")
public class DesempenhoController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDesempenho() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalQuestoes", 0);
        response.put("questoesRespondidas", 0);
        response.put("questoesCorretas", 0);
        response.put("questoesIncorretas", 0);
        response.put("questoesNaoRespondidas", 0);
        response.put("percentualAcerto", 0.0);
        response.put("pontuacao", 0.0);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evolucao")
    public ResponseEntity<Map<String, Object>> getEvolucao() {
        // Dados mockados de evolução
        Map<String, Object> response = new HashMap<>();
        response.put("periodo", "Últimos 30 dias");
        response.put("evolucao", new Object[]{
                Map.of("data", "2026-09-01", "percentual", 65.0),
                Map.of("data", "2026-09-02", "percentual", 70.0),
                Map.of("data", "2026-09-03", "percentual", 68.0),
                Map.of("data", "2026-09-04", "percentual", 75.0)
        });
        return ResponseEntity.ok(response);
    }
}