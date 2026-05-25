package com.example.inventaai.data.db;

import android.provider.BaseColumns;

/**
 * Define as constantes do esquema do banco de dados (tabelas e colunas).
 * Usar estas constantes evita erros de digitação e garante consistência.
 */
public final class DatabaseContract {

    private DatabaseContract() {}

    /**
     * Constantes para a tabela de itens da despensa.
     */
    public static class DespensaEntry implements BaseColumns {
        public static final String TABLE_NAME           = "despensa_itens";
        public static final String COLUMN_NOME          = "nome";
        public static final String COLUMN_QUANTIDADE    = "quantidade";
        public static final String COLUMN_UNIDADE       = "unidade_medida";
        public static final String COLUMN_DATA_VALIDADE = "data_validade";
        public static final String COLUMN_STATUS        = "status";
    }

    /**
     * Constantes para a tabela de histórico de consumo.
     * Sprint 3: adicionada coluna nome_cached para exibir o nome do item
     * sem necessidade de JOIN (o item pode ter sido deletado da despensa).
     */
    public static class HistoricoEntry implements BaseColumns {
        public static final String TABLE_NAME        = "historico_consumo";
        public static final String COLUMN_ID_ITEM    = "id_item";
        public static final String COLUMN_DATA_ACAO  = "data_acao";
        public static final String COLUMN_MOTIVO     = "motivo";
        public static final String COLUMN_NOME_CACHED = "nome_item"; // nome denormalizado
    }
}
