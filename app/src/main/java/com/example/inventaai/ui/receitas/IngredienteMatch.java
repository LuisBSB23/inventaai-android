package com.example.inventaai.ui.receitas;

import com.example.inventaai.data.model.DespensaItem;

public class IngredienteMatch {

    public enum Status {
        POSSUI,
        INSUFICIENTE,
        FALTA
    }

    private final String textoReceita;
    private final String nomeIngrediente;
    private final double quantidadePedida;
    private final DespensaItem itemDespensa;
    private final Status status;
    private final double quantidadeFaltante;

    public IngredienteMatch(String textoReceita,
                            String nomeIngrediente,
                            double quantidadePedida,
                            DespensaItem itemDespensa,
                            Status status,
                            double quantidadeFaltante) {
        this.textoReceita       = textoReceita;
        this.nomeIngrediente    = nomeIngrediente;
        this.quantidadePedida   = quantidadePedida;
        this.itemDespensa       = itemDespensa;
        this.status             = status;
        this.quantidadeFaltante = quantidadeFaltante;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getTextoReceita()       { return textoReceita; }
    public String getNomeIngrediente()    { return nomeIngrediente; }
    public double getQuantidadePedida()   { return quantidadePedida; }
    public DespensaItem getItemDespensa() { return itemDespensa; }
    public Status getStatus()             { return status; }
    public double getQuantidadeFaltante() { return quantidadeFaltante; }
}