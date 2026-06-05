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
import java.util.Collections;
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
    // VERIFICAR DUPLICIDADE  —  Sprint 13
    // =========================================================================

    /**
     * Verifica se já existe uma receita com o mesmo título E a mesma lista de
     * ingredientes (comparação via JSON com ingredientes ordenados
     * alfabeticamente para evitar falsos negativos por diferença de ordem).
     *
     * @param receita A receita candidata a ser salva.
     * @return {@code true} se já existir uma receita idêntica; {@code false} caso contrário.
     */
    public boolean receitaJaExiste(ReceitaSalva receita) {
        if (receita == null || receita.getTitulo() == null) return false;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Busca todas as receitas do mesmo usuário com o mesmo título
            cursor = db.query(
                    ReceitaEntry.TABLE_NAME,
                    new String[]{ ReceitaEntry._ID, ReceitaEntry.COLUMN_INGREDIENTES },
                    ReceitaEntry.COLUMN_TITULO   + " = ? AND "
                            + ReceitaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ receita.getTitulo(), receita.getUserId() },
                    null, null, null
            );

            // Se não há receita com mesmo título, não é duplicata
            if (!cursor.moveToFirst()) return false;

            // Normaliza os ingredientes da nova receita (ordenados) para comparar
            String jsonNova = normalizarIngredientes(receita.getIngredientes());

            // Compara ingredientes com cada receita existente de mesmo título
            do {
                int col = cursor.getColumnIndex(ReceitaEntry.COLUMN_INGREDIENTES);
                if (col < 0) continue;

                String jsonExistente = cursor.getString(col);
                if (jsonExistente == null) continue;

                // Desserializa e normaliza os ingredientes existentes
                List<String> listaExistente = gson.fromJson(jsonExistente, LIST_STRING_TYPE);
                String jsonExistenteNorm    = normalizarIngredientes(listaExistente);

                if (jsonNova.equals(jsonExistenteNorm)) {
                    Log.d(TAG, "receitaJaExiste: duplicata detectada para título='"
                            + receita.getTitulo() + "'");
                    return true;
                }
            } while (cursor.moveToNext());

            return false;

        } catch (Exception e) {
            Log.e(TAG, "receitaJaExiste: erro ao verificar duplicidade", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Serializa uma lista de ingredientes em JSON com os itens ordenados
     * alfabeticamente (case-insensitive), garantindo comparação consistente
     * independente da ordem original retornada pela IA.
     */
    private String normalizarIngredientes(List<String> ingredientes) {
        if (ingredientes == null || ingredientes.isEmpty()) return "[]";
        List<String> copia = new ArrayList<>(ingredientes);
        Collections.sort(copia, String.CASE_INSENSITIVE_ORDER);
        return gson.toJson(copia);
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