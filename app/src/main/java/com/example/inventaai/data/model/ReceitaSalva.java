package com.example.inventaai.data.model;

import java.io.Serializable;
import java.util.List;

public class ReceitaSalva implements Serializable {

    private static final long serialVersionUID = 11L;

    private long         id;
    private String       titulo;
    private String       descricao;
    private String       tempoPreparo;
    private String       porcoes;
    private String       dificuldade;
    /** Lista de ingredientes — serializada/deserializada via Gson. */
    private List<String> ingredientes;
    /** Lista de passos — serializada/deserializada via Gson. */
    private List<String> passos;
    private String       imagemUrl;
    /** Data em que foi salva (formato YYYY-MM-DD). */
    private String       dataSalvo;
    private String       userId;

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public ReceitaSalva() {}

    /** Construtor a partir de um ReceitaResponse gerado pela IA. */
    public ReceitaSalva(ReceitaResponse response, String userId) {
        this.titulo       = response.getTitulo();
        this.tempoPreparo = response.getTempoPreparo();
        this.porcoes      = response.getPorcoes();
        this.dificuldade  = response.getDificuldade();
        this.ingredientes = response.getIngredientes();
        this.passos       = response.getPassos();
        this.userId       = userId;
    }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public long getId()                           { return id; }
    public void setId(long id)                    { this.id = id; }

    public String getTitulo()                     { return titulo; }
    public void setTitulo(String titulo)          { this.titulo = titulo; }

    public String getDescricao()                  { return descricao; }
    public void setDescricao(String descricao)    { this.descricao = descricao; }

    public String getTempoPreparo()               { return tempoPreparo; }
    public void setTempoPreparo(String t)         { this.tempoPreparo = t; }

    public String getPorcoes()                    { return porcoes; }
    public void setPorcoes(String porcoes)        { this.porcoes = porcoes; }

    public String getDificuldade()                { return dificuldade; }
    public void setDificuldade(String d)          { this.dificuldade = d; }

    public List<String> getIngredientes()         { return ingredientes; }
    public void setIngredientes(List<String> i)   { this.ingredientes = i; }

    public List<String> getPassos()               { return passos; }
    public void setPassos(List<String> passos)    { this.passos = passos; }

    public String getImagemUrl()                  { return imagemUrl; }
    public void setImagemUrl(String imagemUrl)    { this.imagemUrl = imagemUrl; }

    public String getDataSalvo()                  { return dataSalvo; }
    public void setDataSalvo(String dataSalvo)    { this.dataSalvo = dataSalvo; }

    public String getUserId()                     { return userId; }
    public void setUserId(String userId)          { this.userId = userId; }

    @Override
    public String toString() {
        return "ReceitaSalva{id=" + id + ", titulo='" + titulo + "', dataSalvo='" + dataSalvo + "'}";
    }
}