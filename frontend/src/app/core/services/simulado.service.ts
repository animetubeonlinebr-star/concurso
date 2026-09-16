import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Simulado, SimuladoQuestao, CriarSimuladoRequest } from '../models/simulado.model';

@Injectable({
  providedIn: 'root'
})
export class SimuladoService {
  private apiUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  listarSimulados(): Observable<Simulado[]> {
    return this.http.get<Simulado[]>(`${this.apiUrl}/simulados`);
  }

  buscarSimulado(id: number): Observable<Simulado> {
    return this.http.get<Simulado>(`${this.apiUrl}/simulados/${id}`);
  }

  criarSimulado(request: CriarSimuladoRequest): Observable<Simulado> {
    return this.http.post<Simulado>(`${this.apiUrl}/simulados`, request);
  }

  iniciarSimulado(id: number): Observable<Simulado> {
    return this.http.post<Simulado>(`${this.apiUrl}/simulados/${id}/iniciar`, {});
  }

  finalizarSimulado(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/simulados/${id}/finalizar`, {});
  }

  buscarQuestoes(id: number): Observable<SimuladoQuestao[]> {
    return this.http.get<SimuladoQuestao[]>(`${this.apiUrl}/simulados/${id}/questoes`);
  }

  responderQuestao(simuladoId: number, simuladoQuestaoId: number, resposta: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/simulados/${simuladoId}/questoes/${simuladoQuestaoId}/resposta`, {
      alternativaSelecionada: resposta
    });
  }

  buscarResultado(id: number): Observable<any> {
    // O backend expõe o resultado agregado em GET /simulados/{id}/correcao.
    return this.http.get<any>(`${this.apiUrl}/simulados/${id}/correcao`);
  }

  corrigirSimulado(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/simulados/${id}/correcao`, {});
  }
}
