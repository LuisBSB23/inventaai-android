package com.example.inventaai.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {

    // ── Nomes do arquivo de preferências ──────────────────────────────────────
    private static final String PREF_NAME = "InventaAiAppPrefs";

    // ── Chaves ────────────────────────────────────────────────────────────────
    public static final String KEY_DIAS_ALERTA          = "dias_alerta_amarelo";

    public static final String KEY_DIAS_NOTIFICACAO     = "dias_antecedencia_notificacao";

    public static final String KEY_NOTIFICACOES_ATIVAS  = "notificacoes_ativas";

    // ── Valores padrão ────────────────────────────────────────────────────────
    public static final int     DEFAULT_DIAS_ALERTA         = 7;
    public static final int     DEFAULT_DIAS_NOTIFICACAO    = 3;
    public static final boolean DEFAULT_NOTIFICACOES_ATIVAS = true;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile AppPrefs instance;

    private final SharedPreferences prefs;

    private AppPrefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppPrefs getInstance(Context context) {
        if (instance == null) {
            synchronized (AppPrefs.class) {
                if (instance == null) {
                    instance = new AppPrefs(context);
                }
            }
        }
        return instance;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getDiasAlerta() {
        return prefs.getInt(KEY_DIAS_ALERTA, DEFAULT_DIAS_ALERTA);
    }

    public int getDiasNotificacao() {
        return prefs.getInt(KEY_DIAS_NOTIFICACAO, DEFAULT_DIAS_NOTIFICACAO);
    }

    public boolean isNotificacoesAtivas() {
        return prefs.getBoolean(KEY_NOTIFICACOES_ATIVAS, DEFAULT_NOTIFICACOES_ATIVAS);
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setDiasAlerta(int dias) {
        prefs.edit().putInt(KEY_DIAS_ALERTA, dias).apply();
    }

    public void setDiasNotificacao(int dias) {
        prefs.edit().putInt(KEY_DIAS_NOTIFICACAO, dias).apply();
    }

    public void setNotificacoesAtivas(boolean ativo) {
        prefs.edit().putBoolean(KEY_NOTIFICACOES_ATIVAS, ativo).apply();
    }
}