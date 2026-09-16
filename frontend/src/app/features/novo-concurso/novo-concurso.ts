import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import { EditalImportacaoService } from '../../core/services/edital-importacao.service';

interface Etapa {
  nome: string;
  status: 'pendente' | 'em-andamento' | 'concluido' | 'erro';
}

@Component({
  selector: 'app-novo-concurso',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './novo-concurso.html',
  styleUrls: ['./novo-concurso.scss'],
})
export class NovoConcurso {
  arquivoSelecionado: File | null = null;
  processando = false;
  dragOver = false;

  progresso = 0;
  etapas: Etapa[] = [
    { nome: 'Enviando arquivo', status: 'pendente' },
    { nome: 'Extraindo texto do PDF', status: 'pendente' },
    { nome: 'Identificando dados', status: 'pendente' },
    { nome: 'Preparando confirmação', status: 'pendente' },
  ];
  mensagemProgresso = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private editalService: EditalImportacaoService,
    private cdr: ChangeDetectorRef,
  ) {}

  // ===== Upload handlers =====

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      this.arquivoSelecionado = file;
    } else {
      alert('Por favor, selecione um arquivo PDF válido.');
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      const file = files[0];
      if (file.type === 'application/pdf') {
        this.arquivoSelecionado = file;
      } else {
        alert('Por favor, solte um arquivo PDF válido.');
      }
    }
  }

  limparArquivo() {
    this.arquivoSelecionado = null;
    this.resetarProgresso();
  }

  resetarProgresso() {
    this.progresso = 0;
    this.mensagemProgresso = '';
    this.etapas.forEach((e) => (e.status = 'pendente'));
    this.processando = false;
  }

  // ===== Status helpers (template) =====

  getStatusIcon(status: string): string {
    switch (status) {
      case 'concluido': return '✅';
      case 'em-andamento': return '⏳';
      case 'erro': return '❌';
      default: return '⭕';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'concluido': return 'text-success';
      case 'em-andamento': return 'text-primary';
      case 'erro': return 'text-danger';
      default: return 'text-muted';
    }
  }

  // ===== Upload principal =====

  /**
   * O upload agora só registra a importação e devolve o concursoId. O
   * processamento continua em background, acompanhado na tela /processando.
   */
  upload(): void {
    if (!this.arquivoSelecionado) return;

    this.processando = true;
    this.progresso = 0;
    this.etapas.forEach((e) => (e.status = 'pendente'));

    this.atualizarEtapa(0, 'em-andamento', 'Enviando arquivo...');

    // Progresso visual enquanto o backend recebe e persiste o arquivo
    this.simularProgresso(0, 40, 800, () => {
      this.atualizarEtapa(0, 'concluido', 'Arquivo enviado ✓');
      this.atualizarEtapa(1, 'em-andamento', 'Extraindo texto do PDF...');
    });

    this.editalService.importar(this.arquivoSelecionado).subscribe({
      next: (resposta) => {
        this.atualizarEtapa(1, 'concluido', 'Texto extraído ✓');
        this.atualizarEtapa(2, 'em-andamento', 'Processando estrutura...');
        this.progresso = 60;

        setTimeout(() => {
          this.processando = false;
          this.cdr.detectChanges();
          this.router.navigate([
            '/concursos', resposta.concursoId, 'processando',
          ]);
        }, 500);
      },
      error: (err) => this.tratarErro(err),
    });
  }

  private tratarErro(err: any): void {
    console.error('❌ Erro no upload:', err);

    this.processando = false;
    this.etapas.forEach((e) => {
      if (e.status === 'em-andamento') e.status = 'erro';
    });
    this.mensagemProgresso = '❌ Erro ao enviar o edital.';
    this.cdr.detectChanges();

    // O mesmo arquivo já importado devolve 409 com o concursoId existente:
    // a ação útil é abrir o concurso, não repetir o upload.
    if (err?.status === 409) {
      const concursoId = err?.error?.details?.concursoId;
      if (concursoId) {
        const abrir = confirm(
          'Este edital já foi importado. Deseja abrir o concurso existente?');
        if (abrir) {
          this.router.navigate(['/concursos', concursoId, 'processando']);
          return;
        }
        return;
      }
    }

    alert(err?.error?.message
      ?? 'Erro ao enviar o edital. Verifique o arquivo e tente novamente.');
  }

  // ===== Helpers privados =====

  private atualizarEtapa(
    index: number,
    status: Etapa['status'],
    mensagem?: string,
  ): void {
    this.etapas[index].status = status;
    if (mensagem) this.mensagemProgresso = mensagem;
    this.cdr.detectChanges();
  }

  private simularProgresso(
    inicio: number,
    fim: number,
    duracao: number,
    callback: () => void,
  ): void {
    const passos = 20;
    const passo = (fim - inicio) / passos;
    const intervalo = duracao / passos;
    let atual = inicio;

    const timer = setInterval(() => {
      atual += passo;
      if (atual >= fim) {
        atual = fim;
        clearInterval(timer);
        this.progresso = Math.round(atual);
        this.cdr.detectChanges();
        callback();
      } else {
        this.progresso = Math.round(atual);
        this.cdr.detectChanges();
      }
    }, intervalo);
  }
}
