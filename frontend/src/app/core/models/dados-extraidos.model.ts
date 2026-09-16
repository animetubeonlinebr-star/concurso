/**
 * Estrutura devolvida pela pré-visualização (POST /concursos/upload).
 * Não confundir com a revisão persistida: aqui nada foi gravado.
 */
export interface TopicoExtraido {
  nome: string;
  codigo?: string;
  detalhe?: string;
}

export interface MateriaExtraida {
  nome: string;
  topicos: TopicoExtraido[];
}

export interface DadosExtraidos {
  nome: string;
  banca: string;
  orgao: string;
  ano: number | null;
  materias: MateriaExtraida[];
}
