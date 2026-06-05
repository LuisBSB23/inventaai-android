package com.example.inventaai.ui.perfil;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.User;
import com.example.inventaai.data.repository.UserRepository;
import com.example.inventaai.ui.login.LoginActivity;
import com.example.inventaai.util.GlideHelper;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PerfilActivity extends AppCompatActivity {

    private ImageView         ivAvatar;
    private TextView          tvIniciais, tvNomeDisplay, tvId;
    private TextInputLayout   tilNome, tilSenhaAtual, tilNovaSenha;
    private TextInputEditText etNome, etSenhaAtual, etNovaSenha;
    private MaterialButton    btnSalvarNome, btnTrocarSenha, btnLogout;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private User usuarioAtual;

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) salvarAvatarLocalmente(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });

        vincularViews();
        carregarPerfil();
        configurarListeners();
    }

    // =========================================================================
    // Inicialização
    // =========================================================================

    private void vincularViews() {
        ivAvatar       = findViewById(R.id.ivPerfilAvatar);
        tvIniciais     = findViewById(R.id.tvPerfilIniciais);   // Sprint 4
        tvNomeDisplay  = findViewById(R.id.tvPerfilNomeDisplay);
        tvId           = findViewById(R.id.tvPerfilId);
        tilNome        = findViewById(R.id.tilPerfilNome);
        tilSenhaAtual  = findViewById(R.id.tilSenhaAtual);
        tilNovaSenha   = findViewById(R.id.tilNovaSenha);
        etNome         = findViewById(R.id.etPerfilNome);
        etSenhaAtual   = findViewById(R.id.etSenhaAtual);
        etNovaSenha    = findViewById(R.id.etNovaSenha);
        btnSalvarNome  = findViewById(R.id.btnSalvarNome);
        btnTrocarSenha = findViewById(R.id.btnTrocarSenha);
        btnLogout      = findViewById(R.id.btnLogout);

        findViewById(R.id.btnPerfilVoltar).setOnClickListener(v -> finish());
    }

    private void carregarPerfil() {
        String userId = sessionManager.getUserId();
        if (userId == null) { finish(); return; }

        usuarioAtual = userRepository.getUserById(userId);
        if (usuarioAtual == null) { finish(); return; }

        tvNomeDisplay.setText(usuarioAtual.getNome());
        tvId.setText("ID: " + usuarioAtual.getId());
        etNome.setText(usuarioAtual.getNome());

        atualizarAvatar();
    }

    /**
     * Sprint 4: exibe foto via Glide quando disponível;
     * caso contrário mostra a inicial do nome no lugar do ImageView.
     */
    private void atualizarAvatar() {
        if (usuarioAtual.getAvatarPath() != null && !usuarioAtual.getAvatarPath().isEmpty()) {
            GlideHelper.loadCircularImage(this, usuarioAtual.getAvatarPath(), ivAvatar);
            ivAvatar.setColorFilter(null);
            ivAvatar.setVisibility(View.VISIBLE);
            tvIniciais.setVisibility(View.GONE);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvIniciais.setText(usuarioAtual.getIniciais());
            tvIniciais.setVisibility(View.VISIBLE);
        }
    }

    private void configurarListeners() {
        // Copiar UUID completo ao segurar o ID
        tvId.setOnLongClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("user_id", usuarioAtual.getId()));
            Toast.makeText(this, "ID copiado!", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Selecionar avatar
        findViewById(R.id.framePerfilAvatar).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galeriaLauncher.launch(intent);
        });

        btnSalvarNome.setOnClickListener(v  -> salvarNome());
        btnTrocarSenha.setOnClickListener(v -> trocarSenha());
        btnLogout.setOnClickListener(v      -> fazerLogout());
    }

    // =========================================================================
    // Ações
    // =========================================================================

    private void salvarNome() {
        String novoNome = etNome.getText() != null ? etNome.getText().toString().trim() : "";
        if (TextUtils.isEmpty(novoNome)) {
            tilNome.setError("Informe um nome");
            return;
        }
        tilNome.setError(null);

        boolean ok = userRepository.updateNome(usuarioAtual.getId(), novoNome);
        if (ok) {
            sessionManager.salvarSessao(usuarioAtual.getId(), novoNome);
            tvNomeDisplay.setText(novoNome);
            usuarioAtual.setNome(novoNome);
            // Sprint 4: atualiza inicial se não tiver foto
            if (usuarioAtual.getAvatarPath() == null || usuarioAtual.getAvatarPath().isEmpty()) {
                tvIniciais.setText(usuarioAtual.getIniciais());
            }
            Toast.makeText(this, "Nome atualizado!", Toast.LENGTH_SHORT).show();
        } else {
            tilNome.setError("Nome já em uso por outro perfil");
        }
    }

    private void trocarSenha() {
        String atual  = etSenhaAtual.getText() != null ? etSenhaAtual.getText().toString() : "";
        String nova   = etNovaSenha.getText()  != null ? etNovaSenha.getText().toString()  : "";

        boolean valido = true;
        if (atual.isEmpty()) { tilSenhaAtual.setError("Informe a senha atual"); valido = false; }
        else tilSenhaAtual.setError(null);

        if (nova.length() < 4) { tilNovaSenha.setError("Mínimo 4 caracteres"); valido = false; }
        else tilNovaSenha.setError(null);

        if (!valido) return;

        boolean ok = userRepository.updateSenha(usuarioAtual.getId(), atual, nova);
        if (ok) {
            etSenhaAtual.setText("");
            etNovaSenha.setText("");
            Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show();
        } else {
            tilSenhaAtual.setError("Senha atual incorreta");
        }
    }

    private void salvarAvatarLocalmente(Uri uri) {
        try {
            File avatarDir = new File(getFilesDir(), "avatars");
            if (!avatarDir.exists()) avatarDir.mkdirs();

            File destino = new File(avatarDir, "avatar_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(destino)) {
                byte[] buf = new byte[4096];
                int len;
                while (in != null && (len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            String path = destino.getAbsolutePath();
            userRepository.updateAvatar(usuarioAtual.getId(), path);
            usuarioAtual.setAvatarPath(path);

            // Sprint 4: exibe foto e esconde inicial
            GlideHelper.loadCircularImage(this, path, ivAvatar);
            ivAvatar.setColorFilter(null);
            ivAvatar.setVisibility(View.VISIBLE);
            tvIniciais.setVisibility(View.GONE);

            Toast.makeText(this, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar foto.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fazerLogout() {
        sessionManager.encerrarSessao();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}