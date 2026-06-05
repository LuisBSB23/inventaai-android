package com.example.inventaai.ui.historico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.HistoricoItem;
import com.example.inventaai.data.repository.HistoricoRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.chefIA.ChefIAActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class HistoricoActivity extends AppCompatActivity {

    private RecyclerView              rvHistorico;
    private LinearLayout              layoutEmptyHistory;
    private BottomNavigationView      bottomNavigation;
    private CircularProgressIndicator progressBar;

    private HistoricoRepository historicoRepository;
    private HistoricoAdapter    historicoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        historicoRepository = new HistoricoRepository(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            return insets;
        });

        vincularViews();
        configurarRecyclerView();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tarefa 1: garante que o indicador correto fique sempre
        // selecionado ao voltar para esta tela (ex: após pressionar Voltar).
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_history);
        }
        carregarHistorico();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvHistorico        = findViewById(R.id.rvHistorico);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);
        bottomNavigation   = findViewById(R.id.bottomNavigation);
        progressBar        = findViewById(R.id.progressBarHistorico);

        // Tarefa 3: btnBack aciona finish() com animação de "voltar".
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void configurarRecyclerView() {
        rvHistorico.setLayoutManager(new LinearLayoutManager(this));
        historicoAdapter = new HistoricoAdapter(new ArrayList<>());
        rvHistorico.setAdapter(historicoAdapter);
    }

    // =========================================================================
    // CARREGAR HISTÓRICO (background thread)
    // =========================================================================

    private void carregarHistorico() {
        mostrarCarregando(true);

        final String userId = new SessionManager(this).getUserId();

        AppExecutors.diskIO().execute(() -> {
            final List<HistoricoItem> itens = historicoRepository.listarTodos(userId);

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                historicoAdapter.atualizarLista(itens);

                if (itens.isEmpty()) {
                    rvHistorico.setVisibility(View.GONE);
                    layoutEmptyHistory.setVisibility(View.VISIBLE);
                } else {
                    rvHistorico.setVisibility(View.VISIBLE);
                    layoutEmptyHistory.setVisibility(View.GONE);
                }

                mostrarCarregando(false);
            });
        });
    }

    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null) {
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        }
        rvHistorico.setVisibility(carregando ? View.INVISIBLE : View.VISIBLE);
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void configurarBottomNavigation() {
        // Tarefa 1: setSelectedItemId movido para onResume().
        // Mantemos aqui apenas o listener de cliques.

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                // Já estamos aqui — não faz nada
                return true;
            } else if (id == R.id.nav_pantry) {
                // Tarefa 2: CLEAR_TOP para o Dashboard (home).
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_chef_ia) {
                // Tarefa 2: reutiliza instância existente.
                Intent intent = new Intent(this, ChefIAActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }

            return false;
        });
    }
}