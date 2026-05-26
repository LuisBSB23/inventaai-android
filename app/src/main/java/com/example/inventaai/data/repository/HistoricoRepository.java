package com.example.inventaai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.data.model.HistoricoItem;
import com.example.inventaai.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula as operações de leitura e escrita para a tabela historico_consumo.
 *
 * Sprint 1: todos os métodos de leitura filtram por user_id.
 * Sprint 3: fromCursor() lê o campo nome_cached (denormalizado).
 */
public class HistoricoRepository {

    private static final String TAG = Constants.LOG_TAG;
    private final DatabaseHelper dbHelper;

    public HistoricoRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // =========================================================================
    // LISTAR TODOS (filtrado por usuário)
    // =========================================================================

    /**
     * Retorna todos os registros do histórico do usuário informado,
     * do mais recente para o mais antigo.
     */
    public List<HistoricoItem> listarTodos(String userId) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    HistoricoEntry.TABLE_NAME, null,
                    HistoricoEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ userId },
                    null, null,
                    HistoricoEntry.COLUMN_DATA_ACAO + " DESC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
            Log.d(TAG, "HistoricoRepository.listarTodos: " + lista.size()
                    + " registro(s) para userId=" + userId);
        } catch (Exception e) {
            Log.e(TAG, "HistoricoRepository.listarTodos: erro", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR POR PERÍODO (filtrado por usuário)
    // =========================================================================

    public List<HistoricoItem> listarPorPeriodo(String dataInicio, String dataFim, String userId) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    HistoricoEntry.TABLE_NAME, null,
                    HistoricoEntry.COLUMN_DATA_ACAO + " BETWEEN ? AND ? AND "
                            + HistoricoEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ dataInicio, dataFim, userId },
                    null, null,
                    HistoricoEntry.COLUMN_DATA_ACAO + " DESC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarPorPeriodo: erro", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private HistoricoItem fromCursor(Cursor cursor) {
        HistoricoItem item = new HistoricoItem();
        item.setIdHistorico(cursor.getLong(  cursor.getColumnIndexOrThrow(HistoricoEntry._ID)));
        item.setIdItem(     cursor.getLong(  cursor.getColumnIndexOrThrow(HistoricoEntry.COLUMN_ID_ITEM)));
        item.setDataAcao(   cursor.getString(cursor.getColumnIndexOrThrow(HistoricoEntry.COLUMN_DATA_ACAO)));
        item.setMotivo(     cursor.getString(cursor.getColumnIndexOrThrow(HistoricoEntry.COLUMN_MOTIVO)));

        int nomeCol = cursor.getColumnIndex(HistoricoEntry.COLUMN_NOME_CACHED);
        if (nomeCol >= 0 && !cursor.isNull(nomeCol)) {
            item.setNomeCached(cursor.getString(nomeCol));
        } else {
            item.setNomeCached("Item #" + item.getIdItem());
        }
        return item;
    }
}