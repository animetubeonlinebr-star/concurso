import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-questao',
  standalone: true,
  imports: [],
  templateUrl: './questao.html',
  styleUrls: ['./questao.scss']
})
export class QuestaoComponent {
  constructor(private router: Router) {}

  finalizarSimulado() {
    this.router.navigate(['/resultado']);
  }

  proximaQuestao() {
  }

  questaoAnterior() {
  }

  marcarQuestao() {
  }

  selecionarAlternativa(letra: string) {
  }
}