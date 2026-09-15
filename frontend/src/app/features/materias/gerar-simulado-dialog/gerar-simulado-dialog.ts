import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CriarSimuladoRequest, Simulado } from '../../../core/models/simulado.model';
import { SimuladoService } from '../../../core/services/simulado.service';

export interface GerarSimuladoDialogData {
  concursoId: number;
  materiaId: number;
  materiaNome: string;
}

@Component({
  selector: 'app-gerar-simulado-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatRadioModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './gerar-simulado-dialog.html',
  styleUrls: ['./gerar-simulado-dialog.scss'],
})
export class GerarSimuladoDialogComponent {
  quantidadeQuestoes: number = 20;
  enviando = false;

  readonly opcoes = [10, 20, 30, 40];

  constructor(
    public dialogRef: MatDialogRef<GerarSimuladoDialogComponent, Simulado | undefined>,
    @Inject(MAT_DIALOG_DATA) public data: GerarSimuladoDialogData,
    private simuladoService: SimuladoService,
    private snackBar: MatSnackBar,
  ) {}

  cancelar(): void {
    this.dialogRef.close(undefined);
  }

  gerar(): void {
    if (this.enviando) return;
    this.enviando = true;

    const request: CriarSimuladoRequest = {
      concursoId: this.data.concursoId,
      materiaId: this.data.materiaId,
      quantidadeQuestoes: this.quantidadeQuestoes,
    };

    this.simuladoService.criarSimulado(request).subscribe({
      next: (simulado) => {
        this.enviando = false;
        this.dialogRef.close(simulado);
      },
      error: (err) => {
        this.enviando = false;
        console.error('Erro ao criar simulado', err);
        const msg =
          err?.status === 403
            ? 'Sem permissão para gerar simulado.'
            : err?.status === 503
            ? 'Serviço de IA indisponível. Tente novamente.'
            : 'Não foi possível gerar o simulado.';
        this.snackBar.open(msg, 'Fechar', { duration: 4000 });
      },
    });
  }
}
