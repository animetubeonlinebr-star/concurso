import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Concurso } from '../../../core/models/concurso.model';
import { ConcursoService } from '../../../core/services/concurso.service';

@Component({
  selector: 'app-lista-concursos',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './lista-concursos.html',
  styleUrls: ['./lista-concursos.scss'],
})
export class ListaConcursosComponent implements OnInit {
  concursos: Concurso[] = [];
  carregando = true;

  constructor(
    private concursoService: ConcursoService,
    private router: Router,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  private carregar(): void {
    this.carregando = true;

    this.concursoService.listarConcursos().subscribe({
      next: (resposta: any) => {
        console.log('📦 RESPOSTA do backend:', resposta);
        console.log('É array?', Array.isArray(resposta));

        let lista: Concurso[] = [];

        if (Array.isArray(resposta)) {
          lista = resposta;
        } else if (resposta && Array.isArray(resposta.content)) {
          lista = resposta.content;
        } else if (resposta && Array.isArray(resposta.data)) {
          lista = resposta.data;
        } else {
          console.warn('⚠️ Formato inesperado:', resposta);
        }

        console.log(`✅ ${lista.length} concursos recebidos`);
        this.concursos = lista;

        console.log('🔽 carregando ANTES:', this.carregando);
        this.carregando = false;
        console.log('🔽 carregando DEPOIS:', this.carregando);

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Erro ao listar concursos:', err);
        this.carregando = false;
        this.cdr.detectChanges();
        this.snackBar.open('Não foi possível carregar os concursos.', 'Fechar', {
          duration: 3500,
        });
      },
    });
  }
abrirConcurso(concurso: Concurso): void {
  this.router.navigate(['/concursos', concurso.id]);
}


  novoConcurso(): void {
    this.router.navigate(['/novo-concurso']);
  }
}
