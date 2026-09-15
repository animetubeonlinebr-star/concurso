import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-resultado',
  standalone: true,
  imports: [],
  templateUrl: './resultado.html',
  styleUrls: ['./resultado.scss']
})
export class ResultadoComponent {
  constructor(private router: Router) {}

  voltarSimulados() {
    this.router.navigate(['/simulados']);
  }

  verExplicacao() {
    this.router.navigate(['/explicacao']);
  }
}
