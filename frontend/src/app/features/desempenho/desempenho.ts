import { Component } from '@angular/core';

@Component({
  selector: 'app-desempenho',
  standalone: true,
  imports: [],
  templateUrl: './desempenho.html',
  styleUrls: ['./desempenho.scss']
})
export class DesempenhoComponent {
  switchTab(tab: string) {
    console.log('Tab selecionada:', tab);
  }
}