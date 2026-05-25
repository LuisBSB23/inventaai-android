package com.example.inventaai.ui.chefIA;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.model.ReceitaResponse;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * ChefIAActivity — gera receitas personalizadas usando a API do Gemini (Sprint 4).
 *
 * Fluxo:
 *  1. onCreate  → busca itens da despensa (próximos ao vencimento primeiro, depois demais)
 *  2. Exibe ProgressBar e chama GeminiService.gerarReceita()
 *  3. onSucesso → preenche título, metadados, grid de ingredientes e lista de passos
 *  4. onErro    → exibe mensagem amigável e reabilita o botão "Gerar Nova Receita"
 *  5. btnNovaReceita → repete o fluxo com os dados mais recentes da despensa
 */
public class ChefIAActivity extends AppCompatActivity {

    // ──────────────────────────────────────────────────────────────────────────
    // Views
    // ──────────────────────────────────────────────────────────────────────────

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
    private ProgressBar          progressBar;       // estado de carregamento
    private View                 viewConteudo;      // agrupa título + metadados + listas

    // ──────────────────────────────────────────────────────────────────────────
    // Dependências
    // ──────────────────────────────────────────────────────────────────────────

    private DespensaRepository despensaRepository;
    private GeminiService      geminiService;

    // Receita atual em memória (para o botão Salvar)
    private ReceitaResponse receitaAtual;

