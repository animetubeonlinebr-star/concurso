import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-configurar',
  standalone: true,
  imports: [],
  templateUrl: './configurar.html',
  styleUrls: ['./configurar.scss']
})
export class ConfigurarComponent {
  constructor(private router: Router) {}

  voltarSimulados() {
    this.router.navigate(['/simulados']);
  }

  iniciarSimulado() {
    this.router.navigate(['/questao']);
  }
}
