package com.example.inventaai.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.chefIA.ChefIAActivity;
import com.example.inventaai.ui.despensa.DespensaAdapter;
import com.example.inventaai.ui.detalhes.DetalhesActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * DashboardActivity — tela principal do InventaAí.
 *
 * Sprint 3: conectada ao DespensaRepository.
 * - rvExpiringSoon → itens que vencem nos próximos 7 dias (horizontal)
 * - rvPantryItems  → todos os itens ativos (vertical)
 * - onResume() recarrega os dados automaticamente ao voltar de outras telas
 */
public class DashboardActivity extends AppCompatActivity {

    // Views
    private RecyclerView          rvExpiringSoon;
    private RecyclerView          rvPantryItems;
    private TextView              tvEmpty;
    private MaterialButton        btnGenerateRecipe;
    private BottomNavigationView  bottomNavigation;

    // Adapters
    private DespensaAdapter adapterExpiringSoon;
    private DespensaAdapter adapterPantry;

    // Repositório
    private DespensaRepository despensaRepository;

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        despensaRepository = new DespensaRepository(this);

        vincularViews();
        configurarRecyclerViews();
        configurarBotoes();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sempre que a tela volta ao foco, recarrega os dados do banco
        atualizarListas();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvExpiringSoon    = findViewById(R.id.rvExpiringSoon);
        rvPantryItems     = findViewById(R.id.rvPantryItems);
        tvEmpty           = findViewById(R.id.tvEmpty);
        btnGenerateRecipe = findViewById(R.id.btnGenerateRecipe);
        bottomNavigation  = findViewById(R.id.bottomNavigation);
    }

    private void configurarRecyclerViews() {
        // RecyclerView horizontal — vencendo em breve
        rvExpiringSoon.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterExpiringSoon = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvExpiringSoon.setAdapter(adapterExpiringSoon);

        // RecyclerView vertical — lista principal
        rvPantryItems.setLayoutManager(new LinearLayoutManager(this));
        adapterPantry = new DespensaAdapter(new ArrayList<>(), this::abrirDetalhes);
        rvPantryItems.setAdapter(adapterPantry);
    }

    // =========================================================================
    // CARREGAR / ATUALIZAR DADOS
    // =========================================================================

    private void atualizarListas() {
        // Lista completa de itens ativos (ordenada por validade)
        List<DespensaItem> todos = despensaRepository.listarAtivos();
        adapterPantry.atualizarLista(todos);

        // Lista de próximos a vencer (janela de 7 dias)
        List<DespensaItem> expirando = despensaRepository.listarProximosVencimento(7);
        adapterExpiringSoon.atualizarLista(expirando);

        // Visibilidade do estado vazio
        if (todos.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvPantryItems.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPantryItems.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    /**
     * Abre a tela de detalhes passando o objeto DespensaItem serializado.
     */
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

            if (id == R.id.nav_pantry) {
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoricoActivity.class));
                return true;
            } else if (id == R.id.nav_chef_ia) {
                startActivity(new Intent(this, ChefIAActivity.class));
                return true;
            }

            return false;
        });
    }
}