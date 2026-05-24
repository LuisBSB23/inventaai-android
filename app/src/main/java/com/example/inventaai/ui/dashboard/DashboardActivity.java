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
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * DashboardActivity — tela principal do InventaAí.
 *
 * Sprint 2: UI estática com navegação entre telas.
 * Sprint 3: conectar os RecyclerViews ao DespensaRepository.
 */
public class DashboardActivity extends AppCompatActivity {

    // Views
    private RecyclerView rvExpiringSoon;
    private RecyclerView rvPantryItems;
    private TextView tvEmpty;
    private MaterialButton btnGenerateRecipe;
    private BottomNavigationView bottomNavigation;

    // Repositório (pronto para Sprint 3)
    private DespensaRepository despensaRepository;

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

        inicializarRepositorio();
        vincularViews();
        configurarRecyclerViews();
        configurarBotoes();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sprint 3: recarregar dados ao voltar de outra tela
        // carregarDados();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void inicializarRepositorio() {
        despensaRepository = new DespensaRepository(this);
    }

    private void vincularViews() {
        rvExpiringSoon    = findViewById(R.id.rvExpiringSoon);
        rvPantryItems     = findViewById(R.id.rvPantryItems);
        tvEmpty           = findViewById(R.id.tvEmpty);
        btnGenerateRecipe = findViewById(R.id.btnGenerateRecipe);
        bottomNavigation  = findViewById(R.id.bottomNavigation);
    }

    private void configurarRecyclerViews() {
        // RecyclerView horizontal (vencendo logo)
        rvExpiringSoon.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // RecyclerView vertical (lista principal)
        rvPantryItems.setLayoutManager(new LinearLayoutManager(this));

        // Sprint 3: atribuir adapters reais e carregar dados do repositório
        // Exemplo:
        //   List<DespensaItem> itens = despensaRepository.listarTodos();
        //   DespensaAdapter adapter = new DespensaAdapter(itens, item -> abrirDetalhes(item));
        //   rvPantryItems.setAdapter(adapter);
        //   tvEmpty.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // EVENTOS
    // =========================================================================

    private void configurarBotoes() {
        // FAB "Gerar Receita" → abre ChefIAActivity
        btnGenerateRecipe.setOnClickListener(v ->
                startActivity(new Intent(this, ChefIAActivity.class)));
    }

    private void configurarBottomNavigation() {
        // Marca a aba Despensa como selecionada nesta tela
        bottomNavigation.setSelectedItemId(R.id.nav_pantry);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_pantry) {
                // Já estamos aqui; apenas consome o evento
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
