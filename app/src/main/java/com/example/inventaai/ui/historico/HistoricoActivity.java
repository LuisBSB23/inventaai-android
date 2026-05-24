package com.example.inventaai.ui.historico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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

import java.util.List;

/**
 * HistoricoActivity — exibe a cronologia de itens consumidos e descartados.
 *
 * Sprint 2: RecyclerView com adapter pronto para dados reais.
 * Sprint 3: Conectar ao HistoricoRepository e popular o adapter.
 */
public class HistoricoActivity extends AppCompatActivity {

    private RecyclerView rvHistorico;
    private TextView tvHistoricoEmpty;
    private BottomNavigationView bottomNavigation;

    private HistoricoRepository historicoRepository;

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
        rvHistorico       = findViewById(R.id.rvHistorico);
        tvHistoricoEmpty  = findViewById(R.id.tvHistoricoEmpty);
        bottomNavigation  = findViewById(R.id.bottomNavigation);
    }

    private void configurarRecyclerView() {
        rvHistorico.setLayoutManager(new LinearLayoutManager(this));
        // Sprint 3: rvHistorico.setAdapter(new HistoricoAdapter(dados));
    }

    private void carregarHistorico() {
        List<HistoricoItem> itens = historicoRepository.listarTodos();

        if (itens.isEmpty()) {
            rvHistorico.setVisibility(View.GONE);
            tvHistoricoEmpty.setVisibility(View.VISIBLE);
        } else {
            rvHistorico.setVisibility(View.VISIBLE);
            tvHistoricoEmpty.setVisibility(View.GONE);
            // Sprint 3: adapter.atualizarLista(itens);
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
