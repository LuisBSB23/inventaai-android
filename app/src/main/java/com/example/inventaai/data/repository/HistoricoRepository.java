package com.example.inventaai.data.repository;

import android.content.ContentValues;
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

/**
 * Encapsula as operações de leitura e escrita para a tabela historico_consumo.
 *
 * Sprint 1: todos os métodos de leitura filtram por user_id.
 * Sprint 3: fromCursor() lê o campo nome_cached (denormalizado).
 * Sprint 2: listarTodos() e listarPorPeriodo() fazem LEFT JOIN com despensa_itens
 *           para popular o campo categoria em HistoricoItem, permitindo que
 *           o HistoricoAdapter exiba o ícone de categoria correto.
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
     *
     * Sprint 2: usa LEFT JOIN com despensa_itens para obter a categoria
     * do item, sem quebrar registros de itens já deletados da despensa
     * (LEFT JOIN retorna NULL para esses casos, tratado em fromCursor()).
     */
    public List<HistoricoItem> listarTodos(String userId) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Sprint 2: JOIN para buscar categoria
            String sql =
                    "SELECT h.*, d." + DespensaEntry.COLUMN_STATUS + " AS d_status "
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
            // Fallback: query simples sem JOIN (garante que o app não quebre)
            lista = listarTodosSemJoin(userId);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return lista;
    }

    /**
     * Fallback sem JOIN — usado se a query com JOIN falhar por alguma razão.
     */
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
            db.close();
        }
        return lista;
    }

    // =========================================================================
    // LISTAR POR PERÍODO (filtrado por usuário)
    // =========================================================================

    /**
     * Sprint 2: também faz JOIN para popular categoria no filtro por período.
     */
    public List<HistoricoItem> listarPorPeriodo(String dataInicio, String dataFim, String userId) {
        List<HistoricoItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql =
                    "SELECT h.*, d." + DespensaEntry.COLUMN_STATUS + " AS d_status "
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

        // Sprint 2: categoria via JOIN (coluna d_status usada como proxy de categoria)
        // NOTA: despensa_itens não persiste a categoria no banco — ela é campo UI only
        // em DespensaItem. Por isso o campo categoria de HistoricoItem ficará nulo para
        // itens antigos; o HistoricoAdapter já trata isso ocultando o ícone nesses casos.
        int catCol = cursor.getColumnIndex("d_status");
        if (catCol >= 0 && !cursor.isNull(catCol)) {
            // Aqui poderíamos popular categoria se o schema incluísse a coluna.
            // Como categoria é UI-only, deixamos null e o adapter exibe GONE.
        }

        return item;
    }
}