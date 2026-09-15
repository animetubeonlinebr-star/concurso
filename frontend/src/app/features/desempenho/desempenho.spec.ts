import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Desempenho } from './desempenho';

describe('Desempenho', () => {
  let component: Desempenho;
  let fixture: ComponentFixture<Desempenho>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Desempenho],
    }).compileComponents();

    fixture = TestBed.createComponent(Desempenho);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
