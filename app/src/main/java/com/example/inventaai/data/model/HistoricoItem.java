package com.example.inventaai.data.model;

import java.io.Serializable;

/**
 * POJO que representa um registro de histórico de consumo ou descarte.
 * Implementa Serializable para permitir passagem via Intent entre Activities.
 *
 * Sprint 3: campo nomeCached adicionado para exibir o nome do item
 * sem precisar de um JOIN extra (desnormalização intencional).
 */
public class HistoricoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private long   idHistorico;
    private long   idItem;
    private String dataAcao;    // formato: YYYY-MM-DD
    private String motivo;      // "CONSUMIDO" ou "DESCARTADO"
    private String nomeCached;  // nome do item no momento do registro (denormalizado)

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    /** Construtor vazio. */
    public HistoricoItem() {}

    /** Construtor para inserção nova (idHistorico gerado pelo banco). */
    public HistoricoItem(long idItem, String dataAcao, String motivo) {
        this.idItem   = idItem;
        this.dataAcao = dataAcao;
        this.motivo   = motivo;
    }

    /** Construtor completo — para reconstruir objetos lidos do banco. */
    public HistoricoItem(long idHistorico, long idItem, String dataAcao, String motivo) {
        this.idHistorico = idHistorico;
        this.idItem      = idItem;
        this.dataAcao    = dataAcao;
        this.motivo      = motivo;
    }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public long getIdHistorico()                 { return idHistorico; }
    public void setIdHistorico(long idHistorico) { this.idHistorico = idHistorico; }

    public long getIdItem()                      { return idItem; }
    public void setIdItem(long idItem)           { this.idItem = idItem; }

    public String getDataAcao()                  { return dataAcao; }
    public void setDataAcao(String dataAcao)     { this.dataAcao = dataAcao; }

    public String getMotivo()                    { return motivo; }
    public void setMotivo(String motivo)         { this.motivo = motivo; }

    public String getNomeCached()                { return nomeCached; }
    public void setNomeCached(String nome)       { this.nomeCached = nome; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "HistoricoItem{"
                + "idHistorico=" + idHistorico
                + ", idItem=" + idItem
                + ", dataAcao='" + dataAcao + '\''
                + ", motivo='" + motivo + '\''
                + ", nome='" + nomeCached + '\''
                + '}';
    }
}
