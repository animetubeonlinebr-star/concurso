import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { RevisaoComponent } from './revisao';
import { EditalImportacaoService } from '../../../core/services/edital-importacao.service';
import {
  MateriaSugerida,
  RevisaoEstrutura,
  RevisaoEstruturaRequest,
  TopicoSugerido,
} from '../../../core/models/edital-importacao.model';

describe('RevisaoComponent', () => {
  let component: RevisaoComponent;
  let fixture: ComponentFixture<RevisaoComponent>;

  const buscarRevisao = vi.fn();
  const atualizarRevisao = vi.fn();
  const confirmar = vi.fn();
  const navigate = vi.fn();
  const snackOpen = vi.fn();
  let concursoParam: string | null = '7';

  const topico = (id: number, nome: string): TopicoSugerido => ({
    id,
    nome,
    ordem: id,
    selecionado: true,
    possivelDuplicidade: false,
    similarAId: null,
    similarA: null,
    jaExisteConfirmado: false,
  });

  const materia = (
    id: number,
    nome: string,
    extra: Partial<MateriaSugerida> = {},
  ): MateriaSugerida => ({
    id,
    nome,
    ordem: id,
    selecionada: true,
    possivelDuplicidade: false,
    similarAId: null,
    similarA: null,
    jaExisteConfirmada: false,
    topicos: [topico(id * 10, 'Tópico ' + id)],
    ...extra,
  });

  const revisao = (materias: MateriaSugerida[]): RevisaoEstrutura => ({
    concursoId: 7,
    importacaoId: 42,
    status: 'AGUARDANDO_REVISAO',
    statusExtracao: 'PROCESSADO',
    mensagem: null,
    dadosConcurso: {
      nome: 'PF 2026',
      orgao: 'Polícia Federal',
      cargo: null,
      banca: 'CEBRASPE',
      ano: 2026,
    },
    materias,
  });

  function montar(estrutura: RevisaoEstrutura) {
    buscarRevisao.mockReturnValue(of(estrutura));

    fixture = TestBed.createComponent(RevisaoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();
  }

  beforeEach(async () => {
    buscarRevisao.mockReset();
    atualizarRevisao.mockReset();
    confirmar.mockReset();
    navigate.mockReset();
    snackOpen.mockReset();
    concursoParam = '7';

    atualizarRevisao.mockImplementation(
      (_id: number, req: RevisaoEstruturaRequest) =>
        of(revisao(req.materias.map((m) => materia(m.id, m.nome ?? '')))),
    );
    confirmar.mockReturnValue(
      of({ materiasPersistidas: 2, topicosPersistidos: 2 }),
    );

    await TestBed.configureTestingModule({
      imports: [RevisaoComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: EditalImportacaoService,
          useValue: { buscarRevisao, atualizarRevisao, confirmar },
        },
        { provide: Router, useValue: { navigate } },
        { provide: MatSnackBar, useValue: { open: snackOpen } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => concursoParam } } },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('carrega a revisão e deixa de exibir o carregando', () => {
    montar(revisao([materia(1, 'Língua Portuguesa')]));

    expect(component.carregando).toBe(false);
    expect(component.materias.length).toBe(1);
    expect(component.erro).toBeNull();
  });

  it('avisa o usuário quando a extração não identificou o conteúdo', () => {
    montar({
      ...revisao([]),
      statusExtracao: 'NAO_IDENTIFICADO',
      mensagem: 'Nenhuma matéria foi extraída: o edital pode não ter bloco de conteúdo programático.',
    });

    const aviso: HTMLElement | null =
      fixture.nativeElement.querySelector('.aviso-extracao');
    expect(aviso?.textContent).toContain('Nenhuma matéria foi extraída');
  });

  it('expõe erro de carregamento', () => {
    buscarRevisao.mockReturnValue(
      throwError(() => ({ error: { message: 'Estrutura não revisável' } })),
    );

    fixture = TestBed.createComponent(RevisaoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(component.erro).toBe('Estrutura não revisável');
    expect(component.carregando).toBe(false);
  });

  it('alterna seleção de matéria e de tópico', () => {
    montar(revisao([materia(1, 'Língua Portuguesa')]));
    const m = component.materias[0];

    component.toggleMateria(m);
    expect(m.selecionada).toBe(false);

    const t = m.topicos[0];
    component.toggleTopico(t);
    expect(t.selecionado).toBe(false);
  });

  it('remove matéria e tópico da lista em edição', () => {
    montar(revisao([materia(1, 'A'), materia(2, 'B')]));

    component.removerMateria(component.materias[1]);
    expect(component.materias.map((m) => m.id)).toEqual([1]);

    component.removerTopico(component.materias[0], component.materias[0].topicos[0]);
    expect(component.materias[0].topicos).toEqual([]);
  });

  it('recusa confirmar sem nenhuma matéria selecionada', () => {
    montar(revisao([materia(1, 'Língua Portuguesa')]));
    component.toggleMateria(component.materias[0]);

    component.confirmar();

    expect(confirmar).not.toHaveBeenCalled();
    expect(snackOpen).toHaveBeenCalled();
  });

  it('salva a revisão antes de confirmar e navega para o conteúdo', () => {
    montar(revisao([materia(1, 'Língua Portuguesa')]));

    component.confirmar();

    expect(atualizarRevisao).toHaveBeenCalled();
    expect(confirmar).toHaveBeenCalledWith(7);
    expect(navigate).toHaveBeenCalledWith(['/concursos', 7, 'conteudo']);
  });

  it('envia mesclarEmId apenas na origem e só após confirmação do usuário', () => {
    montar(revisao([
      materia(1, 'Direito Constitucional'),
      materia(2, 'Noções de Direito Constitucional', { similarAId: 1 }),
    ]));

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.mesclar(component.materias[1]);

    const req = atualizarRevisao.mock.calls.at(-1)![1] as RevisaoEstruturaRequest;
    expect(req.materias.find((m) => m.id === 2)!.mesclarEmId).toBe(1);
    expect(req.materias.find((m) => m.id === 1)!.mesclarEmId).toBeNull();
  });

  it('não mescla quando o usuário cancela', () => {
    montar(revisao([materia(1, 'A'), materia(2, 'B', { similarAId: 1 })]));

    vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.mesclar(component.materias[1]);

    expect(atualizarRevisao).not.toHaveBeenCalled();
  });

  it('avisa quando o alvo da mesclagem não está mais na lista', () => {
    montar(revisao([materia(2, 'Órfã', { similarAId: 99 })]));

    component.mesclar(component.materias[0]);

    expect(atualizarRevisao).not.toHaveBeenCalled();
    expect(snackOpen).toHaveBeenCalled();
  });

  it('ignora confirmação concorrente', () => {
    montar(revisao([materia(1, 'A')]));
    component.confirmando = true;

    component.confirmar();

    expect(atualizarRevisao).not.toHaveBeenCalled();
  });

  it('exibe erro quando a confirmação falha', () => {
    montar(revisao([materia(1, 'A')]));
    confirmar.mockReturnValue(
      throwError(() => ({ error: { message: 'Já existe no conteúdo confirmado' } })),
    );

    component.confirmar();

    expect(component.erro).toBe('Já existe no conteúdo confirmado');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('propaga a flag de colisão com o conteúdo confirmado', () => {
    montar(revisao([materia(1, 'Língua Portuguesa', { jaExisteConfirmada: true })]));

    expect(component.materias[0].jaExisteConfirmada).toBe(true);
  });
});