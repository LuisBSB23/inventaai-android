package com.example.inventaai.ui.dashboard;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseContract.UserEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SeedTestActivity extends AppCompatActivity {

    private static final String TAG = "SeedTest";

    private TextView tvLog;
    private Button   btnSeed;
    private Button   btnLimpar;
    private Button   btnIrDashboard;
    private ScrollView scrollLog;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private StringBuilder  logBuffer;

    // ── formato de data que o app usa internamente ────────────────────────────
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper       = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);
        logBuffer      = new StringBuilder();

        // ── Layout programático simples (sem XML extra) ───────────────────────
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(32, 64, 32, 32);
        root.setBackgroundColor(0xFF121212);

        TextView titulo = new TextView(this);
        titulo.setText("🌱  Seed de Teste — Sprint 7");
        titulo.setTextSize(20f);
        titulo.setTextColor(0xFFFFFFFF);
        titulo.setPadding(0, 0, 0, 24);
        root.addView(titulo);

        btnSeed = new Button(this);
        btnSeed.setText("POPULAR BANCO DE DADOS (30 itens)");
        btnSeed.setBackgroundColor(0xFF4CAF50);
        btnSeed.setTextColor(0xFFFFFFFF);
        btnSeed.setPadding(16, 16, 16, 16);
        root.addView(btnSeed);

        btnLimpar = new Button(this);
        btnLimpar.setText("LIMPAR TODOS OS ITENS DA DESPENSA");
        btnLimpar.setBackgroundColor(0xFFf44336);
        btnLimpar.setTextColor(0xFFFFFFFF);
        btnLimpar.setPadding(16, 16, 16, 16);
        android.widget.LinearLayout.LayoutParams btnLimparParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLimparParams.setMargins(0, 16, 0, 0);
        btnLimpar.setLayoutParams(btnLimparParams);
        root.addView(btnLimpar);

        btnIrDashboard = new Button(this);
        btnIrDashboard.setText("IR PARA O DASHBOARD →");
        btnIrDashboard.setBackgroundColor(0xFF2196F3);
        btnIrDashboard.setTextColor(0xFFFFFFFF);
        android.widget.LinearLayout.LayoutParams btnDashParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnDashParams.setMargins(0, 16, 0, 0);
        btnIrDashboard.setLayoutParams(btnDashParams);
        root.addView(btnIrDashboard);

        TextView tvLogTitulo = new TextView(this);
        tvLogTitulo.setText("\n📋  Log de execução:");
        tvLogTitulo.setTextColor(0xFFAAAAAA);
        tvLogTitulo.setTextSize(13f);
        root.addView(tvLogTitulo);

        scrollLog = new ScrollView(this);
        android.widget.LinearLayout.LayoutParams scrollParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLog.setLayoutParams(scrollParams);

        tvLog = new TextView(this);
        tvLog.setTextColor(0xFF00E676);
        tvLog.setTextSize(12f);
        tvLog.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvLog.setPadding(8, 8, 8, 8);
        scrollLog.addView(tvLog);
        root.addView(scrollLog);

        setContentView(root);

        // ── Listeners ─────────────────────────────────────────────────────────
        btnSeed.setOnClickListener(v -> executarSeed());
        btnLimpar.setOnClickListener(v -> limparDespensa());
        btnIrDashboard.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });

        log("Pronto. Pressione o botão verde para popular o banco.");
    }

    // =========================================================================
    //  SEED PRINCIPAL
    // =========================================================================

    private void executarSeed() {
        btnSeed.setEnabled(false);
        logBuffer.setLength(0);
        log("════════════════════════════════");
        log("Iniciando seed de 30 itens...");
        log("════════════════════════════════\n");

        String userId = garantirUsuarioLogado();
        if (userId == null) {
            log("❌  ERRO: Nenhum usuário logado.");
            log("    Faça login no app antes de rodar o seed.");
            btnSeed.setEnabled(true);
            return;
        }
        log("✅  Usuário logado: " + userId.substring(0, 8) + "...\n");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int inseridos = 0;

        // ── GRUPO 1: 4 itens VENCIDOS ─────────────────────────────────────────
        log("── GRUPO 1: Itens VENCIDOS (4) ──");
        Object[][] vencidos = {
                // { nome, qtd, unidade, diasNoPassado, categoria }
                { "Leite integral",       1.0,  "L",    -3,  "Laticínios"  },
                { "Iogurte natural",      2.0,  "unid", -7,  "Laticínios"  },
                { "Pão de forma",         1.0,  "unid", -1,  "Padaria"     },
                { "Presunto fatiado",     150.0,"g",    -10, "Frios"       },
        };
        for (Object[] row : vencidos) {
            long id = inserirItem(db,
                    (String) row[0], (Double) row[1], (String) row[2],
                    dataRelativa((int) row[3]), "ATIVO", (String) row[4], userId);
            log(id > 0
                    ? "  ✅  [VENCIDO  ] " + row[0] + "  (" + row[3] + " dias)"
                    : "  ❌  Falha ao inserir: " + row[0]);
            if (id > 0) inseridos++;
        }

        // ── GRUPO 2: 6 itens PRÓXIMOS DO VENCIMENTO (1–7 dias) ───────────────
        log("\n── GRUPO 2: Próximos do VENCIMENTO (6) ──");
        Object[][] proximos = {
                { "Queijo mussarela",     200.0,"g",   1,  "Laticínios" },
                { "Ovos caipira",         6.0,  "unid",2,  "Ovos"       },
                { "Banana nanica",        5.0,  "unid",3,  "Frutas"     },
                { "Tomate cereja",        300.0,"g",   4,  "Verduras"   },
                { "Manteiga sem sal",     1.0,  "unid",5,  "Laticínios" },
                { "Alface americana",     1.0,  "unid",7,  "Verduras"   },
        };
        for (Object[] row : proximos) {
            long id = inserirItem(db,
                    (String) row[0], (Double) row[1], (String) row[2],
                    dataRelativa((int) row[3]), "ATIVO", (String) row[4], userId);
            log(id > 0
                    ? "  ✅  [PRÓXIMO  ] " + row[0] + "  (+" + row[3] + " dias)"
                    : "  ❌  Falha ao inserir: " + row[0]);
            if (id > 0) inseridos++;
        }

        // ── GRUPO 3: 20 itens FRESCOS (8–180 dias) ───────────────────────────
        log("\n── GRUPO 3: Itens FRESCOS (20) ──");
        Object[][] frescos = {
                { "Arroz branco 5kg",     5.0,  "kg",   30,  "Grãos"       },
                { "Feijão carioca",       1.0,  "kg",   45,  "Grãos"       },
                { "Macarrão espaguete",   500.0,"g",    60,  "Massas"      },
                { "Molho de tomate",      340.0,"g",    90,  "Conservas"   },
                { "Azeite extra virgem",  500.0,"ml",   120, "Óleos"       },
                { "Sal refinado",         1.0,  "kg",   180, "Temperos"    },
                { "Açúcar cristal",       2.0,  "kg",   150, "Grãos"       },
                { "Farinha de trigo",     1.0,  "kg",   90,  "Farinhas"    },
                { "Café em pó",           250.0,"g",    60,  "Bebidas"     },
                { "Leite condensado",     395.0,"g",    120, "Laticínios"  },
                { "Atum em lata",         170.0,"g",    180, "Proteínas"   },
                { "Sardinha em lata",     125.0,"g",    150, "Proteínas"   },
                { "Milho em conserva",    200.0,"g",    100, "Conservas"   },
                { "Ervilha em conserva",  200.0,"g",    100, "Conservas"   },
                { "Extrato de tomate",    140.0,"g",    90,  "Conservas"   },
                { "Biscoito cream cracker",200.0,"g",   45,  "Lanches"     },
                { "Achocolatado pó",      400.0,"g",    80,  "Bebidas"     },
                { "Óleo de soja",         900.0,"ml",   120, "Óleos"       },
                { "Vinagre de álcool",    750.0,"ml",   180, "Temperos"    },
                { "Maionese",             500.0,"g",    30,  "Condimentos" },
        };
        for (Object[] row : frescos) {
            long id = inserirItem(db,
                    (String) row[0], (Double) row[1], (String) row[2],
                    dataRelativa((int) row[3]), "ATIVO", (String) row[4], userId);
            log(id > 0
                    ? "  ✅  [FRESCO   ] " + row[0] + "  (+" + row[3] + " dias)"
                    : "  ❌  Falha ao inserir: " + row[0]);
            if (id > 0) inseridos++;
        }

        // ── Resumo ────────────────────────────────────────────────────────────
        log("\n════════════════════════════════");
        log("SEED CONCLUÍDO: " + inseridos + "/30 itens inseridos.");
        log("  Vencidos       : 4");
        log("  Próx. vencimento: 6");
        log("  Frescos        : 20");
        log("════════════════════════════════");
        log("\nPressione 'IR PARA O DASHBOARD' para verificar.");

        Log.i(TAG, "Seed concluído: " + inseridos + " itens inseridos para userId=" + userId);
        btnSeed.setEnabled(true);
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    // =========================================================================
    //  LIMPAR
    // =========================================================================

    private void limparDespensa() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            log("❌  Nenhum usuário logado para limpar.");
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletados = db.delete(
                DespensaEntry.TABLE_NAME,
                DespensaEntry.COLUMN_USER_ID + " = ?",
                new String[]{ userId });
        log("🗑️  " + deletados + " itens removidos da despensa.");
        Log.w(TAG, "Limpeza: " + deletados + " itens deletados para userId=" + userId);
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    /**
     * Insere um único item no banco e retorna o rowId (>0 = sucesso, -1 = falha).
     */
    private long inserirItem(SQLiteDatabase db,
                             String nome, double quantidade, String unidade,
                             String dataValidade, String status,
                             String categoria, String userId) {
        ContentValues cv = new ContentValues();
        cv.put(DespensaEntry.COLUMN_NOME,          nome);
        cv.put(DespensaEntry.COLUMN_QUANTIDADE,    quantidade);
        cv.put(DespensaEntry.COLUMN_UNIDADE,       unidade);
        cv.put(DespensaEntry.COLUMN_DATA_VALIDADE, dataValidade);
        cv.put(DespensaEntry.COLUMN_STATUS,        status);
        cv.put(DespensaEntry.COLUMN_CATEGORIA,     categoria);
        cv.put(DespensaEntry.COLUMN_USER_ID,       userId);
        try {
            return db.insertOrThrow(DespensaEntry.TABLE_NAME, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inserir '" + nome + "': " + e.getMessage());
            return -1;
        }
    }

    /**
     * Retorna uma data no formato yyyy-MM-dd relativa a hoje.
     * diasOffset negativo = passado, positivo = futuro.
     */
    private String dataRelativa(int diasOffset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, diasOffset);
        return sdf.format(cal.getTime());
    }

    /**
     * Garante que há um usuário logado.
     * Se não houver, tenta usar o primeiro usuário do banco (modo debug).
     */
    private String garantirUsuarioLogado() {
        String userId = sessionManager.getUserId();
        if (userId != null) return userId;

        // Fallback: pega o primeiro usuário cadastrado (apenas para testes)
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor c = null;
        try {
            c = db.query(UserEntry.TABLE_NAME,
                    new String[]{ UserEntry._ID, UserEntry.COLUMN_NOME },
                    null, null, null, null, null, "1");
            if (c.moveToFirst()) {
                String id   = c.getString(0);
                String nome = c.getString(1);
                sessionManager.salvarSessao(id, nome);
                log("⚠️  Sem sessão ativa. Usando usuário: " + nome);
                return id;
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    /** Adiciona linha ao TextView de log e ao Logcat. */
    private void log(String mensagem) {
        logBuffer.append(mensagem).append("\n");
        tvLog.setText(logBuffer.toString());
        Log.d(TAG, mensagem);
    }
}