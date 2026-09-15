import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Materia } from '../../../core/models/materia.model';
import { Simulado } from '../../../core/models/simulado.model';
import { MateriaService } from '../../../core/services/materia.service';
import { SimuladoService } from '../../../core/services/simulado.service';
import { GerarSimuladoDialogComponent } from '../gerar-simulado-dialog/gerar-simulado-dialog';

@Component({
  selector: 'app-materia-detalhe',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './materia-detalhe.html',
  styleUrls: ['./materia-detalhe.scss'],
})
export class MateriaDetalheComponent implements OnInit {
  materia: Materia | null = null;
  simuladosRecentes: Simulado[] = [];
  carregando = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private materiaService: MateriaService,
    private simuladoService: SimuladoService,
  ) {}

  ngOnInit(): void {
    const materiaId = Number(this.route.snapshot.paramMap.get('materiaId'));
    if (!materiaId) {
      this.snackBar.open('Matéria inválida.', 'Fechar', { duration: 3000 });
      this.carregando = false;
      return;
    }
    this.carregar(materiaId);
  }

  private carregar(materiaId: number): void {
    this.carregando = true;

    this.materiaService.buscarMateria(materiaId).subscribe({
      next: (m) => {
        this.materia = m;
        this.carregando = false;
      },
      error: (err) => {
        console.error('Erro ao carregar matéria', err);
        this.snackBar.open('Não foi possível carregar a matéria.', 'Fechar', {
          duration: 3500,
        });
        this.carregando = false;
      },
    });
  }

  gerarSimulado(): void {
    if (!this.materia) return;

    const ref = this.dialog.open(GerarSimuladoDialogComponent, {
      width: '440px',
      maxWidth: '95vw',
      data: {
        concursoId: this.materia.concursoId,
        materiaId: this.materia.id,
        materiaNome: this.materia.nome,
      },
    });

    ref.afterClosed().subscribe((criado: Simulado | undefined) => {
      if (criado) {
        this.snackBar.open('Simulado criado!', 'Fechar', { duration: 2500 });
        this.router.navigate(['/simulados']);
      }
    });
  }

  voltar(): void {
    this.router.navigate(['/dashboard']);
  }
}
