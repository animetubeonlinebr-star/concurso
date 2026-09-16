import { Component, DestroyRef, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { interval, switchMap, takeWhile, catchError, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { EditalImportacaoService } from '../../../core/services/edital-importacao.service';
import { StatusProcessamentoResponse } from '../../../core/models/edital-importacao.model';

interface Etapa {
  nome: string;
  status: 'pendente' | 'em-andamento' | 'concluido' | 'erro';
}

/**
 * Acompanha o processamento do edital por polling.
 *
 * O polling para sozinho quando o status deixa de ser transitório: sem isso
 * a tela continuaria batendo no backend depois de concluído ou falhado.
 */
@Component({
  selector: 'app-processando',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './processando.html',
  styleUrls: ['./processando.scss'],
})
export class ProcessandoComponent implements OnInit {

  concursoId!: number;
  status: StatusProcessamentoResponse | null = null;
  progresso = 0;
  mensagem = 'Enviando arquivo...';
  erro: string | null = null;
  reprocessando = false;

  etapas: Etapa[] = [
    { nome: 'Arquivo recebido', status: 'pendente' },
    { nome: 'Extraindo texto do PDF', status: 'pendente' },
    { nome: 'Identificando matérias e tópicos', status: 'pendente' },
    { nome: 'Preparando revisão', status: 'pendente' },
  ];

  private readonly intervaloMs = 1500;

  // inject() exige contexto de injeção (inicializador de campo, construtor).
  // iniciarPolling() roda no ngOnInit, que não é um, então a referência é
  // resolvida aqui e apenas consumida lá.
  private readonly destroyRef = inject(DestroyRef);

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
      return;
    }

    this.iniciarPolling();
  }

  private iniciarPolling(): void {
    interval(this.intervaloMs)
      .pipe(
        switchMap(() => this.editalService.buscarStatus(this.concursoId).pipe(
          catchError((err) => {
            this.erro = err?.error?.message
              ?? 'Não foi possível consultar o processamento.';
            return of(null);
          }),
        )),
        takeWhile((resposta) => {
          if (resposta === null) return false;
          return resposta.status === 'RECEBIDO' || resposta.status === 'PROCESSANDO';
        }, true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((resposta) => {
        if (resposta) this.aplicar(resposta);
      });

    // Primeira consulta imediata: sem isso a tela fica parada até o
    // primeiro tick do interval.
    this.consultarUmaVez();
  }

  private consultarUmaVez(): void {
    this.editalService.buscarStatus(this.concursoId).subscribe({
      next: (resposta) => this.aplicar(resposta),
      error: (err) => {
        this.erro = err?.error?.message ?? 'Não foi possível consultar o processamento.';
        this.cdr.detectChanges();
      },
    });
  }

  private aplicar(resposta: StatusProcessamentoResponse): void {
    this.status = resposta;
    this.progresso = resposta.progresso;
    this.mensagem = resposta.mensagem ?? '';
    this.erro = resposta.mensagemErro;
    this.atualizarEtapas(resposta);

    if (resposta.status === 'AGUARDANDO_REVISAO') {
      this.router.navigate(['/concursos', this.concursoId, 'revisao']);
    }

    this.cdr.detectChanges();
  }

  private atualizarEtapas(resposta: StatusProcessamentoResponse): void {
    const indice = this.indiceDaEtapa(resposta.status);

    this.etapas.forEach((etapa, i) => {
      if (resposta.status === 'ERRO') {
        etapa.status = i < indice ? 'concluido' : i === indice ? 'erro' : 'pendente';
      } else if (i < indice) {
        etapa.status = 'concluido';
      } else if (i === indice) {
        etapa.status = resposta.status === 'CONFIRMADO' ? 'concluido' : 'em-andamento';
      } else {
        etapa.status = 'pendente';
      }
    });
  }

  private indiceDaEtapa(status: string): number {
    switch (status) {
      case 'RECEBIDO': return 1;
      case 'PROCESSANDO': return 2;
      case 'AGUARDANDO_REVISAO': return 3;
      case 'CONFIRMADO': return 4;
      default: return 2;
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'concluido': return '✅';
      case 'em-andamento': return '⏳';
      case 'erro': return '❌';
      default: return '⭕';
    }
  }

  reprocessar(): void {
    if (this.reprocessando) return;

    this.reprocessando = true;
    this.erro = null;

    this.editalService.reprocessar(this.concursoId).subscribe({
      next: () => {
        this.reprocessando = false;
        this.etapas.forEach((e) => (e.status = 'pendente'));
        this.iniciarPolling();
      },
      error: (err) => {
        this.reprocessando = false;
        this.erro = err?.error?.message ?? 'Não foi possível reprocessar o edital.';
        this.cdr.detectChanges();
      },
    });
  }

  voltar(): void {
    this.router.navigate(['/novo-concurso']);
  }
}
