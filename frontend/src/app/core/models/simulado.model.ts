export interface Simulado {
  id: number;
  concursoId: number;
  materiaId: number;
  quantidadeQuestoes: number;
  status: 'CRIADO' | 'EM_ANDAMENTO' | 'FINALIZADO' | 'CANCELADO';
  criadoEm: string;
  iniciadoEm?: string;
  finalizadoEm?: string;
}

export interface Alternativa {
  letra: string;
  texto: string;
}

export interface SimuladoQuestao {
  id: number;
  ordem: number;
  questaoId: number;
  enunciado: string;
  tipo: 'MULTIPLA_ESCOLHA' | 'CERTO_ERRADO' | 'DISCURSIVA';
  alternativas?: Alternativa[] | null;
  banca?: string;
  ano?: number;
  dificuldade?: string;
  respondida: boolean;
  alternativaSelecionada?: string;
  respostaTexto?: string;
}

export interface CriarSimuladoRequest {
  concursoId: number;
  materiaId: number;
  quantidadeQuestoes: number;
}
