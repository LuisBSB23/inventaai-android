package com.example.inventaai.data.db;

import android.provider.BaseColumns;

public final class DatabaseContract {

    private DatabaseContract() {}

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME        = "users";
        public static final String COLUMN_NOME       = "nome";
        public static final String COLUMN_SENHA_HASH = "senha_hash";
        public static final String COLUMN_AVATAR     = "avatar_path";
        public static final String COLUMN_CREATED_AT = "created_at";
    }

    public static class DespensaEntry implements BaseColumns {
        public static final String TABLE_NAME           = "despensa_itens";
        public static final String COLUMN_NOME          = "nome";
        public static final String COLUMN_QUANTIDADE    = "quantidade";
        public static final String COLUMN_UNIDADE       = "unidade_medida";
        public static final String COLUMN_DATA_VALIDADE = "data_validade";
        public static final String COLUMN_STATUS        = "status";
        public static final String COLUMN_USER_ID       = "user_id";
        public static final String COLUMN_CATEGORIA     = "categoria";
    }

    public static class HistoricoEntry implements BaseColumns {
        public static final String TABLE_NAME         = "historico_consumo";
        public static final String COLUMN_ID_ITEM     = "id_item";
        public static final String COLUMN_DATA_ACAO   = "data_acao";
        public static final String COLUMN_MOTIVO      = "motivo";
        public static final String COLUMN_NOME_CACHED = "nome_item";
        public static final String COLUMN_USER_ID     = "user_id";
    }
}