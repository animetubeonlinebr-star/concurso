import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ConteudoComponent } from './conteudo';
import { EditalImportacaoService } from '../../../core/services/edital-importacao.service';
import { ConteudoConcurso } from '../../../core/models/edital-importacao.model';

describe('ConteudoComponent', () => {
  let component: ConteudoComponent;
  let fixture: ComponentFixture<ConteudoComponent>;

  const buscarConteudo = vi.fn();
  const navigate = vi.fn();
  let concursoParam: string | null = '7';

  const conteudo = (materias: ConteudoConcurso['materias']): ConteudoConcurso => ({
    concursoId: 7,
    nome: 'PF 2026',
    statusProcessamento: 'CONFIRMADO',
    resumo: {
      totalMaterias: materias.length,
      totalTopicos: materias.reduce((s, m) => s + m.topicos.length, 0),
      totalQuestoes: 0,
    },
    materias,
  });

  function montar(valor: ConteudoConcurso) {
    buscarConteudo.mockReturnValue(of(valor));

    fixture = TestBed.createComponent(ConteudoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();
  }

  beforeEach(async () => {
    buscarConteudo.mockReset();
    navigate.mockReset();
    concursoParam = '7';

    await TestBed.configureTestingModule({
      imports: [ConteudoComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: EditalImportacaoService, useValue: { buscarConteudo } },
        { provide: Router, useValue: { navigate } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => concursoParam } } },
        },
      ],
    }).compileComponents();
  });

  it('renderiza a árvore confirmada de matérias e tópicos', () => {
    montar(conteudo([
      {
        id: 1,
        nome: 'Língua Portuguesa',
        ordem: 1,
        origem: 'EDITAL',
        totalTopicos: 2,
        totalQuestoes: 0,
        topicos: [
          { id: 10, nome: 'Concordância', ordem: 1, ativo: true, totalQuestoes: 0 },
          { id: 11, nome: 'Regência', ordem: 2, ativo: true, totalQuestoes: 0 },
        ],
      },
    ]));

    expect(component.carregando).toBe(false);
    expect(component.conteudo!.materias[0].topicos.length).toBe(2);
    expect(component.conteudo!.resumo.totalTopicos).toBe(2);
    expect(component.vazio).toBe(false);
  });

  it('marca como vazio quando o concurso não tem matérias confirmadas', () => {
    montar(conteudo([]));

    expect(component.vazio).toBe(true);
    expect(component.erro).toBeNull();
  });

  it('expõe erro de carregamento', () => {
    buscarConteudo.mockReturnValue(
      throwError(() => ({ error: { message: 'Acesso negado' } })),
    );

    fixture = TestBed.createComponent(ConteudoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(component.erro).toBe('Acesso negado');
    expect(component.carregando).toBe(false);
  });

  it('sinaliza concurso inválido sem consultar o backend', () => {
    concursoParam = null;

    fixture = TestBed.createComponent(ConteudoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(component.erro).toBe('Concurso inválido.');
    expect(buscarConteudo).not.toHaveBeenCalled();
  });

  it('navega para a revisão e de volta ao concurso', () => {
    montar(conteudo([]));

    component.irParaRevisao();
    expect(navigate).toHaveBeenCalledWith(['/concursos', 7, 'revisao']);

    component.voltar();
    expect(navigate).toHaveBeenCalledWith(['/concursos', 7]);
  });
});