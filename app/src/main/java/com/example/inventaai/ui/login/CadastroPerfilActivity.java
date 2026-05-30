package com.example.inventaai.ui.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.data.model.User;
import com.example.inventaai.data.repository.UserRepository;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CadastroPerfilActivity extends AppCompatActivity {

    private TextInputLayout   tilNome, tilSenha, tilConfirmar;
    private TextInputEditText etNome, etSenha, etConfirmar;
    private MaterialButton    btnCriarPerfil;
    private FrameLayout       frameAvatar;
    private ImageView         ivAvatar;       // foto selecionada da galeria
    private TextView          tvIniciais;     // Sprint 5: inicial do nome

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private String avatarPathLocal = null;

    // Launcher para selecionar imagem da galeria
    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                salvarAvatarLocalmente(uri);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_perfil);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        vincularViews();
        configurarListeners();
    }

    // =========================================================================
    // Inicialização
    // =========================================================================

    private void vincularViews() {
        tilNome       = findViewById(R.id.tilCadastroNome);
        tilSenha      = findViewById(R.id.tilCadastroSenha);
        tilConfirmar  = findViewById(R.id.tilCadastroConfirmarSenha);
        etNome        = findViewById(R.id.etCadastroNome);
        etSenha       = findViewById(R.id.etCadastroSenha);
        etConfirmar   = findViewById(R.id.etCadastroConfirmarSenha);
        btnCriarPerfil = findViewById(R.id.btnCriarPerfil);
        frameAvatar   = findViewById(R.id.frameAvatar);
        ivAvatar      = findViewById(R.id.ivCadastroAvatar);
        tvIniciais    = findViewById(R.id.tvCadastroIniciais); // Sprint 5

        // Estado inicial: mostrar "?" até o usuário digitar o nome
        atualizarAvatarIniciais("");

        findViewById(R.id.btnCadastroVoltar).setOnClickListener(v -> finish());
    }

    private void configurarListeners() {
        frameAvatar.setOnClickListener(v -> abrirGaleria());
        btnCriarPerfil.setOnClickListener(v -> tentarCriar());

        // Sprint 5: atualiza a inicial do avatar enquanto o usuário digita o nome
        etNome.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Só atualiza as iniciais se nenhuma foto foi selecionada ainda
                if (avatarPathLocal == null) {
                    atualizarAvatarIniciais(s.toString().trim());
                }
            }
        });
    }

    // =========================================================================
    // Avatar — iniciais (Sprint 5)
    // =========================================================================

    /**
     * Exibe a primeira letra maiúscula do nome no círculo do avatar.
     * Se o nome estiver vazio, exibe "?".
     */
    private void atualizarAvatarIniciais(String nome) {
        if (tvIniciais == null) return;
        String inicial = (!nome.isEmpty()) ? String.valueOf(nome.charAt(0)).toUpperCase() : "?";
        tvIniciais.setText(inicial);
        tvIniciais.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // Galeria
    // =========================================================================

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galeriaLauncher.launch(intent);
    }

    /**
     * Copia a imagem para o armazenamento interno do app (filesDir/avatars/).
     * Sprint 5: após carregar a foto, oculta as iniciais e exibe a ImageView.
     */
    private void salvarAvatarLocalmente(Uri uri) {
        try {
            File avatarDir = new File(getFilesDir(), "avatars");
            if (!avatarDir.exists()) avatarDir.mkdirs();

            File destino = new File(avatarDir, "avatar_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(destino)) {
                byte[] buf = new byte[4096];
                int len;
                while (in != null && (len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            avatarPathLocal = destino.getAbsolutePath();

            // Exibe a foto e oculta as iniciais
            ivAvatar.setImageURI(uri);
            ivAvatar.setColorFilter(null);
            ivAvatar.setVisibility(View.VISIBLE);
            if (tvIniciais != null) tvIniciais.setVisibility(View.GONE);

        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível carregar a foto.", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // Criar perfil
    // =========================================================================

    private void tentarCriar() {
        String nome      = etNome.getText()      != null ? etNome.getText().toString().trim() : "";
        String senha     = etSenha.getText()     != null ? etSenha.getText().toString()       : "";
        String confirmar = etConfirmar.getText() != null ? etConfirmar.getText().toString()   : "";

        boolean valido = true;

        if (TextUtils.isEmpty(nome)) {
            tilNome.setError("Informe um nome");
            valido = false;
        } else { tilNome.setError(null); }

        if (senha.length() < 4) {
            tilSenha.setError("Mínimo 4 caracteres");
            valido = false;
        } else { tilSenha.setError(null); }

        if (!senha.equals(confirmar)) {
            tilConfirmar.setError("As senhas não coincidem");
            valido = false;
        } else { tilConfirmar.setError(null); }

        if (!valido) return;

        User novoUser = userRepository.createUser(nome, senha);
        if (novoUser == null) {
            tilNome.setError("Este nome já está em uso");
            return;
        }

        if (avatarPathLocal != null) {
            userRepository.updateAvatar(novoUser.getId(), avatarPathLocal);
        }

        sessionManager.salvarSessao(novoUser.getId(), novoUser.getNome());
        Toast.makeText(this, "Perfil criado! Bem-vindo, " + nome + "!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}