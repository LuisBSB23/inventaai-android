package com.example.inventaai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.ReceitaEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.data.model.ReceitaSalva;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReceitaRepository {

    private static final String TAG = Constants.LOG_TAG;

    private final DatabaseHelper dbHelper;
    private final Gson           gson;
    /** Tipo para TypeToken de List<String>. */
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {}.getType();

    public ReceitaRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.gson     = new Gson();
    }

    // =========================================================================
    // SALVAR
    // =========================================================================

    public long salvar(ReceitaSalva receita) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            long id = db.insertOrThrow(ReceitaEntry.TABLE_NAME, null, toContentValues(receita));
            receita.setId(id);
            Log.d(TAG, "ReceitaRepository.salvar: receita id=" + id
                    + " — '" + receita.getTitulo() + "'");
            return id;
        } catch (Exception e) {
            Log.e(TAG, "ReceitaRepository.salvar: erro", e);
            return -1;
        }
    }

    // =========================================================================
    // LISTAR TODAS (por usuário)
    // =========================================================================

    public List<ReceitaSalva> listarTodas(String userId) {
        List<ReceitaSalva> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    ReceitaEntry.TABLE_NAME,
                    null,
                    ReceitaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ userId },
                    null, null,
                    ReceitaEntry.COLUMN_DATA_SALVO + " DESC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
            Log.d(TAG, "ReceitaRepository.listarTodas: " + lista.size() + " receita(s).");
        } catch (Exception e) {
            Log.e(TAG, "ReceitaRepository.listarTodas: erro", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    // =========================================================================
    // BUSCAR POR ID
    // =========================================================================

    public ReceitaSalva buscarPorId(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    ReceitaEntry.TABLE_NAME,
                    null,
                    ReceitaEntry._ID + " = ?",
                    new String[]{ String.valueOf(id) },
                    null, null, null, "1"
            );
            if (cursor.moveToFirst()) return fromCursor(cursor);
        } catch (Exception e) {
            Log.e(TAG, "ReceitaRepository.buscarPorId: erro id=" + id, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    public boolean deletar(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            int linhas = db.delete(
                    ReceitaEntry.TABLE_NAME,
                    ReceitaEntry._ID + " = ?",
                    new String[]{ String.valueOf(id) }
            );
            Log.d(TAG, "ReceitaRepository.deletar: id=" + id + " → " + linhas + " linha(s)");
            return linhas > 0;
        } catch (Exception e) {
            Log.e(TAG, "ReceitaRepository.deletar: erro id=" + id, e);
            return false;
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private ContentValues toContentValues(ReceitaSalva r) {
        ContentValues cv = new ContentValues();
        cv.put(ReceitaEntry.COLUMN_TITULO,        r.getTitulo());
        cv.put(ReceitaEntry.COLUMN_DESCRICAO,     r.getDescricao());
        cv.put(ReceitaEntry.COLUMN_TEMPO_PREPARO, r.getTempoPreparo());
        cv.put(ReceitaEntry.COLUMN_PORCOES,       r.getPorcoes());
        cv.put(ReceitaEntry.COLUMN_DIFICULDADE,   r.getDificuldade());
        // Serializa listas para JSON
        cv.put(ReceitaEntry.COLUMN_INGREDIENTES,  gson.toJson(r.getIngredientes()));
        cv.put(ReceitaEntry.COLUMN_PASSOS,        gson.toJson(r.getPassos()));
        cv.put(ReceitaEntry.COLUMN_IMAGEM_URL,    r.getImagemUrl());
        // Se dataSalvo não foi preenchida, usa o dia de hoje
        String data = r.getDataSalvo() != null ? r.getDataSalvo() : DateUtils.hoje();
        cv.put(ReceitaEntry.COLUMN_DATA_SALVO, data);
        cv.put(ReceitaEntry.COLUMN_USER_ID,    r.getUserId());
        return cv;
    }

    private ReceitaSalva fromCursor(Cursor c) {
        ReceitaSalva r = new ReceitaSalva();
        r.setId(          c.getLong(  c.getColumnIndexOrThrow(ReceitaEntry._ID)));
        r.setTitulo(      c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_TITULO)));
        r.setDescricao(   c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_DESCRICAO)));
        r.setTempoPreparo(c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_TEMPO_PREPARO)));
        r.setPorcoes(     c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_PORCOES)));
        r.setDificuldade( c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_DIFICULDADE)));
        r.setImagemUrl(   c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_IMAGEM_URL)));
        r.setDataSalvo(   c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_DATA_SALVO)));
        r.setUserId(      c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_USER_ID)));

        // Desserializa JSON → List<String>
        String jsonIngredientes = c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_INGREDIENTES));
        String jsonPassos       = c.getString(c.getColumnIndexOrThrow(ReceitaEntry.COLUMN_PASSOS));

        if (jsonIngredientes != null && !jsonIngredientes.isEmpty()) {
            r.setIngredientes(gson.fromJson(jsonIngredientes, LIST_STRING_TYPE));
        }
        if (jsonPassos != null && !jsonPassos.isEmpty()) {
            r.setPassos(gson.fromJson(jsonPassos, LIST_STRING_TYPE));
        }
        return r;
    }
}