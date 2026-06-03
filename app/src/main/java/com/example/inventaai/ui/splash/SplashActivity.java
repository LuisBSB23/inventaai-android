package com.example.inventaai.ui.splash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.inventaai.R;
import com.example.inventaai.ui.login.LoginActivity;
import com.example.inventaai.util.NotificationHelper;
import com.example.inventaai.util.SessionManager;
import com.example.inventaai.worker.VencimentoWorker;

import java.util.concurrent.TimeUnit;

/**
 * Sprint 12: Adicionado agendamento do VencimentoWorker e pedido de permissão
 * de notificação (Android 13+). As demais funcionalidades do Splash são preservadas.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    /** Duração exata do fade conforme especificado na Sprint 6. */
    private static final long DURACAO_FADE_MS = 1200L;

    /** Pequena pausa extra após o fade para o usuário ver o logo estático. */
    private static final long PAUSA_EXTRA_MS  = 300L;

    /** Tag única para o Worker — evita duplicação de jobs no WorkManager. */
    private static final String WORKER_TAG = "VencimentoWorkerPeriodico";

    // Launcher para o diálogo de permissão de notificação (Android 13+)
    private final ActivityResultLauncher<String> permissaoNotifLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        // Independente do resultado, continuamos o fluxo normal.
                        // O Worker só emitirá notificações se a permissão for concedida.
                        agendarWorkerVencimento();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Sprint 12: cria o canal de notificação o mais cedo possível
        NotificationHelper.criarCanal(this);

        ImageView ivLogo = findViewById(R.id.ivSplashLogo);

        // ── Animação de fade-in com duração exata de 1.2 segundos ────────────
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(DURACAO_FADE_MS);
        fadeIn.setFillAfter(true);

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation)  { /* noop */ }
            @Override public void onAnimationRepeat(Animation animation) { /* noop */ }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler(Looper.getMainLooper())
                        .postDelayed(SplashActivity.this::solicitarPermissaoENavegar,
                                PAUSA_EXTRA_MS);
            }
        });

        ivLogo.startAnimation(fadeIn);
    }

    /**
     * Solicita permissão de notificação no Android 13+ antes de navegar.
     * Em versões anteriores, apenas agenda o Worker e navega diretamente.
     */
    private void solicitarPermissaoENavegar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean permissaoConcedida = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;

            if (!permissaoConcedida) {
                // Solicita permissão — o launcher chama agendarWorkerVencimento() no callback
                permissaoNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                // Navega para a próxima tela em paralelo (não bloqueia o usuário)
                navegarParaProximaTela();
                return;
            }
        }

        // Android 12 ou inferior, ou permissão já concedida
        agendarWorkerVencimento();
        navegarParaProximaTela();
    }

    /**
     * Agenda o PeriodicWorkRequest do VencimentoWorker (24h, sem restrição de rede).
     * Usa KEEP para não substituir um job já agendado (ex: abertura dupla do app).
     */
    private void agendarWorkerVencimento() {
        Constraints constraints = new Constraints.Builder()
                .build(); // NETWORK_NOT_REQUIRED é o padrão — não precisa de rede

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(VencimentoWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .addTag(WORKER_TAG)
                        .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                        WORKER_TAG,
                        ExistingPeriodicWorkPolicy.KEEP, // mantém job existente
                        workRequest
                );
    }

    private void navegarParaProximaTela() {
        SessionManager sessionManager = new SessionManager(this);
        Class<?> destino = sessionManager.isLoggedIn()
                ? com.example.inventaai.ui.dashboard.DashboardActivity.class
                : LoginActivity.class;

        Intent intent = new Intent(this, destino);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}