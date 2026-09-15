import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmarConcurso } from './confirmar-concurso';

describe('ConfirmarConcurso', () => {
  let component: ConfirmarConcurso;
  let fixture: ComponentFixture<ConfirmarConcurso>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmarConcurso],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmarConcurso);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
