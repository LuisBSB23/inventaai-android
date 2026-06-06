package com.example.inventaai.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;
import com.example.inventaai.data.db.DatabaseContract.ReceitaEntry;
import com.example.inventaai.data.db.DatabaseContract.UserEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "InventaAi.DB";

    public static final String DATABASE_NAME    = "inventaai.db";
    // v8: Sprint 15 — adiciona coluna origem em historico_consumo
    public static final int    DATABASE_VERSION = 8;

    // =========================================================================
    // Singleton
    // =========================================================================

    private static volatile DatabaseHelper instance;

    public static DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(ctx.getApplicationContext());
                    Log.d(TAG, "getInstance: instância Singleton criada.");
                }
            }
        }
        return instance;
    }

    // =========================================================================
    // SQL de criação das tabelas
    // =========================================================================

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + UserEntry.TABLE_NAME + " ("
                    + UserEntry._ID                + " TEXT PRIMARY KEY, "
                    + UserEntry.COLUMN_NOME        + " TEXT NOT NULL, "
                    + UserEntry.COLUMN_SENHA_HASH  + " TEXT NOT NULL, "
                    + UserEntry.COLUMN_AVATAR      + " TEXT, "
                    + UserEntry.COLUMN_CREATED_AT  + " TEXT NOT NULL"
                    + ");";

    // REAL garante suporte a decimais (ex: 0.5, 1.75)
    private static final String SQL_CREATE_DESPENSA =
            "CREATE TABLE " + DespensaEntry.TABLE_NAME + " ("
                    + DespensaEntry._ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DespensaEntry.COLUMN_NOME          + " TEXT NOT NULL, "
                    + DespensaEntry.COLUMN_QUANTIDADE    + " REAL NOT NULL DEFAULT 0, "
                    + DespensaEntry.COLUMN_UNIDADE       + " TEXT, "
                    + DespensaEntry.COLUMN_DATA_VALIDADE + " TEXT, "
                    + DespensaEntry.COLUMN_STATUS        + " TEXT NOT NULL DEFAULT 'ATIVO', "
                    + DespensaEntry.COLUMN_USER_ID       + " TEXT, "
                    + DespensaEntry.COLUMN_CATEGORIA     + " TEXT"
                    + ");";

    // Sprint 15: inclui coluna origem
    private static final String SQL_CREATE_HISTORICO =
            "CREATE TABLE " + HistoricoEntry.TABLE_NAME + " ("
                    + HistoricoEntry._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + HistoricoEntry.COLUMN_ID_ITEM     + " INTEGER NOT NULL, "
                    + HistoricoEntry.COLUMN_DATA_ACAO   + " TEXT NOT NULL, "
                    + HistoricoEntry.COLUMN_MOTIVO      + " TEXT, "
                    + HistoricoEntry.COLUMN_NOME_CACHED + " TEXT, "
                    + HistoricoEntry.COLUMN_USER_ID     + " TEXT, "
                    + HistoricoEntry.COLUMN_ORIGEM      + " TEXT"
                    + ");";

    private static final String SQL_CREATE_RECEITAS =
            "CREATE TABLE " + ReceitaEntry.TABLE_NAME + " ("
                    + ReceitaEntry._ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ReceitaEntry.COLUMN_TITULO        + " TEXT NOT NULL, "
                    + ReceitaEntry.COLUMN_DESCRICAO     + " TEXT, "
                    + ReceitaEntry.COLUMN_TEMPO_PREPARO + " TEXT, "
                    + ReceitaEntry.COLUMN_PORCOES       + " TEXT, "
                    + ReceitaEntry.COLUMN_DIFICULDADE   + " TEXT, "
                    + ReceitaEntry.COLUMN_INGREDIENTES  + " TEXT, "    // JSON
                    + ReceitaEntry.COLUMN_PASSOS        + " TEXT, "    // JSON
                    + ReceitaEntry.COLUMN_IMAGEM_URL    + " TEXT, "
                    + ReceitaEntry.COLUMN_DATA_SALVO    + " TEXT NOT NULL, "
                    + ReceitaEntry.COLUMN_USER_ID       + " TEXT, "
                    + ReceitaEntry.COLUMN_STATUS        + " TEXT NOT NULL DEFAULT 'SALVA'"
                    + ");";

    // =========================================================================
    // Construtor privado
    // =========================================================================

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // =========================================================================
    // Ciclo de vida do SQLiteOpenHelper
    // =========================================================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_DESPENSA);
        db.execSQL(SQL_CREATE_HISTORICO);
        db.execSQL(SQL_CREATE_RECEITAS);
        Log.d(TAG, "onCreate: tabelas criadas (v" + DATABASE_VERSION + ").");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: " + oldVersion + " → " + newVersion);
        if (oldVersion < 4) migrarParaV4(db);
        if (oldVersion < 5) migrarParaV5(db);
        if (oldVersion < 6) migrarParaV6(db);
        if (oldVersion < 7) migrarParaV7(db);
        if (oldVersion < 8) migrarParaV8(db); // Sprint 15
    }

    // =========================================================================
    // Migrações
    // =========================================================================

    private void migrarParaV4(SQLiteDatabase db) {
        try { db.execSQL(SQL_CREATE_USERS); }
        catch (Exception e) { Log.w(TAG, "v4: users já existe"); }

        try { db.execSQL("ALTER TABLE " + DespensaEntry.TABLE_NAME
                + " ADD COLUMN " + DespensaEntry.COLUMN_USER_ID + " TEXT"); }
        catch (Exception e) { Log.w(TAG, "v4: user_id já existe em despensa"); }

        try { db.execSQL("ALTER TABLE " + HistoricoEntry.TABLE_NAME
                + " ADD COLUMN " + HistoricoEntry.COLUMN_USER_ID + " TEXT"); }
        catch (Exception e) { Log.w(TAG, "v4: user_id já existe em historico"); }

        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DespensaEntry.TABLE_NAME
                        + " WHERE " + DespensaEntry.COLUMN_USER_ID + " IS NULL", null);
        long semUser = 0;
        if (c.moveToFirst()) semUser = c.getLong(0);
        c.close();

        if (semUser > 0) {
            String hoje      = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String defaultId = UUID.randomUUID().toString();
            String senhaHash = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

            ContentValues cv = new ContentValues();
            cv.put(UserEntry._ID, defaultId);
            cv.put(UserEntry.COLUMN_NOME, "Usuário");
            cv.put(UserEntry.COLUMN_SENHA_HASH, senhaHash);
            cv.put(UserEntry.COLUMN_CREATED_AT, hoje);
            db.insert(UserEntry.TABLE_NAME, null, cv);

            ContentValues upd = new ContentValues();
            upd.put(DespensaEntry.COLUMN_USER_ID, defaultId);
            db.update(DespensaEntry.TABLE_NAME, upd, DespensaEntry.COLUMN_USER_ID + " IS NULL", null);
            upd = new ContentValues();
            upd.put(HistoricoEntry.COLUMN_USER_ID, defaultId);
            db.update(HistoricoEntry.TABLE_NAME, upd, HistoricoEntry.COLUMN_USER_ID + " IS NULL", null);
        }
        Log.d(TAG, "migrarParaV4: concluída.");
    }

    private void migrarParaV5(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + DespensaEntry.TABLE_NAME
                    + " ADD COLUMN " + DespensaEntry.COLUMN_CATEGORIA + " TEXT");
            Log.d(TAG, "migrarParaV5: coluna 'categoria' adicionada.");
        } catch (Exception e) {
            Log.w(TAG, "migrarParaV5: coluna já existe — " + e.getMessage());
        }
    }

    private void migrarParaV6(SQLiteDatabase db) {
        try {
            db.execSQL(SQL_CREATE_RECEITAS);
            Log.d(TAG, "migrarParaV6: tabela 'receitas_salvas' criada.");
        } catch (Exception e) {
            Log.w(TAG, "migrarParaV6: tabela já existe — " + e.getMessage());
        }
    }

    private void migrarParaV7(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + ReceitaEntry.TABLE_NAME
                    + " ADD COLUMN " + ReceitaEntry.COLUMN_STATUS
                    + " TEXT NOT NULL DEFAULT 'SALVA'");
            Log.d(TAG, "migrarParaV7: coluna 'status_execucao' adicionada em receitas_salvas.");
        } catch (Exception e) {
            Log.w(TAG, "migrarParaV7: coluna já existe — " + e.getMessage());
        }
    }

    /** Sprint 15: adiciona coluna origem na tabela historico_consumo. */
    private void migrarParaV8(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + HistoricoEntry.TABLE_NAME
                    + " ADD COLUMN " + HistoricoEntry.COLUMN_ORIGEM + " TEXT");
            Log.d(TAG, "migrarParaV8: coluna 'origem' adicionada em historico_consumo.");
        } catch (Exception e) {
            Log.w(TAG, "migrarParaV8: coluna já existe — " + e.getMessage());
        }
    }
}