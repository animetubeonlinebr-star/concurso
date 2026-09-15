export interface Concurso {
  id: number;
  nome: string;
  orgao?: string;
  cargo?: string;
  banca?: string;
  ano?: number;
  descricao?: string;
  status: 'ATIVO' | 'INATIVO' | 'EXCLUIDO';
}

export interface CriarConcursoRequest {
  nome: string;
  orgao?: string;
  cargo?: string;
  banca?: string;
  ano?: number;
  descricao?: string;
}
