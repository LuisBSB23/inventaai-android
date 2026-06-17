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

public class DespensaRepository {

    private static final String TAG = "DespensaRepository";

    private final DatabaseHelper dbHelper;

    public DespensaRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    // ── INSERIR ──────────────────────────────────────────────────────────────

    public long inserir(DespensaItem item, String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            return db.insertOrThrow(DespensaEntry.TABLE_NAME, null, toContentValues(item, userId));
        } catch (Exception e) {
            Log.e(TAG, "inserir: erro", e);
            return -1;
        }
    }

    // ── BUSCAR POR ID ─────────────────────────────────────────────────────────

    public DespensaItem buscarPorId(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            return buscarPorIdInterno(db, id);
        } catch (Exception e) {
            Log.e(TAG, "buscarPorId: erro id=" + id, e);
            return null;
        }
    }

    private DespensaItem buscarPorIdInterno(SQLiteDatabase db, long id) {
        Cursor cursor = null;
        try {
            cursor = db.query(DespensaEntry.TABLE_NAME, null,
                    DespensaEntry._ID + " = ?", new String[]{ String.valueOf(id) },
                    null, null, null, "1");
            if (cursor.moveToFirst()) return fromCursor(cursor);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    // ── LISTAR ATIVOS ─────────────────────────────────────────────────────────

    public List<DespensaItem> listarAtivos(String userId) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DespensaEntry.TABLE_NAME, null,
                    DespensaEntry.COLUMN_STATUS + " = ? AND " + DespensaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ Constants.STATUS_ATIVO, userId },
                    null, null, DespensaEntry.COLUMN_DATA_VALIDADE + " ASC");
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarAtivos: erro", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    // ── LISTAR ATIVOS FILTRADO ────────────────────────────────────────────────

    public List<DespensaItem> listarAtivosFiltrado(String query, String userId) {
        if (query == null || query.trim().isEmpty()) {
            return listarAtivos(userId);
        }

        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String filtro = "%" + query.trim() + "%";
            cursor = db.query(
                    DespensaEntry.TABLE_NAME,
                    null,
                    DespensaEntry.COLUMN_STATUS   + " = ? AND "
                            + DespensaEntry.COLUMN_USER_ID + " = ? AND ("
                            + DespensaEntry.COLUMN_NOME    + " LIKE ? OR "
                            + DespensaEntry.COLUMN_CATEGORIA + " LIKE ?)",
                    new String[]{ Constants.STATUS_ATIVO, userId, filtro, filtro },
                    null, null,
                    DespensaEntry.COLUMN_DATA_VALIDADE + " ASC"
            );
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarAtivosFiltrado: erro", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    // ── LISTAR TODOS ──────────────────────────────────────────────────────────

    public List<DespensaItem> listarTodos(String userId) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DespensaEntry.TABLE_NAME, null,
                    DespensaEntry.COLUMN_USER_ID + " = ?", new String[]{ userId },
                    null, null, DespensaEntry.COLUMN_DATA_VALIDADE + " ASC");
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarTodos: erro", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    // ── LISTAR PRÓXIMOS DO VENCIMENTO ─────────────────────────────────────────

    public List<DespensaItem> listarProximosVencimento(int dias, String userId) {
        List<DespensaItem> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String hoje   = DateUtils.hoje();
            String limite = DateUtils.hojeAdicionarDias(dias);
            cursor = db.query(DespensaEntry.TABLE_NAME, null,
                    DespensaEntry.COLUMN_STATUS + " = ? AND "
                            + DespensaEntry.COLUMN_DATA_VALIDADE + " BETWEEN ? AND ? AND "
                            + DespensaEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ Constants.STATUS_ATIVO, hoje, limite, userId },
                    null, null, DespensaEntry.COLUMN_DATA_VALIDADE + " ASC");
            while (cursor.moveToNext()) lista.add(fromCursor(cursor));
        } catch (Exception e) {
            Log.e(TAG, "listarProximosVencimento: erro", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return lista;
    }

    // ── ATUALIZAR ─────────────────────────────────────────────────────────────

    public int atualizar(DespensaItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int linhas = 0;
        try {
            DespensaItem existente = buscarPorIdInterno(db, item.getId());
            String userId = existente != null ? existente.getUserId() : null;

            linhas = db.update(DespensaEntry.TABLE_NAME, toContentValues(item, userId),
                    DespensaEntry._ID + " = ?", new String[]{ String.valueOf(item.getId()) });
            Log.d(TAG, "atualizar: id=" + item.getId() + " → " + linhas + " linha(s)");
        } catch (Exception e) {
            Log.e(TAG, "atualizar: erro id=" + item.getId(), e);
        }
        return linhas;
    }

    // ── MOVER PARA HISTÓRICO ──────────────────────────────────────────────────

    public boolean moverParaHistorico(long idItem, String nomeItem, String motivo, String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            ContentValues status = new ContentValues();
            status.put(DespensaEntry.COLUMN_STATUS, motivo);
            db.update(DespensaEntry.TABLE_NAME, status,
                    DespensaEntry._ID + " = ?", new String[]{ String.valueOf(idItem) });

            ContentValues hist = new ContentValues();
            hist.put(HistoricoEntry.COLUMN_ID_ITEM,     idItem);
            hist.put(HistoricoEntry.COLUMN_NOME_CACHED, nomeItem);
            hist.put(HistoricoEntry.COLUMN_MOTIVO,      motivo);
            hist.put(HistoricoEntry.COLUMN_USER_ID,     userId);
            hist.put(HistoricoEntry.COLUMN_DATA_ACAO,   DateUtils.hoje());
            db.insertOrThrow(HistoricoEntry.TABLE_NAME, null, hist);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "moverParaHistorico: erro", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ── MOVER PARA HISTÓRICO COM ORIGEM (Sprint 15) ───────────────────────────

    public boolean moverParaHistoricoComOrigem(long idItem, String nomeItem,
                                               String motivo, String userId, String origem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            ContentValues statusCV = new ContentValues();
            statusCV.put(DespensaEntry.COLUMN_STATUS, motivo);
            db.update(DespensaEntry.TABLE_NAME, statusCV,
                    DespensaEntry._ID + " = ?", new String[]{ String.valueOf(idItem) });

            ContentValues hist = new ContentValues();
            hist.put(HistoricoEntry.COLUMN_ID_ITEM,     idItem);
            hist.put(HistoricoEntry.COLUMN_NOME_CACHED, nomeItem);
            hist.put(HistoricoEntry.COLUMN_MOTIVO,      motivo);
            hist.put(HistoricoEntry.COLUMN_USER_ID,     userId);
            hist.put(HistoricoEntry.COLUMN_DATA_ACAO,   DateUtils.hoje());
            if (origem != null) hist.put(HistoricoEntry.COLUMN_ORIGEM, origem);
            db.insertOrThrow(HistoricoEntry.TABLE_NAME, null, hist);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "moverParaHistoricoComOrigem: erro", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ── PROCESSAR BAIXAS EM LOTE (Sprint 15) ──────────────────────────────────

    public boolean processarBaixas(List<BaixaItem> baixas, String userId, String origem) {
        if (baixas == null || baixas.isEmpty()) return true;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            for (BaixaItem baixa : baixas) {
                DespensaItem item     = baixa.item;
                double       reduzir  = baixa.qtdReduzir;
                double       novaQtd  = item.getQuantidade() - reduzir;

                if (novaQtd <= 0) {
                    ContentValues statusCV = new ContentValues();
                    statusCV.put(DespensaEntry.COLUMN_STATUS, Constants.STATUS_CONSUMIDO);
                    db.update(DespensaEntry.TABLE_NAME, statusCV,
                            DespensaEntry._ID + " = ?",
                            new String[]{ String.valueOf(item.getId()) });
                } else {
                    ContentValues qtdCV = new ContentValues();
                    qtdCV.put(DespensaEntry.COLUMN_QUANTIDADE, novaQtd);
                    db.update(DespensaEntry.TABLE_NAME, qtdCV,
                            DespensaEntry._ID + " = ?",
                            new String[]{ String.valueOf(item.getId()) });
                }

                ContentValues hist = new ContentValues();
                hist.put(HistoricoEntry.COLUMN_ID_ITEM,     item.getId());
                hist.put(HistoricoEntry.COLUMN_NOME_CACHED, item.getNome());
                hist.put(HistoricoEntry.COLUMN_MOTIVO,      Constants.STATUS_CONSUMIDO);
                hist.put(HistoricoEntry.COLUMN_USER_ID,     userId);
                hist.put(HistoricoEntry.COLUMN_DATA_ACAO,   DateUtils.hoje());
                if (origem != null) hist.put(HistoricoEntry.COLUMN_ORIGEM, origem);
                db.insertOrThrow(HistoricoEntry.TABLE_NAME, null, hist);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "processarBaixas: " + baixas.size() + " item(ns) processado(s).");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "processarBaixas: erro na transação", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ── DELETAR ───────────────────────────────────────────────────────────────

    public boolean deletar(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            return db.delete(DespensaEntry.TABLE_NAME,
                    DespensaEntry._ID + " = ?", new String[]{ String.valueOf(id) }) > 0;
        } catch (Exception e) {
            Log.e(TAG, "deletar: erro id=" + id, e);
            return false;
        }
    }

    // ── DELETAR VÁRIOS (Sprint 18) ────────────────────────────────────────────

    public int deletarVarios(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int totalDeletados = 0;
        try {
            db.beginTransaction();

            for (Long id : ids) {
                int linhas = db.delete(
                        DespensaEntry.TABLE_NAME,
                        DespensaEntry._ID + " = ?",
                        new String[]{ String.valueOf(id) }
                );
                totalDeletados += linhas;
                Log.d(TAG, "deletarVarios: id=" + id + " deletado=" + (linhas > 0));
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "deletarVarios: transação concluída — " + totalDeletados + " item(ns) removido(s).");
            return totalDeletados;

        } catch (Exception e) {
            Log.e(TAG, "deletarVarios: erro na transação", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private DespensaItem fromCursor(Cursor cursor) {
        DespensaItem item = new DespensaItem();
        item.setId(           cursor.getLong(  cursor.getColumnIndexOrThrow(DespensaEntry._ID)));
        item.setNome(         cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_NOME)));
        item.setQuantidade(   cursor.getDouble(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_QUANTIDADE)));
        item.setUnidadeMedida(cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_UNIDADE)));
        item.setDataValidade( cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_DATA_VALIDADE)));
        item.setStatus(       cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_STATUS)));
        item.setUserId(       cursor.getString(cursor.getColumnIndexOrThrow(DespensaEntry.COLUMN_USER_ID)));
        int colCat = cursor.getColumnIndex(DespensaEntry.COLUMN_CATEGORIA);
        if (colCat >= 0) item.setCategoria(cursor.getString(colCat));
        return item;
    }

    private ContentValues toContentValues(DespensaItem item, String userId) {
        ContentValues v = new ContentValues();
        v.put(DespensaEntry.COLUMN_NOME,          item.getNome());
        v.put(DespensaEntry.COLUMN_QUANTIDADE,    item.getQuantidade());
        v.put(DespensaEntry.COLUMN_UNIDADE,       item.getUnidadeMedida());
        v.put(DespensaEntry.COLUMN_DATA_VALIDADE, item.getDataValidade());
        v.put(DespensaEntry.COLUMN_STATUS,        item.getStatus() != null ? item.getStatus() : Constants.STATUS_ATIVO);
        v.put(DespensaEntry.COLUMN_CATEGORIA,     item.getCategoria());
        if (userId != null) v.put(DespensaEntry.COLUMN_USER_ID, userId);
        return v;
    }

    // ── Classe auxiliar para processarBaixas ─────────────────────────────────

    public static class BaixaItem {
        public final DespensaItem item;
        public final double       qtdReduzir;

        public BaixaItem(DespensaItem item, double qtdReduzir) {
            this.item       = item;
            this.qtdReduzir = qtdReduzir;
        }
    }
}