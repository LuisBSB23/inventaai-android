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
            Log.d(TAG, "ReceitaRepository.salvar: id=" + id + " — '" + receita.getTitulo() + "'");
            return id;
        } catch (Exception e) {
            Log.e(TAG, "ReceitaRepository.salvar: erro", e);
            return -1;
        }
    }

    // =========================================================================
    // VERIFICAR DUPLICIDADE
    // =========================================================================

    public boolean receitaJaExiste(ReceitaSalva receita) {
        if (receita == null || receita.getTitulo() == null) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    ReceitaEntry.TABLE_NAME,
                    new String[]{ ReceitaEntry._ID },
                    ReceitaEntry.COLUMN_TITULO   + " = ? AND "
                            + ReceitaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ receita.getTitulo().trim(), receita.getUserId() },
                    null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "receitaJaExiste: duplicata detectada — '" + receita.getTitulo() + "'");
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "receitaJaExiste: erro", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
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
                    ReceitaEntry.TABLE_NAME, null,
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
    // BUSCAR POR TÍTULO OU INGREDIENTE — Sprint 14
    // =========================================================================

    public List<ReceitaSalva> buscarPorTituloOuIngrediente(String query, String userId) {
        if (query == null || query.trim().isEmpty()) return listarTodas(userId);

        List<ReceitaSalva> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String filtro = "%" + query.trim().toLowerCase() + "%";
            cursor = db.query(
                    ReceitaEntry.TABLE_NAME, null,
                    ReceitaEntry.COLUMN_USER_ID + " = ? AND ("
                            + "LOWER(" + ReceitaEntry.COLUMN_TITULO + ") LIKE ? OR "
                            + "LOWER(" + ReceitaEntry.COLUMN_INGREDIENTES + ") LIKE ?)",
                    new String[]{ userId, filtro, filtro },
                    null, null,
                    ReceitaEntry.COLUMN_DATA_SALVO + " DESC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
            Log.d(TAG, "buscarPorTituloOuIngrediente: '" + query + "' → " + lista.size() + " resultado(s).");
        } catch (Exception e) {
            Log.e(TAG, "buscarPorTituloOuIngrediente: erro", e);
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
                    ReceitaEntry.TABLE_NAME, null,
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
    // ATUALIZAR STATUS — Sprint 14
    // =========================================================================

    public boolean atualizarStatusReceita(long receitaId, String novoStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(ReceitaEntry.COLUMN_STATUS, novoStatus);
            int linhas = db.update(
                    ReceitaEntry.TABLE_NAME, cv,
                    ReceitaEntry._ID + " = ?",
                    new String[]{ String.valueOf(receitaId) }
            );
            Log.d(TAG, "atualizarStatusReceita: id=" + receitaId
                    + " → '" + novoStatus + "' (" + linhas + " linha(s))");
            return linhas > 0;
        } catch (Exception e) {
            Log.e(TAG, "atualizarStatusReceita: erro id=" + receitaId, e);
            return false;
        }
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
        cv.put(ReceitaEntry.COLUMN_TITULO,        r.getTitulo().trim());
        cv.put(ReceitaEntry.COLUMN_DESCRICAO,     r.getDescricao());
        cv.put(ReceitaEntry.COLUMN_TEMPO_PREPARO, r.getTempoPreparo());
        cv.put(ReceitaEntry.COLUMN_PORCOES,       r.getPorcoes());
        cv.put(ReceitaEntry.COLUMN_DIFICULDADE,   r.getDificuldade());
        cv.put(ReceitaEntry.COLUMN_INGREDIENTES,  gson.toJson(r.getIngredientes()));
        cv.put(ReceitaEntry.COLUMN_PASSOS,        gson.toJson(r.getPassos()));
        cv.put(ReceitaEntry.COLUMN_IMAGEM_URL,    r.getImagemUrl());
        String data = r.getDataSalvo() != null ? r.getDataSalvo() : DateUtils.hoje();
        cv.put(ReceitaEntry.COLUMN_DATA_SALVO, data);
        cv.put(ReceitaEntry.COLUMN_USER_ID,    r.getUserId());
        cv.put(ReceitaEntry.COLUMN_STATUS,     r.getStatus() != null ? r.getStatus() : "SALVA");
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

        // Sprint 14: lê status (coluna pode não existir em registros antigos via migração)
        int statusCol = c.getColumnIndex(ReceitaEntry.COLUMN_STATUS);
        if (statusCol >= 0 && !c.isNull(statusCol)) {
            r.setStatus(c.getString(statusCol));
        } else {
            r.setStatus("SALVA");
        }

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