package com.example.inventaai.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.User;
import com.example.inventaai.data.repository.UserRepository;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout             tilNome, tilSenha;
    private TextInputEditText           etNome, etSenha;
    private MaterialButton              btnEntrar;
    private TextView                    tvCriarConta;
    private CircularProgressIndicator   progressIndicator;   // TAREFA #5

    private UserRepository  userRepository;
    private SessionManager  sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        vincularViews();
        configurarListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sessionManager.isLoggedIn()) {
            irParaDashboard();
        }
    }

    // =========================================================================
    // Inicialização
    // =========================================================================

    private void vincularViews() {
        tilNome           = findViewById(R.id.tilLoginNome);
        tilSenha          = findViewById(R.id.tilLoginSenha);
        etNome            = findViewById(R.id.etLoginNome);
        etSenha           = findViewById(R.id.etLoginSenha);
        btnEntrar         = findViewById(R.id.btnEntrar);
        tvCriarConta      = findViewById(R.id.tvCriarConta);
        progressIndicator = findViewById(R.id.progressIndicatorLogin);   // TAREFA #5
    }

    private void configurarListeners() {
        btnEntrar.setOnClickListener(v -> tentarLogin());
        tvCriarConta.setOnClickListener(v ->
                startActivity(new Intent(this, CadastroPerfilActivity.class))
        );
    }

    // =========================================================================
    // Login
    // =========================================================================

    private void tentarLogin() {
        String nome  = etNome.getText()  != null ? etNome.getText().toString().trim()  : "";
        String senha = etSenha.getText() != null ? etSenha.getText().toString() : "";

        // Validação local — sem feedback de loading durante validação de campos
        boolean valido = true;
        if (TextUtils.isEmpty(nome)) {
            tilNome.setError("Informe seu nome");
            valido = false;
        } else {
            tilNome.setError(null);
        }
        if (TextUtils.isEmpty(senha)) {
            tilSenha.setError("Informe sua senha");
            valido = false;
        } else {
            tilSenha.setError(null);
        }
        if (!valido) return;

        // TAREFA #5 — Inicia estado de loading: desabilita botão + mostra indicador
        setLoadingState(true);

        // Autentica no banco (operação síncrona local; não bloqueia a UI pois
        // SQLite local é rápido, mas o estado de loading garante que não haverá
        // múltiplos cliques simultâneos)
        User user = userRepository.login(nome, senha);

        if (user != null) {
            // Login bem-sucedido — navega para Dashboard (loading fica ativo durante a transição)
            sessionManager.salvarSessao(user.getId(), user.getNome());
            irParaDashboard();
        } else {
            // TAREFA #5 — Restaura estado normal + exibe erro via Snackbar (substituiu Toast)
            setLoadingState(false);
            tilSenha.setError("Credenciais inválidas");
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "Nome ou senha incorretos.",
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================================
    // TAREFA #5 — Estado de carregamento
    // =========================================================================

    /**
     * Alterna entre o estado "carregando" e o estado normal do formulário de login.
     *
     * Quando {@code isLoading} for {@code true}:
     *   - Botão Entrar fica desabilitado (impede múltiplos cliques)
     *   - Texto do botão muda para "Entrando..."
     *   - CircularProgressIndicator fica visível
     *
     * Quando {@code isLoading} for {@code false}:
     *   - Botão Entrar fica habilitado novamente
     *   - Texto do botão volta para "Entrar"
     *   - CircularProgressIndicator some
     */
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnEntrar.setEnabled(false);
            btnEntrar.setText("Entrando...");
            if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);
        } else {
            btnEntrar.setEnabled(true);
            btnEntrar.setText("Entrar");
            if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
        }
    }

    private void irParaDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}