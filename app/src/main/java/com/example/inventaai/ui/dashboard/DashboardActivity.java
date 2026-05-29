package com.example.inventaai.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.example.inventaai.util.GlideHelper;  // Sprint 3
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * DashboardActivity — tela principal do InventaAí.
 *
 * Sprint 1: integração do Navigation Drawer, SessionManager e filtragem
 * de dados por user_id. Redireciona ao Login se não há sessão ativa.
 *
 * Sprint 3: carregamento de avatares via GlideHelper (circular, com cache).
 */
public class DashboardActivity extends AppCompatActivity {

    // Views principais
    private DrawerLayout         drawerLayout;
    private NavigationView       navigationView;
    private RecyclerView         rvExpiringSoon;
    private RecyclerView         rvPantryItems;
    private TextView             tvEmpty;
    private TextView             tvGreetingUser;
    private FrameLayout          ivAvatar;
    private ImageView            ivAvatarImg;
    private TextView             tvAvatarIniciais;
    private MaterialButton       btnGenerateRecipe;
    private BottomNavigationView bottomNavigation;

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
        drawerLayout     = findViewById(R.id.drawerLayout);
        navigationView   = findViewById(R.id.navigationView);
        rvExpiringSoon   = findViewById(R.id.rvExpiringSoon);
        rvPantryItems    = findViewById(R.id.rvPantryItems);
        tvEmpty          = findViewById(R.id.tvEmpty);
        tvGreetingUser   = findViewById(R.id.tvGreetingUser);
        ivAvatar         = findViewById(R.id.ivAvatar);
        ivAvatarImg      = findViewById(R.id.ivAvatarImg);
        tvAvatarIniciais = findViewById(R.id.tvAvatarIniciais);
        btnGenerateRecipe = findViewById(R.id.btnGenerateRecipe);
        bottomNavigation  = findViewById(R.id.bottomNavigation);
    }

    private void configurarRecyclerViews() {
        rvExpiringSoon.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterExpiringSoon = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvExpiringSoon.setAdapter(adapterExpiringSoon);

        rvPantryItems.setLayoutManager(new LinearLayoutManager(this));
        adapterPantry = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvPantryItems.setAdapter(adapterPantry);
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
            } else if (id == R.id.nav_sair) {
                sessionManager.encerrarSessao();
                irParaLogin();
            }
            return true;
        });
    }

    /**
     * Atualiza nome e avatar no header do drawer e na toolbar.
     *
     * Sprint 3: substituída a leitura manual de URI por GlideHelper.loadCircularImage(),
     * que aproveita o cache do Glide para carregamento instantâneo em onResume.
     */
    private void atualizarHeaderDrawer() {
        User user = userRepository.getUserById(currentUserId);
        if (user == null) return;

        // Saudação
        tvGreetingUser.setText(user.getNome() + "!");

        // ── Avatar da toolbar (ivAvatarImg) ──────────────────────────────────
        if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
            // Sprint 3: Glide com circleCrop — cache automático, sem blink no onResume
            GlideHelper.loadCircularImage(this, user.getAvatarPath(), ivAvatarImg);
            ivAvatarImg.setColorFilter(null);
            tvAvatarIniciais.setVisibility(View.GONE);
        } else {
            // Sem foto: exibe iniciais
            tvAvatarIniciais.setText(user.getIniciais());
            tvAvatarIniciais.setVisibility(View.VISIBLE);
        }

        // ── Header do drawer ─────────────────────────────────────────────────
        View header = navigationView.getHeaderView(0);
        if (header == null) return;

        TextView  tvDrawerNome        = header.findViewById(R.id.tvDrawerNome);
        TextView  tvDrawerIdAbreviado = header.findViewById(R.id.tvDrawerIdAbreviado);
        TextView  tvDrawerIniciais    = header.findViewById(R.id.tvDrawerIniciais);
        ImageView ivDrawerAvatar      = header.findViewById(R.id.ivDrawerAvatar);

        tvDrawerNome.setText(user.getNome());
        tvDrawerIdAbreviado.setText("ID: " + user.getIdAbreviado());

        if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
            // Sprint 3: Glide circular no header do drawer
            GlideHelper.loadCircularImage(this, user.getAvatarPath(), ivDrawerAvatar);
            ivDrawerAvatar.setColorFilter(null);
            ivDrawerAvatar.setVisibility(View.VISIBLE);
            tvDrawerIniciais.setVisibility(View.GONE);
        } else {
            // Sem foto: exibe iniciais
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

        tvEmpty.setVisibility(todos.isEmpty() ? View.VISIBLE : View.GONE);
        rvPantryItems.setVisibility(todos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void abrirDetalhes(DespensaItem item) {
        Intent intent = new Intent(this, DetalhesActivity.class);
        intent.putExtra(DetalhesActivity.EXTRA_ITEM, item);
        startActivity(intent);
    }

    private void configurarBotoes() {
        btnGenerateRecipe.setOnClickListener(v ->
                startActivity(new Intent(this, ChefIAActivity.class)));
    }

    private void configurarBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_pantry);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_pantry)   return true;
            if (id == R.id.nav_add)      { startActivity(new Intent(this, CadastroActivity.class));  return true; }
            if (id == R.id.nav_history)  { startActivity(new Intent(this, HistoricoActivity.class)); return true; }
            if (id == R.id.nav_chef_ia)  { startActivity(new Intent(this, ChefIAActivity.class));    return true; }
            return false;
        });
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}