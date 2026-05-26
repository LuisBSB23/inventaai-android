package com.example.inventaai.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.User;
import com.example.inventaai.data.repository.UserRepository;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * LoginActivity — ponto de entrada do app para usuários já cadastrados.
 *
 * Sprint 1:
 * - Verifica sessão ativa no onStart() e redireciona direto ao Dashboard.
 * - Salva user_id no SharedPreferences após login bem-sucedido.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout    tilNome, tilSenha;
    private TextInputEditText  etNome, etSenha;
    private MaterialButton     btnEntrar;
    private TextView           tvCriarConta;

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
        // Se já há sessão ativa, vai direto ao Dashboard
        if (sessionManager.isLoggedIn()) {
            irParaDashboard();
        }
    }

    // =========================================================================
    // Inicialização
    // =========================================================================

    private void vincularViews() {
        tilNome    = findViewById(R.id.tilLoginNome);
        tilSenha   = findViewById(R.id.tilLoginSenha);
        etNome     = findViewById(R.id.etLoginNome);
        etSenha    = findViewById(R.id.etLoginSenha);
        btnEntrar  = findViewById(R.id.btnEntrar);
        tvCriarConta = findViewById(R.id.tvCriarConta);
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

        // Validação local
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

        // Autentica no banco
        User user = userRepository.login(nome, senha);
        if (user != null) {
            sessionManager.salvarSessao(user.getId(), user.getNome());
            irParaDashboard();
        } else {
            Toast.makeText(this, "Nome ou senha incorretos.", Toast.LENGTH_SHORT).show();
            tilSenha.setError("Credenciais inválidas");
        }
    }

    private void irParaDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}