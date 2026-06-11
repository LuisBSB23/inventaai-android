package com.example.inventaai.ui.sincronizacao;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.PlanilhaHelper;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.OutputStream;
import java.util.List;

/**
 * Sprint 20 — Tela central de Gestão da Despensa via Planilha Excel (.xlsx).
 */
public class SincronizacaoActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_TAG;

    // MIME type padrão para planilhas XLSX
    private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // ── Views ─────────────────────────────────────────────────────────────────
    private CircularProgressIndicator progressBar;

    // ── Dependências ──────────────────────────────────────────────────────────
    private DespensaRepository despensaRepository;
    private SessionManager     sessionManager;

    // ── SAF Launchers ─────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> salvarModeloLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument(MIME_TYPE_XLSX),
                    uri -> {
                        if (uri != null) salvarModeloNoUri(uri);
                    });

    private final ActivityResultLauncher<Intent> abrirXlsxLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) iniciarPreviewImport(uri);
                        }
                    });

    private final ActivityResultLauncher<String> salvarExportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument(MIME_TYPE_XLSX),
                    uri -> {
                        if (uri != null) exportarDespensaParaUri(uri);
                    });

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sincronizacao);

        despensaRepository = new DespensaRepository(this);
        sessionManager     = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        vincularViews();
        configurarBotoes();
    }

    private void vincularViews() {
        progressBar = findViewById(R.id.progressBar);
    }

    private void configurarBotoes() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardBaixarModelo).setOnClickListener(v ->
                salvarModeloLauncher.launch("modelo_despensa.xlsx"));

        findViewById(R.id.cardImportar).setOnClickListener(v -> abrirSeletorXlsx());

        findViewById(R.id.cardExportar).setOnClickListener(v -> {
            String dataHoje = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            salvarExportLauncher.launch("despensa_" + dataHoje + ".xlsx");
        });

        findViewById(R.id.cardWhatsApp).setOnClickListener(v -> compartilharWhatsApp());
    }

    // =========================================================================
    // SELETOR DE ARQUIVO XLSX
    // =========================================================================

    private void abrirSeletorXlsx() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(MIME_TYPE_XLSX);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                MIME_TYPE_XLSX,
                "application/vnd.ms-excel",
                "application/octet-stream"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            abrirXlsxLauncher.launch(Intent.createChooser(intent, "Selecionar planilha XLSX"));
            return;
        }

        Intent fallback = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fallback.setType("*/*");
        fallback.addCategory(Intent.CATEGORY_OPENABLE);
        abrirXlsxLauncher.launch(Intent.createChooser(fallback, "Selecionar arquivo"));
    }

    // =========================================================================
    // 1. BAIXAR MODELO
    // =========================================================================

    private void salvarModeloNoUri(Uri uri) {
        mostrarCarregando(true);
        AppExecutors.diskIO().execute(() -> {
            boolean sucesso = false;
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    PlanilhaHelper.escreverModeloVazio(os);
                    sucesso = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "salvarModelo: erro", e);
            }

            final boolean ok = sucesso;
            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);
                if (ok) {
                    Toast.makeText(this, "Modelo Excel salvo com sucesso!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Erro ao salvar o modelo. Tente novamente.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // =========================================================================
    // 2. IMPORTAR PLANILHA
    // =========================================================================

    private void iniciarPreviewImport(Uri uri) {
        mostrarCarregando(true);
        final String userId = sessionManager.getUserId();

        AppExecutors.diskIO().execute(() -> {
            PlanilhaHelper.ResultadoLeitura resultado = PlanilhaHelper.lerXlsx(this, uri, userId);

            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);

                if (resultado.mensagemErro != null) {
                    Toast.makeText(this, resultado.mensagemErro, Toast.LENGTH_LONG).show();
                    return;
                }

                if (resultado.itens.isEmpty()) {
                    Toast.makeText(this, "Nenhum item encontrado na planilha.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(this, PreviewImportActivity.class);
                intent.putExtra(PreviewImportActivity.EXTRA_ITENS, new java.util.ArrayList<>(resultado.itens));
                intent.putExtra(PreviewImportActivity.EXTRA_TOTAL_INVALIDOS, resultado.totalInvalidos);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        });
    }

    // =========================================================================
    // 3. EXPORTAR DESPENSA
    // =========================================================================

    private void exportarDespensaParaUri(Uri uri) {
        mostrarCarregando(true);
        final String userId = sessionManager.getUserId();

        AppExecutors.diskIO().execute(() -> {
            boolean sucesso = false;
            int total = 0;
            try {
                List<DespensaItem> itens = despensaRepository.listarAtivos(userId);
                total = itens.size();
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    PlanilhaHelper.escreverXlsx(itens, os);
                    sucesso = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "exportarDespensa: erro", e);
            }

            final boolean ok  = sucesso;
            final int     qty = total;
            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);
                if (ok) {
                    Toast.makeText(this, qty + " item(ns) exportado(s) com sucesso!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Erro ao exportar. Tente novamente.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // =========================================================================
    // 4. COMPARTILHAR WHATSAPP
    // =========================================================================

    private void compartilharWhatsApp() {
        mostrarCarregando(true);
        final String userId   = sessionManager.getUserId();
        final String nomeUser = sessionManager.getUserName();

        AppExecutors.diskIO().execute(() -> {
            List<DespensaItem> itens = despensaRepository.listarAtivos(userId);
            String texto = PlanilhaHelper.formatarParaWhatsApp(itens, nomeUser);

            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, texto);

                intent.setPackage("com.whatsapp");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    intent.setPackage(null);
                    startActivity(Intent.createChooser(intent, "Compartilhar via…"));
                }
            });
        });
    }

    private void mostrarCarregando(boolean carregando) {
        if (progressBar == null) return;
        progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
    }
}