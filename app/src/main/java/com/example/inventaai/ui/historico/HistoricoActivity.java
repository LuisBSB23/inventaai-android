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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HistoricoActivity extends AppCompatActivity {

    private RecyclerView         rvHistorico;

    // Sprint 5: empty state ilustrado (substitui tvHistoricoEmpty)
    private LinearLayout         layoutEmptyHistory;

    private BottomNavigationView bottomNavigation;

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
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory); // Sprint 5
        bottomNavigation   = findViewById(R.id.bottomNavigation);

        // Botão voltar na toolbar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void configurarRecyclerView() {
        rvHistorico.setLayoutManager(new LinearLayoutManager(this));
        historicoAdapter = new HistoricoAdapter(new ArrayList<>());
        rvHistorico.setAdapter(historicoAdapter);
    }

    // =========================================================================
    // CARREGAR DADOS
    // =========================================================================

    /**
     * Sprint 5: usa layoutEmptyHistory (ilustrado) no lugar do tvHistoricoEmpty anterior.
     */
    private void carregarHistorico() {
        com.example.inventaai.util.SessionManager sm =
                new com.example.inventaai.util.SessionManager(this);
        List<HistoricoItem> itens = historicoRepository.listarTodos(sm.getUserId());
        historicoAdapter.atualizarLista(itens);

        if (itens.isEmpty()) {
            rvHistorico.setVisibility(View.GONE);
            layoutEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            rvHistorico.setVisibility(View.VISIBLE);
            layoutEmptyHistory.setVisibility(View.GONE);
        }
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