import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard';
import { SimuladosComponent } from './features/simulados/simulados';
import { DesempenhoComponent } from './features/desempenho/desempenho';
import { HistoricoComponent } from './features/historico/historico';
import { PerfilComponent } from './features/perfil/perfil';
import { LoginComponent } from './features/login/login';
import { CadastroComponent } from './features/cadastro/cadastro';
import { ResultadoComponent } from './features/resultado/resultado';
import { ConfigurarComponent } from './features/configurar/configurar';
import { QuestaoComponent } from './features/questao/questao';
import { ExplicacaoComponent } from './features/explicacao/explicacao';
import { AuthGuard } from './core/guards/auth.guard';
import { MateriaDetalheComponent } from './features/materias/materia-detalhe/materia-detalhe';
import { NovoConcurso } from './features/novo-concurso/novo-concurso';
import { ProcessandoComponent } from './features/edital/processando/processando';
import { RevisaoComponent } from './features/edital/revisao/revisao';
import { ConteudoComponent } from './features/edital/conteudo/conteudo';
import { ListaConcursosComponent } from './features/concursos/lista-concursos/lista-concursos';
import { DetalheConcursoComponent } from './features/concursos/detalhe-concurso/detalhe-concurso';


export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'cadastro', component: CadastroComponent },


  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],   
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'simulados', component: SimuladosComponent },
      { path: 'configurar', component: ConfigurarComponent },
      { path: 'questao', component: QuestaoComponent },
      { path: 'resultado', component: ResultadoComponent },
      { path: 'explicacao', component: ExplicacaoComponent },
      { path: 'desempenho', component: DesempenhoComponent },
      { path: 'historico', component: HistoricoComponent },
      { path: 'perfil', component: PerfilComponent },
      { path: 'novo-concurso', component: NovoConcurso },
      { path: 'concursos/:concursoId/processando', component: ProcessandoComponent },
      { path: 'concursos/:concursoId/revisao', component: RevisaoComponent },
      { path: 'concursos/:concursoId/conteudo', component: ConteudoComponent },
      { path: 'materias/:materiaId', component: MateriaDetalheComponent },
      { path: 'concursos', component: ListaConcursosComponent },
      { path: 'concursos/:concursoId', component: DetalheConcursoComponent },
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];
