import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LoginRequest, LoginResponse } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials);
  }

  register(data: {
    nome: string;
    email: string;
    senha: string;
    confirmacaoSenha: string;
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, data);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  getUsuario(): any {
    const usuario = localStorage.getItem('usuario');
    return usuario && usuario !== 'undefined' ? JSON.parse(usuario) : null;
  }

  setUsuario(usuario: any): void {
    if (usuario !== undefined && usuario !== null) {
      localStorage.setItem('usuario', JSON.stringify(usuario));
    }
  }


  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;

    if (this.isTokenExpired(token)) {
      console.warn('🔒 Token expirado – limpando sessão');
      this.logout();
      return false;
    }

    return true;
  }


  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (!payload.exp) return false; // sem exp = considera válido

      const agoraEmSegundos = Math.floor(Date.now() / 1000);
      return payload.exp < agoraEmSegundos;
    } catch (e) {
      console.error('Erro ao decodificar token:', e);
      return true; 
    }
  }
}
