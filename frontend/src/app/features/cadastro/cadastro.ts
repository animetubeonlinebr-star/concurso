import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './cadastro.html',
  styleUrls: ['./cadastro.scss']
})
export class CadastroComponent {
  nome: string = '';
  email: string = '';
  senha: string = '';
  confirmacaoSenha: string = '';
  carregando: boolean = false;
  erro: string = '';
  sucesso: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    // Validações básicas
    if (!this.nome || !this.email || !this.senha || !this.confirmacaoSenha) {
      this.erro = 'Todos os campos são obrigatórios.';
      return;
    }

    if (this.senha !== this.confirmacaoSenha) {
      this.erro = 'As senhas não coincidem.';
      return;
    }

    if (this.senha.length < 6) {
      this.erro = 'A senha deve ter pelo menos 6 caracteres.';
      return;
    }

    this.carregando = true;
    this.erro = '';

    this.authService.register({
      nome: this.nome,
      email: this.email,
      senha: this.senha,
      confirmacaoSenha: this.confirmacaoSenha
    }).subscribe({
      next: (response) => {
        this.sucesso = true;
        this.carregando = false;
        // Redireciona para o login após 2 segundos
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.erro = err.error?.message || 'Erro ao cadastrar. Tente novamente.';
        this.carregando = false;
        console.error('Erro no cadastro:', err);
      }
    });
  }

  irParaLogin() {
    this.router.navigate(['/login']);
  }
}