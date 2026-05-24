package com.example.inventaai.data.model;

import java.io.Serializable;

/**
 * POJO que representa um item da despensa.
 * Implementa Serializable para permitir passagem via Intent entre Activities.
 * dataValidade deve estar no formato YYYY-MM-DD para ordenação correta no SQLite.
 */
public class DespensaItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private long   id;
    private String nome;
    private double quantidade;
    private String unidadeMedida;
    private String dataValidade;   // formato: YYYY-MM-DD
    private String status;         // ATIVO | CONSUMIDO | DESCARTADO
    private String categoria;      // campo auxiliar (não persiste no DB ainda)

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    /** Construtor vazio — necessário para instanciar antes de popular com setters. */
    public DespensaItem() {}

    /** Construtor completo — para inserção nova (id será atribuído pelo banco). */
    public DespensaItem(String nome, double quantidade, String unidadeMedida,
                        String dataValidade, String status) {
        this.nome          = nome;
        this.quantidade    = quantidade;
        this.unidadeMedida = unidadeMedida;
        this.dataValidade  = dataValidade;
        this.status        = status;
    }

    /** Construtor com id — para reconstruir objetos lidos do banco de dados. */
    public DespensaItem(long id, String nome, double quantidade, String unidadeMedida,
                        String dataValidade, String status) {
        this.id            = id;
        this.nome          = nome;
        this.quantidade    = quantidade;
        this.unidadeMedida = unidadeMedida;
        this.dataValidade  = dataValidade;
        this.status        = status;
    }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public long getId()                        { return id; }
    public void setId(long id)                 { this.id = id; }

    public String getNome()                    { return nome; }
    public void setNome(String nome)           { this.nome = nome; }

    public double getQuantidade()              { return quantidade; }
    public void setQuantidade(double q)        { this.quantidade = q; }

    public String getUnidadeMedida()           { return unidadeMedida; }
    public void setUnidadeMedida(String u)     { this.unidadeMedida = u; }

    public String getDataValidade()            { return dataValidade; }
    public void setDataValidade(String d)      { this.dataValidade = d; }

    public String getStatus()                  { return status; }
    public void setStatus(String status)       { this.status = status; }

    public String getCategoria()               { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    // -------------------------------------------------------------------------
    // toString — útil para logs e depuração
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "DespensaItem{"
                + "id=" + id
                + ", nome='" + nome + '\''
                + ", quantidade=" + quantidade
                + ", unidade='" + unidadeMedida + '\''
                + ", validade='" + dataValidade + '\''
                + ", status='" + status + '\''
                + '}';
    }
}
