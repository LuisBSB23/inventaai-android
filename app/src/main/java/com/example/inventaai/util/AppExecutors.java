package com.example.inventaai.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class AppExecutors {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile AppExecutors instance;

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    // ── Executores ────────────────────────────────────────────────────────────

    private final Executor diskIO = Executors.newFixedThreadPool(3);

    private final Executor mainThread = new MainThreadExecutor();

    private AppExecutors() {}

    // ── Acessores estáticos (conveniência) ────────────────────────────────────

    /** Executor para operações de banco/disco — roda fora da UI thread. */
    public static Executor diskIO() {
        return getInstance().diskIO;
    }

    /** Executor que posta na main thread — use para atualizar a UI. */
    public static Executor mainThread() {
        return getInstance().mainThread;
    }

    // ── Implementação do MainThreadExecutor ───────────────────────────────────

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}