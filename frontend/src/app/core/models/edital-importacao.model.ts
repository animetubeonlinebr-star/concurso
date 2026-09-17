/**
 * Contratos do fluxo de importação do edital, espelhando os DTOs do backend.
 */

export type StatusProcessamento =
  | 'RECEBIDO'
  | 'PROCESSANDO'
  | 'AGUARDANDO_REVISAO'
  | 'CONFIRMADO'
  | 'ERRO';

/**
 * Qualidade da extração, independente do ciclo de vida: um edital sem bloco
 * de conteúdo programático fica em `AGUARDANDO_REVISAO` com
 * `NAO_IDENTIFICADO`, e a tela precisa avisar em vez de mostrar revisão vazia.
 */
export type StatusExtracao =
  | 'PROCESSADO'
  | 'PARCIAL'
  | 'BAIXA_CONFIANCA'
  | 'NAO_IDENTIFICADO'
  | 'ERRO';

/** Resposta de POST /concursos/importar. */
export interface IniciarImportacaoResponse {
  concursoId: number;
  importacaoId: number;
  status: StatusProcessamento;
  jaExistia: boolean;
}

/** Resposta de GET /concursos/{id}/processamento. */
export interface StatusProcessamentoResponse {
  concursoId: number;
  status: StatusProcessamento;
  progresso: number;
  statusExtracao: StatusExtracao | null;
  mensagem: string | null;
  mensagemErro: string | null;
}

export interface TopicoSugerido {
  id: number;
  nome: string;
  ordem: number;
  selecionado: boolean;
  possivelDuplicidade: boolean;
  /** Id do item semelhante, quando há sinalização de duplicidade. */
  similarAId: number | null;
  similarA: string | null;
  /** O nome já existe no conteúdo confirmado do concurso. */
  jaExisteConfirmado: boolean;
}

export interface MateriaSugerida {
  id: number;
  nome: string;
  ordem: number;
  selecionada: boolean;
  possivelDuplicidade: boolean;
  /** Id do item semelhante, destino natural de uma mesclagem. */
  similarAId: number | null;
  similarA: string | null;
  /** O nome já existe no conteúdo confirmado do concurso. */
  jaExisteConfirmada: boolean;
  topicos: TopicoSugerido[];
}

/** Resposta de GET/PUT /concursos/{id}/revisao. */
export interface RevisaoEstrutura {
  concursoId: number;
  importacaoId: number;
  status: StatusProcessamento;
  statusExtracao: StatusExtracao | null;
  dadosConcurso: DadosConcursoExtraido | null;
  materias: MateriaSugerida[];
  mensagem: string | null;
}

export interface DadosConcursoExtraido {
  nome: string | null;
  orgao: string | null;
  cargo: string | null;
  banca: string | null;
  ano: number | null;
}

/**
 * Corpo de PUT /concursos/{id}/revisao.
 *
 * A ausência de um item na lista significa remoção; `mesclarEmId` só é
 * enviado quando o usuário escolhe mesclar explicitamente.
 */
export interface RevisaoEstruturaRequest {
  materias: MateriaRevisaoRequest[];
}

export interface MateriaRevisaoRequest {
  id: number;
  nome?: string;
  selecionada?: boolean;
  mesclarEmId?: number | null;
  remover?: boolean;
  topicos?: TopicoRevisaoRequest[];
}

export interface TopicoRevisaoRequest {
  id: number;
  nome?: string;
  selecionado?: boolean;
  mesclarEmId?: number | null;
  remover?: boolean;
}

/** Resposta de POST /concursos/{id}/confirmar. */
export interface ConfirmacaoEstruturaResponse {
  concursoId: number;
  materiasPersistidas: number;
  topicosPersistidos: number;
  materiasIgnoradas: string[];
}

/** Resposta de GET /concursos/{id}/conteudo. */
export interface ConteudoConcurso {
  concursoId: number;
  nome: string;
  statusProcessamento: StatusProcessamento;
  resumo: ResumoConteudo;
  materias: MateriaConteudo[];
}

export interface ResumoConteudo {
  totalMaterias: number;
  totalTopicos: number;
  totalQuestoes: number;
}

export interface MateriaConteudo {
  id: number;
  nome: string;
  ordem: number;
  origem: string | null;
  totalTopicos: number;
  totalQuestoes: number;
  topicos: TopicoConteudo[];
}

export interface TopicoConteudo {
  id: number;
  nome: string;
  ordem: number;
  ativo: boolean;
  totalQuestoes: number;
}
