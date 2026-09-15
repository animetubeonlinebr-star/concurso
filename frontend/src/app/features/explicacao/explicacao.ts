import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-explicacao',
  standalone: true,
  imports: [],
  templateUrl: './explicacao.html',
  styleUrls: ['./explicacao.scss']
})
export class ExplicacaoComponent {
  constructor(private router: Router) {}

  voltarResultado() {
    this.router.navigate(['/resultado']);
  }

  proximaQuestao() {

}

  questaoAnterior() {
    
  }
}