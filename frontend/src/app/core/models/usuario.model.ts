export interface Usuario {
  id?: number;
  nome: string;
  email: string;
  senha?: string;
  perfil?: 'USER' | 'ADMIN';
  ativo?: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}
