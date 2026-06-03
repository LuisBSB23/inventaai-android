package com.example.inventaai.ui.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.ReceitaSalva;
import com.example.inventaai.data.repository.ReceitaRepository;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.chefIA.ChefIAActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ReceitasActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_TAG;

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView              rvReceitas;
    private LinearLayout              layoutEmptyReceitas;
    private LinearProgressIndicator   progressBar;
    private BottomNavigationView      bottomNavigation;

    // ── Dependências ───────────────────────────────────────────────────────────
    private ReceitaRepository receitaRepository;
    private ReceitasAdapter   adapter;
    private SessionManager    sessionManager;
    private String            currentUserId;

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receitas);

        sessionManager    = new SessionManager(this);
        receitaRepository = new ReceitaRepository(this);
        currentUserId     = sessionManager.getUserId();

        vincularViews();
        configurarRecyclerView();
        configurarBotoes();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_chef_ia).setChecked(true);
        }
        carregarReceitas();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvReceitas          = findViewById(R.id.rvReceitas);
        layoutEmptyReceitas = findViewById(R.id.layoutEmptyReceitas);
        progressBar         = findViewById(R.id.progressBarReceitas);
        bottomNavigation    = findViewById(R.id.bottomNavigation);

        // Botão voltar na toolbar
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void configurarRecyclerView() {
        rvReceitas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceitasAdapter(
                new ArrayList<>(),
                this::abrirDetalheReceita,   // clique no card
                this::confirmarDelecao        // clique na lixeira
        );
        rvReceitas.setAdapter(adapter);
    }

    private void configurarBotoes() {
        // Botão no empty state para ir ao Chef IA
        View btnIrParaChefIA = findViewById(R.id.btnIrParaChefIA);
        if (btnIrParaChefIA != null) {
            btnIrParaChefIA.setOnClickListener(v -> {
                finish(); // Apenas fechamos para voltar à tela anterior do ChefIA
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    // =========================================================================
    // CARREGAR DADOS
    // =========================================================================

    private void carregarReceitas() {
        if (currentUserId == null) return;

        mostrarCarregando(true);

        AppExecutors.diskIO().execute(() -> {
            final List<ReceitaSalva> lista = receitaRepository.listarTodas(currentUserId);
            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                mostrarCarregando(false);
                adapter.atualizarLista(lista);
                atualizarEstadoVazio(lista.isEmpty());
            });
        });
    }

    // =========================================================================
    // DELETAR RECEITA
    // =========================================================================

    private void confirmarDelecao(ReceitaSalva receita, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remover receita")
                .setMessage("Deseja remover \"" + receita.getTitulo() + "\" das receitas salvas?")
                .setPositiveButton("Remover", (dialog, which) -> deletarReceita(receita, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarReceita(ReceitaSalva receita, int position) {
        AppExecutors.diskIO().execute(() -> {
            final boolean sucesso = receitaRepository.deletar(receita.getId());
            AppExecutors.mainThread().execute(() -> {
                if (sucesso) {
                    adapter.removerItem(position);
                    atualizarEstadoVazio(adapter.getItemCount() == 0);
                    Toast.makeText(this, "\"" + receita.getTitulo() + "\" removida.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao remover receita.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // =========================================================================
    // ABRIR DETALHES
    // =========================================================================

    private void abrirDetalheReceita(ReceitaSalva receita) {
        Toast.makeText(this,
                "\"" + receita.getTitulo() + "\" — em breve detalhes completos!",
                Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // ESTADO DE UI
    // =========================================================================

    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null) {
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
        }
    }

    private void atualizarEstadoVazio(boolean vazio) {
        rvReceitas.setVisibility(vazio ? View.GONE : View.VISIBLE);
        layoutEmptyReceitas.setVisibility(vazio ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // BOTTOM NAVIGATION
    // =========================================================================

    private void configurarBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_chef_ia) {
                // CORREÇÃO: Já estamos na extensão do Chef IA, se clicar de novo aqui,
                // fechamos as receitas para mostrar a ecrã inicial do ChefIA.
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_pantry) {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_history) {
                Intent intent = new Intent(this, HistoricoActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}