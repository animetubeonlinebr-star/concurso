export interface TopicoExtraido {
  nome: string;
}

export interface MateriaExtraida {
  nome: string;
  topicos: string[];
}

export interface DadosExtraidos {
  nome: string;
  banca: string;
  orgao: string;
  ano: number | null;
  materias: MateriaExtraida[];
}
