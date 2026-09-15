export interface Topico {
  id: number;
  materiaId: number;
  nome: string;
  descricao?: string;
  ordem: number;
  ativo: boolean;
}
