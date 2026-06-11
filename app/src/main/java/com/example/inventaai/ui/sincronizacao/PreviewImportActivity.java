package com.example.inventaai.ui.sincronizacao;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.db.DatabaseContract.DespensaEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 20 — Tela de Pré-visualização e Confirmação da Importação CSV.
 *
 * Fluxo:
 *  1. Recebe lista de DespensaItem via Intent (lida pelo CsvHelper)
 *  2. Exibe num RecyclerView com validação visual
 *  3. Ao confirmar, verifica duplicatas (Smart Merge)
 *  4. Se há duplicatas → AlertDialog com opções Somar / Substituir
 *  5. Executa inserção/atualização em lote via transação SQLite
 */
public class PreviewImportActivity extends AppCompatActivity {

    public static final String EXTRA_ITENS            = "extra_itens_csv";
    public static final String EXTRA_TOTAL_INVALIDOS  = "extra_total_invalidos";

    private static final String TAG = Constants.LOG_TAG;

    // ── Opções de Smart Merge ─────────────────────────────────────────────────
    private static final int MERGE_SOMAR      = 0;
    private static final int MERGE_SUBSTITUIR = 1;

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView               rvPreview;
    private TextView                   tvStatus;
    private ImageView                  ivStatusIcon;
    private MaterialButton             btnConfirmar;
    private CircularProgressIndicator  progressBar;

