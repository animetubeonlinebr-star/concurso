import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  email: string = '';
  senha: string = '';
  carregando: boolean = false;
  erro: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    this.carregando = true;
    this.erro = '';

    this.authService.login({ email: this.email, senha: this.senha }).subscribe({
      next: (response) => {
        this.authService.setToken(response.token);
        this.authService.setUsuario(response.usuario);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.erro = 'Usuário ou senha inválidos';
        this.carregando = false;
        console.error('Erro no login:', err);
      },
      complete: () => {
        this.carregando = false;
      }
    });
  }

  cadastrar() {
    this.router.navigate(['/cadastro']);
  }
}
