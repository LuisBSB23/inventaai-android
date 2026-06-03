package com.example.inventaai.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.ui.dashboard.DashboardActivity;

/**
 * Sprint 12 — Helper centralizado para notificações locais de vencimento.
 *
 * Responsabilidades:
 *  1. Criar o canal de notificação (deve ser chamado o quanto antes, ex: Application ou Splash).
 *  2. Emitir uma notificação para cada item próximo do vencimento.
 */
public final class NotificationHelper {

    // ── Canal ─────────────────────────────────────────────────────────────────
    public static final String CHANNEL_VENCIMENTO    = "channel_vencimento";
    private static final String CHANNEL_NOME         = "Alertas de Vencimento";
    private static final String CHANNEL_DESCRICAO    =
            "Avisa quando itens da sua despensa estão próximos do vencimento.";

    // ID base para notificações (incrementado por item para não sobrescrever)
    private static final int NOTIF_ID_BASE = 1000;

    private NotificationHelper() {}

    // ── Criação do canal ──────────────────────────────────────────────────────

    /**
     * Registra o canal de notificação no sistema (Android 8+).
     * É seguro chamar mais de uma vez — o sistema ignora chamadas duplicadas.
     * Deve ser chamado em SplashActivity.onCreate() antes do Worker ser agendado.
     */
    public static void criarCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CHANNEL_VENCIMENTO,
                    CHANNEL_NOME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.setDescription(CHANNEL_DESCRICAO);
            canal.enableVibration(true);
            canal.setShowBadge(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }
    }

    // ── Emissão de notificação ────────────────────────────────────────────────

    /**
     * Emite uma notificação para um item próximo do vencimento.
     *
     * @param context   Contexto da aplicação.
     * @param item      Item da despensa a ser alertado.
     * @param diasRestantes Quantidade de dias até o vencimento (pode ser 0 = hoje).
     */
    public static void notificarVencimento(Context context, DespensaItem item, int diasRestantes) {
        // Verifica permissão em runtime (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return; // Permissão não concedida — não tenta emitir
            }
        }

        // Intent que abre o Dashboard ao tocar na notificação
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) item.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Monta o texto conforme os dias restantes
        String titulo;
        String mensagem;
        if (diasRestantes <= 0) {
            titulo   = "⚠️ Item vencido!";
            mensagem = item.getNome() + " venceu hoje. Verifique sua despensa.";
        } else if (diasRestantes == 1) {
            titulo   = "⏰ Vence amanhã!";
            mensagem = item.getNome() + " vence amanhã. Use-o em breve!";
        } else {
            titulo   = "🕐 Vencendo em breve";
            mensagem = item.getNome() + " vence em " + diasRestantes + " dias.";
        }

        Notification notificacao = new NotificationCompat.Builder(context, CHANNEL_VENCIMENTO)
                .setSmallIcon(R.drawable.ic_nav_pantry)
                .setColor(context.getResources().getColor(R.color.colorPrimary, context.getTheme()))
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        // Usa o ID do item como notifId para evitar duplicatas (mesmo item = mesma notif)
        int notifId = NOTIF_ID_BASE + (int) item.getId();
        NotificationManagerCompat.from(context).notify(notifId, notificacao);
    }
}