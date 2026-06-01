package com.example.inventaai.data.model;

import java.util.List;

public class ReceitaResponse {

    private String       titulo;
    private String       tempo_preparo;
    private String       porcoes;
    private String       dificuldade;
    private List<String> ingredientes;
    private List<String> passos;

    // ──────────────────────────────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────────────────────────────

    public String getTitulo()            { return titulo != null        ? titulo        : "Receita do Chef IA"; }
    public String getTempoPreparo()      { return tempo_preparo != null ? tempo_preparo : "—"; }
    public String getPorcoes()           { return porcoes != null       ? porcoes       : "—"; }
    public String getDificuldade()       { return dificuldade != null   ? dificuldade   : "—"; }
    public List<String> getIngredientes(){ return ingredientes; }
    public List<String> getPassos()      { return passos; }
}