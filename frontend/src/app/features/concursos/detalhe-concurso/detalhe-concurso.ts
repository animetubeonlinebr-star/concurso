import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Concurso } from '../../../core/models/concurso.model';
import { Materia } from '../../../core/models/materia.model';
import { ConcursoService } from '../../../core/services/concurso.service';
import { MateriaService } from '../../../core/services/materia.service';

@Component({
  selector: 'app-detalhe-concurso',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './detalhe-concurso.html',
  styleUrls: ['./detalhe-concurso.scss'],
})
export class DetalheConcursoComponent implements OnInit {
  concurso: Concurso | null = null;
  materias: Materia[] = [];
  carregando = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private concursoService: ConcursoService,
    private materiaService: MateriaService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const concursoId = Number(this.route.snapshot.paramMap.get('concursoId'));
    if (!concursoId) {
      this.snackBar.open('Concurso inválido.', 'Fechar', { duration: 3000 });
      this.carregando = false;
      return;
    }
    this.carregar(concursoId);
  }

  private carregar(concursoId: number): void {
    this.carregando = true;

    this.concursoService.buscarConcurso(concursoId).subscribe({
      next: (c) => {
        this.concurso = c;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Erro ao buscar concurso', err);
        this.snackBar.open('Não foi possível carregar o concurso.', 'Fechar', {
          duration: 3500,
        });
        this.carregando = false;
        this.cdr.detectChanges();
      },
    });

    this.materiaService.listarMaterias(concursoId).subscribe({
      next: (resposta: any) => {
        let lista: Materia[] = [];
        if (Array.isArray(resposta)) lista = resposta;
        else if (resposta?.content) lista = resposta.content;
        else if (resposta?.data) lista = resposta.data;

        this.materias = lista;
        this.carregando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Erro ao listar matérias', err);
        this.snackBar.open('Não foi possível carregar as matérias.', 'Fechar', {
          duration: 3500,
        });
        this.carregando = false;
        this.cdr.detectChanges();
      },
    });
  }

  abrirMateria(materia: Materia): void {
    if (!this.concurso) return;
    this.router.navigate([
      '/concursos',
      this.concurso.id,
      'materias',
      materia.id,
    ]);
  }

  /** A árvore completa vive em /conteudo; aqui é só o atalho. */
  abrirConteudo(): void {
    if (!this.concurso) return;
    this.router.navigate(['/concursos', this.concurso.id, 'conteudo']);
  }

  voltar(): void {
    this.router.navigate(['/concursos']);
  }
}
