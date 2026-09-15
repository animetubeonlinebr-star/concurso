package br.com.marcosbassetto.concursos.domain.edital.dto;

import java.util.List;

public class DadosExtraidos {
    private String nome;
    private String orgao;
    private String banca;
    private Integer ano;
    private List<MateriaExtraida> materias;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getOrgao() { return orgao; }
    public void setOrgao(String orgao) { this.orgao = orgao; }
    public String getBanca() { return banca; }
    public void setBanca(String banca) { this.banca = banca; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public List<MateriaExtraida> getMaterias() { return materias; }
    public void setMaterias(List<MateriaExtraida> materias) { this.materias = materias; }
}
