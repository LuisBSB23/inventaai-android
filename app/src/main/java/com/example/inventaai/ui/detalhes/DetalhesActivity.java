package com.example.inventaai.ui.detalhes;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * DetalhesActivity — detalhes e edição de um item da despensa.
 *
 * Sprint 3: recebe o DespensaItem serializado via Intent (EXTRA_ITEM),
 * permite editar a quantidade com o stepper e persiste via repositório.
 * O botão "Remover" abre AlertDialog para escolher entre Consumido e Descartado.
 */
public class DetalhesActivity extends AppCompatActivity {

    /** Chave para passar o DespensaItem serializado via Intent */
    public static final String EXTRA_ITEM = "extra_despensa_item";

    // Views
    private Chip          chipStatus;
    private Chip          chipCategoria;
    private TextView      tvItemName;
    private TextView      tvAddedDate;
    private TextView      tvQuantidade;
    private TextView      tvUnidade;
    private ImageButton   btnDecrease;
    private ImageButton   btnIncrease;
    private MaterialButton btnSalvar;
    private MaterialButton btnRemover;

    // Estado
    private DespensaItem item;
    private double       quantidadeAtual;
    private DespensaRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes);

        repository = new DespensaRepository(this);

        vincularViews();
        carregarItem();
        configurarStepper();
        configurarBotoes();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        chipStatus   = findViewById(R.id.chipStatus);
        chipCategoria = findViewById(R.id.chipCategoria);
        tvItemName   = findViewById(R.id.tvItemName);
        tvAddedDate  = findViewById(R.id.tvAddedDate);
        tvQuantidade = findViewById(R.id.tvQuantidade);
        tvUnidade    = findViewById(R.id.tvUnidade);
        btnDecrease  = findViewById(R.id.btnDecrease);
        btnIncrease  = findViewById(R.id.btnIncrease);
        btnSalvar    = findViewById(R.id.btnSalvar);
        btnRemover   = findViewById(R.id.btnRemover);
    }

    /**
     * Recupera o DespensaItem enviado pelo Dashboard e preenche os campos.
     */
    @SuppressWarnings("deprecation")
    private void carregarItem() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_ITEM)) {
            item = (DespensaItem) getIntent().getSerializableExtra(EXTRA_ITEM);
        }

        if (item == null) {
            // Fallback de segurança: exibe dados de exemplo se não receber item
            tvItemName.setText("Item não encontrado");
            quantidadeAtual = 1;
            atualizarDisplayQuantidade();
            return;
        }

        preencherItem(item);
    }

    private void preencherItem(DespensaItem item) {
        // Nome
        tvItemName.setText(item.getNome());

        // Unidade
        String unidade = item.getUnidadeMedida() != null ? item.getUnidadeMedida().toUpperCase() : "UNIDADES";
        tvUnidade.setText(unidade);

        // Quantidade inicial
        quantidadeAtual = item.getQuantidade();
        atualizarDisplayQuantidade();

        // Categoria
        String cat = item.getCategoria();
        chipCategoria.setText(cat != null && !cat.isEmpty() ? cat : "Sem categoria");

        // Status de validade
        int dias = DateUtils.calcularDiasRestantes(item.getDataValidade());
        String alerta = DateUtils.getStatusAlerta(dias);

        if (dias < 0) {
            chipStatus.setText("Vencido");
        } else if (dias == 0) {
            chipStatus.setText("Vence hoje");
        } else {
            chipStatus.setText("Vence em " + dias + " dia" + (dias == 1 ? "" : "s"));
        }

        // Data de validade exibida na linha secundária
        tvAddedDate.setText("Validade: " + DateUtils.formatarParaExibicao(item.getDataValidade()));
    }

    // =========================================================================
    // STEPPER
    // =========================================================================

    private void configurarStepper() {
        atualizarDisplayQuantidade();

        btnDecrease.setOnClickListener(v -> {
            if (quantidadeAtual > 0) {
                quantidadeAtual = Math.max(0, quantidadeAtual - 1);
                atualizarDisplayQuantidade();
            }
        });

        btnIncrease.setOnClickListener(v -> {
            quantidadeAtual++;
            atualizarDisplayQuantidade();
        });
    }

    private void atualizarDisplayQuantidade() {
        if (quantidadeAtual == Math.floor(quantidadeAtual)) {
            tvQuantidade.setText(String.valueOf((int) quantidadeAtual));
        } else {
            tvQuantidade.setText(String.valueOf(quantidadeAtual));
        }
    }

    // =========================================================================
    // AÇÕES
    // =========================================================================

    private void configurarBotoes() {
        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
        btnRemover.setOnClickListener(v -> confirmarRemocao());
    }

    private void salvarAlteracoes() {
        if (item == null) {
            finish();
            return;
        }

        item.setQuantidade(quantidadeAtual);
        int linhas = repository.atualizar(item);

        if (linhas > 0) {
            Toast.makeText(this, "Alterações salvas!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nenhuma alteração detectada.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmarRemocao() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remover item")
                .setMessage("O que deseja fazer com \"" + (item != null ? item.getNome() : "este item") + "\"?")
                .setPositiveButton("Descartado", (dialog, which) -> descartarItem())
                .setNegativeButton("Consumido",  (dialog, which) -> marcarConsumido())
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void descartarItem() {
        if (item != null) {
            boolean ok = repository.moverParaHistorico(
                    item.getId(), item.getNome(), Constants.STATUS_DESCARTADO);
            Toast.makeText(this,
                    ok ? "Item descartado." : "Erro ao descartar item.",
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void marcarConsumido() {
        if (item != null) {
            boolean ok = repository.moverParaHistorico(
                    item.getId(), item.getNome(), Constants.STATUS_CONSUMIDO);
            Toast.makeText(this,
                    ok ? "Item marcado como consumido." : "Erro ao registrar consumo.",
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}