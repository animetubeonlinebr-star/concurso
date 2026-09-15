import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-simulados',
  standalone: true,
  imports: [],
  templateUrl: './simulados.html',
  styleUrls: ['./simulados.scss']
})
export class SimuladosComponent {
  constructor(private router: Router) {}

  criarSimulado() {
    this.router.navigate(['/configurar']);
  }

  iniciarSimulado() {
    this.router.navigate(['/questao']);
  }

  verResultado() {
    this.router.navigate(['/resultado']);
  }
}
