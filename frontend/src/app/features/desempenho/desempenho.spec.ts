import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DesempenhoComponent } from './desempenho';

describe('DesempenhoComponent', () => {
  let component: DesempenhoComponent;
  let fixture: ComponentFixture<DesempenhoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DesempenhoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DesempenhoComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
