package com.example.inventaai.ui.detalhes;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.CategoryIconHelper;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class DetalhesActivity extends AppCompatActivity {

    /** Chave para passar o DespensaItem serializado via Intent */
    public static final String EXTRA_ITEM = "extra_despensa_item";

    public static final String TRANSITION_NAME_ICON = "transition_item_icon";

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
    private ImageView     ivItemImage;
    private ImageButton   btnVoltarDetalhes;

    // Sprint 13: campo e botão para editar data de validade
    private TextInputLayout    tilDataValidade;
    private TextInputEditText  etDataValidade;
    private ImageButton        btnPickDate;

    // Estado
    private DespensaItem item;
    private double       quantidadeAtual;
    /** Data de validade selecionada no formato YYYY-MM-DD (pode mudar via DatePicker). */
    private String       dataValidadeAtual;
    private DespensaRepository repository;
    private com.example.inventaai.util.SessionManager sessionManagerDetalhes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes);

        repository = new DespensaRepository(this);
        sessionManagerDetalhes = new com.example.inventaai.util.SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vincularViews();
        carregarItem();
        configurarStepper();
        configurarDatePicker();  // Sprint 13
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
        ivItemImage   = findViewById(R.id.ivItemImage);
        btnVoltarDetalhes = findViewById(R.id.btnVoltarDetalhes);

        // Sprint 13: campo de data de validade editável
        tilDataValidade = findViewById(R.id.tilDataValidadeDetalhes);
        etDataValidade  = findViewById(R.id.etDataValidadeDetalhes);
        btnPickDate     = findViewById(R.id.btnPickDateDetalhes);

        if (btnVoltarDetalhes != null) {
            btnVoltarDetalhes.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void carregarItem() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_ITEM)) {
            item = (DespensaItem) getIntent().getSerializableExtra(EXTRA_ITEM);
        }

        if (item == null) {
            tvItemName.setText("Item não encontrado");
            quantidadeAtual  = 1;
            dataValidadeAtual = "";
            atualizarDisplayQuantidade();
            return;
        }

        dataValidadeAtual = item.getDataValidade(); // guarda para edição
        preencherItem(item);
    }

    private void preencherItem(DespensaItem item) {
        tvItemName.setText(item.getNome());

        String unidade = item.getUnidadeMedida() != null
                ? item.getUnidadeMedida().toUpperCase()
                : "UNIDADES";
        tvUnidade.setText(unidade);

        quantidadeAtual = item.getQuantidade();
        atualizarDisplayQuantidade();

        String cat   = item.getCategoria();
        int iconRes  = CategoryIconHelper.getIcon(cat);

        chipCategoria.setText(cat != null && !cat.isEmpty() ? cat : "Sem categoria");
        chipCategoria.setChipIconResource(iconRes);
        chipCategoria.setChipIconVisible(true);

        if (ivItemImage != null) {
            ivItemImage.setImageResource(iconRes);
            ivItemImage.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
        }

        int dias = DateUtils.calcularDiasRestantes(item.getDataValidade());

        if (dias < 0) {
            chipStatus.setText("Vencido");
        } else if (dias == 0) {
            chipStatus.setText("Vence hoje");
        } else {
            chipStatus.setText("Vence em " + dias + " dia" + (dias == 1 ? "" : "s"));
        }

        tvAddedDate.setText("Validade: " + DateUtils.formatarParaExibicao(item.getDataValidade()));

        // Sprint 13: preenche o campo de data editável com a data atual do item
        if (etDataValidade != null) {
            etDataValidade.setText(DateUtils.formatarParaExibicao(item.getDataValidade()));
        }
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
    // DATE PICKER  —  Sprint 13
    // =========================================================================

    /**
     * Configura o campo de data editável e o botão calendário para abrir o
     * DatePickerDialog, permitindo alterar a data de validade do item.
     */
    private void configurarDatePicker() {
        if (etDataValidade == null) return; // views não existem no layout atual → sem-op

        // Clique no próprio campo de texto
        etDataValidade.setOnClickListener(v -> abrirDatePicker());

        // Clique no ícone de calendário (opcional, mas melhor UX)
        if (btnPickDate != null) {
            btnPickDate.setOnClickListener(v -> abrirDatePicker());
        }

        // O end icon do TextInputLayout também pode abrir o picker
        if (tilDataValidade != null) {
            tilDataValidade.setEndIconOnClickListener(v -> abrirDatePicker());
        }
    }

    private void abrirDatePicker() {
        // Parte do valor atual (YYYY-MM-DD) para inicializar o picker na data certa
        Calendar cal = Calendar.getInstance();
        if (dataValidadeAtual != null && dataValidadeAtual.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] partes = dataValidadeAtual.split("-");
                cal.set(Integer.parseInt(partes[0]),
                        Integer.parseInt(partes[1]) - 1,
                        Integer.parseInt(partes[2]));
            } catch (Exception ignored) { /* usa a data de hoje */ }
        }

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // Persiste internamente no formato ISO
                    dataValidadeAtual = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    // Exibe no campo em formato brasileiro
                    String exibicao = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etDataValidade.setText(exibicao);

                    // Atualiza o chip de status com o novo prazo
                    int dias = DateUtils.calcularDiasRestantes(dataValidadeAtual);
                    if (dias < 0)       chipStatus.setText("Vencido");
                    else if (dias == 0) chipStatus.setText("Vence hoje");
                    else                chipStatus.setText("Vence em " + dias + " dia" + (dias == 1 ? "" : "s"));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
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

        // Sprint 13: persiste a nova data de validade se foi alterada
        if (dataValidadeAtual != null && !dataValidadeAtual.isEmpty()) {
            item.setDataValidade(dataValidadeAtual);
        }

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
                .setMessage("O que deseja fazer com \""
                        + (item != null ? item.getNome() : "este item") + "\"?")
                .setPositiveButton("Descartado", (dialog, which) -> descartarItem())
                .setNegativeButton("Consumido",  (dialog, which) -> marcarConsumido())
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void descartarItem() {
        if (item != null) {
            boolean ok = repository.moverParaHistorico(
                    item.getId(), item.getNome(),
                    Constants.STATUS_DESCARTADO,
                    sessionManagerDetalhes.getUserId());
            Toast.makeText(this,
                    ok ? "Item descartado." : "Erro ao descartar item.",
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void marcarConsumido() {
        if (item != null) {
            boolean ok = repository.moverParaHistorico(
                    item.getId(), item.getNome(),
                    Constants.STATUS_CONSUMIDO,
                    sessionManagerDetalhes.getUserId());
            Toast.makeText(this,
                    ok ? "Item marcado como consumido." : "Erro ao registrar consumo.",
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    // =========================================================================
    // Animação ao voltar
    // =========================================================================

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}