import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ConfirmarConcurso } from './confirmar-concurso';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('ConfirmarConcursoComponent', () => {
  let component: ConfirmarConcurso;
  let fixture: ComponentFixture<ConfirmarConcurso>;
  let router: Router;

  beforeEach(async () => {
    // 👇 Cria um spy manual sem usar jasmine.createSpy
    const navigateSpy = () => {};

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        HttpClientTestingModule,
        ConfirmarConcurso
      ],
      providers: [
        {
          provide: Router,
          useValue: {
            navigate: navigateSpy, // ← spy manual
            getCurrentNavigation: () => ({
              extras: {
                state: {
                  dados: {
                    nome: 'Polícia Federal',
                    banca: 'CEBRASPE',
                    orgao: 'Polícia Federal',
                    ano: 2026,
                    materias: [
                      { nome: 'Direito Constitucional', topicos: ['Princípios'] },
                      { nome: 'Língua Portuguesa', topicos: ['Interpretação'] }
                    ]
                  }
                }
              }
            })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmarConcurso);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('deve criar o componente', () => {
    expect(component).toBeTruthy();
  });

  it('deve carregar os dados do state', () => {
    expect(component.dados.nome).toBe('Polícia Federal');
    expect(component.dados.banca).toBe('CEBRASPE');
    expect(component.dados.materias.length).toBe(2);
    expect(component.materiasSelecionadas.length).toBe(2);
  });

  it('deve alternar a seleção de uma matéria', () => {
    const materia = { nome: 'Direito Constitucional', topicos: ['Princípios'] };
    component.toggleMateria(materia);
    expect(component.materiasSelecionadas.length).toBe(1);
  });

  it('deve validar se o nome está preenchido antes de confirmar', () => {
    component.dados.nome = '';
    component.confirmar();
    expect(component.mensagem).toContain('obrigatório');
  });
});
