package com.example.inventaai.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.ui.login.LoginActivity;
import com.example.inventaai.util.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    /** Duração exata do fade conforme especificado na Sprint 6. */
    private static final long DURACAO_FADE_MS  = 1200L;

    /** Pequena pausa extra após o fade para o usuário ver o logo estático. */
    private static final long PAUSA_EXTRA_MS   = 300L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.ivSplashLogo);

        // ── Animação de fade-in com duração exata de 1.2 segundos ────────────
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(DURACAO_FADE_MS);
        fadeIn.setFillAfter(true);  // mantém o logo visível após o fade

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { /* noop */ }
            @Override public void onAnimationRepeat(Animation animation) { /* noop */ }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Aguarda pausa extra e então navega
                new Handler(Looper.getMainLooper())
                        .postDelayed(SplashActivity.this::navegarParaProximaTela,
                                PAUSA_EXTRA_MS);
            }
        });

        ivLogo.startAnimation(fadeIn);
    }

    private void navegarParaProximaTela() {
        Intent intent = new Intent(this, LoginActivity.class);
        // Limpa o back-stack para que o botão Voltar não retorne ao Splash
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Transição suave de saída do splash (fade-out)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}