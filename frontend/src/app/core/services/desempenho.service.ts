import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Desempenho, DesempenhoMateria } from '../models/desempenho.model';

@Injectable({
  providedIn: 'root'
})
export class DesempenhoService {
  private apiUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  buscarDesempenhoGeral(): Observable<Desempenho> {
    return this.http.get<Desempenho>(`${this.apiUrl}/desempenho`);
  }

  buscarDesempenhoConcurso(concursoId: number): Observable<Desempenho> {
    return this.http.get<Desempenho>(`${this.apiUrl}/desempenho/concursos/${concursoId}`);
  }

  buscarDesempenhoMaterias(concursoId: number): Observable<DesempenhoMateria[]> {
    return this.http.get<DesempenhoMateria[]>(`${this.apiUrl}/desempenho/concursos/${concursoId}/materias`);
  }

  buscarEvolucao(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/desempenho/evolucao`);
  }
}
