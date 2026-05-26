package com.example.inventaai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula todas as operações CRUD para a tabela despensa_itens.
 * A camada de UI nunca acessa o SQLiteDatabase diretamente.
 *
 * Sprint 1: todos os métodos de leitura/escrita agora filtram por user_id,
 * garantindo isolamento total entre perfis distintos.
 */
public class DespensaRepository {

    private static final String TAG = Constants.LOG_TAG;
    private final DatabaseHelper dbHelper;

    public DespensaRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // =========================================================================
    // INSERIR
    // =========================================================================

    /**
     * Insere um novo item na despensa vinculado ao usuário logado.
     *
     * @param item   DespensaItem a ser inserido (id será ignorado).
     * @param userId UUID do usuário proprietário do item.
     * @return ID gerado pelo banco, ou -1 em caso de erro.
     */
    public long inserir(DespensaItem item, String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long novoId = -1;
        try {
            ContentValues values = toContentValues(item, userId);
            novoId = db.insert(DespensaEntry.TABLE_NAME, null, values);
            Log.d(TAG, "inserir: item '" + item.getNome() + "' inserido com id=" + novoId
                    + " para userId=" + userId);
        } catch (Exception e) {
            Log.e(TAG, "inserir: erro ao inserir item", e);
        } finally {
            db.close();
        }
        return novoId;
    }

    // =========================================================================
    // BUSCAR POR ID
    // =========================================================================

