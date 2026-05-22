package com.example.inventaai.ui.dashboard;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.model.HistoricoItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.data.repository.HistoricoRepository;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // =====================================================================
        // CÓDIGO DE TESTE DA SPRINT 1 — remover após validação
        // =====================================================================
    //    executarTestesSprint1();
    }

    /**
     * Executa todos os testes manuais da Sprint 1.
     * Acompanhe a saída no Logcat filtrando pela tag "InventaAi".
     */
    private void executarTestesSprint1() {
        Log.d(TAG, "======================================================");
        Log.d(TAG, "  INÍCIO DOS TESTES — SPRINT 1 · Base de Dados");
        Log.d(TAG, "======================================================");

        DespensaRepository despensaRepo = new DespensaRepository(this);
        HistoricoRepository historicoRepo = new HistoricoRepository(this);

        // ------------------------------------------------------------------
        // TESTE 1 — Inserção de itens
        // ------------------------------------------------------------------
        Log.d(TAG, "\n--- TESTE 1: Inserção ---");

        // Item que vence hoje (VERMELHO)
        DespensaItem maca = new DespensaItem("Maçã", 2.0, "un",
                DateUtils.hoje(), Constants.STATUS_ATIVO);
        long idMaca = despensaRepo.inserir(maca);
        Log.d(TAG, "Maçã inserida com id=" + idMaca);

        // Item que vence amanhã (AMARELO)
        DespensaItem leite = new DespensaItem("Leite", 1.0, "L",
                DateUtils.hojeAdicionarDias(1), Constants.STATUS_ATIVO);
        long idLeite = despensaRepo.inserir(leite);
        Log.d(TAG, "Leite inserido com id=" + idLeite);

        // Item que vence em 5 dias (VERDE)
        DespensaItem arroz = new DespensaItem("Arroz", 5.0, "kg",
                DateUtils.hojeAdicionarDias(5), Constants.STATUS_ATIVO);
        long idArroz = despensaRepo.inserir(arroz);
        Log.d(TAG, "Arroz inserido com id=" + idArroz);

        // ------------------------------------------------------------------
        // TESTE 2 — listarTodos (ordenado por data de validade)
        // ------------------------------------------------------------------
        Log.d(TAG, "\n--- TESTE 2: listarTodos ---");
        List<DespensaItem> todos = despensaRepo.listarTodos();
        for (DespensaItem item : todos) {
            int diasRestantes = DateUtils.calcularDiasRestantes(item.getDataValidade());
            String alerta     = DateUtils.getStatusAlerta(diasRestantes);
            Log.d(TAG, item + " | diasRestantes=" + diasRestantes + " | alerta=" + alerta
                    + " | validadeFormatada=" + DateUtils.formatarParaExibicao(item.getDataValidade()));
        }

        // ------------------------------------------------------------------
        // TESTE 3 — Atualização
        // ------------------------------------------------------------------
        Log.d(TAG, "\n--- TESTE 3: Atualização ---");
        leite.setId(idLeite);
        leite.setQuantidade(2.5);
        int linhas = despensaRepo.atualizar(leite);
        Log.d(TAG, "Atualização do Leite: " + linhas + " linha(s) afetada(s).");

        // Confirma a atualização
        List<DespensaItem> aposAtualizacao = despensaRepo.listarTodos();
        for (DespensaItem item : aposAtualizacao) {
            if (item.getId() == idLeite) {
                Log.d(TAG, "Leite atualizado: quantidade=" + item.getQuantidade()
                        + " (esperado: 2.5)");
            }
        }

        // ------------------------------------------------------------------
        // TESTE 4 — listarProximosVencimento (janela de 3 dias)
        // ------------------------------------------------------------------
        Log.d(TAG, "\n--- TESTE 4: listarProximosVencimento(3) ---");
        List<DespensaItem> proximos = despensaRepo.listarProximosVencimento(3);
        Log.d(TAG, "Itens nos próximos 3 dias: " + proximos.size()
                + " (esperado: 2 — Maçã e Leite)");
        for (DespensaItem item : proximos) {
            Log.d(TAG, "  → " + item.getNome() + " | validade=" + item.getDataValidade());
        }

        // ------------------------------------------------------------------
        // TESTE 5 — moverParaHistorico
        // ------------------------------------------------------------------
        Log.d(TAG, "\n--- TESTE 5: moverParaHistorico ---");
        boolean moveu = despensaRepo.moverParaHistorico(idMaca, Constants.STATUS_CONSUMIDO);
        Log.d(TAG, "moverParaHistorico(Maçã): " + moveu + " (esperado: true)");

        // Despensa não deve mais ter a Maçã
        List<DespensaItem> aposMove = despensaRepo.listarTodos();
        Log.d(TAG, "Itens na despensa após mover Maçã: " + aposMove.size()
                + " (esperado: 2 — Leite e Arroz)");
        for (DespensaItem item : aposMove) {
            Log.d(TAG, "  → " + item.getNome());
        }

        // Histórico deve ter 1 registro
        List<HistoricoItem> historico = historicoRepo.listarTodos();
        Log.d(TAG, "Registros no histórico: " + historico.size() + " (esperado: 1)");
        for (HistoricoItem h : historico) {
            Log.d(TAG, "  → " + h);
        }

        // ------------------------------------------------------------------
        // TESTE 6 — DateUtils
        // ------------------------------------------------------------------
        Log.d(TAG, "\n--- TESTE 6: DateUtils ---");
        Log.d(TAG, "hoje()=" + DateUtils.hoje());
        Log.d(TAG, "hojeAdicionarDias(7)=" + DateUtils.hojeAdicionarDias(7));
        Log.d(TAG, "calcularDiasRestantes(hoje)=" + DateUtils.calcularDiasRestantes(DateUtils.hoje()));
        Log.d(TAG, "calcularDiasRestantes(+5)="   + DateUtils.calcularDiasRestantes(DateUtils.hojeAdicionarDias(5)));
        Log.d(TAG, "formatarParaExibicao('2026-12-31')=" + DateUtils.formatarParaExibicao("2026-12-31"));
        Log.d(TAG, "getStatusAlerta(-1)="  + DateUtils.getStatusAlerta(-1)  + " (esperado: VERMELHO)");
        Log.d(TAG, "getStatusAlerta(0)="   + DateUtils.getStatusAlerta(0)   + " (esperado: VERMELHO)");
        Log.d(TAG, "getStatusAlerta(2)="   + DateUtils.getStatusAlerta(2)   + " (esperado: AMARELO)");
        Log.d(TAG, "getStatusAlerta(3)="   + DateUtils.getStatusAlerta(3)   + " (esperado: AMARELO)");
        Log.d(TAG, "getStatusAlerta(10)="  + DateUtils.getStatusAlerta(10)  + " (esperado: VERDE)");

        Log.d(TAG, "======================================================");
        Log.d(TAG, "  FIM DOS TESTES — verifique os resultados acima");
        Log.d(TAG, "======================================================");
    }
}