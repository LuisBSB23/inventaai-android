package com.example.inventaai.ui.detalhes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * DetalhesActivity — detalhes e edição de um item da despensa.
 *
 * Pode ser usada como Activity normal ou convertida para BottomSheetDialogFragment na Sprint 3.
 * Recebe o ID do item via Intent extra (EXTRA_ITEM_ID).
 *
 * Sprint 2: Layout com stepper funcional e botões de ação.
 * Sprint 3: Carregar dados reais pelo ID, persistir alterações.
 */
public class DetalhesActivity extends AppCompatActivity {

    /** Chave para passar o ID do item via Intent */
    public static final String EXTRA_ITEM_ID = "extra_item_id";

    // Views
    private Chip chipStatus;
    private Chip chipCategoria;
    private TextView tvItemName;
    private TextView tvAddedDate;
    private TextView tvQuantidade;
    private TextView tvUnidade;
    private ImageButton btnDecrease;
    private ImageButton btnIncrease;
    private MaterialButton btnSalvar;
    private MaterialButton btnRemover;

    // Estado
    private int quantidadeAtual = 1;
    private long itemId = -1;
    private DespensaRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes);

        repository = new DespensaRepository(this);
        itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);

        vincularViews();
        carregarItem();
        configurarStepper();
        configurarBotoes();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        chipStatus    = findViewById(R.id.chipStatus);
        chipCategoria = findViewById(R.id.chipCategoria);
        tvItemName    = findViewById(R.id.tvItemName);
        tvAddedDate   = findViewById(R.id.tvAddedDate);
        tvQuantidade  = findViewById(R.id.tvQuantidade);
        tvUnidade     = findViewById(R.id.tvUnidade);
        btnDecrease   = findViewById(R.id.btnDecrease);
        btnIncrease   = findViewById(R.id.btnIncrease);
        btnSalvar     = findViewById(R.id.btnSalvar);
        btnRemover    = findViewById(R.id.btnRemover);
    }

    /**
     * Carrega os dados do item a partir do repositório.
     * Se o ID for inválido, exibe dados de exemplo para visualização do layout.
     */
    private void carregarItem() {
        if (itemId == -1) {
            // Dados de exemplo para teste de layout (Sprint 2)
            tvItemName.setText("Abacate Hass");
            tvAddedDate.setText("Adicionado há 4 dias");
            quantidadeAtual = 2;
            tvQuantidade.setText(String.valueOf(quantidadeAtual));
            tvUnidade.setText("UNIDADES");
            chipStatus.setText("Vence em 3 dias");
            chipCategoria.setText("Hortifruti");
            return;
        }

        // Sprint 3: buscar item pelo ID
        // DespensaItem item = repository.buscarPorId(itemId);
        // if (item != null) { preencherItem(item); }
    }

    // =========================================================================
    // STEPPER
    // =========================================================================

    private void configurarStepper() {
        atualizarDisplayQuantidade();

        btnDecrease.setOnClickListener(v -> {
            if (quantidadeAtual > 0) {
                quantidadeAtual--;
                atualizarDisplayQuantidade();
            }
        });

        btnIncrease.setOnClickListener(v -> {
            quantidadeAtual++;
            atualizarDisplayQuantidade();
        });
    }

    private void atualizarDisplayQuantidade() {
        tvQuantidade.setText(String.valueOf(quantidadeAtual));
    }

    // =========================================================================
    // AÇÕES
    // =========================================================================

    private void configurarBotoes() {
        btnSalvar.setOnClickListener(v -> {
            if (itemId == -1) {
                Toast.makeText(this, "Item salvo! (Sprint 3 persiste)", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // Sprint 3:
            // DespensaItem item = repository.buscarPorId(itemId);
            // item.setQuantidade(quantidadeAtual);
            // repository.atualizar(item);
            Toast.makeText(this, "Alterações salvas!", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnRemover.setOnClickListener(v -> confirmarRemocao());
    }

    private void confirmarRemocao() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remover item")
                .setMessage("Deseja remover este item da despensa?")
                .setPositiveButton("Descartar", (dialog, which) -> descartarItem())
                .setNegativeButton("Consumido", (dialog, which) -> marcarConsumido())
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void descartarItem() {
        if (itemId != -1) {
            // Sprint 3: repository.moverParaHistorico(itemId, Constants.STATUS_DESCARTADO);
        }
        Toast.makeText(this, "Item descartado.", Toast.LENGTH_SHORT).show();
        voltarAoDashboard();
    }

    private void marcarConsumido() {
        if (itemId != -1) {
            // Sprint 3: repository.moverParaHistorico(itemId, Constants.STATUS_CONSUMIDO);
        }
        Toast.makeText(this, "Item marcado como consumido.", Toast.LENGTH_SHORT).show();
        voltarAoDashboard();
    }

    private void voltarAoDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
