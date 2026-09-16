import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimuladosComponent } from './simulados';

describe('SimuladosComponent', () => {
  let component: SimuladosComponent;
  let fixture: ComponentFixture<SimuladosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SimuladosComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SimuladosComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