    // ──────────────────────────────────────────────────────────────────────────
    // Ciclo de vida
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chef_ia);

        despensaRepository = new DespensaRepository(this);
        geminiService      = new GeminiService();

        vincularViews();
        configurarBotoes();
        configurarBottomNavigation();

        // Gera a receita automaticamente ao abrir a tela
        gerarReceita();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inicialização de views
    // ──────────────────────────────────────────────────────────────────────────

    private void vincularViews() {
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

        // ProgressBar e contêiner de conteúdo são adicionados via lógica de estado —
        // se não existirem no layout atual, os métodos de estado tratam o null com segurança.
        progressBar  = findViewById(R.id.progressBarChef);
        viewConteudo = findViewById(R.id.scrollViewConteudo);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lógica principal: gerar receita
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Monta a lista de ingredientes priorizando os mais próximos do vencimento
     * (janela de 7 dias) e completa com os demais itens ativos.
     * Em seguida, chama o GeminiService.
     */
    private void gerarReceita() {
        // Prioridade: próximos ao vencimento
        List<DespensaItem> proximosVencer = despensaRepository.listarProximosVencimento(7);
        List<DespensaItem> todosAtivos    = despensaRepository.listarAtivos();

        // Combina sem duplicatas
        List<DespensaItem> itensParaReceita = new ArrayList<>(proximosVencer);
        for (DespensaItem item : todosAtivos) {
            if (!contemId(itensParaReceita, item.getId())) {
                itensParaReceita.add(item);
            }
        }

        if (itensParaReceita.isEmpty()) {
            mostrarErro(getString(R.string.pantry_empty_for_recipe));
            return;
        }

        mostrarCarregando(true);

        geminiService.gerarReceita(itensParaReceita, new GeminiService.ReceitaCallback() {
            @Override
            public void onSucesso(ReceitaResponse receita) {
                runOnUiThread(() -> {
                    receitaAtual = receita;
                    preencherReceita(receita, itensParaReceita);
                    mostrarCarregando(false);
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

    // ──────────────────────────────────────────────────────────────────────────
    // Preencher UI com a receita recebida
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Popula todos os campos visuais com os dados retornados pelo Gemini.
     *
     * @param receita  Objeto ReceitaResponse mapeado pelo Gson.
     * @param itensUsados Lista de itens enviados ao prompt (fallback para ingredientes).
     */
    private void preencherReceita(ReceitaResponse receita, List<DespensaItem> itensUsados) {
        // ── Cabeçalho ──────────────────────────────────────────────────────
        tvRecipeTitle.setText(receita.getTitulo());

        // A descrição não vem da IA; usamos um texto motivacional genérico
        tvRecipeDescription.setText(
                "Uma receita criada especialmente para os ingredientes da sua despensa. " +
                        "Aproveite ao máximo o que você já tem!");

        // ── Metadados ──────────────────────────────────────────────────────
        tvTime.setText(receita.getTempoPreparo());
        tvServings.setText(receita.getPorcoes());
        tvDifficulty.setText(receita.getDificuldade());

        // ── Grid de ingredientes ───────────────────────────────────────────
        gridIngredientes.removeAllViews();

        List<String> ingredientes = receita.getIngredientes();
        if (ingredientes == null || ingredientes.isEmpty()) {
            // Fallback: usa os nomes dos itens da despensa
            for (DespensaItem item : itensUsados) {
                adicionarCartaoIngrediente(item.getNome(),
                        formatarQtd(item.getQuantidade()) + " " + item.getUnidadeMedida());
            }
        } else {
            for (String ingrediente : ingredientes) {
                // Tenta separar "Nome - quantidade" ou usa o texto inteiro como nome
                String[] partes = ingrediente.split(" - ", 2);
                String nome = partes[0].trim();
                String qtd  = partes.length > 1 ? partes[1].trim() : "";
                adicionarCartaoIngrediente(nome, qtd);
            }
        }

        // ── Passos ─────────────────────────────────────────────────────────
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

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers de UI — ingrediente e passo
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Adiciona dinamicamente um card de ingrediente ao GridLayout de 2 colunas.
     */
    private void adicionarCartaoIngrediente(String nome, String quantidade) {
        // Card externo
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

        // Container interno horizontal
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = dpToPx(12);
        row.setPadding(pad, pad, pad, pad);

        // Coluna de texto (nome + quantidade)
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

    /**
     * Adiciona dinamicamente um passo (número + texto) ao LinearLayout de passos.
     */
    private void adicionarPasso(int numero, String descricao) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(20));
        row.setLayoutParams(rowParams);

        // Círculo numerado
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

        // Texto do passo
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

    // ──────────────────────────────────────────────────────────────────────────
    // Gerenciamento de estado de UI
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Alterna entre o estado de carregamento (ProgressBar visível, botões desabilitados)
     * e o estado normal (conteúdo visível, botões habilitados).
     */
    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null) {
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        }
        if (viewConteudo != null) {
            viewConteudo.setVisibility(carregando ? View.GONE : View.VISIBLE);
        }
        btnNovaReceita.setEnabled(!carregando);
        btnSalvarReceita.setEnabled(!carregando);

        if (carregando) {
            tvRecipeTitle.setText(R.string.generating_recipe);
            tvRecipeDescription.setText("");
        }
    }

    /**
     * Exibe uma mensagem de erro via Toast e restaura o estado da tela.
     */
    private void mostrarErro(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
        tvRecipeTitle.setText(R.string.recipe_title_placeholder);
        tvRecipeDescription.setText(mensagem);
        btnNovaReceita.setEnabled(true);
        btnSalvarReceita.setEnabled(false);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Eventos de botão
    // ──────────────────────────────────────────────────────────────────────────

    private void configurarBotoes() {
        btnNovaReceita.setOnClickListener(v -> gerarReceita());

        btnSalvarReceita.setOnClickListener(v -> {
            if (receitaAtual != null) {
                // Sprint 5 (futuro): persistir receita no banco de dados local
                Toast.makeText(this,
                        "\"" + receitaAtual.getTitulo() + "\" salva nos favoritos!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Nenhuma receita para salvar. Gere uma primeiro.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Navegação inferior
    // ──────────────────────────────────────────────────────────────────────────

    private void configurarBottomNavigation() {
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

    // ──────────────────────────────────────────────────────────────────────────
    // Utilitários
    // ──────────────────────────────────────────────────────────────────────────

    /** Verifica se uma lista já contém um item com determinado id. */
    private boolean contemId(List<DespensaItem> lista, long id) {
        for (DespensaItem i : lista) {
            if (i.getId() == id) return true;
        }
        return false;
    }

    /** Converte dp em pixels para uso programático em LayoutParams. */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /** Formata quantidade sem casas decimais desnecessárias. */
    private String formatarQtd(double qtd) {
        return qtd == Math.floor(qtd) ? String.valueOf((int) qtd) : String.valueOf(qtd);
    }
}