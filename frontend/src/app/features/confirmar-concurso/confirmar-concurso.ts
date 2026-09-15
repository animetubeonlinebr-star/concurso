import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { ConcursoDadosService } from '../../core/services/concurso-dados.service';
import { DadosExtraidos, MateriaExtraida } from '../../core/models/dados-extraidos.model';

@Component({
  selector: 'app-confirmar-concurso',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './confirmar-concurso.html',
  styleUrls: ['./confirmar-concurso.scss'],
})
export class ConfirmarConcurso implements OnInit {
  dados: DadosExtraidos = {
    nome: '',
    banca: '',
    orgao: '',
    ano: null,
    materias: [],
  };

  materiasSelecionadas: MateriaExtraida[] = [];

  carregando = false;
  submitted = false;
  mensagem = '';
  mensagemTipo: 'success' | 'error' | '' = '';

  constructor(
    private router: Router,
    private http: HttpClient,
    private concursoDados: ConcursoDadosService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const dados = this.concursoDados.getDados();

    if (!dados) {
      console.warn('⚠️ Nenhum dado recebido. Redirecionando para upload...');
      this.router.navigate(['/novo-concurso']);
      return;
    }

    console.log('📥 Dados recebidos do service:', dados);

    this.dados = {
      nome: dados.nome ?? '',
      banca: dados.banca ?? '',
      orgao: dados.orgao ?? '',
      ano: dados.ano ?? null,
      materias: dados.materias ?? [],
    };

    // Por padrão, todas as matérias vêm selecionadas
    this.materiasSelecionadas = [...this.dados.materias];

    console.log('📌 Dados prontos para exibição:', this.dados);
    console.log('📌 Matérias selecionadas:', this.materiasSelecionadas.length);

    this.cdr.detectChanges();
  }

  // ===== Matérias =====

  isMateriaSelecionada(materia: MateriaExtraida): boolean {
    return this.materiasSelecionadas.some((m) => m.nome === materia.nome);
  }

  toggleMateria(materia: MateriaExtraida): void {
    const idx = this.materiasSelecionadas.findIndex((m) => m.nome === materia.nome);
    if (idx >= 0) {
      this.materiasSelecionadas.splice(idx, 1);
    } else {
      this.materiasSelecionadas.push(materia);
    }
    console.log('📋 Selecionadas:', this.materiasSelecionadas.map((m) => m.nome));
  }

  temMateriasSelecionadas(): boolean {
    return this.materiasSelecionadas.length > 0;
  }

  verTopicos(materia: MateriaExtraida): void {
    const topicos = materia.topicos ?? [];
    const lista = topicos.length > 0
      ? topicos.map((t, i) => `${i + 1}. ${t}`).join('\n')
      : 'Nenhum tópico encontrado.';
    alert(`📖 Tópicos de ${materia.nome}:\n\n${lista}`);
  }

  // ===== Ações =====

  cancelar(): void {
    if (this.carregando) return;
    this.concursoDados.limpar();
    this.router.navigate(['/novo-concurso']);
  }

  confirmar(): void {
    this.submitted = true;
    this.mensagem = '';
    this.mensagemTipo = '';

    if (!this.dados.nome || this.dados.nome.trim() === '') {
      this.mensagem = '⚠️ O nome do concurso é obrigatório.';
      this.mensagemTipo = 'error';
      return;
    }

    if (!this.temMateriasSelecionadas()) {
      this.mensagem = '⚠️ Selecione pelo menos uma matéria para continuar.';
      this.mensagemTipo = 'error';
      return;
    }

    this.carregando = true;

    const payload = {
      nome: this.dados.nome.trim(),
      banca: this.dados.banca,
      orgao: this.dados.orgao,
      ano: this.dados.ano,
      materias: this.materiasSelecionadas,
    };

    console.log('📤 Payload enviado:', payload);

    this.http.post('/api/v1/concursos', payload).subscribe({
      next: () => {
        this.mensagem = '✅ Concurso salvo com sucesso!';
        this.mensagemTipo = 'success';
        this.carregando = false;
        this.concursoDados.limpar();
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/concursos']), 1500);
      },
      error: (err) => {
        console.error('❌ Erro ao salvar:', err);
        this.mensagem = `❌ Erro ao salvar concurso: ${err.error?.message || 'Tente novamente.'}`;
        this.mensagemTipo = 'error';
        this.carregando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
