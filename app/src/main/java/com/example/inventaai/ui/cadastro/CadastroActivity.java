package com.example.inventaai.ui.cadastro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

/**
 * CadastroActivity — formulário para adicionar um novo item à despensa.
 *
 * Sprint 2: Layout com validação básica e DatePicker funcional.
 * Sprint 3: Persistência via DespensaRepository já chamada aqui.
 */
public class CadastroActivity extends AppCompatActivity {

    // Views
    private TextInputLayout tilNome, tilCategoria, tilQuantidade, tilDataValidade;
    private TextInputEditText etNome, etQuantidade, etDataValidade;
    private AutoCompleteTextView actvCategoria;
    private MaterialButtonToggleGroup toggleUnit;
    private MaterialButton btnSalvar;

    // Estado
    private String dataSelecionada = ""; // formato YYYY-MM-DD
    private DespensaRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        repository = new DespensaRepository(this);

        vincularViews();
        configurarCategoria();
        configurarDatePicker();
        configurarBotaoSalvar();
        configurarBotaoVoltar();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        tilNome          = findViewById(R.id.tilNome);
        tilCategoria     = findViewById(R.id.tilCategoria);
        tilQuantidade    = findViewById(R.id.tilQuantidade);
        tilDataValidade  = findViewById(R.id.tilDataValidade);
        etNome           = findViewById(R.id.etNome);
        etQuantidade     = findViewById(R.id.etQuantidade);
        etDataValidade   = findViewById(R.id.etDataValidade);
        actvCategoria    = findViewById(R.id.actvCategoria);
        toggleUnit       = findViewById(R.id.toggleUnit);
        btnSalvar        = findViewById(R.id.btnSalvar);
    }

    private void configurarCategoria() {
        String[] categorias = getResources().getStringArray(R.array.categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categorias);
        actvCategoria.setAdapter(adapter);
    }

    private void configurarDatePicker() {
        // Abre o DatePicker ao clicar no campo ou no ícone de calendário
        etDataValidade.setOnClickListener(v -> mostrarDatePicker());
        tilDataValidade.setEndIconOnClickListener(v -> mostrarDatePicker());
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // Armazena no formato YYYY-MM-DD (banco)
                    dataSelecionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    // Exibe no formato dd/MM/yyyy (UI)
                    String exibicao = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etDataValidade.setText(exibicao);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // =========================================================================
    // SALVAR
    // =========================================================================

    private void configurarBotaoSalvar() {
        btnSalvar.setOnClickListener(v -> {
            if (validarFormulario()) {
                salvarItem();
            }
        });
    }

    private boolean validarFormulario() {
        boolean valido = true;

        // Nome obrigatório
        String nome = etNome.getText() != null ? etNome.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nome)) {
            tilNome.setError("Informe o nome do alimento");
            valido = false;
        } else {
            tilNome.setError(null);
        }

        // Quantidade obrigatória e > 0
        String qtdStr = etQuantidade.getText() != null ? etQuantidade.getText().toString().trim() : "";
        if (TextUtils.isEmpty(qtdStr)) {
            tilQuantidade.setError("Informe a quantidade");
            valido = false;
        } else {
            try {
                double qtd = Double.parseDouble(qtdStr);
                if (qtd <= 0) {
                    tilQuantidade.setError("Quantidade deve ser maior que zero");
                    valido = false;
                } else {
                    tilQuantidade.setError(null);
                }
            } catch (NumberFormatException e) {
                tilQuantidade.setError("Quantidade inválida");
                valido = false;
            }
        }

        // Data obrigatória
        if (TextUtils.isEmpty(dataSelecionada)) {
            tilDataValidade.setError("Selecione a data de validade");
            valido = false;
        } else {
            tilDataValidade.setError(null);
        }

        return valido;
    }

    private void salvarItem() {
        String nome        = etNome.getText().toString().trim();
        double quantidade  = Double.parseDouble(etQuantidade.getText().toString().trim());
        String unidade     = getUnidadeSelecionada();
        String categoria   = actvCategoria.getText().toString().trim();
        String validadeDB  = dataSelecionada; // YYYY-MM-DD

        DespensaItem item = new DespensaItem(nome, quantidade, unidade, validadeDB, Constants.STATUS_ATIVO);

        long novoId = repository.inserir(item);

        if (novoId != -1) {
            Toast.makeText(this, nome + " adicionado à despensa!", Toast.LENGTH_SHORT).show();
            // Volta ao Dashboard
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Erro ao salvar. Tente novamente.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getUnidadeSelecionada() {
        int checkedId = toggleUnit.getCheckedButtonId();
        if (checkedId == R.id.btnUnidKg)  return "kg";
        if (checkedId == R.id.btnUnidLb)  return "lb";
        return "unid";
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void configurarBotaoVoltar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }
}
