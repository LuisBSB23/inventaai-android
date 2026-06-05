package com.example.inventaai.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.AppPrefs;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.example.inventaai.util.NotificationHelper;
import com.example.inventaai.util.SessionManager;

import java.util.List;

/**
 * Sprint 12 — Worker que roda em background a cada 24 horas.
 *
 * Consulta a despensa do usuário logado e emite notificações para
 * itens próximos do vencimento conforme configurado em AppPrefs.
 *
 * Agendamento: PeriodicWorkRequest com intervalo de 24h, sem restrição de rede.
 */
public class VencimentoWorker extends Worker {

    private static final String TAG = Constants.LOG_TAG;

    public VencimentoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.d(TAG, "VencimentoWorker: iniciando verificação de vencimentos...");

        try {
            // 1. Verifica se notificações estão ativas nas preferências
            AppPrefs prefs = AppPrefs.getInstance(context);
            if (!prefs.isNotificacoesAtivas()) {
                Log.d(TAG, "VencimentoWorker: notificações desativadas pelo usuário. Encerrando.");
                return Result.success();
            }

            // 2. Obtém o userId da sessão atual
            SessionManager sessionManager = new SessionManager(context);
            String userId = sessionManager.getUserId();
            if (userId == null) {
                Log.d(TAG, "VencimentoWorker: nenhum usuário logado. Encerrando.");
                return Result.success();
            }

            // 3. Lê o limiar de dias configurado pelo usuário
            int diasLimiar = prefs.getDiasNotificacao();
            Log.d(TAG, "VencimentoWorker: verificando itens que vencem em até " + diasLimiar + " dias.");

            // 4. Consulta os itens próximos do vencimento
            DespensaRepository repository = new DespensaRepository(context);
            List<DespensaItem> itensProximos =
                    repository.listarProximosVencimento(diasLimiar, userId);

            if (itensProximos.isEmpty()) {
                Log.d(TAG, "VencimentoWorker: nenhum item próximo do vencimento. Nada a notificar.");
                return Result.success();
            }

            Log.d(TAG, "VencimentoWorker: " + itensProximos.size() + " item(ns) para notificar.");

            // 5. Garante que o canal existe antes de emitir notificações
            NotificationHelper.criarCanal(context);

            // 6. Emite uma notificação para cada item crítico
            for (DespensaItem item : itensProximos) {
                int diasRestantes = DateUtils.calcularDiasRestantes(item.getDataValidade());
                Log.d(TAG, "VencimentoWorker: notificando \"" + item.getNome()
                        + "\" — " + diasRestantes + " dia(s) restantes.");
                NotificationHelper.notificarVencimento(context, item, diasRestantes);
            }

            Log.d(TAG, "VencimentoWorker: verificação concluída com sucesso.");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "VencimentoWorker: erro inesperado durante a execução.", e);
            // Retorna failure para o WorkManager retentar na próxima janela
            return Result.failure();
        }
    }
}