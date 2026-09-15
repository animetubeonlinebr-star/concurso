import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Materia } from '../models/materia.model';

@Injectable({
  providedIn: 'root'
})
export class MateriaService {
  private apiUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  listarMaterias(concursoId: number): Observable<Materia[]> {
    return this.http.get<Materia[]>(`${this.apiUrl}/concursos/${concursoId}/materias`);
  }

  buscarMateria(id: number): Observable<Materia> {
    return this.http.get<Materia>(`${this.apiUrl}/materias/${id}`);
  }
}
