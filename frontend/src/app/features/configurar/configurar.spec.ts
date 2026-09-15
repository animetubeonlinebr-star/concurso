import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Configurar } from './configurar';

describe('Configurar', () => {
  let component: Configurar;
  let fixture: ComponentFixture<Configurar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Configurar],
    }).compileComponents();

    fixture = TestBed.createComponent(Configurar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
