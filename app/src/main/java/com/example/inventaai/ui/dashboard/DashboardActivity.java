package com.example.inventaai.ui.dashboard;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.model.User;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.data.repository.UserRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.chefIA.ChefIAActivity;
import com.example.inventaai.ui.configuracoes.ConfiguracoesActivity;
import com.example.inventaai.ui.despensa.DespensaAdapter;
import com.example.inventaai.ui.detalhes.DetalhesActivity;
import com.example.inventaai.ui.receitas.ReceitasConcluidasActivity;
import com.example.inventaai.ui.receitas.ReceitasEmAndamentoActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.example.inventaai.ui.login.LoginActivity;
import com.example.inventaai.ui.perfil.PerfilActivity;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.example.inventaai.util.GlideHelper;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // ── Views principais ──────────────────────────────────────────────────────
    private DrawerLayout              drawerLayout;
    private NavigationView            navigationView;
    private RecyclerView              rvExpiringSoon;
    private RecyclerView              rvPantryItems;
    private LinearLayout              layoutEmptyPantry;
    private TextView                  tvGreetingUser;
    private FrameLayout               ivAvatar;
    private ImageView                 ivAvatarImg;
    private TextView                  tvAvatarIniciais;
    private MaterialButton            btnGenerateRecipe;
    private BottomNavigationView      bottomNavigation;
    private CircularProgressIndicator progressBar;
    private View                      cardSaudeDespensa;

    // Barra de modo de seleção
    private LinearLayout   layoutBarraSelecao;
    private TextView       tvContadorSelecao;
    private MaterialButton btnCancelarSelecao;
    private MaterialButton btnExcluirSelecionados; // Sprint 18

    // ── Card de Saúde dinâmico ──────────────────────────────────
    private TextView  tvSaudePercent;
    private TextView  tvSaudeLabel;
    private ImageView ivSaudeIcon;

    // ── Seção "Vencendo Logo" ocultável ─────────────────────────
    private LinearLayout layoutSectionVencendo;

    // ── Busca
    private LinearLayout      layoutSearchBar;
    private TextInputEditText etBusca;
    private View              btnBuscar;

    // ── Adapters ──────────────────────────────────────────────────────────────
    private DespensaAdapter adapterExpiringSoon;
    private DespensaAdapter adapterPantry;

    // ── Dependências ──────────────────────────────────────────────────────────
    private DespensaRepository despensaRepository;
    private UserRepository     userRepository;
    private SessionManager     sessionManager;
    private String             currentUserId;

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager     = new SessionManager(this);
        despensaRepository = new DespensaRepository(this);
        userRepository     = new UserRepository(this);

        currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            irParaLogin();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        vincularViews();
        configurarRecyclerViews();
        configurarBotoes();
        configurarBottomNavigation();
        configurarDrawer();
        configurarBusca();
        animarCardSaude();

        verificarMensagemDeRetorno();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUserId = sessionManager.getUserId();
        if (currentUserId == null) { irParaLogin(); return; }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_pantry);
        }

        if (etBusca != null && etBusca.getText() != null
                && !etBusca.getText().toString().trim().isEmpty()) {
            filtrarLista(etBusca.getText().toString().trim());
        } else {
            atualizarListas();
        }

        atualizarHeaderDrawer();
    }

    // =========================================================================
    // SPRINT 13: mensagem de retorno do ChefIA
    // =========================================================================

    private void verificarMensagemDeRetorno() {
        Intent intent = getIntent();
        if (intent != null
                && intent.getBooleanExtra(ChefIAActivity.EXTRA_MOSTRAR_MSG_SELECAO, false)) {
            Toast.makeText(this,
                    "Selecione ou adicione novos itens para gerar uma receita",
                    Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        drawerLayout      = findViewById(R.id.drawerLayout);
        navigationView    = findViewById(R.id.navigationView);
        rvExpiringSoon    = findViewById(R.id.rvExpiringSoon);
        rvPantryItems     = findViewById(R.id.rvPantryItems);
        layoutEmptyPantry = findViewById(R.id.layoutEmptyPantry);
        tvGreetingUser    = findViewById(R.id.tvGreetingUser);
        ivAvatar          = findViewById(R.id.ivAvatar);
        ivAvatarImg       = findViewById(R.id.ivAvatarImg);
        tvAvatarIniciais  = findViewById(R.id.tvAvatarIniciais);
        btnGenerateRecipe = findViewById(R.id.btnGenerateRecipe);
        bottomNavigation  = findViewById(R.id.bottomNavigation);
        cardSaudeDespensa = findViewById(R.id.cardPantryHealth);
        progressBar       = findViewById(R.id.progressBarDashboard);

        layoutBarraSelecao    = findViewById(R.id.layoutBarraSelecao);
        tvContadorSelecao     = findViewById(R.id.tvContadorSelecao);
        btnCancelarSelecao    = findViewById(R.id.btnCancelarSelecao);
        btnExcluirSelecionados = findViewById(R.id.btnExcluirSelecionados); // Sprint 18

        tvSaudePercent = findViewById(R.id.tvSaudePercent);
        tvSaudeLabel   = findViewById(R.id.tvSaudeLabel);
        ivSaudeIcon    = findViewById(R.id.ivSaudeIcon);

        layoutSectionVencendo = findViewById(R.id.layoutSectionVencendo);

        layoutSearchBar = findViewById(R.id.layoutSearchBar);
        etBusca         = findViewById(R.id.etBusca);
        btnBuscar       = findViewById(R.id.btnBuscar);

        ivAvatar.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class))
        );
    }

    private void configurarRecyclerViews() {
        rvExpiringSoon.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterExpiringSoon = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvExpiringSoon.setAdapter(adapterExpiringSoon);

        rvPantryItems.setLayoutManager(new GridLayoutManager(this, 2));
        adapterPantry = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvPantryItems.setAdapter(adapterPantry);

        adapterPantry.setOnItemLongClickListener(item -> {
            if (!adapterPantry.isModoSelecao()) {
                adapterPantry.setModoSelecao(true);
            }
            adapterPantry.selecionarItem(item.getId());
            atualizarBarraSelecao();
        });

        adapterPantry.setOnSelecaoChangedListener(total ->
                tvContadorSelecao.setText(total + " selecionado(s)")
        );
    }

    // =========================================================================
    // BUSCA E FILTRO
    // =========================================================================

    private void configurarBusca() {
        if (layoutSearchBar == null || etBusca == null || btnBuscar == null) return;

        btnBuscar.setOnClickListener(v -> {
            if (layoutSearchBar.getVisibility() == View.VISIBLE) {
                layoutSearchBar.animate()
                        .translationY(-layoutSearchBar.getHeight())
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> {
                            layoutSearchBar.setVisibility(View.GONE);
                            layoutSearchBar.setTranslationY(0f);
                            layoutSearchBar.setAlpha(1f);
                            etBusca.setText("");
                            atualizarListas();
                        })
                        .start();
            } else {
                layoutSearchBar.setTranslationY(-layoutSearchBar.getHeight() > 0
                        ? -layoutSearchBar.getHeight() : -120);
                layoutSearchBar.setAlpha(0f);
                layoutSearchBar.setVisibility(View.VISIBLE);
                layoutSearchBar.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(250)
                        .start();
                etBusca.requestFocus();
            }
        });

        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s != null ? s.toString().trim() : "";
                if (query.isEmpty()) {
                    atualizarListas();
                } else {
                    filtrarLista(query);
                }
            }
        });
    }

    private void filtrarLista(String query) {
        final String userId = currentUserId;
        AppExecutors.diskIO().execute(() -> {
            final List<DespensaItem> resultado =
                    despensaRepository.listarAtivosFiltrado(query, userId);

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                adapterPantry.atualizarLista(resultado);

                if (layoutSectionVencendo != null) {
                    layoutSectionVencendo.setVisibility(View.GONE);
                }

                if (resultado.isEmpty()) {
                    rvPantryItems.setVisibility(View.GONE);
                    layoutEmptyPantry.setVisibility(View.VISIBLE);
                } else {
                    rvPantryItems.setVisibility(View.VISIBLE);
                    layoutEmptyPantry.setVisibility(View.GONE);
                }
            });
        });
    }

    // =========================================================================
    // BARRA DE SELEÇÃO
    // =========================================================================

    private void atualizarBarraSelecao() {
        boolean modoAtivo = adapterPantry.isModoSelecao();
        int quantidade    = adapterPantry.getQuantidadeSelecionados();

        if (modoAtivo) {
            layoutBarraSelecao.setVisibility(View.VISIBLE);
            tvContadorSelecao.setText(quantidade + " selecionado(s)");
            btnGenerateRecipe.setText("Gerar Receita");
        } else {
            layoutBarraSelecao.setVisibility(View.GONE);
            btnGenerateRecipe.setText(getString(R.string.generate_recipe));
        }
    }

    // =========================================================================
    // CARREGAR DADOS
    // =========================================================================

    private void atualizarListas() {
        mostrarCarregando(true);
        final String userId = currentUserId;

        AppExecutors.diskIO().execute(() -> {
            final List<DespensaItem> todos     = despensaRepository.listarAtivos(userId);
            final List<DespensaItem> expirando =
                    despensaRepository.listarProximosVencimento(Constants.DIAS_ALERTA_AMARELO, userId);

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                adapterPantry.atualizarLista(todos);
                adapterExpiringSoon.atualizarLista(expirando);

                int percent = calcularSaudePercent(todos);
                atualizarCardSaude(percent);

                atualizarVisibilidadeSectionVencendo(expirando);

                if (todos.isEmpty()) {
                    rvPantryItems.setVisibility(View.GONE);
                    layoutEmptyPantry.setVisibility(View.VISIBLE);
                } else {
                    rvPantryItems.setVisibility(View.VISIBLE);
                    layoutEmptyPantry.setVisibility(View.GONE);
                }

                mostrarCarregando(false);
            });
        });
    }

    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null) {
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        }
        rvExpiringSoon.setVisibility(carregando ? View.INVISIBLE : View.VISIBLE);
        rvPantryItems.setVisibility( carregando ? View.INVISIBLE : View.VISIBLE);
    }

    // =========================================================================
    // SAÚDE DA DESPENSA DINÂMICA
    // =========================================================================

    private int calcularSaudePercent(List<DespensaItem> itens) {
        if (itens == null || itens.isEmpty()) return 100;

        int total     = itens.size();
        int saudaveis = 0;

        for (DespensaItem item : itens) {
            int dias = DateUtils.calcularDiasRestantes(item.getDataValidade());
            if (dias > Constants.DIAS_ALERTA_AMARELO) {
                saudaveis++;
            }
        }

        return (saudaveis * 100) / total;
    }

    private void atualizarCardSaude(int percent) {
        if (tvSaudePercent == null || tvSaudeLabel == null) return;

        final String label;
        final int    corPercent;
        final int    drawableIcon;

        if (percent >= 85) {
            label        = getString(R.string.saude_label_ideal);
            corPercent   = ContextCompat.getColor(this, R.color.colorPrimary);
            drawableIcon = R.drawable.ic_saude_sparkles;
        } else if (percent >= 60) {
            label        = getString(R.string.saude_label_bom);
            corPercent   = ContextCompat.getColor(this, R.color.colorSecondary);
            drawableIcon = R.drawable.ic_saude_alerta;
        } else {
            label        = getString(R.string.saude_label_atencao);
            corPercent   = ContextCompat.getColor(this, R.color.colorError);
            drawableIcon = R.drawable.ic_saude_perigo;
        }

        tvSaudeLabel.setText(label);
        tvSaudeLabel.setTextColor(corPercent);
        if (ivSaudeIcon != null) {
            ivSaudeIcon.setImageResource(drawableIcon);
            ivSaudeIcon.setColorFilter(corPercent);
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, percent);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int valor = (int) animation.getAnimatedValue();
            tvSaudePercent.setText(valor + "%");
            tvSaudePercent.setTextColor(corPercent);
        });
        animator.start();
    }

    // =========================================================================
    // VISIBILIDADE DA SEÇÃO "VENCENDO LOGO"
    // =========================================================================

    private void atualizarVisibilidadeSectionVencendo(List<DespensaItem> expirando) {
        if (layoutSectionVencendo == null) return;

        if (expirando.isEmpty()) {
            layoutSectionVencendo.setVisibility(View.GONE);
        } else {
            if (layoutSectionVencendo.getVisibility() != View.VISIBLE) {
                layoutSectionVencendo.setAlpha(0f);
                layoutSectionVencendo.setVisibility(View.VISIBLE);
                layoutSectionVencendo.animate()
                        .alpha(1f)
                        .setDuration(350)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            } else {
                layoutSectionVencendo.setVisibility(View.VISIBLE);
            }
        }
    }

    // =========================================================================
    // ANIMAÇÃO CARD DE SAÚDE
    // =========================================================================

    private void animarCardSaude() {
        if (cardSaudeDespensa == null) return;
        cardSaudeDespensa.setAlpha(0f);
        cardSaudeDespensa.setScaleX(0.85f);
        cardSaudeDespensa.setScaleY(0.85f);
        cardSaudeDespensa.postDelayed(() ->
                        cardSaudeDespensa.animate()
                                .alpha(1f).scaleX(1f).scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(new DecelerateInterpolator())
                                .start(),
                150);
    }

    // =========================================================================
    // NAVIGATION DRAWER — inclui nav_receitas_em_andamento
    // =========================================================================

    private void configurarDrawer() {
        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();

            if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            } else if (id == R.id.nav_receitas_salvas) {
                Intent intent = new Intent(this, ChefIAActivity.class);
                intent.putExtra("ABRIR_SALVAS", true);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                // Nova entrada no drawer
            } else if (id == R.id.nav_receitas_em_andamento) {
                startActivity(new Intent(this, ReceitasEmAndamentoActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            } else if (id == R.id.nav_receitas_concluidas) {
                startActivity(new Intent(this, ReceitasConcluidasActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            } else if (id == R.id.nav_configuracoes) {
                startActivity(new Intent(this, ConfiguracoesActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            } else if (id == R.id.nav_sair) {
                sessionManager.encerrarSessao();
                irParaLogin();
            }
            return true;
        });
    }

    private void atualizarHeaderDrawer() {
        final String userId = currentUserId;
        AppExecutors.diskIO().execute(() -> {
            final User user = userRepository.getUserById(userId);
            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed() || user == null) return;

                tvGreetingUser.setText(user.getNome() + "!");

                if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
                    GlideHelper.loadCircularImage(this, user.getAvatarPath(), ivAvatarImg);
                    ivAvatarImg.setColorFilter(null);
                    tvAvatarIniciais.setVisibility(View.GONE);
                } else {
                    tvAvatarIniciais.setText(user.getIniciais());
                    tvAvatarIniciais.setVisibility(View.VISIBLE);
                }

                View header = navigationView.getHeaderView(0);
                if (header == null) return;

                TextView  tvDrawerNome        = header.findViewById(R.id.tvDrawerNome);
                TextView  tvDrawerIdAbreviado = header.findViewById(R.id.tvDrawerIdAbreviado);
                TextView  tvDrawerIniciais    = header.findViewById(R.id.tvDrawerIniciais);
                ImageView ivDrawerAvatar      = header.findViewById(R.id.ivDrawerAvatar);

                tvDrawerNome.setText(user.getNome());
                tvDrawerIdAbreviado.setText("ID: " + user.getIdAbreviado());

                if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
                    GlideHelper.loadCircularImage(this, user.getAvatarPath(), ivDrawerAvatar);
                    ivDrawerAvatar.setColorFilter(null);
                    ivDrawerAvatar.setVisibility(View.VISIBLE);
                    tvDrawerIniciais.setVisibility(View.GONE);
                } else {
                    ivDrawerAvatar.setVisibility(View.GONE);
                    tvDrawerIniciais.setText(user.getIniciais());
                    tvDrawerIniciais.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void abrirDetalhes(DespensaItem item) {
        if (adapterPantry.isModoSelecao()) {
            atualizarBarraSelecao();
            return;
        }
        Intent intent = new Intent(this, DetalhesActivity.class);
        intent.putExtra(DetalhesActivity.EXTRA_ITEM, item);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void abrirDetalhesComSharedElement(DespensaItem item, View sharedView) {
        Intent intent = new Intent(this, DetalhesActivity.class);
        intent.putExtra(DetalhesActivity.EXTRA_ITEM, item);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, new Pair<>(sharedView, sharedView.getTransitionName()));
        startActivity(intent, options.toBundle());
    }

    private void configurarBotoes() {
        btnGenerateRecipe.setOnClickListener(v -> {
            if (adapterPantry.isModoSelecao()) {
                List<DespensaItem> selecionados = adapterPantry.getItensSelecionados();
                if (selecionados.isEmpty()) {
                    Toast.makeText(this,
                            "Selecione pelo menos 1 item para gerar a receita",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, ChefIAActivity.class);
                intent.putExtra(
                        ChefIAActivity.EXTRA_ITENS_SELECIONADOS,
                        new ArrayList<>(selecionados));
                adapterPantry.limparSelecao();
                atualizarBarraSelecao();
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                final String userId = currentUserId;
                AppExecutors.diskIO().execute(() -> {
                    final List<DespensaItem> ativos = despensaRepository.listarAtivos(userId);
                    AppExecutors.mainThread().execute(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (ativos.isEmpty()) {
                            startActivity(new Intent(this, CadastroActivity.class));
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } else {
                            Toast.makeText(this,
                                    "Selecione pelo menos 1 item para gerar a receita",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        btnCancelarSelecao.setOnClickListener(v -> {
            adapterPantry.limparSelecao();
            atualizarBarraSelecao();
        });

        // Sprint 18: excluir selecionados → move para histórico como DESCARTADO
        if (btnExcluirSelecionados != null) {
            btnExcluirSelecionados.setOnClickListener(v -> confirmarExclusaoEmLote());
        }
    }

    private void configurarBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_pantry) {
                return true;
            }
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            if (id == R.id.nav_history) {
                Intent intent = new Intent(this, HistoricoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            if (id == R.id.nav_chef_ia) {
                Intent intent = new Intent(this, ChefIAActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // SPRINT 18: EXCLUSÃO EM LOTE COM HISTÓRICO
    // =========================================================================

    private void confirmarExclusaoEmLote() {
        List<DespensaItem> selecionados = adapterPantry.getItensSelecionados();
        if (selecionados.isEmpty()) return;

        int qtd = selecionados.size();
        String msg = qtd == 1
                ? "Descartar \"" + selecionados.get(0).getNome() + "\"?"
                : "Descartar " + qtd + " itens selecionados?";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Descartar itens")
                .setMessage(msg + "\n\nEles serão movidos para o histórico como descartados.")
                .setPositiveButton("Descartar", (dialog, which) -> executarExclusaoEmLote(selecionados))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarExclusaoEmLote(List<DespensaItem> itens) {
        final String userId = currentUserId;
        AppExecutors.diskIO().execute(() -> {
            int totalDescartados = 0;
            for (DespensaItem item : itens) {
                boolean ok = despensaRepository.moverParaHistorico(
                        item.getId(),
                        item.getNome(),
                        Constants.STATUS_DESCARTADO,
                        userId
                );
                if (ok) totalDescartados++;
            }
            final int total = totalDescartados;
            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                adapterPantry.limparSelecao();
                atualizarBarraSelecao();
                atualizarListas();
                String feedback = total == 1
                        ? "1 item descartado."
                        : total + " itens descartados.";
                Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else if (layoutSearchBar != null
                && layoutSearchBar.getVisibility() == View.VISIBLE) {
            btnBuscar.performClick();
        } else if (adapterPantry.isModoSelecao()) {
            adapterPantry.limparSelecao();
            atualizarBarraSelecao();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}