import { Topico } from './topico.model';

export interface Materia {
  id: number;
  concursoId: number;
  nome: string;
  descricao?: string;
  ordem: number;
  peso?: number;
  status: 'ATIVO' | 'INATIVO' | 'EXCLUIDO';
  topicos?: Topico[];   
}