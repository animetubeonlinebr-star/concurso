import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { EditalImportacaoService } from '../../../core/services/edital-importacao.service';
import { ConteudoConcurso } from '../../../core/models/edital-importacao.model';

/**
 * Árvore final do concurso: matérias confirmadas com seus tópicos.
 * Somente leitura — a edição acontece na revisão, antes da confirmação.
 */
@Component({
  selector: 'app-conteudo',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './conteudo.html',
  styleUrls: ['./conteudo.scss'],
})
export class ConteudoComponent implements OnInit {

  concursoId!: number;
  conteudo: ConteudoConcurso | null = null;
  carregando = true;
  erro: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private editalService: EditalImportacaoService,
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

    this.editalService.buscarConteudo(this.concursoId).subscribe({
      next: (conteudo) => {
        this.conteudo = conteudo;
        this.carregando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.erro = err?.error?.message
          ?? 'Não foi possível carregar o conteúdo do concurso.';
        this.carregando = false;
        this.cdr.detectChanges();
      },
    });
  }

  get vazio(): boolean {
    return !!this.conteudo && this.conteudo.materias.length === 0;
  }

  irParaRevisao(): void {
    this.router.navigate(['/concursos', this.concursoId, 'revisao']);
  }

  voltar(): void {
    this.router.navigate(['/concursos', this.concursoId]);
  }
}
