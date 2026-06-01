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

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "InventaAi.DB";

    public static final String DATABASE_NAME    = "inventaai.db";
    // v5: adiciona coluna "categoria" em despensa_itens
    public static final int    DATABASE_VERSION = 5;

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + UserEntry.TABLE_NAME + " ("
                    + UserEntry._ID                + " TEXT PRIMARY KEY, "
                    + UserEntry.COLUMN_NOME        + " TEXT NOT NULL, "
                    + UserEntry.COLUMN_SENHA_HASH  + " TEXT NOT NULL, "
                    + UserEntry.COLUMN_AVATAR      + " TEXT, "
                    + UserEntry.COLUMN_CREATED_AT  + " TEXT NOT NULL"
                    + ");";

    private static final String SQL_CREATE_DESPENSA =
            "CREATE TABLE " + DespensaEntry.TABLE_NAME + " ("
                    + DespensaEntry._ID                  + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DespensaEntry.COLUMN_NOME          + " TEXT NOT NULL, "
                    + DespensaEntry.COLUMN_QUANTIDADE    + " REAL NOT NULL DEFAULT 0, "
                    + DespensaEntry.COLUMN_UNIDADE       + " TEXT, "
                    + DespensaEntry.COLUMN_DATA_VALIDADE + " TEXT, "
                    + DespensaEntry.COLUMN_STATUS        + " TEXT NOT NULL DEFAULT 'ATIVO', "
                    + DespensaEntry.COLUMN_USER_ID       + " TEXT, "
                    + DespensaEntry.COLUMN_CATEGORIA     + " TEXT"   // Sprint 6
                    + ");";

    private static final String SQL_CREATE_HISTORICO =
            "CREATE TABLE " + HistoricoEntry.TABLE_NAME + " ("
                    + HistoricoEntry._ID                + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + HistoricoEntry.COLUMN_ID_ITEM     + " INTEGER NOT NULL, "
                    + HistoricoEntry.COLUMN_DATA_ACAO   + " TEXT NOT NULL, "
                    + HistoricoEntry.COLUMN_MOTIVO      + " TEXT, "
                    + HistoricoEntry.COLUMN_NOME_CACHED + " TEXT, "
                    + HistoricoEntry.COLUMN_USER_ID     + " TEXT"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_DESPENSA);
        db.execSQL(SQL_CREATE_HISTORICO);
        Log.d(TAG, "onCreate: tabelas criadas (v" + DATABASE_VERSION + ").");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: " + oldVersion + " → " + newVersion);
        if (oldVersion < 4) migrarParaV4(db);
        if (oldVersion < 5) migrarParaV5(db);
    }

    // ── Migração v4: adiciona users + user_id ────────────────────────────────
    private void migrarParaV4(SQLiteDatabase db) {
        try { db.execSQL(SQL_CREATE_USERS); }
        catch (Exception e) { Log.w(TAG, "v4: users já existe"); }

        try { db.execSQL("ALTER TABLE " + DespensaEntry.TABLE_NAME
                + " ADD COLUMN " + DespensaEntry.COLUMN_USER_ID + " TEXT"); }
        catch (Exception e) { Log.w(TAG, "v4: user_id já existe em despensa"); }

        try { db.execSQL("ALTER TABLE " + HistoricoEntry.TABLE_NAME
                + " ADD COLUMN " + HistoricoEntry.COLUMN_USER_ID + " TEXT"); }
        catch (Exception e) { Log.w(TAG, "v4: user_id já existe em historico"); }

        // Vincula registros órfãos a um usuário-padrão
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

    // ── Migração v5: adiciona coluna categoria em despensa_itens ─────────────
    private void migrarParaV5(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + DespensaEntry.TABLE_NAME
                    + " ADD COLUMN " + DespensaEntry.COLUMN_CATEGORIA + " TEXT");
            Log.d(TAG, "migrarParaV5: coluna 'categoria' adicionada.");
        } catch (Exception e) {
            Log.w(TAG, "migrarParaV5: coluna já existe — " + e.getMessage());
        }
    }
}