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
import com.example.inventaai.util.CsvHelper;
import com.example.inventaai.util.SessionManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.OutputStream;
import java.util.List;

/**
 * Sprint 20 — Tela central de Gestão da Despensa via Planilha CSV.
 *
 * Fluxos disponíveis:
 *  1. Baixar Modelo  → gera CSV vazio com cabeçalhos e exemplos (SAF)
 *  2. Importar       → abre explorador, lê CSV, abre PreviewImportActivity
 *  3. Exportar       → salva todos itens ativos em CSV no dispositivo (SAF)
 *  4. WhatsApp       → formata lista e dispara Intent para WhatsApp
 */
public class SincronizacaoActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_TAG;

    // ── Views ─────────────────────────────────────────────────────────────────
    private CircularProgressIndicator progressBar;

    // ── Dependências ──────────────────────────────────────────────────────────
    private DespensaRepository despensaRepository;
    private SessionManager     sessionManager;

    // ── SAF Launchers ─────────────────────────────────────────────────────────

    /** Abre seletor para SALVAR o modelo vazio. */
    private final ActivityResultLauncher<String> salvarModeloLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/csv"),
                    uri -> {
                        if (uri != null) salvarModeloNoUri(uri);
                    });

    private final ActivityResultLauncher<Intent> abrirCsvLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) iniciarPreviewImport(uri);
                        }
                    });

    /** Abre seletor para SALVAR a exportação da despensa. */
    private final ActivityResultLauncher<String> salvarExportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/csv"),
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

    // =========================================================================
    // CONFIGURAÇÃO INICIAL
    // =========================================================================

    private void vincularViews() {
        progressBar = findViewById(R.id.progressBar);
    }

    private void configurarBotoes() {
        // Voltar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Card: Baixar Modelo
        findViewById(R.id.cardBaixarModelo).setOnClickListener(v ->
                salvarModeloLauncher.launch("modelo_despensa.csv"));

        // Card: Importar Planilha
        // Card: Importar Planilha
        findViewById(R.id.cardImportar).setOnClickListener(v -> abrirSeletorCsv());

        // Card: Exportar Despensa
        findViewById(R.id.cardExportar).setOnClickListener(v -> {
            String dataHoje = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            salvarExportLauncher.launch("despensa_" + dataHoje + ".csv");
        });

        // Card: Compartilhar WhatsApp
        findViewById(R.id.cardWhatsApp).setOnClickListener(v -> compartilharWhatsApp());
    }

    // =========================================================================
    // SELETOR DE ARQUIVO CSV — cadeia de fallback
    // =========================================================================

    private void abrirSeletorCsv() {
        // Tentativa 1: ACTION_GET_CONTENT com multiplos MIME types
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "text/plain",
                "application/csv",
                "application/octet-stream",
                "application/vnd.ms-excel"   // .csv no Windows e reconhecido assim
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Verifica se ha algum app que resolve GET_CONTENT
        if (intent.resolveActivity(getPackageManager()) != null) {
            abrirCsvLauncher.launch(Intent.createChooser(intent, "Selecionar arquivo CSV"));
            return;
        }

        // Fallback: ACTION_OPEN_DOCUMENT com */* — DocumentsUI sempre resolve isso
        Intent fallback = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fallback.setType("*/*");
        fallback.addCategory(Intent.CATEGORY_OPENABLE);
        abrirCsvLauncher.launch(Intent.createChooser(fallback, "Selecionar arquivo CSV"));
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
                    CsvHelper.escreverModeloVazio(os);
                    os.close();
                    sucesso = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "salvarModelo: erro", e);
            }

            final boolean ok = sucesso;
            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);
                if (ok) {
                    Toast.makeText(this,
                            "Modelo salvo! Edite o arquivo e depois importe.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this,
                            "Erro ao salvar o modelo. Tente novamente.",
                            Toast.LENGTH_SHORT).show();
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
            CsvHelper.ResultadoLeitura resultado = CsvHelper.lerCsv(this, uri, userId);
            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);

                if (resultado.mensagemErro != null) {
                    Toast.makeText(this, resultado.mensagemErro, Toast.LENGTH_LONG).show();
                    return;
                }

                if (resultado.itens.isEmpty()) {
                    Toast.makeText(this,
                            "Nenhum item encontrado no arquivo CSV.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Passa para a tela de pré-visualização
                Intent intent = new Intent(this, PreviewImportActivity.class);
                intent.putExtra(PreviewImportActivity.EXTRA_ITENS,
                        new java.util.ArrayList<>(resultado.itens));
                intent.putExtra(PreviewImportActivity.EXTRA_TOTAL_INVALIDOS,
                        resultado.totalInvalidos);
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
                    CsvHelper.escreverCsv(itens, os);
                    os.close();
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
                    Toast.makeText(this,
                            qty + " item(ns) exportado(s) com sucesso!",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this,
                            "Erro ao exportar. Tente novamente.",
                            Toast.LENGTH_SHORT).show();
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
            String texto = CsvHelper.formatarParaWhatsApp(itens, nomeUser);

            AppExecutors.mainThread().execute(() -> {
                mostrarCarregando(false);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, texto);

                // Tenta abrir diretamente o WhatsApp; cai para chooser se não instalado
                intent.setPackage("com.whatsapp");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // WhatsApp não instalado — abre seletor genérico
                    intent.setPackage(null);
                    startActivity(Intent.createChooser(intent, "Compartilhar via…"));
                }
            });
        });
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void mostrarCarregando(boolean carregando) {
        if (progressBar == null) return;
        progressBar.setVisibility(carregando ? View.VISIBLE : View.GONE);
    }
}