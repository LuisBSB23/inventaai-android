package com.example.inventaai.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;

/**
 * Gerencia a criação e atualização do banco de dados SQLite do InventaAí.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME    = "inventaai.db";
    public static final int    DATABASE_VERSION = 2; // incrementado para forçar onUpgrade

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

    // SQL para criar a tabela historico_consumo.
    // Sem FOREIGN KEY: o id_item é rastreabilidade, não integridade referencial.
    // Uma FK impediria deletar o item da despensa enquanto existe registro no histórico.
    private static final String SQL_CREATE_HISTORICO =
            "CREATE TABLE " + HistoricoEntry.TABLE_NAME + " ("
                    + HistoricoEntry._ID              + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + HistoricoEntry.COLUMN_ID_ITEM   + " INTEGER NOT NULL, "
                    + HistoricoEntry.COLUMN_DATA_ACAO + " TEXT NOT NULL, "
                    + HistoricoEntry.COLUMN_MOTIVO    + " TEXT"
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
        // Recria as tabelas limpas a cada upgrade de versão durante desenvolvimento.
        db.execSQL(SQL_DROP_HISTORICO);
        db.execSQL(SQL_DROP_DESPENSA);
        onCreate(db);
    }

    // onConfigure removido: sem FK, não precisamos de setForeignKeyConstraintsEnabled.
}