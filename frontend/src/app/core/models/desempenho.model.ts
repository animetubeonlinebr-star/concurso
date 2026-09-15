export interface Desempenho {
  totalQuestoes: number;
  questoesRespondidas: number;
  questoesCorretas: number;
  questoesIncorretas: number;
  questoesNaoRespondidas: number;
  percentualAcerto: number;
  pontuacao: number;
}

export interface DesempenhoMateria {
  materiaId: number;
  materiaNome: string;
  totalQuestoes: number;
  questoesCorretas: number;
  questoesIncorretas: number;
  percentualAcerto: number;
}
