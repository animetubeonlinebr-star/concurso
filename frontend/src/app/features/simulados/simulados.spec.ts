import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Simulados } from './simulados';

describe('Simulados', () => {
  let component: Simulados;
  let fixture: ComponentFixture<Simulados>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Simulados],
    }).compileComponents();

    fixture = TestBed.createComponent(Simulados);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
