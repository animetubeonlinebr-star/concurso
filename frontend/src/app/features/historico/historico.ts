import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-historico',
  standalone: true,
  imports: [],
  templateUrl: './historico.html',
  styleUrls: ['./historico.scss']
})
export class HistoricoComponent {
  constructor(private router: Router) {}

  verResultado() {
    this.router.navigate(['/resultado']);
  }
}