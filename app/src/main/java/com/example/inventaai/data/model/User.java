package com.example.inventaai.data.model;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;          // UUID gerado na criação
    private String nome;
    private String senhaHash;   // SHA-256 da senha — nunca armazenar texto puro
    private String avatarPath;  // caminho absoluto no armazenamento interno (pode ser null)
    private String createdAt;   // data de criação no formato YYYY-MM-DD

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public User() {}

    /** Construtor para criação de novo usuário (sem avatar ainda). */
    public User(String id, String nome, String senhaHash, String createdAt) {
        this.id        = id;
        this.nome      = nome;
        this.senhaHash = senhaHash;
        this.createdAt = createdAt;
    }

    /** Construtor completo — para reconstruir registros lidos do banco. */
    public User(String id, String nome, String senhaHash, String avatarPath, String createdAt) {
        this.id          = id;
        this.nome        = nome;
        this.senhaHash   = senhaHash;
        this.avatarPath  = avatarPath;
        this.createdAt   = createdAt;
    }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public String getId()                        { return id; }
    public void setId(String id)                 { this.id = id; }

    public String getNome()                      { return nome; }
    public void setNome(String nome)             { this.nome = nome; }

    public String getSenhaHash()                 { return senhaHash; }
    public void setSenhaHash(String senhaHash)   { this.senhaHash = senhaHash; }

    public String getAvatarPath()                { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public String getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(String createdAt)   { this.createdAt = createdAt; }

    /** Retorna as duas primeiras letras do nome em maiúsculo — usadas como iniciais no avatar. */
    public String getIniciais() {
        if (nome == null || nome.isEmpty()) return "?";
        String[] partes = nome.trim().split("\\s+");
        if (partes.length >= 2) {
            return String.valueOf(partes[0].charAt(0)).toUpperCase()
                    + String.valueOf(partes[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(nome.charAt(0)).toUpperCase();
    }

    /** Retorna os primeiros 8 caracteres do UUID para exibição abreviada. */
    public String getIdAbreviado() {
        if (id == null || id.length() < 8) return id;
        return id.substring(0, 8).toUpperCase();
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', nome='" + nome + "', createdAt='" + createdAt + "'}";
    }
}