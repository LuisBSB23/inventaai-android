package com.example.inventaai.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;
import com.example.inventaai.data.db.DatabaseContract.UserEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

// Gerencia a criação e atualização do banco de dados SQLite do InventaAí.

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "InventaAi.DB";

    public static final String DATABASE_NAME    = "inventaai.db";
    public static final int    DATABASE_VERSION = 4;   // era 3 na Sprint 3

    // =========================================================================
    // DDL — tabela USERS
    // =========================================================================

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + UserEntry.TABLE_NAME + " ("
                    + UserEntry._ID                + " TEXT PRIMARY KEY, "   // UUID
                    + UserEntry.COLUMN_NOME        + " TEXT NOT NULL, "
                    + UserEntry.COLUMN_SENHA_HASH  + " TEXT NOT NULL, "
                    + UserEntry.COLUMN_AVATAR      + " TEXT, "
                    + UserEntry.COLUMN_CREATED_AT  + " TEXT NOT NULL"
                    + ");";

    // =========================================================================
    // DDL — despensa_itens (com user_id)
    // =========================================================================

    private static final String SQL_CREATE_DESPENSA =
            "CREATE TABLE " + DespensaEntry.TABLE_NAME + " ("
                    + DespensaEntry._ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DespensaEntry.COLUMN_NOME          + " TEXT NOT NULL, "
                    + DespensaEntry.COLUMN_QUANTIDADE    + " REAL NOT NULL DEFAULT 0, "
                    + DespensaEntry.COLUMN_UNIDADE       + " TEXT, "
                    + DespensaEntry.COLUMN_DATA_VALIDADE + " TEXT, "
                    + DespensaEntry.COLUMN_STATUS        + " TEXT NOT NULL DEFAULT 'ATIVO', "
                    + DespensaEntry.COLUMN_USER_ID       + " TEXT"
                    + ");";

    // =========================================================================
    // DDL — historico_consumo (com user_id e nome_cached)
    // =========================================================================

    private static final String SQL_CREATE_HISTORICO =
            "CREATE TABLE " + HistoricoEntry.TABLE_NAME + " ("
                    + HistoricoEntry._ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + HistoricoEntry.COLUMN_ID_ITEM    + " INTEGER NOT NULL, "
                    + HistoricoEntry.COLUMN_DATA_ACAO  + " TEXT NOT NULL, "
                    + HistoricoEntry.COLUMN_MOTIVO     + " TEXT, "
                    + HistoricoEntry.COLUMN_NOME_CACHED + " TEXT, "
                    + HistoricoEntry.COLUMN_USER_ID    + " TEXT"
                    + ");";

    // =========================================================================
    // DROP helpers
    // =========================================================================

    private static final String SQL_DROP_USERS     = "DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME;
    private static final String SQL_DROP_DESPENSA  = "DROP TABLE IF EXISTS " + DespensaEntry.TABLE_NAME;
    private static final String SQL_DROP_HISTORICO = "DROP TABLE IF EXISTS " + HistoricoEntry.TABLE_NAME;

    // =========================================================================
    // Construtor
    // =========================================================================

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // =========================================================================
    // onCreate — banco criado do zero (instalação limpa)
    // =========================================================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_DESPENSA);
        db.execSQL(SQL_CREATE_HISTORICO);
        Log.d(TAG, "onCreate: tabelas criadas (versão " + DATABASE_VERSION + ").");
    }

    // =========================================================================
    // onUpgrade — migração incremental
    // =========================================================================

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: " + oldVersion + " → " + newVersion);

        // Versão 3 → 4: adiciona tabela users e colunas user_id
        if (oldVersion < 4) {
            migrarParaV4(db);
        }
    }

    private void migrarParaV4(SQLiteDatabase db) {
        // 1. Cria tabela users
        db.execSQL(SQL_CREATE_USERS);
        Log.d(TAG, "migrarParaV4: tabela users criada.");

        // 2. Adiciona colunas user_id (ALTER TABLE não suporta restrições FK em SQLite)
        try { db.execSQL("ALTER TABLE " + DespensaEntry.TABLE_NAME
                + " ADD COLUMN " + DespensaEntry.COLUMN_USER_ID + " TEXT"); }
        catch (Exception e) { Log.w(TAG, "Coluna user_id já existe em despensa_itens: " + e.getMessage()); }

        try { db.execSQL("ALTER TABLE " + HistoricoEntry.TABLE_NAME
                + " ADD COLUMN " + HistoricoEntry.COLUMN_USER_ID + " TEXT"); }
        catch (Exception e) { Log.w(TAG, "Coluna user_id já existe em historico_consumo: " + e.getMessage()); }

        // 3. Se existirem dados sem user_id, vincula ao usuário-padrão
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DespensaEntry.TABLE_NAME
                        + " WHERE " + DespensaEntry.COLUMN_USER_ID + " IS NULL", null);
        long semUser = 0;
        if (c.moveToFirst()) semUser = c.getLong(0);
        c.close();

        if (semUser > 0) {
            String hoje       = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String defaultId  = UUID.randomUUID().toString();
            String senhaHash  = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"; // "password" SHA-256

            ContentValues cv = new ContentValues();
            cv.put(UserEntry._ID,               defaultId);
            cv.put(UserEntry.COLUMN_NOME,       "Usuário");
            cv.put(UserEntry.COLUMN_SENHA_HASH, senhaHash);
            cv.put(UserEntry.COLUMN_CREATED_AT, hoje);
            db.insert(UserEntry.TABLE_NAME, null, cv);

            // Vincula registros órfãos ao usuário-padrão
            ContentValues update = new ContentValues();
            update.put(DespensaEntry.COLUMN_USER_ID, defaultId);
            db.update(DespensaEntry.TABLE_NAME, update,
                    DespensaEntry.COLUMN_USER_ID + " IS NULL", null);

            update = new ContentValues();
            update.put(HistoricoEntry.COLUMN_USER_ID, defaultId);
            db.update(HistoricoEntry.TABLE_NAME, update,
                    HistoricoEntry.COLUMN_USER_ID + " IS NULL", null);

            Log.d(TAG, "migrarParaV4: " + semUser + " registro(s) vinculados ao usuário-padrão id=" + defaultId
                    + ". Nome='Usuário', Senha='password'.");
        }

        Log.d(TAG, "migrarParaV4: concluída.");
    }
}