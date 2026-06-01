package com.example.inventaai.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
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
import com.example.inventaai.ui.despensa.DespensaAdapter;
import com.example.inventaai.ui.detalhes.DetalhesActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.example.inventaai.ui.login.LoginActivity;
import com.example.inventaai.ui.perfil.PerfilActivity;
import com.example.inventaai.util.GlideHelper;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // Views principais
    private DrawerLayout         drawerLayout;
    private NavigationView       navigationView;
    private RecyclerView         rvExpiringSoon;
    private RecyclerView         rvPantryItems;

    // Empty state ilustrado (substitui tvEmpty)
    private LinearLayout         layoutEmptyPantry;

    private TextView             tvGreetingUser;
    private FrameLayout          ivAvatar;
    private ImageView            ivAvatarImg;
    private TextView             tvAvatarIniciais;
    private MaterialButton       btnGenerateRecipe;
    private BottomNavigationView bottomNavigation;

    // Card de Saúde da Despensa para animação de entrada
    private View                 cardSaudeDespensa;

    // Adapters
    private DespensaAdapter adapterExpiringSoon;
    private DespensaAdapter adapterPantry;

    // Dependências
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

        // Verifica sessão — sem usuário logado vai para Login
        currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            irParaLogin();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vincularViews();
        configurarRecyclerViews();
        configurarBotoes();
        configurarBottomNavigation();
        configurarDrawer();

        // Sprint 6: anima o card de Saúde da Despensa na entrada da tela
        animarCardSaude();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUserId = sessionManager.getUserId();
        if (currentUserId == null) { irParaLogin(); return; }
        atualizarListas();
        atualizarHeaderDrawer();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        drawerLayout      = findViewById(R.id.drawerLayout);
        navigationView    = findViewById(R.id.navigationView);
        rvExpiringSoon    = findViewById(R.id.rvExpiringSoon);
        rvPantryItems     = findViewById(R.id.rvPantryItems);

        // Sprint 5: empty state ilustrado
        layoutEmptyPantry = findViewById(R.id.layoutEmptyPantry);

        tvGreetingUser    = findViewById(R.id.tvGreetingUser);
        ivAvatar          = findViewById(R.id.ivAvatar);
        ivAvatarImg       = findViewById(R.id.ivAvatarImg);
        tvAvatarIniciais  = findViewById(R.id.tvAvatarIniciais);
        btnGenerateRecipe = findViewById(R.id.btnGenerateRecipe);
        bottomNavigation  = findViewById(R.id.bottomNavigation);

        // Sprint 6: card de saúde (use o ID real do card no seu layout)
        cardSaudeDespensa = findViewById(R.id.cardPantryHealth);
    }

    private void configurarRecyclerViews() {
        rvExpiringSoon.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterExpiringSoon = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvExpiringSoon.setAdapter(adapterExpiringSoon);

        rvPantryItems.setLayoutManager(new GridLayoutManager(this, 2));
        adapterPantry = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvPantryItems.setAdapter(adapterPantry);
    }

    // =========================================================================
    // SPRINT 6: ANIMAÇÃO DO CARD DE SAÚDE DA DESPENSA
    // =========================================================================

    private void animarCardSaude() {
        if (cardSaudeDespensa == null) return;

        // Estado inicial: invisível e levemente menor
        cardSaudeDespensa.setAlpha(0f);
        cardSaudeDespensa.setScaleX(0.85f);
        cardSaudeDespensa.setScaleY(0.85f);

        // Pequeno delay para que o layout já esteja completo
        cardSaudeDespensa.postDelayed(() ->
                        cardSaudeDespensa.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start(),
                150);
    }

    // =========================================================================
    // NAVIGATION DRAWER
    // =========================================================================

    private void configurarDrawer() {
        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        ivAvatar.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();

            if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (id == R.id.nav_sair) {
                sessionManager.encerrarSessao();
                irParaLogin();
            }
            return true;
        });
    }

    private void atualizarHeaderDrawer() {
        User user = userRepository.getUserById(currentUserId);
        if (user == null) return;

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
    }

    // =========================================================================
    // CARREGAR / ATUALIZAR DADOS
    // =========================================================================

    private void atualizarListas() {
        List<DespensaItem> todos = despensaRepository.listarAtivos(currentUserId);
        adapterPantry.atualizarLista(todos);

        List<DespensaItem> expirando = despensaRepository.listarProximosVencimento(7, currentUserId);
        adapterExpiringSoon.atualizarLista(expirando);

        // Controle do empty state ilustrado
        if (todos.isEmpty()) {
            rvPantryItems.setVisibility(View.GONE);
            layoutEmptyPantry.setVisibility(View.VISIBLE);
        } else {
            rvPantryItems.setVisibility(View.VISIBLE);
            layoutEmptyPantry.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // NAVEGAÇÃO — Sprint 6: transições em todas as saídas
    // =========================================================================

    private void abrirDetalhes(DespensaItem item) {
        Intent intent = new Intent(this, DetalhesActivity.class);
        intent.putExtra(DetalhesActivity.EXTRA_ITEM, item);
        startActivity(intent);
        // Sprint 6: transição padrão ao abrir detalhes
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Sprint 6: versão com Shared Element Transition.
     * Chame este método quando tiver a View do ícone do card clicado.
     *
     * @param item      DespensaItem selecionado
     * @param sharedView View do elemento compartilhado (ex: ivItemIcon no card)
     */
    public void abrirDetalhesComSharedElement(DespensaItem item, View sharedView) {
        Intent intent = new Intent(this, DetalhesActivity.class);
        intent.putExtra(DetalhesActivity.EXTRA_ITEM, item);

        // O transitionName deve ser definido no XML do card e no layout de Detalhes
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                new Pair<>(sharedView, sharedView.getTransitionName())
        );

        startActivity(intent, options.toBundle());
        // Nota: não chame overridePendingTransition com shared elements —
        // o sistema cuida da animação automaticamente.
    }

    private void configurarBotoes() {
        btnGenerateRecipe.setOnClickListener(v -> {
            startActivity(new Intent(this, ChefIAActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void configurarBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_pantry);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pantry)  return true;
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoricoActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            if (id == R.id.nav_chef_ia) {
                startActivity(new Intent(this, ChefIAActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
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
        } else {
            super.onBackPressed();
            // Sprint 6: animação ao voltar (slide para a direita = sentido "voltar")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}