package com.example.inventaai.ui.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.example.inventaai.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ReceitasEmAndamentoActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView             rvReceitas;
    private LinearLayout             layoutEmpty;
    private LinearProgressIndicator  progressBar;
    private BottomNavigationView     bottomNavigation;
    private TextInputEditText        etBusca;

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
        setContentView(R.layout.activity_receitas_em_andamento);

        sessionManager    = new SessionManager(this);
        receitaRepository = new ReceitaRepository(this);
        currentUserId     = sessionManager.getUserId();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        vincularViews();
        configurarRecyclerView();
        configurarBusca();
        configurarBotoes();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_chef_ia).setChecked(true);
        }
        String query = etBusca != null && etBusca.getText() != null
                ? etBusca.getText().toString() : "";
        carregarReceitas(query);
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvReceitas       = findViewById(R.id.rvReceitas);
        layoutEmpty      = findViewById(R.id.layoutEmptyReceitas);
        progressBar      = findViewById(R.id.progressBarReceitas);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        etBusca          = findViewById(R.id.etBuscaReceitas);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void configurarRecyclerView() {
        rvReceitas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceitasAdapter(
                new ArrayList<>(),
                this::abrirDetalhe,
                this::confirmarDelecao
        );
        rvReceitas.setAdapter(adapter);
    }

    private void configurarBusca() {
        if (etBusca == null) return;
        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                carregarReceitas(s != null ? s.toString() : "");
            }
        });
    }

    private void configurarBotoes() {
        View btnIrParaChefIA = findViewById(R.id.btnIrParaChefIA);
        if (btnIrParaChefIA != null) {
            btnIrParaChefIA.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    // =========================================================================
    // CARREGAR DADOS — filtra por status "EM_ANDAMENTO"
    // =========================================================================

    private void carregarReceitas(String query) {
        if (currentUserId == null) return;
        mostrarCarregando(true);

        AppExecutors.diskIO().execute(() -> {
            final List<ReceitaSalva> lista =
                    receitaRepository.listarPorStatus(currentUserId, "EM_ANDAMENTO", query);

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                mostrarCarregando(false);
                adapter.atualizarLista(lista);
                atualizarEstadoVazio(lista.isEmpty());
            });
        });
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    private void confirmarDelecao(ReceitaSalva receita, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remover receita")
                .setMessage("Deseja remover \"" + receita.getTitulo() + "\" das receitas em andamento?")
                .setPositiveButton("Remover", (d, w) -> deletarReceita(receita, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarReceita(ReceitaSalva receita, int position) {
        AppExecutors.diskIO().execute(() -> {
            final boolean ok = receitaRepository.deletar(receita.getId());
            AppExecutors.mainThread().execute(() -> {
                if (ok) {
                    adapter.removerItem(position);
                    atualizarEstadoVazio(adapter.getItemCount() == 0);
                    Toast.makeText(this, "\"" + receita.getTitulo() + "\" removida.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao remover.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // =========================================================================
    // DETALHE
    // =========================================================================

    private void abrirDetalhe(ReceitaSalva receita) {
        Intent intent = new Intent(this, ReceitaDetalheActivity.class);
        intent.putExtra(ReceitaDetalheActivity.EXTRA_RECEITA, receita);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // =========================================================================
    // UI STATE
    // =========================================================================

    private void mostrarCarregando(boolean carregando) {
        if (progressBar != null)
            progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
    }

    private void atualizarEstadoVazio(boolean vazio) {
        rvReceitas.setVisibility(vazio ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(vazio ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // BOTTOM NAVIGATION
    // =========================================================================

    private void configurarBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chef_ia) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_pantry) {
                Intent i = new Intent(this, DashboardActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_history) {
                Intent i = new Intent(this, HistoricoActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}