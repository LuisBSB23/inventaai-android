package com.example.inventaai.ui.cadastro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;


import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.example.inventaai.ui.perfil.PerfilActivity;
import com.example.inventaai.ui.receitas.ReceitasActivity;
import com.example.inventaai.ui.receitas.ReceitasConcluidasActivity;
import com.example.inventaai.ui.receitas.ReceitasEmAndamentoActivity;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class CadastroActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // ── Views principais ──────────────────────────────────────────────────────
    private DrawerLayout              drawerLayout;
    private NavigationView            navigationView;
    private TextInputLayout           tilNome, tilCategoria, tilQuantidade, tilDataValidade;
    private TextInputEditText         etNome, etQuantidade, etDataValidade;
    private AutoCompleteTextView      actvCategoria;
    private MaterialButtonToggleGroup toggleUnit;
    private MaterialButton            btnSalvar;
    private View                      rootView;

    // ── Estado ────────────────────────────────────────────────────────────────
    private String              dataSelecionada = "";
    private DespensaRepository  repository;
    private SessionManager      sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        repository     = new DespensaRepository(this);
        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cadastroCoordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vincularViews();
        configurarDrawer();
        configurarCategoria();
        configurarDatePicker();
        configurarBotaoSalvar();
        configurarBotaoVoltar();
    }

    // =========================================================================
    // NAVIGATION DRAWER (Sprint 18)
    // =========================================================================

    private void configurarDrawer() {
        // Abre o drawer pelo botão hamburguer da toolbar
        View btnMenu = findViewById(R.id.btnMenuCadastro);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        navigationView.setNavigationItemSelectedListener(this);
        preencherCabecalhoDrawer();
    }

    private void preencherCabecalhoDrawer() {
        View header = navigationView.getHeaderView(0);
        if (header == null) return;

        android.widget.TextView tvNome      = header.findViewById(R.id.tvDrawerNome);
        android.widget.TextView tvId        = header.findViewById(R.id.tvDrawerIdAbreviado);
        android.widget.TextView tvIniciais  = header.findViewById(R.id.tvDrawerIniciais);
        ShapeableImageView      ivAvatar    = header.findViewById(R.id.ivDrawerAvatar);

        String nome   = sessionManager.getUserName();
        String userId = sessionManager.getUserId();

        if (tvNome != null && nome != null)
            tvNome.setText(nome);

        if (tvId != null && userId != null)
            tvId.setText("ID: ..." + userId.substring(Math.max(0, userId.length() - 6)));

        // Sem getUserAvatar() no SessionManager — exibe sempre as iniciais
        if (tvIniciais != null && nome != null && !nome.isEmpty()) {
            tvIniciais.setVisibility(View.VISIBLE);
            if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
            tvIniciais.setText(obterIniciais(nome));
        }
    }

    private String obterIniciais(String nome) {
        if (nome == null || nome.isEmpty()) return "?";
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) return String.valueOf(partes[0].charAt(0)).toUpperCase();
        return (String.valueOf(partes[0].charAt(0)) + String.valueOf(partes[partes.length - 1].charAt(0)))
                .toUpperCase();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        int id = item.getItemId();

        if (id == R.id.nav_perfil) {
            startActivity(new Intent(this, PerfilActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (id == R.id.nav_receitas_salvas) {
            startActivity(new Intent(this, ReceitasActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (id == R.id.nav_receitas_em_andamento) {
            startActivity(new Intent(this, ReceitasEmAndamentoActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (id == R.id.nav_receitas_concluidas) {
            startActivity(new Intent(this, ReceitasConcluidasActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (id == R.id.nav_configuracoes) {
            startActivity(new Intent(this, HistoricoActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (id == R.id.nav_sair) {
            sessionManager.encerrarSessao();
            Intent intent = new Intent(this, com.example.inventaai.ui.login.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        drawerLayout    = findViewById(R.id.cadastroDrawerLayout);
        navigationView  = findViewById(R.id.cadastroNavigationView);
        rootView        = findViewById(R.id.cadastroCoordinatorLayout);
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
    // VALIDAÇÃO
    // =========================================================================

    private boolean validarFormulario() {
        boolean valido = true;

        String nome = etNome.getText() != null ? etNome.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nome)) {
            tilNome.setError("Informe o nome do alimento");
            valido = false;
        } else {
            tilNome.setError(null);
        }

        String qtdStr = etQuantidade.getText() != null
                ? etQuantidade.getText().toString().trim() : "";
        if (TextUtils.isEmpty(qtdStr)) {
            tilQuantidade.setError("Informe a quantidade");
            valido = false;
        } else {
            try {
                double qtd = Double.parseDouble(qtdStr.replace(",", "."));
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

    // =========================================================================
    // SALVAR
    // =========================================================================

    private void configurarBotaoSalvar() {
        btnSalvar.setOnClickListener(v -> {
            if (validarFormulario()) salvarItem();
        });
    }

    private void salvarItem() {
        String nome = etNome.getText().toString().trim();

        String qtdStr    = etQuantidade.getText().toString().trim().replace(",", ".");
        double quantidade = Double.parseDouble(qtdStr);

        String unidade   = getUnidadeSelecionada();
        String categoria = actvCategoria.getText().toString().trim();

        DespensaItem item = new DespensaItem(nome, quantidade, unidade, dataSelecionada, Constants.STATUS_ATIVO);
        item.setCategoria(categoria);

        long novoId = repository.inserir(item, sessionManager.getUserId());

        if (novoId != -1) {
            limparCampos();
            Snackbar.make(rootView, nome + " adicionado à despensa!", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.colorPrimary))
                    .setTextColor(getColor(R.color.colorOnPrimary))
                    .show();
        } else {
            Snackbar.make(rootView, "Erro ao salvar. Tente novamente.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void limparCampos() {
        etNome.setText("");
        etQuantidade.setText("");
        etDataValidade.setText("");
        actvCategoria.setText("", false);
        dataSelecionada = "";
        toggleUnit.clearChecked();
        tilNome.setError(null);
        tilQuantidade.setError(null);
        tilDataValidade.setError(null);
        etNome.requestFocus();
    }

    private String getUnidadeSelecionada() {
        int checkedId = toggleUnit.getCheckedButtonId();
        if (checkedId == R.id.btnUnidKg) return "kg";
        if (checkedId == R.id.btnUnidL)  return "L";
        return "unid";
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void configurarBotaoVoltar() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }
    }
}