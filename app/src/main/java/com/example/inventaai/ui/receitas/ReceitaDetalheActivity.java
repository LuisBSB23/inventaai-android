package com.example.inventaai.ui.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.model.ReceitaSalva;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.data.repository.ReceitaRepository;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.GlideHelper;
import com.example.inventaai.util.SessionManager;
import com.example.inventaai.util.UnitConverterUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class ReceitaDetalheActivity extends AppCompatActivity {

    public static final String EXTRA_RECEITA = "extra_receita_salva";

    // ── Views ──────────────────────────────────────────────────────────────────
    private ImageView    ivRecipeImage;
    private TextView     tvRecipeTitle;
    private TextView     tvTime;
    private TextView     tvServings;
    private TextView     tvDifficulty;
    private GridLayout   gridIngredientes;
    private LinearLayout llSteps;
    private LinearLayout llCrossCheck;
    private ImageButton  btnShare;

    // Botões de execução
    private MaterialButton btnIniciarPreparo;
    private MaterialButton btnCancelarPreparo;
    private MaterialButton btnFinalizarReceita;

    // Sprint 16: botão para marcar como CONCLUIDA
    private MaterialButton btnConcluirReceita;

    // ── Dados ──────────────────────────────────────────────────────────────────
    private ReceitaSalva       receita;
    private ReceitaRepository  receitaRepo;
    private DespensaRepository despensaRepo;
    private SessionManager     sessionManager;
    private String             currentUserId;

    /** Resultado do cross-check — reutilizado no fluxo de finalização. */
    private List<IngredienteMatch> matches;

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receita_detalhe);

        sessionManager = new SessionManager(this);
        currentUserId  = sessionManager.getUserId();
        receitaRepo    = new ReceitaRepository(this);
        despensaRepo   = new DespensaRepository(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        receita = (ReceitaSalva) getIntent().getSerializableExtra(EXTRA_RECEITA);
        if (receita == null) { finish(); return; }

        vincularViews();
        preencherReceita();
        configurarBotoesExecucao();
        configurarBotaoConcluir();      // Sprint 16
        configurarBotaoCompartilhar();
        executarCrossCheck();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        ivRecipeImage       = findViewById(R.id.ivRecipeImage);
        tvRecipeTitle       = findViewById(R.id.tvRecipeTitle);
        tvTime              = findViewById(R.id.tvTime);
        tvServings          = findViewById(R.id.tvServings);
        tvDifficulty        = findViewById(R.id.tvDifficulty);
        gridIngredientes    = findViewById(R.id.gridIngredientes);
        llSteps             = findViewById(R.id.llSteps);
        llCrossCheck        = findViewById(R.id.llCrossCheck);
        btnShare            = findViewById(R.id.btnShare);
        btnIniciarPreparo   = findViewById(R.id.btnIniciarPreparo);
        btnCancelarPreparo  = findViewById(R.id.btnCancelarPreparo);
        btnFinalizarReceita = findViewById(R.id.btnFinalizarReceita);

        // Sprint 16: botão CONCLUÍDA (pode ser null se o layout ainda não tiver a view)
        btnConcluirReceita  = findViewById(R.id.btnConcluirReceita);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    // =========================================================================
    // PREENCHER UI
    // =========================================================================

    private void preencherReceita() {
        if (receita.getImagemUrl() != null && !receita.getImagemUrl().isEmpty()) {
            GlideHelper.loadImage(this, receita.getImagemUrl(), ivRecipeImage);
        }
        tvRecipeTitle.setText(receita.getTitulo() != null ? receita.getTitulo() : "Receita");
        tvTime.setText(receita.getTempoPreparo() != null ? receita.getTempoPreparo() : "—");
        tvServings.setText(receita.getPorcoes() != null ? receita.getPorcoes() : "—");
        tvDifficulty.setText(receita.getDificuldade() != null ? receita.getDificuldade() : "—");

        gridIngredientes.removeAllViews();
        List<String> ingredientes = receita.getIngredientes();
        if (ingredientes != null) {
            for (String ingrediente : ingredientes) {
                String[] partes = ingrediente.split(" - ", 2);
                adicionarCartaoIngrediente(
                        partes[0].trim(),
                        partes.length > 1 ? partes[1].trim() : "");
            }
        }

        llSteps.removeAllViews();
        List<String> passos = receita.getPassos();
        if (passos != null) {
            for (int i = 0; i < passos.size(); i++) adicionarPasso(i + 1, passos.get(i));
        }
    }

    // =========================================================================
    // CROSS-CHECK com normalização matemática — Sprint 16
    // =========================================================================

    private void executarCrossCheck() {
        if (llCrossCheck == null || currentUserId == null) return;
        if (receita.getIngredientes() == null || receita.getIngredientes().isEmpty()) return;

        AppExecutors.diskIO().execute(() -> {
            List<DespensaItem> itensAtivos = despensaRepo.listarAtivos(currentUserId);

            // Sprint 16: usa helper que aplica normalização de unidades antes da comparação
            final List<IngredienteMatch> resultado =
                    IngredienteMatchHelper.cruzarComNormalizacao(receita.getIngredientes(), itensAtivos);

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                matches = resultado;
                exibirCrossCheck(resultado);
            });
        });
    }

    private void exibirCrossCheck(List<IngredienteMatch> resultado) {
        if (llCrossCheck == null) return;
        llCrossCheck.removeAllViews();

        TextView tvSec = new TextView(this);
        tvSec.setText("Status dos Ingredientes");
        tvSec.setTextSize(16f);
        tvSec.setTypeface(getResources().getFont(R.font.inter_semibold));
        tvSec.setTextColor(getColor(R.color.colorOnSurface));
        tvSec.setPadding(0, 0, 0, dpToPx(12));
        llCrossCheck.addView(tvSec);

        for (IngredienteMatch m : resultado) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dpToPx(8));
            row.setLayoutParams(rowParams);

            TextView tvIcone = new TextView(this);
            tvIcone.setTextSize(18f);
            tvIcone.setPadding(0, 0, dpToPx(10), 0);
            switch (m.getStatus()) {
                case POSSUI:       tvIcone.setText("🟢"); break;
                case INSUFICIENTE: tvIcone.setText("🟡"); break;
                default:           tvIcone.setText("🔴"); break;
            }

            TextView tvDesc = new TextView(this);
            tvDesc.setTextSize(13f);
            tvDesc.setTextColor(getColor(R.color.colorOnSurface));
            tvDesc.setTypeface(getResources().getFont(R.font.inter_regular));
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvDesc.setLayoutParams(descParams);

            switch (m.getStatus()) {
                case POSSUI:
                    tvDesc.setText(m.getNomeIngrediente() + " ✓");
                    tvDesc.setTextColor(getColor(android.R.color.holo_green_dark));
                    break;
                case INSUFICIENTE:
                    tvDesc.setText(m.getNomeIngrediente() + " — falta "
                            + formatarQtd(m.getQuantidadeFaltante()));
                    tvDesc.setTextColor(getColor(android.R.color.holo_orange_dark));
                    break;
                default:
                    tvDesc.setText(m.getNomeIngrediente() + " — não encontrado");
                    tvDesc.setTextColor(getColor(android.R.color.holo_red_dark));
                    break;
            }

            row.addView(tvIcone);
            row.addView(tvDesc);
            llCrossCheck.addView(row);
        }
    }

    private String formatarQtd(double qtd) {
        if (qtd == Math.floor(qtd)) return String.valueOf((int) qtd);
        return String.format(Locale.getDefault(), "%.1f", qtd);
    }

    // =========================================================================
    // CONTROLE DE EXECUÇÃO
    // =========================================================================

    private void configurarBotoesExecucao() {
        if (btnIniciarPreparo == null) return;
        atualizarEstadoBotoes(receita.getStatus());

        btnIniciarPreparo.setOnClickListener(v -> alterarStatus("EM_ANDAMENTO"));
        btnCancelarPreparo.setOnClickListener(v -> alterarStatus("SALVA"));
        btnFinalizarReceita.setOnClickListener(v -> abrirDialogFinalizar());
    }

    private void configurarBotaoConcluir() {
        if (btnConcluirReceita == null) return;

        // Exibe o botão somente quando a receita está FINALIZADA (preparo concluído)
        boolean mostrar = "FINALIZADA".equals(receita.getStatus())
                || "EM_ANDAMENTO".equals(receita.getStatus());
        btnConcluirReceita.setVisibility(mostrar ? View.VISIBLE : View.GONE);

        btnConcluirReceita.setOnClickListener(v -> concluirReceita());
    }

    private void concluirReceita() {
        AppExecutors.diskIO().execute(() -> {
            receitaRepo.atualizarStatusReceita(receita.getId(), "CONCLUIDA");
            receita.setStatus("CONCLUIDA");
            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this,
                        "\"" + receita.getTitulo() + "\" marcada como concluída!",
                        Toast.LENGTH_SHORT).show();
                if (btnConcluirReceita != null) btnConcluirReceita.setVisibility(View.GONE);
                if (btnIniciarPreparo  != null) btnIniciarPreparo.setVisibility(View.GONE);
                if (btnCancelarPreparo != null) btnCancelarPreparo.setVisibility(View.GONE);
                if (btnFinalizarReceita!= null) btnFinalizarReceita.setVisibility(View.GONE);
            });
        });
    }

    private void alterarStatus(String novoStatus) {
        AppExecutors.diskIO().execute(() -> {
            receitaRepo.atualizarStatusReceita(receita.getId(), novoStatus);
            receita.setStatus(novoStatus);
            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                atualizarEstadoBotoes(novoStatus);
                // Atualiza visibilidade do botão CONCLUIR
                if (btnConcluirReceita != null) {
                    boolean mostrar = "FINALIZADA".equals(novoStatus)
                            || "EM_ANDAMENTO".equals(novoStatus);
                    btnConcluirReceita.setVisibility(mostrar ? View.VISIBLE : View.GONE);
                }
                String msg = "EM_ANDAMENTO".equals(novoStatus)
                        ? "Preparo iniciado!" : "Preparo cancelado.";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void atualizarEstadoBotoes(String status) {
        boolean emAndamento = "EM_ANDAMENTO".equals(status);
        btnIniciarPreparo.setVisibility(emAndamento ? View.GONE    : View.VISIBLE);
        btnCancelarPreparo.setVisibility(emAndamento ? View.VISIBLE : View.GONE);
        btnFinalizarReceita.setVisibility(emAndamento ? View.VISIBLE : View.GONE);
    }

    private void abrirDialogFinalizar() {
        if (matches == null || matches.isEmpty()) {
            alterarStatus("FINALIZADA");
            Toast.makeText(this, "Receita finalizada!", Toast.LENGTH_SHORT).show();
            return;
        }

        ConfirmarIngredientesDialog dialog = ConfirmarIngredientesDialog.newInstance(
                receita.getTitulo(),
                currentUserId,
                matches,
                () -> {
                    alterarStatus("FINALIZADA");
                    Toast.makeText(this,
                            "Receita finalizada! Despensa atualizada.", Toast.LENGTH_SHORT).show();
                }
        );
        dialog.show(getSupportFragmentManager(), "confirmar_ingredientes");
    }

    // =========================================================================
    // COMPARTILHAMENTO
    // =========================================================================

    private void configurarBotaoCompartilhar() {
        if (btnShare == null) return;
        btnShare.setOnClickListener(v -> compartilharReceita());
    }

    private void compartilharReceita() {
        StringBuilder sb = new StringBuilder();
        sb.append("🍽️ ").append(receita.getTitulo()).append("\n\n");
        if (receita.getTempoPreparo() != null)
            sb.append("⏱️ Tempo de preparo: ").append(receita.getTempoPreparo()).append("\n");
        if (receita.getPorcoes() != null)
            sb.append("🍴 Porções: ").append(receita.getPorcoes()).append("\n");
        if (receita.getDificuldade() != null)
            sb.append("⭐ Dificuldade: ").append(receita.getDificuldade()).append("\n");
        sb.append("\n📋 INGREDIENTES\n");
        List<String> ingredientes = receita.getIngredientes();
        if (ingredientes != null) {
            for (String ing : ingredientes) sb.append("• ").append(ing).append("\n");
        }
        sb.append("\n👨‍🍳 MODO DE PREPARO\n");
        List<String> passos = receita.getPassos();
        if (passos != null) {
            for (int i = 0; i < passos.size(); i++) {
                sb.append(i + 1).append(". ").append(passos.get(i)).append("\n\n");
            }
        }
        sb.append("\n🤖 Receita gerada pelo Chef IA do InventaAí");
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, receita.getTitulo());
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Compartilhar receita via"));
    }

    // =========================================================================
    // HELPERS DE UI
    // =========================================================================

    private void adicionarCartaoIngrediente(String nome, String quantidade) {
        MaterialCardView card = new MaterialCardView(this);
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.width = 0;
        cardParams.setMargins(8, 8, 8, 8);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getColor(R.color.colorSurfaceContainerLowest));
        card.setRadius(dpToPx(16));
        card.setCardElevation(0f);
        card.setStrokeColor(getColor(R.color.colorSurfaceContainerHighest));
        card.setStrokeWidth(dpToPx(1));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = dpToPx(12);
        row.setPadding(pad, pad, pad, pad);

        LinearLayout colTexto = new LinearLayout(this);
        colTexto.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colTexto.setLayoutParams(colParams);

        TextView tvNome = new TextView(this);
        tvNome.setText(nome);
        tvNome.setTextSize(13f);
        tvNome.setTextColor(getColor(R.color.colorOnSurface));
        tvNome.setTypeface(getResources().getFont(R.font.inter_semibold));
        tvNome.setMaxLines(2);
        tvNome.setEllipsize(android.text.TextUtils.TruncateAt.END);
        colTexto.addView(tvNome);

        if (quantidade != null && !quantidade.isEmpty()) {
            TextView tvQtd = new TextView(this);
            tvQtd.setText(quantidade);
            tvQtd.setTextSize(12f);
            tvQtd.setTextColor(getColor(R.color.colorOnSurfaceVariant));
            tvQtd.setTypeface(getResources().getFont(R.font.inter_regular));
            colTexto.addView(tvQtd);
        }

        row.addView(colTexto);
        card.addView(row);
        gridIngredientes.addView(card);
    }

    private void adicionarPasso(int numero, String descricao) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(20));
        row.setLayoutParams(rowParams);

        TextView tvNum = new TextView(this);
        tvNum.setText(String.valueOf(numero));
        tvNum.setTextSize(13f);
        tvNum.setTextColor(getColor(R.color.colorOnPrimaryContainer));
        tvNum.setTypeface(getResources().getFont(R.font.inter_bold));
        tvNum.setGravity(android.view.Gravity.CENTER);
        tvNum.setBackground(getDrawable(R.drawable.bg_circle_primary_container));
        int size = dpToPx(32);
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(size, size);
        numParams.setMargins(0, dpToPx(4), dpToPx(16), 0);
        tvNum.setLayoutParams(numParams);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(descricao);
        tvDesc.setTextSize(15f);
        tvDesc.setTextColor(getColor(R.color.colorOnSurface));
        tvDesc.setTypeface(getResources().getFont(R.font.inter_regular));
        tvDesc.setLineSpacing(dpToPx(4), 1f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvDesc.setLayoutParams(descParams);

        row.addView(tvNum);
        row.addView(tvDesc);
        llSteps.addView(row);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}