package com.example.inventaai.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;

/**
 * Gerencia a criação e atualização do banco de dados SQLite do InventaAí.
 *
 * Sprint 3 — versão 3:
 * - Adicionada coluna nome_item à tabela historico_consumo para exibir
 *   o nome do item mesmo após ele ser removido da despensa (desnormalização).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME    = "inventaai.db";
    public static final int    DATABASE_VERSION = 3;

    // SQL para criar a tabela despensa_itens
    private static final String SQL_CREATE_DESPENSA =
            "CREATE TABLE " + DespensaEntry.TABLE_NAME + " ("
                    + DespensaEntry._ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DespensaEntry.COLUMN_NOME          + " TEXT NOT NULL, "
                    + DespensaEntry.COLUMN_QUANTIDADE    + " REAL NOT NULL DEFAULT 0, "
                    + DespensaEntry.COLUMN_UNIDADE       + " TEXT, "
                    + DespensaEntry.COLUMN_DATA_VALIDADE + " TEXT, "
                    + DespensaEntry.COLUMN_STATUS        + " TEXT NOT NULL DEFAULT 'ATIVO'"
                    + ");";

    // SQL para criar a tabela historico_consumo (com nome_item denormalizado)
    private static final String SQL_CREATE_HISTORICO =
            "CREATE TABLE " + HistoricoEntry.TABLE_NAME + " ("
                    + HistoricoEntry._ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + HistoricoEntry.COLUMN_ID_ITEM    + " INTEGER NOT NULL, "
                    + HistoricoEntry.COLUMN_DATA_ACAO  + " TEXT NOT NULL, "
                    + HistoricoEntry.COLUMN_MOTIVO     + " TEXT, "
                    + HistoricoEntry.COLUMN_NOME_CACHED + " TEXT"
                    + ");";

    private static final String SQL_DROP_DESPENSA  =
            "DROP TABLE IF EXISTS " + DespensaEntry.TABLE_NAME;
    private static final String SQL_DROP_HISTORICO =
            "DROP TABLE IF EXISTS " + HistoricoEntry.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DESPENSA);
        db.execSQL(SQL_CREATE_HISTORICO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Durante o desenvolvimento: descarta e recria as tabelas.
        // Em produção isso seria uma migração incremental com ALTER TABLE.
        db.execSQL(SQL_DROP_HISTORICO);
        db.execSQL(SQL_DROP_DESPENSA);
        onCreate(db);
    }
}
