import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ConfirmacaoEstruturaResponse,
  ConteudoConcurso,
  IniciarImportacaoResponse,
  RevisaoEstrutura,
  RevisaoEstruturaRequest,
  StatusProcessamentoResponse,
} from '../models/edital-importacao.model';

/**
 * Fluxo persistido de importação do edital: upload, acompanhamento,
 * revisão, confirmação e leitura do conteúdo consolidado.
 */
@Injectable({ providedIn: 'root' })
export class EditalImportacaoService {

  private apiUrl = '/api/v1/concursos';

  constructor(private http: HttpClient) {}

  importar(arquivo: File): Observable<IniciarImportacaoResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    return this.http.post<IniciarImportacaoResponse>(
      `${this.apiUrl}/importar`, formData);
  }

  buscarStatus(concursoId: number): Observable<StatusProcessamentoResponse> {
    return this.http.get<StatusProcessamentoResponse>(
      `${this.apiUrl}/${concursoId}/processamento`);
  }

  reprocessar(concursoId: number): Observable<StatusProcessamentoResponse> {
    return this.http.post<StatusProcessamentoResponse>(
      `${this.apiUrl}/${concursoId}/reprocessar`, {});
  }

  buscarRevisao(concursoId: number): Observable<RevisaoEstrutura> {
    return this.http.get<RevisaoEstrutura>(
      `${this.apiUrl}/${concursoId}/revisao`);
  }

  atualizarRevisao(
    concursoId: number,
    request: RevisaoEstruturaRequest,
  ): Observable<RevisaoEstrutura> {
    return this.http.put<RevisaoEstrutura>(
      `${this.apiUrl}/${concursoId}/revisao`, request);
  }

  confirmar(concursoId: number): Observable<ConfirmacaoEstruturaResponse> {
    return this.http.post<ConfirmacaoEstruturaResponse>(
      `${this.apiUrl}/${concursoId}/confirmar`, {});
  }

  buscarConteudo(concursoId: number): Observable<ConteudoConcurso> {
    return this.http.get<ConteudoConcurso>(
      `${this.apiUrl}/${concursoId}/conteudo`);
  }
}
