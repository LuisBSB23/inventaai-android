package com.example.inventaai.ui.detalhes;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.EditText;
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

    private TextInputLayout    tilDataValidade;
    private TextInputEditText  etDataValidade;

    // Estado
    private DespensaItem item;
    private double       quantidadeAtual;
    private String       dataValidadeAtual;
    private DespensaRepository repository;
    private com.example.inventaai.util.SessionManager sessionManagerDetalhes;

    // Handler para clique longo contínuo
    private android.os.Handler autoUpdateHandler = new android.os.Handler();

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
        configurarDatePicker();
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

        tilDataValidade = findViewById(R.id.tilDataValidadeDetalhes);
        etDataValidade  = findViewById(R.id.etDataValidadeDetalhes);

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

        dataValidadeAtual = item.getDataValidade();
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

        if (etDataValidade != null) {
            etDataValidade.setText(DateUtils.formatarParaExibicao(item.getDataValidade()));
        }
    }

    // =========================================================================
    // STEPPER (Aumentar, Diminuir e Digitar)
    // =========================================================================

    private void configurarStepper() {
        atualizarDisplayQuantidade();

        // Segurar para diminuir
        btnDecrease.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    diminuirQuantidade();
                    autoUpdateHandler.postDelayed(autoDecrementRunnable, 400); // 400ms antes de começar a repetir
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    autoUpdateHandler.removeCallbacks(autoDecrementRunnable);
                    return true;
            }
            return false;
        });

        // Segurar para aumentar
        btnIncrease.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    aumentarQuantidade();
                    autoUpdateHandler.postDelayed(autoIncrementRunnable, 400);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    autoUpdateHandler.removeCallbacks(autoIncrementRunnable);
                    return true;
            }
            return false;
        });

        // Clicar para digitar o valor direto
        tvQuantidade.setOnClickListener(v -> abrirDialogEdicaoQuantidade());
    }

    private void diminuirQuantidade() {
        if (quantidadeAtual > 0) {
            quantidadeAtual = Math.max(0, quantidadeAtual - 1);
            atualizarDisplayQuantidade();
        }
    }

    private void aumentarQuantidade() {
        quantidadeAtual++;
        atualizarDisplayQuantidade();
    }

    private Runnable autoDecrementRunnable = new Runnable() {
        @Override
        public void run() {
            diminuirQuantidade();
            autoUpdateHandler.postDelayed(this, 100); // Repete a cada 100ms
        }
    };

    private Runnable autoIncrementRunnable = new Runnable() {
        @Override
        public void run() {
            aumentarQuantidade();
            autoUpdateHandler.postDelayed(this, 100);
        }
    };

    private void atualizarDisplayQuantidade() {
        if (quantidadeAtual == Math.floor(quantidadeAtual)) {
            tvQuantidade.setText(String.valueOf((int) quantidadeAtual));
        } else {
            tvQuantidade.setText(String.format(java.util.Locale.US, "%.1f", quantidadeAtual));
        }
    }

    private void abrirDialogEdicaoQuantidade() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setPadding(48, 48, 48, 48);

        if (quantidadeAtual == Math.floor(quantidadeAtual)) {
            input.setText(String.valueOf((int) quantidadeAtual));
        } else {
            input.setText(String.valueOf(quantidadeAtual));
        }
        input.setSelection(input.getText().length());
        input.requestFocus();

        new android.app.AlertDialog.Builder(this)
                .setTitle("Editar Quantidade")
                .setView(input)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String val = input.getText().toString().replace(",", ".");
                    if (!val.isEmpty()) {
                        try {
                            quantidadeAtual = Double.parseDouble(val);
                            if (quantidadeAtual < 0) quantidadeAtual = 0;
                            atualizarDisplayQuantidade();
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // DATE PICKER
    // =========================================================================

    private void configurarDatePicker() {
        if (etDataValidade == null) return;

        etDataValidade.setOnClickListener(v -> abrirDatePicker());

        if (tilDataValidade != null) {
            tilDataValidade.setEndIconOnClickListener(v -> abrirDatePicker());
        }
    }

    private void abrirDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (dataValidadeAtual != null && dataValidadeAtual.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] partes = dataValidadeAtual.split("-");
                cal.set(Integer.parseInt(partes[0]),
                        Integer.parseInt(partes[1]) - 1,
                        Integer.parseInt(partes[2]));
            } catch (Exception ignored) { }
        }

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    dataValidadeAtual = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    String exibicao = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etDataValidade.setText(exibicao);

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}