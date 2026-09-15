import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ConcursoService } from '../../core/services/concurso.service';
import { SimuladoService } from '../../core/services/simulado.service';
import { DesempenhoService } from '../../core/services/desempenho.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {
  simulados: any[] = [];
  desempenho: any = {};
  concursos: any[] = [];

  constructor(
    private router: Router,
    private simuladoService: SimuladoService,
    private desempenhoService: DesempenhoService
  ) {}

  ngOnInit() {
    this.carregarDados();
  }

  carregarDados() {
    // Carregar simulados recentes
    this.simuladoService.listarSimulados().subscribe({
      next: (data) => {
        this.simulados = data.slice(0, 3); // Últimos 3
      },
      error: (err) => console.error('Erro ao carregar simulados:', err)
    });

    // Carregar desempenho geral
    this.desempenhoService.buscarDesempenhoGeral().subscribe({
      next: (data) => {
        this.desempenho = data;
      },
      error: (err) => console.error('Erro ao carregar desempenho:', err)
    });
  }

  irParaSimulados() {
    this.router.navigate(['/simulados']);
  }

  irParaResultado() {
    this.router.navigate(['/resultado']);
  }
}
