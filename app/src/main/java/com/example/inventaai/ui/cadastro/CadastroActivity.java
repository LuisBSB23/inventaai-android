package com.example.inventaai.ui.cadastro;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class CadastroActivity extends AppCompatActivity {

    // Views
    private TextInputLayout           tilNome, tilCategoria, tilQuantidade, tilDataValidade;
    private TextInputEditText         etNome, etQuantidade, etDataValidade;
    private AutoCompleteTextView      actvCategoria;
    private MaterialButtonToggleGroup toggleUnit;
    private MaterialButton            btnSalvar;

    // Estado
    private String dataSelecionada = ""; // formato YYYY-MM-DD
    private DespensaRepository repository;
    private com.example.inventaai.util.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        repository     = new DespensaRepository(this);
        sessionManager = new com.example.inventaai.util.SessionManager(this);

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
        tilNome         = findViewById(R.id.tilNome);
        tilCategoria    = findViewById(R.id.tilCategoria);
        tilQuantidade   = findViewById(R.id.tilQuantidade);
        tilDataValidade = findViewById(R.id.tilDataValidade);
        etNome          = findViewById(R.id.etNome);
        etQuantidade    = findViewById(R.id.etQuantidade);
        etDataValidade  = findViewById(R.id.etDataValidade);
        actvCategoria   = findViewById(R.id.actvCategoria);
        toggleUnit      = findViewById(R.id.toggleUnit);
        btnSalvar       = findViewById(R.id.btnSalvar);
    }

    private void configurarCategoria() {
        String[] categorias = getResources().getStringArray(R.array.categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categorias);
        actvCategoria.setAdapter(adapter);
    }

    private void configurarDatePicker() {
        etDataValidade.setOnClickListener(v -> mostrarDatePicker());
        tilDataValidade.setEndIconOnClickListener(v -> mostrarDatePicker());
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    dataSelecionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
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

        String nome = etNome.getText() != null ? etNome.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nome)) {
            tilNome.setError("Informe o nome do alimento");
            valido = false;
        } else {
            tilNome.setError(null);
        }

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

        if (TextUtils.isEmpty(dataSelecionada)) {
            tilDataValidade.setError("Selecione a data de validade");
            valido = false;
        } else {
            tilDataValidade.setError(null);
        }

        return valido;
    }

    private void salvarItem() {
        String nome       = etNome.getText().toString().trim();
        double quantidade = Double.parseDouble(etQuantidade.getText().toString().trim());
        String unidade    = getUnidadeSelecionada();
        String categoria  = actvCategoria.getText().toString().trim();

        DespensaItem item = new DespensaItem(nome, quantidade, unidade, dataSelecionada, Constants.STATUS_ATIVO);
        item.setCategoria(categoria);

        long novoId = repository.inserir(item, sessionManager.getUserId());

        if (novoId != -1) {
            Toast.makeText(this, nome + " adicionado à despensa!", Toast.LENGTH_SHORT).show();
            // Tarefa 3: animação correta de "voltar" após salvar.
            // slide_in_left: tela anterior (Dashboard) entra pela esquerda.
            // slide_out_right: tela atual (Cadastro) sai pela direita.
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            Toast.makeText(this, "Erro ao salvar. Tente novamente.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getUnidadeSelecionada() {
        int checkedId = toggleUnit.getCheckedButtonId();
        if (checkedId == R.id.btnUnidKg) return "kg";
        if (checkedId == R.id.btnUnidLb) return "lb";
        return "unid";
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void configurarBotaoVoltar() {
        // Tarefa 3: o botão "Voltar" da toolbar usa a mesma
        // animação de retorno que o botão Salvar: tela anterior entra pela
        // esquerda e a tela atual sai pela direita.
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    @Override
    public void onBackPressed() {
        // Tarefa 3: garante que o botão físico/gesto de Voltar
        // use a mesma animação de retorno definida no guia.
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}