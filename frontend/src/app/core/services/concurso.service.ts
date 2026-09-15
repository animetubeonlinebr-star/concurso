import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Concurso, CriarConcursoRequest } from '../models/concurso.model';

@Injectable({
  providedIn: 'root'
})
export class ConcursoService {
  private apiUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  listarConcursos(): Observable<Concurso[]> {
    return this.http.get<Concurso[]>(`${this.apiUrl}/concursos`);
  }

  buscarConcurso(id: number): Observable<Concurso> {
    return this.http.get<Concurso>(`${this.apiUrl}/concursos/${id}`);
  }

  criarConcurso(request: CriarConcursoRequest): Observable<Concurso> {
    return this.http.post<Concurso>(`${this.apiUrl}/concursos`, request);
  }

  atualizarConcurso(id: number, request: Partial<Concurso>): Observable<Concurso> {
    return this.http.put<Concurso>(`${this.apiUrl}/concursos/${id}`, request);
  }

  excluirConcurso(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/concursos/${id}`);
  }
}
