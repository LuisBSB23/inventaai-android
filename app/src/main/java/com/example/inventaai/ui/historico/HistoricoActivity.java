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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class HistoricoActivity extends AppCompatActivity {

    // ── Chave para salvar/restaurar o filtro ativo ────────────────────────────
    private static final String KEY_FILTRO_ATIVO = "filtro_ativo";

    // ── Valores de motivo — devem bater com o que está gravado no banco ───────
    // Ajuste as strings abaixo se o seu banco usa valores diferentes.
    private static final String MOTIVO_CONSUMIDO      = "CONSUMIDO";
    private static final String MOTIVO_DESCARTADO     = "DESCARTADO";
    private static final String MOTIVO_USADO_RECEITA  = "USADO_EM_RECEITA";

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView              rvHistorico;
    private LinearLayout              layoutEmptyHistory;
    private BottomNavigationView      bottomNavigation;
    private CircularProgressIndicator progressBar;

    // TAREFA #1 — chips de filtro
    private ChipGroup chipGroupFiltro;
    private Chip      chipTodos;
    private Chip      chipConsumido;
    private Chip      chipDescartado;
    private Chip      chipUsadoReceita;

    // ── Lógica ────────────────────────────────────────────────────────────────
    private HistoricoRepository historicoRepository;
    private HistoricoAdapter    historicoAdapter;

    /** ID do chip atualmente selecionado; persistido em savedInstanceState. */
    private int filtroAtivoChipId = R.id.chipTodos;

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        historicoRepository = new HistoricoRepository(this);

        // TAREFA #1 — restaura o filtro ativo ao recriar a Activity (ex: rotação)
        if (savedInstanceState != null) {
            filtroAtivoChipId = savedInstanceState.getInt(KEY_FILTRO_ATIVO, R.id.chipTodos);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        vincularViews();
        configurarRecyclerView();
        configurarChips();
        configurarBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_history);
        }
        // Recarrega respeitando o filtro ativo e atualiza os contadores
        carregarComFiltroAtivo();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TAREFA #1 — persiste o ID do chip selecionado
        outState.putInt(KEY_FILTRO_ATIVO, filtroAtivoChipId);
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        rvHistorico        = findViewById(R.id.rvHistorico);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);
        bottomNavigation   = findViewById(R.id.bottomNavigation);
        progressBar        = findViewById(R.id.progressBarHistorico);

        // TAREFA #1 — chips
        chipGroupFiltro   = findViewById(R.id.chipGroupFiltro);
        chipTodos         = findViewById(R.id.chipTodos);
        chipConsumido     = findViewById(R.id.chipConsumido);
        chipDescartado    = findViewById(R.id.chipDescartado);
        chipUsadoReceita  = findViewById(R.id.chipUsadoReceita);

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
    // CHIPS DE FILTRO — Tarefa #1
    // =========================================================================

    private void configurarChips() {
        // Restaura o chip selecionado após rotação
        chipGroupFiltro.check(filtroAtivoChipId);

        chipGroupFiltro.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return; // singleSelection garante ao menos 1 selecionado
            filtroAtivoChipId = checkedIds.get(0);
            carregarComFiltroAtivo();
        });
    }

    /**
     * Atualiza os textos dos chips com os contadores dinâmicos e
     * recarrega o RecyclerView conforme o filtro ativo.
     */
    private void carregarComFiltroAtivo() {
        mostrarCarregando(true);

        final String userId = new SessionManager(this).getUserId();

        AppExecutors.diskIO().execute(() -> {
            // Busca todos para o filtro "Todos"
            final List<HistoricoItem> todos = historicoRepository.listarTodos(userId);

            // Conta cada motivo para os labels dos chips
            final int qtdConsumido     = historicoRepository.contarPorMotivo(MOTIVO_CONSUMIDO, userId);
            final int qtdDescartado    = historicoRepository.contarPorMotivo(MOTIVO_DESCARTADO, userId);
            final int qtdUsadoReceita  = historicoRepository.contarPorMotivo(MOTIVO_USADO_RECEITA, userId);

            // Lista filtrada conforme o chip ativo
            final List<HistoricoItem> listaFiltrada;
            if (filtroAtivoChipId == R.id.chipConsumido) {
                listaFiltrada = historicoRepository.listarPorMotivo(MOTIVO_CONSUMIDO, userId);
            } else if (filtroAtivoChipId == R.id.chipDescartado) {
                listaFiltrada = historicoRepository.listarPorMotivo(MOTIVO_DESCARTADO, userId);
            } else if (filtroAtivoChipId == R.id.chipUsadoReceita) {
                listaFiltrada = historicoRepository.listarPorMotivo(MOTIVO_USADO_RECEITA, userId);
            } else {
                listaFiltrada = todos;
            }

            AppExecutors.mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                // Atualiza labels dos chips com contadores
                chipTodos.setText(getString(R.string.chip_todos, todos.size()));
                chipConsumido.setText(getString(R.string.chip_consumido, qtdConsumido));
                chipDescartado.setText(getString(R.string.chip_descartado, qtdDescartado));
                chipUsadoReceita.setText(getString(R.string.chip_usado_receita, qtdUsadoReceita));

                // Atualiza RecyclerView
                historicoAdapter.atualizarLista(listaFiltrada);

                if (listaFiltrada.isEmpty()) {
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
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
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
            } else if (id == R.id.nav_chef_ia) {
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