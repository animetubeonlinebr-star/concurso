import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ProcessandoComponent } from './processando';
import { EditalImportacaoService } from '../../../core/services/edital-importacao.service';
import { StatusProcessamentoResponse } from '../../../core/models/edital-importacao.model';

describe('ProcessandoComponent', () => {
  let component: ProcessandoComponent;
  let fixture: ComponentFixture<ProcessandoComponent>;

  const buscarStatus = vi.fn();
  const reprocessar = vi.fn();
  const navigate = vi.fn();
  let concursoParam: string | null = '7';

  function resposta(
    status: string,
    extra: Partial<StatusProcessamentoResponse> = {},
  ): StatusProcessamentoResponse {
    return {
      concursoId: 7,
      status,
      progresso: 50,
      statusExtracao: 'PROCESSADO',
      mensagem: 'ok',
      mensagemErro: null,
      ...extra,
    } as StatusProcessamentoResponse;
  }

  function criar(status: string, extra: Partial<StatusProcessamentoResponse> = {}) {
    buscarStatus.mockReturnValue(of(resposta(status, extra)));

    fixture = TestBed.createComponent(ProcessandoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();
  }

  beforeEach(async () => {
    buscarStatus.mockReset();
    reprocessar.mockReset();
    navigate.mockReset();
    concursoParam = '7';

    await TestBed.configureTestingModule({
      imports: [ProcessandoComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: EditalImportacaoService, useValue: { buscarStatus, reprocessar } },
        { provide: Router, useValue: { navigate } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => concursoParam } } },
        },
      ],
    }).compileComponents();
  });

  it('navega para a revisão quando o processamento termina', () => {
    criar('AGUARDANDO_REVISAO');

    expect(navigate).toHaveBeenCalledWith(['/concursos', 7, 'revisao']);
  });

  it('não navega enquanto o processamento está em andamento', () => {
    criar('PROCESSANDO');

    expect(navigate).not.toHaveBeenCalled();
    expect(component.progresso).toBe(50);
  });

  it('expõe a mensagem de erro devolvida pelo backend', () => {
    criar('ERRO', { mensagemErro: 'PDF sem texto' });

    expect(component.erro).toBe('PDF sem texto');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('marca as etapas anteriores como concluídas e a atual como em andamento', () => {
    criar('PROCESSANDO');

    // PROCESSANDO corresponde à etapa 2 ("Identificando matérias e
    // tópicos"), então tudo antes dela já está concluído.
    expect(component.etapas[0].status).toBe('concluido');
    expect(component.etapas[1].status).toBe('concluido');
    expect(component.etapas[2].status).toBe('em-andamento');
    expect(component.etapas[3].status).toBe('pendente');
  });

  it('marca a etapa corrente como erro quando o status é ERRO', () => {
    criar('ERRO');

    expect(component.etapas.some((e) => e.status === 'erro')).toBe(true);
  });

  it('exibe erro de consulta sem quebrar a tela', () => {
    buscarStatus.mockReturnValue(
      throwError(() => ({ error: { message: 'Falha de rede' } })),
    );

    fixture = TestBed.createComponent(ProcessandoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(component.erro).toBe('Falha de rede');
  });

  it('reprocessa, limpa o erro e reinicia o acompanhamento', () => {
    criar('ERRO', { mensagemErro: 'Falha' });
    reprocessar.mockReturnValue(of(void 0));

    component.reprocessar();

    expect(reprocessar).toHaveBeenCalledWith(7);
    expect(component.reprocessando).toBe(false);
    // O polling recomeça na sequência, então o backend é consultado de novo.
    expect(buscarStatus).toHaveBeenCalledTimes(2);
  });

  it('ignora reprocessamento concorrente', () => {
    criar('ERRO');
    component.reprocessando = true;

    component.reprocessar();

    expect(reprocessar).not.toHaveBeenCalled();
  });

  it('sinaliza concurso inválido sem consultar o backend', () => {
    concursoParam = null;

    fixture = TestBed.createComponent(ProcessandoComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(component.erro).toBe('Concurso inválido.');
    expect(buscarStatus).not.toHaveBeenCalled();
  });
});