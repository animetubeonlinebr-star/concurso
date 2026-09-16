import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { EditalImportacaoService } from '../../../core/services/edital-importacao.service';
import {
  ConfirmacaoEstruturaResponse,
  MateriaSugerida,
  RevisaoEstrutura,
  RevisaoEstruturaRequest,
  TopicoSugerido,
} from '../../../core/models/edital-importacao.model';

/**
 * Revisão da estrutura extraída antes da persistência definitiva.
 *
 * A tela edita o staging, então o usuário pode voltar e ajustar quantas
 * vezes quiser: nada vai para materia/topico até o botão Confirmar.
 */
@Component({
  selector: 'app-revisao',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './revisao.html',
  styleUrls: ['./revisao.scss'],
})
export class RevisaoComponent implements OnInit {

  concursoId!: number;
  revisao: RevisaoEstrutura | null = null;
  carregando = true;
  salvando = false;
  confirmando = false;
  erro: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private editalService: EditalImportacaoService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.concursoId = Number(this.route.snapshot.paramMap.get('concursoId'));

    if (!this.concursoId) {
      this.erro = 'Concurso inválido.';
      this.carregando = false;
      return;
    }

    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;

    this.editalService.buscarRevisao(this.concursoId).subscribe({
      next: (revisao) => {
        this.revisao = revisao;
        this.carregando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.erro = err?.error?.message
          ?? 'Não foi possível carregar a revisão.';
        this.carregando = false;
        this.cdr.detectChanges();
      },
    });
  }

  // ===== Edição no staging =====

  get materias(): MateriaSugerida[] {
    return this.revisao?.materias ?? [];
  }

  get selecionadas(): MateriaSugerida[] {
    return this.materias.filter((m) => m.selecionada);
  }

  toggleMateria(materia: MateriaSugerida): void {
    materia.selecionada = !materia.selecionada;
  }

  toggleTopico(topico: TopicoSugerido): void {
    topico.selecionado = !topico.selecionado;
  }

  removerMateria(materia: MateriaSugerida): void {
    if (this.revisao) {
      this.revisao.materias = this.materias.filter((m) => m.id !== materia.id);
    }
  }

  removerTopico(materia: MateriaSugerida, topico: TopicoSugerido): void {
    materia.topicos = materia.topicos.filter((t) => t.id !== topico.id);
  }

  /**
   * Mesclagem só acontece porque o usuário escolheu o destino: o backend
   * nunca junta duas matérias por conta própria.
   */
  mesclar(materia: MateriaSugerida): void {
    const alvoId = Number(materia.similarAId ?? 0);
    const destino = this.materias.find((m) => m.id === alvoId);

    if (!destino) {
      this.snackBar.open(
        'A matéria sugerida para mesclagem não está mais na lista.',
        'Fechar', { duration: 4000 });
      return;
    }

    const confirmado = confirm(
      `Mesclar "${materia.nome}" em "${destino.nome}"?\n\n`
      + 'Os tópicos serão movidos para a matéria de destino.');

    if (!confirmado) return;

    this.salvando = true;

    const request = this.montarRequest(this.materias.map((m) => {
      if (m.id === materia.id) {
        return { materia: m, mesclarEmId: destino.id };
      }
      return { materia: m, mesclarEmId: null };
    }));

    this.editalService.atualizarRevisao(this.concursoId, request).subscribe({
      next: (revisao) => {
        this.revisao = revisao;
        this.salvando = false;
        this.snackBar.open('Matérias mescladas.', 'Fechar', { duration: 3000 });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.salvando = false;
        this.snackBar.open(
          err?.error?.message ?? 'Não foi possível mesclar as matérias.',
          'Fechar', { duration: 4500 });
        this.cdr.detectChanges();
      },
    });
  }

  // ===== Confirmação =====

  confirmar(): void {
    if (this.confirmando || this.salvando) return;

    if (this.selecionadas.length === 0) {
      this.snackBar.open(
        'Selecione ao menos uma matéria para confirmar.',
        'Fechar', { duration: 4000 });
      return;
    }

    this.confirmando = true;

    // Persiste as edições pendentes antes de promover o staging: confirmar
    // direto gravaria os nomes/flags antigos.
    this.editalService.atualizarRevisao(this.concursoId, this.montarRequest(
      this.materias.map((m) => ({ materia: m, mesclarEmId: null })),
    )).subscribe({
      next: () => this.promover(),
      error: (err) => {
        this.confirmando = false;
        this.snackBar.open(
          err?.error?.message ?? 'Não foi possível salvar a revisão.',
          'Fechar', { duration: 4500 });
        this.cdr.detectChanges();
      },
    });
  }

  private promover(): void {
    this.editalService.confirmar(this.concursoId).subscribe({
      next: (resposta: ConfirmacaoEstruturaResponse) => {
        this.confirmando = false;
        this.snackBar.open(
          `${resposta.materiasPersistidas} matérias e `
          + `${resposta.topicosPersistidos} tópicos salvos.`,
          'Fechar', { duration: 4000 });
        this.router.navigate(['/concursos', this.concursoId, 'conteudo']);
      },
      error: (err) => {
        this.confirmando = false;
        this.erro = err?.error?.message ?? 'Não foi possível confirmar a estrutura.';
        this.cdr.detectChanges();
      },
    });
  }

  cancelar(): void {
    this.router.navigate(['/concursos', this.concursoId]);
  }

  private montarRequest(
    itens: { materia: MateriaSugerida; mesclarEmId: number | null }[],
  ): RevisaoEstruturaRequest {
    return {
      materias: itens.map(({ materia, mesclarEmId }) => ({
        id: materia.id,
        nome: materia.nome,
        selecionada: materia.selecionada,
        mesclarEmId,
        remover: false,
        topicos: materia.topicos.map((t) => ({
          id: t.id,
          nome: t.nome,
          selecionado: t.selecionado,
          remover: false,
        })),
      })),
    };
  }

  compararPorOrdem(a: MateriaSugerida, b: MateriaSugerida): number {
    return (a.ordem ?? 0) - (b.ordem ?? 0);
  }

  exibirTopico(topico: TopicoSugerido): string {
    return topico.nome;
  }
}
