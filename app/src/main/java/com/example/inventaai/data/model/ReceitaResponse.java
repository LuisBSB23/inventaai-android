package com.example.inventaai.data.model;

import java.util.List;

/**
 * POJO que representa a receita retornada pela API do Gemini (Sprint 4).
 *
 * O Gson popula os campos a partir do JSON gerado pela IA, que deve ter
 * exatamente estes campos (em português, conforme o prompt):
 *
 * {
 *   "titulo":        "Nome da Receita",
 *   "tempo_preparo": "30 min",
 *   "porcoes":       "4 porções",
 *   "dificuldade":   "Fácil",
 *   "ingredientes":  ["Ingrediente 1 - 200 g", "Ingrediente 2 - 1 un"],
 *   "passos":        ["Passo 1...", "Passo 2..."]
 * }
 *
 * Todos os campos são opcionais no Java (podem ficar null se a IA omiti-los);
 * a Activity trata null com valores de fallback.
 */
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