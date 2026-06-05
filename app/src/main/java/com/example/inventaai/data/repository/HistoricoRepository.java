package com.example.inventaai.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.data.model.HistoricoItem;
import com.example.inventaai.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class HistoricoRepository {

    private static final String TAG = Constants.LOG_TAG;

    private final DatabaseHelper dbHelper;

    public HistoricoRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    // =========================================================================
    // LISTAR TODOS (filtrado por usuário)
    // =========================================================================

    public List<HistoricoItem> listarTodos(String userId) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // (Tarefa 3): JOIN traz a coluna 'categoria' da despensa
            // para exibir o ícone correto em cada entrada do histórico.
            String sql =
                    "SELECT h.*, d." + DespensaEntry.COLUMN_CATEGORIA + " AS item_categoria "
                            + "FROM " + HistoricoEntry.TABLE_NAME + " h "
                            + "LEFT JOIN " + DespensaEntry.TABLE_NAME + " d "
                            + "  ON h." + HistoricoEntry.COLUMN_ID_ITEM + " = d." + DespensaEntry._ID
                            + " WHERE h." + HistoricoEntry.COLUMN_USER_ID + " = ?"
                            + " ORDER BY h." + HistoricoEntry.COLUMN_DATA_ACAO + " DESC";

            cursor = db.rawQuery(sql, new String[]{ userId });
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
            Log.d(TAG, "HistoricoRepository.listarTodos: " + lista.size()
                    + " registro(s) para userId=" + userId);
        } catch (Exception e) {
            Log.e(TAG, "HistoricoRepository.listarTodos: erro", e);
            lista = listarTodosSemJoin(userId);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    /** Fallback sem JOIN — usado se a query principal falhar. */
    private List<HistoricoItem> listarTodosSemJoin(String userId) {
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
        } catch (Exception e) {
            Log.e(TAG, "HistoricoRepository.listarTodosSemJoin: erro", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR POR PERÍODO
    // =========================================================================

    public List<HistoricoItem> listarPorPeriodo(String dataInicio, String dataFim, String userId) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql =
                    "SELECT h.*, d." + DespensaEntry.COLUMN_CATEGORIA + " AS item_categoria "
                            + "FROM " + HistoricoEntry.TABLE_NAME + " h "
                            + "LEFT JOIN " + DespensaEntry.TABLE_NAME + " d "
                            + "  ON h." + HistoricoEntry.COLUMN_ID_ITEM + " = d." + DespensaEntry._ID
                            + " WHERE h." + HistoricoEntry.COLUMN_DATA_ACAO + " BETWEEN ? AND ?"
                            + "   AND h." + HistoricoEntry.COLUMN_USER_ID + " = ?"
                            + " ORDER BY h." + HistoricoEntry.COLUMN_DATA_ACAO + " DESC";

            cursor = db.rawQuery(sql, new String[]{ dataInicio, dataFim, userId });
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarPorPeriodo: erro", e);
        } finally {
            if (cursor != null) cursor.close();
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

        // Nome denormalizado
        int nomeCol = cursor.getColumnIndex(HistoricoEntry.COLUMN_NOME_CACHED);
        if (nomeCol >= 0 && !cursor.isNull(nomeCol)) {
            item.setNomeCached(cursor.getString(nomeCol));
        } else {
            item.setNomeCached("Item #" + item.getIdItem());
        }

        // (Tarefa 3): lê a categoria do JOIN (alias 'item_categoria')
        int catCol = cursor.getColumnIndex("item_categoria");
        if (catCol >= 0 && !cursor.isNull(catCol)) {
            item.setCategoria(cursor.getString(catCol));
        }

        return item;
    }
}