    /**
     * Busca um único item pelo seu ID (sem filtro de usuário — usado internamente).
     */
    public DespensaItem buscarPorId(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DespensaEntry.TABLE_NAME, null,
                    DespensaEntry._ID + " = ?",
                    new String[]{ String.valueOf(id) },
                    null, null, null, "1"
            );
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "buscarPorId: erro ao buscar id=" + id, e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return null;
    }

    // =========================================================================
    // LISTAR ATIVOS (filtrado por usuário)
    // =========================================================================

    /**
     * Retorna todos os itens ATIVOS do usuário informado,
     * ordenados por data de validade (mais próximo primeiro).
     */
    public List<DespensaItem> listarAtivos(String userId) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DespensaEntry.TABLE_NAME, null,
                    DespensaEntry.COLUMN_STATUS  + " = ? AND "
                            + DespensaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ Constants.STATUS_ATIVO, userId },
                    null, null,
                    DespensaEntry.COLUMN_DATA_VALIDADE + " ASC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
            Log.d(TAG, "listarAtivos: " + lista.size() + " item(ns) para userId=" + userId);
        } catch (Exception e) {
            Log.e(TAG, "listarAtivos: erro", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR TODOS (filtrado por usuário)
    // =========================================================================

    public List<DespensaItem> listarTodos(String userId) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DespensaEntry.TABLE_NAME, null,
                    DespensaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ userId },
                    null, null,
                    DespensaEntry.COLUMN_DATA_VALIDADE + " ASC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarTodos: erro", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR PRÓXIMOS DO VENCIMENTO (filtrado por usuário)
    // =========================================================================

    public List<DespensaItem> listarProximosVencimento(int dias, String userId) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String hoje   = DateUtils.hoje();
            String limite = DateUtils.hojeAdicionarDias(dias);

            cursor = db.query(
                    DespensaEntry.TABLE_NAME, null,
                    DespensaEntry.COLUMN_STATUS        + " = ? AND "
                            + DespensaEntry.COLUMN_DATA_VALIDADE + " BETWEEN ? AND ? AND "
                            + DespensaEntry.COLUMN_USER_ID       + " = ?",
                    new String[]{ Constants.STATUS_ATIVO, hoje, limite, userId },
                    null, null,
                    DespensaEntry.COLUMN_DATA_VALIDADE + " ASC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
            Log.d(TAG, "listarProximosVencimento(" + dias + "d): "
                    + lista.size() + " item(ns) para userId=" + userId);
        } catch (Exception e) {
            Log.e(TAG, "listarProximosVencimento: erro", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // ATUALIZAR
    // =========================================================================

    public int atualizar(DespensaItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int linhasAfetadas = 0;
        try {
            // Mantém o user_id existente — busca antes para preservar
            DespensaItem existente = buscarPorId(item.getId());
            String userId = existente != null ? existente.getUserId() : null;

            ContentValues values = toContentValues(item, userId);
            linhasAfetadas = db.update(DespensaEntry.TABLE_NAME, values,
                    DespensaEntry._ID + " = ?",
                    new String[]{ String.valueOf(item.getId()) });
        } catch (Exception e) {
            Log.e(TAG, "atualizar: erro id=" + item.getId(), e);
        } finally {
            db.close();
        }
        return linhasAfetadas;
    }

    // =========================================================================
    // MOVER PARA HISTÓRICO
    // =========================================================================

    public boolean moverParaHistorico(long idItem, String nomeItem, String motivo, String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues histValues = new ContentValues();
            histValues.put(HistoricoEntry.COLUMN_ID_ITEM,     idItem);
            histValues.put(HistoricoEntry.COLUMN_DATA_ACAO,   DateUtils.hoje());
            histValues.put(HistoricoEntry.COLUMN_MOTIVO,      motivo);
            histValues.put(HistoricoEntry.COLUMN_NOME_CACHED, nomeItem);
            histValues.put(HistoricoEntry.COLUMN_USER_ID,     userId);
            long idHist = db.insert(HistoricoEntry.TABLE_NAME, null, histValues);

            if (idHist == -1) return false;

            int deletados = db.delete(DespensaEntry.TABLE_NAME,
                    DespensaEntry._ID + " = ?",
                    new String[]{ String.valueOf(idItem) });
            if (deletados == 0) return false;

            db.setTransactionSuccessful();
            Log.d(TAG, "moverParaHistorico: item '" + nomeItem + "' → " + motivo);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "moverParaHistorico: erro", e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    public int deletar(long idItem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletados = 0;
        try {
            deletados = db.delete(DespensaEntry.TABLE_NAME,
                    DespensaEntry._ID + " = ?",
                    new String[]{ String.valueOf(idItem) });
        } catch (Exception e) {
            Log.e(TAG, "deletar: erro id=" + idItem, e);
        } finally {
            db.close();
        }
        return deletados;
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private ContentValues toContentValues(DespensaItem item, String userId) {
        ContentValues cv = new ContentValues();
        cv.put(DespensaEntry.COLUMN_NOME,          item.getNome());
        cv.put(DespensaEntry.COLUMN_QUANTIDADE,    item.getQuantidade());
        cv.put(DespensaEntry.COLUMN_UNIDADE,       item.getUnidadeMedida());
        cv.put(DespensaEntry.COLUMN_DATA_VALIDADE, item.getDataValidade());
        cv.put(DespensaEntry.COLUMN_STATUS,        item.getStatus());
        cv.put(DespensaEntry.COLUMN_USER_ID,       userId);
        return cv;
    }

    private DespensaItem fromCursor(Cursor cursor) {
        DespensaItem item = new DespensaItem();
        item.setId(            cursor.getLong(  cursor.getColumnIndexOrThrow(DespensaEntry._ID)));
        item.setNome(          cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_NOME)));
        item.setQuantidade(    cursor.getDouble(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_QUANTIDADE)));
        item.setUnidadeMedida( cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_UNIDADE)));
        item.setDataValidade(  cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_DATA_VALIDADE)));
        item.setStatus(        cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_STATUS)));

        int userIdCol = cursor.getColumnIndex(DespensaEntry.COLUMN_USER_ID);
        if (userIdCol >= 0 && !cursor.isNull(userIdCol)) {
            item.setUserId(cursor.getString(userIdCol));
        }
        return item;
    }
}