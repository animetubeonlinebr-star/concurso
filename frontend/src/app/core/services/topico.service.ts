import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Topico } from '../models/topico.model';

@Injectable({
  providedIn: 'root'
})
export class TopicoService {
  private apiUrl = '/api/v1/materias';

  constructor(private http: HttpClient) {}

  listarPorMateria(materiaId: number): Observable<Topico[]> {
    return this.http.get<Topico[]>(`${this.apiUrl}/${materiaId}/topicos`);
  }
}
