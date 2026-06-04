package com.example.inventaai.ui.chefIA;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.model.ReceitaResponse;
import com.example.inventaai.data.model.ReceitaSalva;
import com.example.inventaai.data.remote.UnsplashService;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.data.repository.ReceitaRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.example.inventaai.ui.receitas.ReceitasActivity;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.CategoryIconHelper;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.GlideHelper;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ChefIAActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_TAG;

    public static final String EXTRA_ITENS_SELECIONADOS = "extra_itens_selecionados";

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView             tvToolbarTitulo;
    private TextView             tvRecipeTitle;
    private TextView             tvRecipeDescription;
    private TextView             tvTime;
    private TextView             tvServings;
    private TextView             tvDifficulty;
    private GridLayout           gridIngredientes;
    private LinearLayout         llSteps;
    private MaterialButton       btnSalvarReceita;
    private MaterialButton       btnNovaReceita;
    private BottomNavigationView bottomNavigation;
    private ProgressBar          progressBar;
    private View                 viewConteudo;
    private ImageView            ivRecipeImage;
    private LinearLayout         layoutEmptyRecipe;

    // Sprint 8 — seção de ingredientes selecionados
    private LinearLayout   layoutIngredientesSelecionados;
    private ChipGroup      chipGroupIngredientes;
    private MaterialButton btnAlterar;

    // Fix 3 — empty state "selecionar ingredientes"
    private LinearLayout layoutSelecionarIngredientes;

    // Fix 4 — botão gerar receita visível quando há itens selecionados
    private MaterialButton btnGerarReceitaComItens;

    // ── Dependências ──────────────────────────────────────────────────────────
    private DespensaRepository despensaRepository;
    private GeminiService      geminiService;
    private UnsplashService    unsplashService;
    // repositório de receitas salvas
    private ReceitaRepository  receitaRepository;

    // Receita atual em memória (para o botão Salvar)
    private ReceitaResponse receitaAtual;
    // URL da imagem carregada via Unsplash (para persistir junto com a receita)
    private String          imagemUrlAtual;

    // lista de itens recebidos da Despensa (pode ser nula)
    private List<DespensaItem> itensSelecionados;

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chef_ia);

        despensaRepository = new DespensaRepository(this);
        geminiService      = new GeminiService();
        unsplashService    = new UnsplashService();
        receitaRepository  = new ReceitaRepository(this);  // Sprint 11

        vincularViews();
        configurarBotoes();
        configurarBottomNavigation();

        verificarIntentEConfigurarTela();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_chef_ia);
        }
    }

    // =========================================================================
    // LÓGICA DE INICIALIZAÇÃO
    // =========================================================================

    @SuppressWarnings("unchecked")
    private void verificarIntentEConfigurarTela() {
        Intent intent = getIntent();

        boolean abrirSalvas = intent.getBooleanExtra("ABRIR_SALVAS", false);
        if (abrirSalvas) {

            startActivity(new Intent(this, ReceitasActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
            return;
        }

        if (intent != null && intent.hasExtra(EXTRA_ITENS_SELECIONADOS)) {
            itensSelecionados = (ArrayList<DespensaItem>)
                    intent.getSerializableExtra(EXTRA_ITENS_SELECIONADOS);

            if (itensSelecionados != null && !itensSelecionados.isEmpty()) {
                exibirIngredientesSelecionados(itensSelecionados);

                if (btnGerarReceitaComItens != null) btnGerarReceitaComItens.setVisibility(View.VISIBLE);
                if (layoutSelecionarIngredientes != null) layoutSelecionarIngredientes.setVisibility(View.GONE);

                mostrarEmptyState(false);
                mostrarCarregando(false);
                viewConteudo.setVisibility(View.GONE);
                btnNovaReceita.setEnabled(true);
                btnSalvarReceita.setEnabled(false);
                return;
            }
        }

        // Cenário B: acesso pelo BottomNav ou itens vazios
        itensSelecionados = null;

        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        AppExecutors.diskIO().execute(() -> {
            final int totalItens = userId != null
                    ? despensaRepository.listarAtivos(userId).size()
                    : 0;
            AppExecutors.mainThread().execute(() -> {
                if (totalItens > 0) {
                    mostrarEstadoSelecionarIngredientes();
                } else {
                    mostrarEmptyStateSemSelecao();
                }
            });
        });
    }

    private void exibirIngredientesSelecionados(List<DespensaItem> itens) {
        layoutIngredientesSelecionados.setVisibility(View.VISIBLE);
        chipGroupIngredientes.removeAllViews();

        for (DespensaItem item : itens) {
            Chip chip = new Chip(this);
            chip.setText(item.getNome());
            chip.setChipIconResource(CategoryIconHelper.getIcon(item.getCategoria()));
            chip.setChipIconVisible(true);
            chip.setClickable(false);
            chip.setFocusable(false);
            chipGroupIngredientes.addView(chip);
        }

        int n = itens.size();
        tvToolbarTitulo.setText("Chef IA · " + (n == 1 ? "1 ingrediente" : n + " ingredientes"));
    }

    private void mostrarEstadoSelecionarIngredientes() {
        if (layoutSelecionarIngredientes != null)   layoutSelecionarIngredientes.setVisibility(View.VISIBLE);
        if (layoutEmptyRecipe != null)              layoutEmptyRecipe.setVisibility(View.GONE);
        if (progressBar != null)                    progressBar.setVisibility(View.GONE);
        if (viewConteudo != null)                   viewConteudo.setVisibility(View.GONE);
        if (layoutIngredientesSelecionados != null) layoutIngredientesSelecionados.setVisibility(View.GONE);
    }

    private void mostrarEmptyStateSemSelecao() {
        if (layoutEmptyRecipe != null)              layoutEmptyRecipe.setVisibility(View.VISIBLE);
        if (layoutSelecionarIngredientes != null)   layoutSelecionarIngredientes.setVisibility(View.GONE);
        if (progressBar != null)                    progressBar.setVisibility(View.GONE);
        if (viewConteudo != null)                   viewConteudo.setVisibility(View.GONE);
        if (layoutIngredientesSelecionados != null) layoutIngredientesSelecionados.setVisibility(View.GONE);
    }

    // =========================================================================
    // GERAÇÃO DA RECEITA
    // =========================================================================

    private void gerarReceitaComItens(List<DespensaItem> itens) {
        // Cada nova geração reseta o estado do botão Salvar
        receitaAtual  = null;
        imagemUrlAtual = null;
        resetarBotaoSalvar();

        mostrarCarregando(true);

        geminiService.gerarReceita(itens, new GeminiService.ReceitaCallback() {
            @Override
            public void onSucesso(ReceitaResponse receita) {
                runOnUiThread(() -> {
                    receitaAtual = receita;
                    preencherReceita(receita, itens);
                    mostrarCarregando(false);
                    buscarImagemParaReceita(receita.getTitulo());
                    btnSalvarReceita.setEnabled(true);
                });
            }

            @Override
            public void onErro(String mensagem) {
                runOnUiThread(() -> {
                    mostrarCarregando(false);
                    mostrarErro(mensagem);
                });
            }
        });
    }

    // =========================================================================
    // SALVAR RECEITA  —  Sprint 11
    // =========================================================================

    private void salvarReceitaAtual() {
        if (receitaAtual == null) {
            Toast.makeText(this, "Nenhuma receita para salvar. Gere uma primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();

        // Monta o objeto ReceitaSalva a partir da ReceitaResponse
        ReceitaSalva receitaSalva = new ReceitaSalva(receitaAtual, userId);
        receitaSalva.setImagemUrl(imagemUrlAtual);

        // Persiste em background
        AppExecutors.diskIO().execute(() -> {
            long id = receitaRepository.salvar(receitaSalva);
            AppExecutors.mainThread().execute(() -> {
                if (id != -1) {
                    Toast.makeText(this,
                            "\"" + receitaAtual.getTitulo() + "\" salva!",
                            Toast.LENGTH_SHORT).show();
                    // Muda texto e desabilita para evitar duplicatas
                    btnSalvarReceita.setText("Salva! ✓");
                    btnSalvarReceita.setEnabled(false);
                } else {
                    Toast.makeText(this, "Erro ao salvar receita.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /** Restaura o botão Salvar ao estado inicial (nova receita). */
    private void resetarBotaoSalvar() {
        btnSalvarReceita.setText(getString(R.string.btn_save_recipe));
        btnSalvarReceita.setEnabled(false);
    }

    // =========================================================================
    // BUSCAR IMAGEM VIA UNSPLASH
    // =========================================================================

    private void buscarImagemParaReceita(String tituloReceita) {
        if (ivRecipeImage == null) return;
        Log.d(TAG, "ChefIA: buscando imagem para \"" + tituloReceita + "\"");

        unsplashService.buscarImagemReceita(tituloReceita, new UnsplashService.ImageCallback() {
            @Override
            public void onSucesso(String imageUrl) {
                runOnUiThread(() -> {
                    imagemUrlAtual = imageUrl;  // Sprint 11: guarda para persistência
                    Log.d(TAG, "ChefIA: carregando imagem → " + imageUrl);
                    GlideHelper.loadImage(ChefIAActivity.this, imageUrl, ivRecipeImage);
                });
            }

            @Override
            public void onErro(String mensagem) {
                Log.w(TAG, "ChefIA: imagem não carregada → " + mensagem);
            }
        });
    }

    // =========================================================================
    // PREENCHER UI COM A RECEITA
    // =========================================================================

    private void preencherReceita(ReceitaResponse receita, List<DespensaItem> itensUsados) {
        tvRecipeTitle.setText(receita.getTitulo());
        tvRecipeDescription.setText(
                "Uma receita criada especialmente para os ingredientes da sua despensa. " +
                        "Aproveite ao máximo o que você já tem!");

        tvTime.setText(receita.getTempoPreparo());
        tvServings.setText(receita.getPorcoes());
        tvDifficulty.setText(receita.getDificuldade());

        gridIngredientes.removeAllViews();
        List<String> ingredientes = receita.getIngredientes();
        if (ingredientes == null || ingredientes.isEmpty()) {
            for (DespensaItem item : itensUsados) {
                adicionarCartaoIngrediente(item.getNome(),
                        formatarQtd(item.getQuantidade()) + " " + item.getUnidadeMedida());
            }
        } else {
            for (String ingrediente : ingredientes) {
                String[] partes = ingrediente.split(" - ", 2);
                adicionarCartaoIngrediente(partes[0].trim(), partes.length > 1 ? partes[1].trim() : "");
            }
        }

        llSteps.removeAllViews();
        List<String> passos = receita.getPassos();
        if (passos != null && !passos.isEmpty()) {
            for (int i = 0; i < passos.size(); i++) adicionarPasso(i + 1, passos.get(i));
        } else {
            adicionarPasso(1, "Siga as instruções da receita e bom apetite!");
        }

        viewConteudo.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // HELPERS DE UI (cards de ingrediente e passos)
    // =========================================================================

    private void adicionarCartaoIngrediente(String nome, String quantidade) {
        MaterialCardView card = new MaterialCardView(this);
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.width      = 0;
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

    // =========================================================================
    // ESTADO DE UI
    // =========================================================================

    private void mostrarEmptyState(boolean vazio) {
        if (layoutEmptyRecipe != null)
            layoutEmptyRecipe.setVisibility(vazio ? View.VISIBLE : View.GONE);
        if (progressBar != null)
            progressBar.setVisibility(View.GONE);
        if (viewConteudo != null)
            viewConteudo.setVisibility(vazio ? View.GONE : View.VISIBLE);
    }

    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null)
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        if (viewConteudo != null)
            viewConteudo.setVisibility(carregando ? View.GONE : View.VISIBLE);
        if (layoutEmptyRecipe != null)
            layoutEmptyRecipe.setVisibility(View.GONE);
        btnNovaReceita.setEnabled(!carregando);
        btnSalvarReceita.setEnabled(!carregando);
        if (carregando) {
            tvRecipeTitle.setText(R.string.generating_recipe);
            tvRecipeDescription.setText("");
        }
    }

    private void mostrarErro(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
        tvRecipeTitle.setText(R.string.recipe_title_placeholder);
        tvRecipeDescription.setText(mensagem);
        btnNovaReceita.setEnabled(true);
        btnSalvarReceita.setEnabled(false);
    }

    // =========================================================================
    // VIEWS E BOTÕES
    // =========================================================================

    private void vincularViews() {
        tvToolbarTitulo     = findViewById(R.id.tvToolbarTitulo);
        tvRecipeTitle       = findViewById(R.id.tvRecipeTitle);
        tvRecipeDescription = findViewById(R.id.tvRecipeDescription);
        tvTime              = findViewById(R.id.tvTime);
        tvServings          = findViewById(R.id.tvServings);
        tvDifficulty        = findViewById(R.id.tvDifficulty);
        gridIngredientes    = findViewById(R.id.gridIngredientes);
        llSteps             = findViewById(R.id.llSteps);
        btnSalvarReceita    = findViewById(R.id.btnSalvarReceita);
        btnNovaReceita      = findViewById(R.id.btnNovaReceita);
        bottomNavigation    = findViewById(R.id.bottomNavigation);
        progressBar         = findViewById(R.id.progressBarChef);
        viewConteudo        = findViewById(R.id.scrollViewConteudo);
        ivRecipeImage       = findViewById(R.id.ivRecipeImage);
        layoutEmptyRecipe   = findViewById(R.id.layoutEmptyRecipe);

        layoutIngredientesSelecionados = findViewById(R.id.layoutIngredientesSelecionados);
        chipGroupIngredientes          = findViewById(R.id.chipGroupIngredientes);
        btnAlterar                     = findViewById(R.id.btnAlterar);

        layoutSelecionarIngredientes = findViewById(R.id.layoutSelecionarIngredientes);
        btnGerarReceitaComItens      = findViewById(R.id.btnGerarReceitaComItens);

        // Botão Voltar
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // botão "Receitas Salvas" na toolbar → abre ReceitasActivity
        View btnSavedRecipes = findViewById(R.id.btnSavedRecipes);
        if (btnSavedRecipes != null) {
            btnSavedRecipes.setOnClickListener(v -> {
                startActivity(new Intent(this, ReceitasActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    private void configurarBotoes() {
        btnNovaReceita   = findViewById(R.id.btnNovaReceita);
        btnSalvarReceita = findViewById(R.id.btnSalvarReceita);

        // Botão principal "Gerar Receita" com itens selecionados
        if (btnGerarReceitaComItens != null) {
            btnGerarReceitaComItens.setOnClickListener(v -> {
                if (itensSelecionados != null && !itensSelecionados.isEmpty()) {
                    if (layoutIngredientesSelecionados != null)
                        layoutIngredientesSelecionados.setVisibility(View.GONE);
                    gerarReceitaComItens(itensSelecionados);
                }
            });
        }

        // Botão "Ir para Despensa" no estado selecionar
        View btnIrParaSelecao = findViewById(R.id.btnIrParaSelecao);
        if (btnIrParaSelecao != null) {
            btnIrParaSelecao.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        // "Gerar Nova Receita"
        btnNovaReceita.setOnClickListener(v -> {
            if (itensSelecionados != null && !itensSelecionados.isEmpty()) {
                gerarReceitaComItens(itensSelecionados);
            } else {
                Toast.makeText(this, "Selecione ingredientes na despensa primeiro", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // "Salvar Receita" → persiste no banco
        btnSalvarReceita.setOnClickListener(v -> salvarReceitaAtual());

        // "Alterar" → volta para a Despensa
        if (btnAlterar != null) {
            btnAlterar.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        // Botão no empty state da despensa vazia
        View btnIrParaDespensa = findViewById(R.id.btnIrParaDespensa);
        if (btnIrParaDespensa != null) {
            btnIrParaDespensa.setOnClickListener(v -> {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    // =========================================================================
    // BOTTOM NAVIGATION
    // =========================================================================

    private void configurarBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_chef_ia) {
                return true;
            } else if (id == R.id.nav_pantry) {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_history) {
                Intent intent = new Intent(this, HistoricoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // UTILITÁRIOS
    // =========================================================================

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String formatarQtd(double qtd) {
        return qtd == Math.floor(qtd) ? String.valueOf((int) qtd) : String.valueOf(qtd);
    }
}