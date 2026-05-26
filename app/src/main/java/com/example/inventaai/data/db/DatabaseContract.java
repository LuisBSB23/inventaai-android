package com.example.inventaai.data.db;

import android.provider.BaseColumns;

/**
 * Define as constantes do esquema do banco de dados (tabelas e colunas).
 * Sprint 1: adicionada tabela USERS e coluna user_id nas tabelas existentes.
 */
public final class DatabaseContract {

    private DatabaseContract() {}

    /**
     * Tabela de usuários locais — Sprint 1.
     */
    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME        = "users";
        public static final String COLUMN_NOME       = "nome";
        public static final String COLUMN_SENHA_HASH = "senha_hash";   // SHA-256
        public static final String COLUMN_AVATAR     = "avatar_path";  // caminho interno (nullable)
        public static final String COLUMN_CREATED_AT = "created_at";   // YYYY-MM-DD
        // _ID aqui é o UUID (TEXT), não INTEGER AUTOINCREMENT
    }

    /**
     * Constantes para a tabela de itens da despensa.
     * Sprint 1: adicionada coluna user_id.
     */
    public static class DespensaEntry implements BaseColumns {
        public static final String TABLE_NAME           = "despensa_itens";
        public static final String COLUMN_NOME          = "nome";
        public static final String COLUMN_QUANTIDADE    = "quantidade";
        public static final String COLUMN_UNIDADE       = "unidade_medida";
        public static final String COLUMN_DATA_VALIDADE = "data_validade";
        public static final String COLUMN_STATUS        = "status";
        public static final String COLUMN_USER_ID       = "user_id";   // FK → users._id
    }

    /**
     * Constantes para a tabela de histórico de consumo.
     * Sprint 1: adicionada coluna user_id.
     * Sprint 3: adicionada coluna nome_cached para exibir o nome do item
     * sem necessidade de JOIN (o item pode ter sido deletado da despensa).
     */
    public static class HistoricoEntry implements BaseColumns {
        public static final String TABLE_NAME         = "historico_consumo";
        public static final String COLUMN_ID_ITEM     = "id_item";
        public static final String COLUMN_DATA_ACAO   = "data_acao";
        public static final String COLUMN_MOTIVO      = "motivo";
        public static final String COLUMN_NOME_CACHED = "nome_item";   // nome denormalizado
        public static final String COLUMN_USER_ID     = "user_id";     // FK → users._id
    }
}