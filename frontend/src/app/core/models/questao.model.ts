export interface Alternativa {
  letra: string;
  texto: string;
}

export interface Questao {
  id?: number;
  materiaId: number;
  topicoId?: number;
  enunciado: string;
  tipo: 'MULTIPLA_ESCOLHA' | 'CERTO_ERRADO';
  alternativas: Alternativa[];
  respostaCorreta: string;
  justificativa?: string;
  banca?: string;
  ano?: number;
  dificuldade?: 'FACIL' | 'MEDIO' | 'DIFICIL';
  origem?: 'BANCA' | 'IA' | 'USUARIO';
  referencia?: string;
  ativo?: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}
