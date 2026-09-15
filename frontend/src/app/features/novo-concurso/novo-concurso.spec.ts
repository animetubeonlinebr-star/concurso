import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NovoConcurso } from './novo-concurso';

describe('NovoConcurso', () => {
  let component: NovoConcurso;
  let fixture: ComponentFixture<NovoConcurso>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NovoConcurso],
    }).compileComponents();

    fixture = TestBed.createComponent(NovoConcurso);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
