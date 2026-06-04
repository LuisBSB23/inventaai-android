package com.example.inventaai.ui.configuracoes;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.AppPrefs;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.example.inventaai.data.db.DatabaseContract.HistoricoEntry;
import com.google.android.material.snackbar.Snackbar;

public class ConfiguracoesActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private SeekBar      seekBarDiasAlerta;
    private TextView     tvDiasAlertaValor;
    private SeekBar      seekBarDiasNotif;
    private TextView     tvDiasNotifValor;
    private SwitchMaterial switchNotificacoes;
    private MaterialButton btnLimparHistorico;

    // ── Dependências ──────────────────────────────────────────────────────────
    private AppPrefs       appPrefs;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    // Opções disponíveis para o SeekBar de antecedência da notificação
    private static final int[] OPCOES_DIAS_NOTIF = {1, 2, 3, 5, 7};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        appPrefs       = AppPrefs.getInstance(this);
        sessionManager = new SessionManager(this);
        dbHelper       = DatabaseHelper.getInstance(this);

        vincularViews();
        configurarToolbar();
        carregarPreferencias();
        configurarListeners();
    }

    // ── Inicialização ─────────────────────────────────────────────────────────

    private void vincularViews() {
        seekBarDiasAlerta    = findViewById(R.id.seekBarDiasAlerta);
        tvDiasAlertaValor    = findViewById(R.id.tvDiasAlertaValor);
        seekBarDiasNotif     = findViewById(R.id.seekBarDiasNotif);
        tvDiasNotifValor     = findViewById(R.id.tvDiasNotifValor);
        switchNotificacoes   = findViewById(R.id.switchNotificacoes);
        btnLimparHistorico   = findViewById(R.id.btnLimparHistorico);
    }

    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarConfiguracoes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Configurações");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /** Carrega os valores salvos e aplica nas views. */
    private void carregarPreferencias() {
        // SeekBar de dias de alerta (1–30, valor = dias - 1)
        int diasAlerta = appPrefs.getDiasAlerta();
        seekBarDiasAlerta.setMax(29); // 0..29 representa 1..30
        seekBarDiasAlerta.setProgress(diasAlerta - 1);
        tvDiasAlertaValor.setText(diasAlerta + " dias");

        // SeekBar de antecedência (índices de OPCOES_DIAS_NOTIF)
        int diasNotif      = appPrefs.getDiasNotificacao();
        int indexNotif     = indiceDias(diasNotif);
        seekBarDiasNotif.setMax(OPCOES_DIAS_NOTIF.length - 1);
        seekBarDiasNotif.setProgress(indexNotif);
        tvDiasNotifValor.setText(OPCOES_DIAS_NOTIF[indexNotif] + " dias antes");

        // Switch de notificações
        switchNotificacoes.setChecked(appPrefs.isNotificacoesAtivas());
        atualizarEstadoSeekBarNotif(appPrefs.isNotificacoesAtivas());
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void configurarListeners() {

        // SeekBar de dias de alerta amarelo
        seekBarDiasAlerta.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int dias = progress + 1; // mínimo 1 dia
                tvDiasAlertaValor.setText(dias + " dias");
                if (fromUser) {
                    appPrefs.setDiasAlerta(dias);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar)  {}
        });

        // SeekBar de antecedência da notificação
        seekBarDiasNotif.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int dias = OPCOES_DIAS_NOTIF[progress];
                tvDiasNotifValor.setText(dias + " dias antes");
                if (fromUser) {
                    appPrefs.setDiasNotificacao(dias);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar)  {}
        });

        // Switch de notificações
        switchNotificacoes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPrefs.setNotificacoesAtivas(isChecked);
            atualizarEstadoSeekBarNotif(isChecked);
        });

        // Botão Limpar Histórico
        btnLimparHistorico.setOnClickListener(v -> confirmarLimparHistorico());
    }

    /** Habilita/desabilita o SeekBar de antecedência conforme o switch. */
    private void atualizarEstadoSeekBarNotif(boolean ativo) {
        seekBarDiasNotif.setEnabled(ativo);
        tvDiasNotifValor.setAlpha(ativo ? 1f : 0.4f);
    }

    // ── Limpar Histórico ──────────────────────────────────────────────────────

    private void confirmarLimparHistorico() {
        new AlertDialog.Builder(this)
                .setTitle("Limpar Histórico")
                .setMessage("Tem certeza? Todo o histórico de consumo e descarte será removido permanentemente.")
                .setPositiveButton("Limpar", (dialog, which) -> limparHistorico())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void limparHistorico() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        AppExecutors.diskIO().execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deletados = db.delete(
                    HistoricoEntry.TABLE_NAME,
                    HistoricoEntry.COLUMN_USER_ID + " = ?",
                    new String[]{ userId }
            );

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                String msg = deletados > 0
                        ? "Histórico limpo (" + deletados + " registro(s) removido(s))."
                        : "Histórico já estava vazio.";
                Snackbar.make(btnLimparHistorico, msg, Snackbar.LENGTH_LONG).show();
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Encontra o índice mais próximo de {@code dias} no array OPCOES_DIAS_NOTIF. */
    private int indiceDias(int dias) {
        int melhorIndice = 0;
        int menorDiff    = Math.abs(OPCOES_DIAS_NOTIF[0] - dias);
        for (int i = 1; i < OPCOES_DIAS_NOTIF.length; i++) {
            int diff = Math.abs(OPCOES_DIAS_NOTIF[i] - dias);
            if (diff < menorDiff) {
                menorDiff    = diff;
                melhorIndice = i;
            }
        }
        return melhorIndice;
    }
}