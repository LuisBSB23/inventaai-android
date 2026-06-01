package com.example.inventaai.ui.historico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
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

    private RecyclerView         rvHistorico;
    private LinearLayout         layoutEmptyHistory;
    private BottomNavigationView bottomNavigation;

    // SPRINT 7 — TAREFA 2: indicador de carregamento
    private CircularProgressIndicator progressBar;

    private HistoricoRepository  historicoRepository;
    private HistoricoAdapter     historicoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        historicoRepository = new HistoricoRepository(this);

        vincularViews();
        configurarRecyclerView();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarHistorico();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvHistorico        = findViewById(R.id.rvHistorico);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);
        bottomNavigation   = findViewById(R.id.bottomNavigation);

        // SPRINT 7 — TAREFA 2: indicador de carregamento
        // O layout precisa ter: <com.google.android.material.progressindicator.CircularProgressIndicator
        //     android:id="@+id/progressBarHistorico" ... android:visibility="gone" />
        progressBar = findViewById(R.id.progressBarHistorico);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void configurarRecyclerView() {
        rvHistorico.setLayoutManager(new LinearLayoutManager(this));
        historicoAdapter = new HistoricoAdapter(new ArrayList<>());
        rvHistorico.setAdapter(historicoAdapter);
    }

    // =========================================================================
    // SPRINT 7 — TAREFA 2: Carregar histórico no background thread
    // =========================================================================

    private void carregarHistorico() {
        mostrarCarregando(true);

        // Captura userId antes de entrar no background thread
        final String userId = new SessionManager(this).getUserId();

        AppExecutors.diskIO().execute(() -> {
            // ── Fora da UI thread: query no banco ──────────────────────────
            final List<HistoricoItem> itens = historicoRepository.listarTodos(userId);

            // ── De volta na UI thread: atualizar views ─────────────────────
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

    /** Exibe ou oculta o CircularProgressIndicator e a lista. */
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
        bottomNavigation.setSelectedItemId(R.id.nav_history);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                return true;
            } else if (id == R.id.nav_pantry) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                return true;
            } else if (id == R.id.nav_chef_ia) {
                startActivity(new Intent(this, ChefIAActivity.class));
                return true;
            }

            return false;
        });
    }
}