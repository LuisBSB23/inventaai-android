package com.example.inventaai.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME       = "InventaAiPrefs";
    private static final String KEY_USER_ID     = "current_user_id";
    private static final String KEY_USER_NAME   = "current_user_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Salva a sessão após login bem-sucedido. */
    public void salvarSessao(String userId, String userName) {
        prefs.edit()
                .putString(KEY_USER_ID,   userId)
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    /** Retorna o UUID do usuário logado, ou null se nenhum estiver logado. */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /** Retorna o nome do usuário logado, ou null. */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /** Verifica se há sessão ativa. */
    public boolean isLoggedIn() {
        return getUserId() != null;
    }

    /** Encerra a sessão (logout). */
    public void encerrarSessao() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .apply();
    }
}