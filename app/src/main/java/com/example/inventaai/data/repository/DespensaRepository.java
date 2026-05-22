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
import com.example.inventaai.data.model.HistoricoItem;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula todas as operações CRUD para a tabela despensa_itens.
 * A camada de UI nunca acessa o SQLiteDatabase diretamente — passa sempre por aqui.
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
     * Insere um novo item na despensa.
     *
     * @param item DespensaItem a ser inserido (id será ignorado).
     * @return ID gerado pelo banco, ou -1 em caso de erro.
     */
    public long inserir(DespensaItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long novoId = -1;
        try {
            ContentValues values = toContentValues(item);
            novoId = db.insert(DespensaEntry.TABLE_NAME, null, values);
            Log.d(TAG, "inserir: item '" + item.getNome() + "' inserido com id=" + novoId);
        } catch (Exception e) {
            Log.e(TAG, "inserir: erro ao inserir item", e);
        } finally {
            db.close();
        }
        return novoId;
    }

    // =========================================================================
    // LISTAR TODOS
    // =========================================================================

    /**
     * Retorna todos os itens da despensa ordenados por data de validade (mais próximo primeiro).
     */
    public List<DespensaItem> listarTodos() {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DespensaEntry.TABLE_NAME,
                    null,                          // todas as colunas
                    null,                          // sem WHERE
                    null,
                    null,
                    null,
                    DespensaEntry.COLUMN_DATA_VALIDADE + " ASC"
            );
            while (cursor.moveToNext()) {
                lista.add(fromCursor(cursor));
            }
            Log.d(TAG, "listarTodos: " + lista.size() + " item(ns) encontrado(s).");
        } catch (Exception e) {
            Log.e(TAG, "listarTodos: erro ao listar itens", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR PRÓXIMOS DO VENCIMENTO
    // =========================================================================

    /**
     * Retorna itens com status ATIVO cuja data de validade está entre hoje e hoje+dias.
     *
     * @param dias Janela em dias a partir de hoje.
     */
    public List<DespensaItem> listarProximosVencimento(int dias) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String hoje      = DateUtils.hoje();
            String limite    = DateUtils.hojeAdicionarDias(dias);

            // WHERE status = 'ATIVO' AND data_validade BETWEEN hoje AND hoje+dias
            String selection = DespensaEntry.COLUMN_STATUS        + " = ? AND "
                    + DespensaEntry.COLUMN_DATA_VALIDADE + " BETWEEN ? AND ?";
            String[] selArgs = { Constants.STATUS_ATIVO, hoje, limite };

            cursor = db.query(
                    DespensaEntry.TABLE_NAME,
                    null,
                    selection,
                    selArgs,
                    null,
                    null,
                    DespensaEntry.COLUMN_DATA_VALIDADE + " ASC"
            );
            while (cursor.moveToNext()) {
                lista.add(fromCursor(cursor));
            }
            Log.d(TAG, "listarProximosVencimento(" + dias + "d): " + lista.size() + " item(ns).");
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

    /**
     * Atualiza um item existente identificado pelo seu id.
     *
     * @return Número de linhas afetadas (deve ser 1 em sucesso).
     */
    public int atualizar(DespensaItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int linhasAfetadas = 0;
        try {
            ContentValues values = toContentValues(item);
            String where    = DespensaEntry._ID + " = ?";
            String[] whereArgs = { String.valueOf(item.getId()) };
            linhasAfetadas = db.update(DespensaEntry.TABLE_NAME, values, where, whereArgs);
            Log.d(TAG, "atualizar: " + linhasAfetadas + " linha(s) afetada(s) para id=" + item.getId());
        } catch (Exception e) {
            Log.e(TAG, "atualizar: erro ao atualizar item id=" + item.getId(), e);
        } finally {
            db.close();
        }
        return linhasAfetadas;
    }

    // =========================================================================
    // MOVER PARA HISTÓRICO
    // =========================================================================

    /**
     * Move um item da despensa para o histórico em uma transação atômica:
     * 1. Insere registro em historico_consumo.
     * 2. Deleta o item de despensa_itens.
     *
     * @param idItem ID do item a ser movido.
     * @param motivo "CONSUMIDO" ou "DESCARTADO".
     * @return true se a operação foi bem-sucedida.
     */
    public boolean moverParaHistorico(long idItem, String motivo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Inserir no histórico
            ContentValues histValues = new ContentValues();
            histValues.put(HistoricoEntry.COLUMN_ID_ITEM,   idItem);
            histValues.put(HistoricoEntry.COLUMN_DATA_ACAO, DateUtils.hoje());
            histValues.put(HistoricoEntry.COLUMN_MOTIVO,    motivo);
            long idHist = db.insert(HistoricoEntry.TABLE_NAME, null, histValues);

            if (idHist == -1) {
                Log.e(TAG, "moverParaHistorico: falha ao inserir no histórico para idItem=" + idItem);
                return false;
            }

            // 2. Deletar da despensa
            String where    = DespensaEntry._ID + " = ?";
            String[] args   = { String.valueOf(idItem) };
            int deletados   = db.delete(DespensaEntry.TABLE_NAME, where, args);

            if (deletados == 0) {
                Log.e(TAG, "moverParaHistorico: item id=" + idItem + " não encontrado para deleção.");
                return false;
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "moverParaHistorico: item id=" + idItem
                    + " movido ao histórico com motivo='" + motivo + "'.");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "moverParaHistorico: erro na transação", e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // =========================================================================
    // DELETAR (direto, sem registro de histórico)
    // =========================================================================

    /**
     * Deleta um item diretamente da despensa (sem mover ao histórico).
     * Use {@link #moverParaHistorico} para deleções rastreadas.
     *
     * @return Número de linhas deletadas.
     */
    public int deletar(long idItem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletados = 0;
        try {
            String where  = DespensaEntry._ID + " = ?";
            String[] args = { String.valueOf(idItem) };
            deletados = db.delete(DespensaEntry.TABLE_NAME, where, args);
            Log.d(TAG, "deletar: " + deletados + " linha(s) removida(s) para id=" + idItem);
        } catch (Exception e) {
            Log.e(TAG, "deletar: erro ao deletar id=" + idItem, e);
        } finally {
            db.close();
        }
        return deletados;
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /** Monta ContentValues a partir de um DespensaItem. */
    private ContentValues toContentValues(DespensaItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DespensaEntry.COLUMN_NOME,          item.getNome());
        cv.put(DespensaEntry.COLUMN_QUANTIDADE,    item.getQuantidade());
        cv.put(DespensaEntry.COLUMN_UNIDADE,       item.getUnidadeMedida());
        cv.put(DespensaEntry.COLUMN_DATA_VALIDADE, item.getDataValidade());
        cv.put(DespensaEntry.COLUMN_STATUS,        item.getStatus());
        return cv;
    }

    /** Constrói um DespensaItem a partir da linha atual do Cursor. */
    private DespensaItem fromCursor(Cursor cursor) {
        DespensaItem item = new DespensaItem();
        item.setId(           cursor.getLong(   cursor.getColumnIndexOrThrow(DespensaEntry._ID)));
        item.setNome(         cursor.getString( cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_NOME)));
        item.setQuantidade(   cursor.getDouble( cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_QUANTIDADE)));
        item.setUnidadeMedida(cursor.getString( cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_UNIDADE)));
        item.setDataValidade( cursor.getString( cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_DATA_VALIDADE)));
        item.setStatus(       cursor.getString( cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_STATUS)));
        return item;
    }
}