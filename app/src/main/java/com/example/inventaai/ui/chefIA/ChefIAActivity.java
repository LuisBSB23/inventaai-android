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
import com.example.inventaai.data.remote.UnsplashService;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.GlideHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * ChefIAActivity — gera receitas personalizadas usando a API do Gemini.
 *
 * Sprint 3: integração com Unsplash API + Glide para imagem do prato.
 * Sprint 4: btnBack (voltar) e btnSavedRecipes (receitas salvas) na toolbar.
 *           Ícones padronizados — zero @android:drawable.
 */
public class ChefIAActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_TAG;

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
    private ProgressBar          progressBar;
    private View                 viewConteudo;

    // Sprint 3: hero image da receita
    private ImageView ivRecipeImage;

    // ──────────────────────────────────────────────────────────────────────────
    // Dependências
    // ──────────────────────────────────────────────────────────────────────────

    private DespensaRepository despensaRepository;
    private GeminiService      geminiService;
    private UnsplashService    unsplashService;

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
        unsplashService    = new UnsplashService();

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
        progressBar         = findViewById(R.id.progressBarChef);
        viewConteudo        = findViewById(R.id.scrollViewConteudo);

        // Sprint 3: hero image
        ivRecipeImage = findViewById(R.id.ivRecipeImage);

        // Sprint 4: botões da toolbar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // btnSavedRecipes — placeholder para tela de receitas salvas (sprint futura)
        findViewById(R.id.btnSavedRecipes).setOnClickListener(v ->
                Toast.makeText(this, "Receitas salvas — em breve!", Toast.LENGTH_SHORT).show()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lógica principal: gerar receita
    // ──────────────────────────────────────────────────────────────────────────

    private void gerarReceita() {
        com.example.inventaai.util.SessionManager smChef =
                new com.example.inventaai.util.SessionManager(this);
        String chefUserId = smChef.getUserId();

        List<DespensaItem> proximosVencer =
                despensaRepository.listarProximosVencimento(7, chefUserId);
        List<DespensaItem> todosAtivos =
                despensaRepository.listarAtivos(chefUserId);

        // Combina sem duplicatas, priorizando os próximos do vencimento
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
                    buscarImagemParaReceita(receita.getTitulo());
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
    // Sprint 3: buscar e exibir imagem da receita via Unsplash
    // ──────────────────────────────────────────────────────────────────────────

    private void buscarImagemParaReceita(String tituloReceita) {
        if (ivRecipeImage == null) return;

        Log.d(TAG, "ChefIA: buscando imagem para \"" + tituloReceita + "\"");

        unsplashService.buscarImagemReceita(tituloReceita, new UnsplashService.ImageCallback() {
            @Override
            public void onSucesso(String imageUrl) {
                runOnUiThread(() -> {
                    Log.d(TAG, "ChefIA: carregando imagem com Glide → " + imageUrl);
                    GlideHelper.loadImage(ChefIAActivity.this, imageUrl, ivRecipeImage);
                });
            }

            @Override
            public void onErro(String mensagem) {
                Log.w(TAG, "ChefIA: imagem não carregada → " + mensagem);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Preencher UI com a receita recebida
    // ──────────────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers de UI
    // ──────────────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────────────
    // Estado de UI
    // ──────────────────────────────────────────────────────────────────────────

    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null)
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        if (viewConteudo != null)
            viewConteudo.setVisibility(carregando ? View.GONE : View.VISIBLE);
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

    // ──────────────────────────────────────────────────────────────────────────
    // Eventos de botão
    // ──────────────────────────────────────────────────────────────────────────

    private void configurarBotoes() {
        btnNovaReceita.setOnClickListener(v -> gerarReceita());

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

    private boolean contemId(List<DespensaItem> lista, long id) {
        for (DespensaItem i : lista) {
            if (i.getId() == id) return true;
        }
        return false;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String formatarQtd(double qtd) {
        return qtd == Math.floor(qtd) ? String.valueOf((int) qtd) : String.valueOf(qtd);
    }
}