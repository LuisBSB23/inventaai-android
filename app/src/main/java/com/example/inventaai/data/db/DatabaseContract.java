package com.example.inventaai.data.db;

import android.provider.BaseColumns;

public final class DatabaseContract {

    private DatabaseContract() {}

    // =========================================================================
    // Tabela: users
    // =========================================================================

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME        = "users";
        public static final String COLUMN_NOME       = "nome";
        public static final String COLUMN_SENHA_HASH = "senha_hash";
        public static final String COLUMN_AVATAR     = "avatar_path";
        public static final String COLUMN_CREATED_AT = "created_at";
    }

    // =========================================================================
    // Tabela: despensa_itens
    // =========================================================================

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

    // =========================================================================
    // Tabela: historico_consumo
    // =========================================================================

    public static class HistoricoEntry implements BaseColumns {
        public static final String TABLE_NAME         = "historico_consumo";
        public static final String COLUMN_ID_ITEM     = "id_item";
        public static final String COLUMN_DATA_ACAO   = "data_acao";
        public static final String COLUMN_MOTIVO      = "motivo";
        public static final String COLUMN_NOME_CACHED = "nome_item";
        public static final String COLUMN_USER_ID     = "user_id";
    }

    // =========================================================================
    // Tabela: receitas_salvas
    // =========================================================================

    public static class ReceitaEntry implements BaseColumns {
        public static final String TABLE_NAME           = "receitas_salvas";
        public static final String COLUMN_TITULO        = "titulo";
        public static final String COLUMN_DESCRICAO     = "descricao";
        public static final String COLUMN_TEMPO_PREPARO = "tempo_preparo";
        public static final String COLUMN_PORCOES       = "porcoes";
        public static final String COLUMN_DIFICULDADE   = "dificuldade";
        public static final String COLUMN_INGREDIENTES  = "ingredientes";
        public static final String COLUMN_PASSOS        = "passos";
        public static final String COLUMN_IMAGEM_URL    = "imagem_url";
        public static final String COLUMN_DATA_SALVO    = "data_salvo";
        public static final String COLUMN_USER_ID       = "user_id";
        // Sprint 14: status de execução da receita
        public static final String COLUMN_STATUS        = "status_execucao";
    }

    // =========================================================================
    // Constantes de status de execução de receita (Sprint 14)
    // =========================================================================

    public static final String RECEITA_STATUS_SALVA       = "SALVA";
    public static final String RECEITA_STATUS_EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String RECEITA_STATUS_FINALIZADA   = "FINALIZADA";
}