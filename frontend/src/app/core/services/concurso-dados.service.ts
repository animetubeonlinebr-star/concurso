import { Injectable } from '@angular/core';
import { DadosExtraidos } from '../models/dados-extraidos.model';

/**
 * Service temporário para compartilhar os dados extraídos do edital
 * entre o componente de upload e o de confirmação.
 *
 * Evita depender de `history.state`, que é frágil (some ao recarregar).
 */
@Injectable({ providedIn: 'root' })
export class ConcursoDadosService {

  private dados: DadosExtraidos | null = null;

  setDados(dados: DadosExtraidos): void {
    this.dados = dados;
  }

  getDados(): DadosExtraidos | null {
    return this.dados;
  }

  temDados(): boolean {
    return this.dados !== null;
  }

  limpar(): void {
    this.dados = null;
  }
}