    // ── Dados e dependências ──────────────────────────────────────────────────
    private ArrayList<DespensaItem>    itens;
    private int                        totalInvalidos;
    private PreviewImportAdapter       adapter;
    private DespensaRepository         despensaRepository;
    private DatabaseHelper             dbHelper;
    private SessionManager             sessionManager;

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_import);

        despensaRepository = new DespensaRepository(this);
        dbHelper           = DatabaseHelper.getInstance(this);
        sessionManager     = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Recupera dados da Intent
        itens          = (ArrayList<DespensaItem>) getIntent()
                .getSerializableExtra(EXTRA_ITENS);
        totalInvalidos = getIntent().getIntExtra(EXTRA_TOTAL_INVALIDOS, 0);

        if (itens == null) itens = new ArrayList<>();

        vincularViews();
        configurarToolbar();
        configurarRecyclerView();
        atualizarBannerStatus();
        configurarBotaoConfirmar();
    }

    // =========================================================================
    // CONFIGURAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvPreview     = findViewById(R.id.rvPreviewItens);
        tvStatus      = findViewById(R.id.tvStatusImport);
        ivStatusIcon  = findViewById(R.id.ivStatusIcon);
        btnConfirmar  = findViewById(R.id.btnConfirmarImport);
        progressBar   = findViewById(R.id.progressBar);
    }

    private void configurarToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void configurarRecyclerView() {
        String[] categorias = getResources().getStringArray(R.array.categories);
        adapter = new PreviewImportAdapter(this, itens, categorias);
        rvPreview.setLayoutManager(new LinearLayoutManager(this));
        rvPreview.setAdapter(adapter);
        // Desabilita animações padrão para evitar flicker nos Spinners
        rvPreview.setItemAnimator(null);
    }

    private void atualizarBannerStatus() {
        int total = itens.size();
        if (totalInvalidos > 0) {
            tvStatus.setText("Encontramos " + total + " item(ns). "
                    + totalInvalidos + " precisam de correção.");
            ivStatusIcon.setVisibility(View.VISIBLE);
            tvStatus.setTextColor(getColor(R.color.colorError));
        } else {
            tvStatus.setText("Encontramos " + total + " item(ns). Todos prontos para importar.");
            ivStatusIcon.setVisibility(View.GONE);
            tvStatus.setTextColor(getColor(android.R.color.tab_indicator_text));
        }
    }

    private void configurarBotaoConfirmar() {
        btnConfirmar.setOnClickListener(v -> verificarDuplicatasEImportar());
    }

    // =========================================================================
    // SMART MERGE — verificação de duplicatas
    // =========================================================================

    /**
     * Compara os itens do CSV com a despensa atual.
     * Se encontrar duplicatas (mesmo nome + categoria), exibe dialog de merge.
     * Caso contrário, importa diretamente.
     */
    private void verificarDuplicatasEImportar() {
        mostrarCarregando(true);
        btnConfirmar.setEnabled(false);
        final String userId = sessionManager.getUserId();

        AppExecutors.diskIO().execute(() -> {
            List<DespensaItem> ativos     = despensaRepository.listarAtivos(userId);
            List<DespensaItem> duplicatas = encontrarDuplicatas(itens, ativos);

            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);
                btnConfirmar.setEnabled(true);

                if (duplicatas.isEmpty()) {
                    // Sem conflitos — importa direto
                    executarImportacao(userId, MERGE_SOMAR);
                } else {
                    // Exibe dialog de resolução de conflitos
                    exibirDialogSmartMerge(duplicatas.size(), userId);
                }
            });
        });
    }

    /**
     * Identifica duplicatas comparando nome (case-insensitive) + categoria.
     */
    private List<DespensaItem> encontrarDuplicatas(List<DespensaItem> novos,
                                                   List<DespensaItem> existentes) {
        List<DespensaItem> duplicatas = new ArrayList<>();
        for (DespensaItem novo : novos) {
            for (DespensaItem existente : existentes) {
                boolean mesmoNome = existente.getNome() != null
                        && existente.getNome().equalsIgnoreCase(novo.getNome());
                boolean mesmaCategoria = existente.getCategoria() != null
                        && existente.getCategoria().equalsIgnoreCase(novo.getCategoria());
                if (mesmoNome && mesmaCategoria) {
                    duplicatas.add(novo);
                    break;
                }
            }
        }
        return duplicatas;
    }

    // =========================================================================
    // DIALOG — Smart Merge
    // =========================================================================

    private void exibirDialogSmartMerge(int qtdDuplicatas, String userId) {
        String mensagem = "Notamos que " + qtdDuplicatas + " item(ns) da planilha "
                + "já existe(m) na sua despensa. Como deseja prosseguir?";

        new AlertDialog.Builder(this)
                .setTitle("Itens Repetidos Encontrados")
                .setMessage(mensagem)
                .setPositiveButton("Somar Quantidades", (dialog, which) ->
                        executarImportacao(userId, MERGE_SOMAR))
                .setNegativeButton("Substituir Quantidades", (dialog, which) ->
                        executarImportacao(userId, MERGE_SUBSTITUIR))
                .setNeutralButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    // =========================================================================
    // IMPORTAÇÃO EM LOTE — transação SQLite
    // =========================================================================

    /**
     * Persiste os itens no banco de dados em uma única transação.
     * Estratégia de merge aplicada aos itens duplicados:
     *  - MERGE_SOMAR:      qtd_banco += qtd_planilha
     *  - MERGE_SUBSTITUIR: qtd_banco  = qtd_planilha
     *
     * Itens marcados como INVALIDO têm a quantidade corrigida para 1.0
     * antes da persistência.
     */
    private void executarImportacao(String userId, int estrategiaMerge) {
        mostrarCarregando(true);
        btnConfirmar.setEnabled(false);

        // Garante categoria dos itens editada no Spinner
        List<DespensaItem> itensParaSalvar = new ArrayList<>(itens);

        AppExecutors.diskIO().execute(() -> {
            int inseridos   = 0;
            int atualizados = 0;
            boolean sucesso = false;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                db.beginTransaction();

                List<DespensaItem> ativos = despensaRepository.listarAtivos(userId);

                for (DespensaItem item : itensParaSalvar) {
                    // Corrige itens inválidos antes de persistir
                    if ("INVALIDO".equals(item.getStatus())) {
                        if (item.getQuantidade() <= 0) item.setQuantidade(1.0);
                        item.setStatus(Constants.STATUS_ATIVO);
                    }

                    // Garante categoria preenchida
                    if (item.getCategoria() == null || item.getCategoria().isEmpty()) {
                        item.setCategoria("Outros");
                    }

                    // Verifica se é duplicata
                    DespensaItem existente = encontrarExistente(item, ativos);

                    if (existente != null) {
                        // ── Atualiza item existente ──────────────────────────
                        ContentValues cv = new ContentValues();
                        if (estrategiaMerge == MERGE_SOMAR) {
                            cv.put(DespensaEntry.COLUMN_QUANTIDADE,
                                    existente.getQuantidade() + item.getQuantidade());
                        } else {
                            cv.put(DespensaEntry.COLUMN_QUANTIDADE, item.getQuantidade());
                        }
                        // Atualiza validade se a planilha trouxer uma nova
                        if (item.getDataValidade() != null && !item.getDataValidade().isEmpty()) {
                            cv.put(DespensaEntry.COLUMN_DATA_VALIDADE, item.getDataValidade());
                        }
                        db.update(DespensaEntry.TABLE_NAME, cv,
                                DespensaEntry._ID + " = ?",
                                new String[]{ String.valueOf(existente.getId()) });
                        atualizados++;
                    } else {
                        // ── Insere novo item ─────────────────────────────────
                        ContentValues cv = toContentValues(item, userId);
                        db.insertOrThrow(DespensaEntry.TABLE_NAME, null, cv);
                        inseridos++;
                    }
                }

                db.setTransactionSuccessful();
                sucesso = true;
                Log.d(TAG, "PreviewImport: " + inseridos + " inseridos, "
                        + atualizados + " atualizados.");

            } catch (Exception e) {
                Log.e(TAG, "executarImportacao: erro na transação", e);
            } finally {
                db.endTransaction();
            }

            final boolean ok  = sucesso;
            final int ins     = inseridos;
            final int upd     = atualizados;

            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);
                if (ok) {
                    String msg = ins + " item(ns) adicionado(s)";
                    if (upd > 0) msg += ", " + upd + " atualizado(s)";
                    msg += ". Importação concluída!";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this,
                            "Erro ao importar. Tente novamente.",
                            Toast.LENGTH_SHORT).show();
                    btnConfirmar.setEnabled(true);
                }
            });
        });
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Busca um item existente na lista de ativos com mesmo nome + categoria.
     */
    private DespensaItem encontrarExistente(DespensaItem novo, List<DespensaItem> ativos) {
        for (DespensaItem ativo : ativos) {
            boolean mesmoNome = ativo.getNome() != null
                    && ativo.getNome().equalsIgnoreCase(novo.getNome());
            boolean mesmaCategoria = ativo.getCategoria() != null
                    && ativo.getCategoria().equalsIgnoreCase(novo.getCategoria());
            if (mesmoNome && mesmaCategoria) return ativo;
        }
        return null;
    }

    private ContentValues toContentValues(DespensaItem item, String userId) {
        ContentValues v = new ContentValues();
        v.put(DespensaEntry.COLUMN_NOME,          item.getNome());
        v.put(DespensaEntry.COLUMN_QUANTIDADE,     item.getQuantidade());
        v.put(DespensaEntry.COLUMN_UNIDADE,        item.getUnidadeMedida() != null
                ? item.getUnidadeMedida() : "unid");
        v.put(DespensaEntry.COLUMN_DATA_VALIDADE,  item.getDataValidade());
        v.put(DespensaEntry.COLUMN_STATUS,         Constants.STATUS_ATIVO);
        v.put(DespensaEntry.COLUMN_CATEGORIA,      item.getCategoria());
        v.put(DespensaEntry.COLUMN_USER_ID,        userId);
        return v;
    }

    private void mostrarCarregando(boolean carregando) {
        if (progressBar == null) return;
        progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
    }
}