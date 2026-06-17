package com.example.inventaai.data.model;

import java.io.Serializable;

public class HistoricoItem implements Serializable {

    private static final long serialVersionUID = 3L; // incrementado na Sprint 15

    private long   idHistorico;
    private long   idItem;
    private String dataAcao;    // formato: YYYY-MM-DD
    private String motivo;      // "CONSUMIDO" ou "DESCARTADO"
    private String nomeCached;  // nome do item no momento do registro (denormalizado)
    private String categoria;   // Sprint 2: categoria do item (campo auxiliar UI only)
    /** Sprint 15: origem do consumo (ex: "Receita: Frango ao Limão"). Pode ser null. */
    private String origem;

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public HistoricoItem() {}

    public HistoricoItem(long idItem, String dataAcao, String motivo) {
        this.idItem   = idItem;
        this.dataAcao = dataAcao;
        this.motivo   = motivo;
    }

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

    public String getCategoria()                 { return categoria; }
    public void setCategoria(String categoria)   { this.categoria = categoria; }

    public String getOrigem()                    { return origem; }
    public void setOrigem(String origem)         { this.origem = origem; }

    public boolean temOrigem() {
        return origem != null && !origem.trim().isEmpty();
    }

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
                + ", categoria='" + categoria + '\''
                + ", origem='" + origem + '\''
                + '}';
    }
}