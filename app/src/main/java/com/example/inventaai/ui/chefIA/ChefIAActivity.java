package com.example.inventaai.ui.chefIA;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.example.inventaai.data.remote.UnsplashService;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
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
    private LinearLayout         layoutIngredientesSelecionados;
    private ChipGroup            chipGroupIngredientes;
    private MaterialButton       btnAlterar;

    // Fix 3 — empty state "selecionar ingredientes"
    private LinearLayout         layoutSelecionarIngredientes;

    // Fix 4 — botão gerar receita visível quando há itens selecionados
    private MaterialButton       btnGerarReceitaComItens;

    // ── Dependências ──────────────────────────────────────────────────────────
    private DespensaRepository despensaRepository;
    private GeminiService      geminiService;
    private UnsplashService    unsplashService;

    // Receita atual em memória (para o botão Salvar)
    private ReceitaResponse receitaAtual;

    // Sprint 8 — lista de itens recebidos da Despensa (pode ser nula)
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

        vincularViews();
        configurarBotoes();
        configurarBottomNavigation();

        // Sprint 8: verifica se viemos com itens selecionados da Despensa
        verificarIntentEConfigurarTela();
    }

    // =========================================================================
    // LÓGICA DE INICIALIZAÇÃO
    // =========================================================================

    @SuppressWarnings("unchecked")
    private void verificarIntentEConfigurarTela() {
        Intent intent = getIntent();

        if (intent != null && intent.hasExtra(EXTRA_ITENS_SELECIONADOS)) {
            // ── Cenário A: viemos da Despensa com itens selecionados ───────
            itensSelecionados = (ArrayList<DespensaItem>)
                    intent.getSerializableExtra(EXTRA_ITENS_SELECIONADOS);

            if (itensSelecionados != null && !itensSelecionados.isEmpty()) {
                exibirIngredientesSelecionados(itensSelecionados);
                // Fix 4: mostra o botão "Gerar Receita" abaixo dos chips
                if (btnGerarReceitaComItens != null) btnGerarReceitaComItens.setVisibility(View.VISIBLE);
                // O conteúdo da receita fica oculto até o usuário clicar em Gerar
                mostrarEmptyState(false);
                mostrarCarregando(false);
                viewConteudo.setVisibility(View.GONE);
                btnNovaReceita.setEnabled(true);
                btnSalvarReceita.setEnabled(false);
                return;
            }
        }

        // ── Cenário B: acesso pelo BottomNav ou itens vazios ──────────────
        itensSelecionados = null;

        // Fix 3: verifica se a despensa tem itens para exibir o estado correto
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        AppExecutors.diskIO().execute(() -> {
            final int totalItens = userId != null
                    ? despensaRepository.listarAtivos(userId).size()
                    : 0;
            AppExecutors.mainThread().execute(() -> {
                if (totalItens > 0) {
                    // Despensa com itens: orienta o usuário a selecionar
                    mostrarEstadoSelecionarIngredientes();
                } else {
                    // Despensa vazia: empty state original
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

            // Ícone da categoria via CategoryIconHelper
            int iconRes = CategoryIconHelper.getIcon(item.getCategoria());
            chip.setChipIconResource(iconRes);
            chip.setChipIconVisible(true);

            chip.setClickable(false);
            chip.setFocusable(false);
            chipGroupIngredientes.addView(chip);
        }

        // Atualiza o título da toolbar: "Chef IA · N ingredientes"
        int n = itens.size();
        String sufixo = n == 1 ? "1 ingrediente" : n + " ingredientes";
        tvToolbarTitulo.setText("Chef IA · " + sufixo);
    }

    private void mostrarEstadoSelecionarIngredientes() {
        if (layoutSelecionarIngredientes != null)
            layoutSelecionarIngredientes.setVisibility(View.VISIBLE);
        if (layoutEmptyRecipe != null)
            layoutEmptyRecipe.setVisibility(View.GONE);
        if (progressBar != null)
            progressBar.setVisibility(View.GONE);
        if (viewConteudo != null)
            viewConteudo.setVisibility(View.GONE);
        layoutIngredientesSelecionados.setVisibility(View.GONE);
    }

    private void mostrarEmptyStateSemSelecao() {
        if (layoutEmptyRecipe != null) {
            layoutEmptyRecipe.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (viewConteudo != null) {
            viewConteudo.setVisibility(View.GONE);
        }
        layoutIngredientesSelecionados.setVisibility(View.GONE);
    }

    // =========================================================================
    // GERAÇÃO DA RECEITA (sob demanda — Sprint 8)
    // =========================================================================

    private void gerarReceitaComItens(List<DespensaItem> itens) {
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
    // Buscar imagem via Unsplash
    // =========================================================================

    private void buscarImagemParaReceita(String tituloReceita) {
        if (ivRecipeImage == null) return;
        Log.d(TAG, "ChefIA: buscando imagem para \"" + tituloReceita + "\"");

        unsplashService.buscarImagemReceita(tituloReceita, new UnsplashService.ImageCallback() {
            @Override
            public void onSucesso(String imageUrl) {
                runOnUiThread(() -> {
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
    // Preencher UI com a receita
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
                String nome = partes[0].trim();
                String qtd  = partes.length > 1 ? partes[1].trim() : "";
                adicionarCartaoIngrediente(nome, qtd);
            }
        }

        llSteps.removeAllViews();
        List<String> passos = receita.getPassos();
        if (passos != null && !passos.isEmpty()) {
            for (int i = 0; i < passos.size(); i++) {
                adicionarPasso(i + 1, passos.get(i));
            }
        } else {
            adicionarPasso(1, "Siga as instruções da receita e bom apetite!");
        }
    }

    // =========================================================================
    // Helpers de UI (cartão de ingrediente e passo)
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
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
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
    // Estado de UI
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

        // Sprint 8
        layoutIngredientesSelecionados = findViewById(R.id.layoutIngredientesSelecionados);
        chipGroupIngredientes          = findViewById(R.id.chipGroupIngredientes);
        btnAlterar                     = findViewById(R.id.btnAlterar);

        // Fix 3 + 4
        layoutSelecionarIngredientes = findViewById(R.id.layoutSelecionarIngredientes);
        btnGerarReceitaComItens      = findViewById(R.id.btnGerarReceitaComItens);

        // Botões da toolbar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSavedRecipes).setOnClickListener(v ->
                Toast.makeText(this, "Receitas salvas — em breve!", Toast.LENGTH_SHORT).show());
    }

    private void configurarBotoes() {
        btnNovaReceita   = findViewById(R.id.btnNovaReceita);
        btnSalvarReceita = findViewById(R.id.btnSalvarReceita);

        // ── Fix 4: botão principal "Gerar Receita" com itens selecionados ─
        if (btnGerarReceitaComItens != null) {
            btnGerarReceitaComItens.setOnClickListener(v -> {
                if (itensSelecionados != null && !itensSelecionados.isEmpty()) {
                    btnGerarReceitaComItens.setVisibility(View.GONE);
                    gerarReceitaComItens(itensSelecionados);
                }
            });
        }

        // ── Fix 3: botão "Ir para a Despensa" no estado selecionar ────────
        View btnIrParaSelecao = findViewById(R.id.btnIrParaSelecao);
        if (btnIrParaSelecao != null) {
            btnIrParaSelecao.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        // ── "Gerar Nova Receita" (botão dentro do scroll — pós geração) ───
        btnNovaReceita.setOnClickListener(v -> {
            if (itensSelecionados != null && !itensSelecionados.isEmpty()) {
                // usa os itens recebidos da Despensa
                gerarReceitaComItens(itensSelecionados);
            } else {
                // sem itens — orienta o usuário a voltar para a Despensa
                Toast.makeText(this,
                        "Selecione ingredientes na despensa primeiro",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // ── "Salvar Receita" ──────────────────────────────────────────────
        btnSalvarReceita.setOnClickListener(v -> {
            if (receitaAtual != null) {
                Toast.makeText(this,
                        "\"" + receitaAtual.getTitulo() + "\" salva nos favoritos!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Nenhuma receita para salvar. Gere uma primeiro.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // ── Sprint 8: "Alterar" — volta para a Despensa para nova seleção ─
        if (btnAlterar != null) {
            btnAlterar.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        // Sprint 8: botão no empty state da despensa vazia
        View btnIrParaDespensa = findViewById(R.id.btnIrParaDespensa);
        if (btnIrParaDespensa != null) {
            btnIrParaDespensa.setOnClickListener(v -> {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    // =========================================================================
    // Bottom Navigation
    // =========================================================================

    private void configurarBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_chef_ia);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chef_ia) {
                return true;
            } else if (id == R.id.nav_pantry) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoricoActivity.class));
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // Utilitários
    // =========================================================================

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String formatarQtd(double qtd) {
        return qtd == Math.floor(qtd) ? String.valueOf((int) qtd) : String.valueOf(qtd);
    }
}