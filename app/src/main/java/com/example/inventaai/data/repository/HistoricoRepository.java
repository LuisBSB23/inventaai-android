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
 * Sprint 3: fromCursor() agora lê o campo nome_item (denormalizado).
 */
public class HistoricoRepository {

    private static final String TAG = Constants.LOG_TAG;
    private final DatabaseHelper dbHelper;

    public HistoricoRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // =========================================================================
    // INSERIR
    // =========================================================================

    public long inserir(HistoricoItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long novoId = -1;
        try {
            ContentValues values = toContentValues(item);
            novoId = db.insert(HistoricoEntry.TABLE_NAME, null, values);
            Log.d(TAG, "HistoricoRepository.inserir: id=" + novoId);
        } catch (Exception e) {
            Log.e(TAG, "HistoricoRepository.inserir: erro", e);
        } finally {
            db.close();
        }
        return novoId;
    }

    // =========================================================================
    // LISTAR TODOS
    // =========================================================================

    /**
     * Retorna todos os registros do histórico, do mais recente para o mais antigo.
     */
    public List<HistoricoItem> listarTodos() {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    HistoricoEntry.TABLE_NAME,
                    null, null, null, null, null,
                    HistoricoEntry.COLUMN_DATA_ACAO + " DESC"
            );
            while (cursor.moveToNext()) {
                lista.add(fromCursor(cursor));
            }
            Log.d(TAG, "HistoricoRepository.listarTodos: " + lista.size() + " registro(s).");
        } catch (Exception e) {
            Log.e(TAG, "HistoricoRepository.listarTodos: erro", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR POR PERÍODO
    // =========================================================================

    public List<HistoricoItem> listarPorPeriodo(String dataInicio, String dataFim) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    HistoricoEntry.TABLE_NAME,
                    null,
                    HistoricoEntry.COLUMN_DATA_ACAO + " BETWEEN ? AND ?",
                    new String[]{ dataInicio, dataFim },
                    null, null,
                    HistoricoEntry.COLUMN_DATA_ACAO + " DESC"
            );
            while (cursor.moveToNext()) {
                lista.add(fromCursor(cursor));
            }
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

    private ContentValues toContentValues(HistoricoItem item) {
        ContentValues cv = new ContentValues();
        cv.put(HistoricoEntry.COLUMN_ID_ITEM,     item.getIdItem());
        cv.put(HistoricoEntry.COLUMN_DATA_ACAO,   item.getDataAcao());
        cv.put(HistoricoEntry.COLUMN_MOTIVO,      item.getMotivo());
        cv.put(HistoricoEntry.COLUMN_NOME_CACHED, item.getNomeCached());
        return cv;
    }

    private HistoricoItem fromCursor(Cursor cursor) {
        HistoricoItem item = new HistoricoItem();
        item.setIdHistorico(cursor.getLong(  cursor.getColumnIndexOrThrow(HistoricoEntry._ID)));
        item.setIdItem(     cursor.getLong(  cursor.getColumnIndexOrThrow(HistoricoEntry.COLUMN_ID_ITEM)));
        item.setDataAcao(   cursor.getString(cursor.getColumnIndexOrThrow(HistoricoEntry.COLUMN_DATA_ACAO)));
        item.setMotivo(     cursor.getString(cursor.getColumnIndexOrThrow(HistoricoEntry.COLUMN_MOTIVO)));

        // nome_item pode ser null em registros antigos (versão 2 do banco)
        int nomeCol = cursor.getColumnIndex(HistoricoEntry.COLUMN_NOME_CACHED);
        if (nomeCol >= 0 && !cursor.isNull(nomeCol)) {
            item.setNomeCached(cursor.getString(nomeCol));
        } else {
            item.setNomeCached("Item #" + item.getIdItem());
        }
        return item;
    }
}