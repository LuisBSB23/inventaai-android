package com.example.inventaai.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.inventaai.data.model.DespensaItem;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sprint 20 — Utilitário para leitura e escrita de planilhas Excel (.xlsx) da despensa.
 * Utiliza a biblioteca FastExcel para máxima performance e baixo consumo de memória.
 */
public final class PlanilhaHelper {

    private static final String TAG = Constants.LOG_TAG;

    public static class ResultadoLeitura {
        public final List<DespensaItem> itens;
        public final int                totalInvalidos;
        public final String             mensagemErro;

        public ResultadoLeitura(List<DespensaItem> itens, int totalInvalidos, String mensagemErro) {
            this.itens          = itens;
            this.totalInvalidos = totalInvalidos;
            this.mensagemErro   = mensagemErro;
        }
    }

    private PlanilhaHelper() {}

    // =========================================================================
    // LEITURA (XLSX)
    // =========================================================================

    public static ResultadoLeitura lerXlsx(Context context, Uri uri, String userId) {
        List<DespensaItem> itens = new ArrayList<>();
        int invalidos = 0;

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             ReadableWorkbook wb = new ReadableWorkbook(is)) {

            Sheet sheet = wb.getFirstSheet();
            try (Stream<Row> rows = sheet.openStream()) {
                List<Row> rowList = rows.collect(Collectors.toList());
                if (rowList.isEmpty()) {
                    return new ResultadoLeitura(itens, 0, "A planilha está vazia.");
                }

                // Verifica se a primeira linha é o cabeçalho
                boolean isPrimeiraLinhaHeader = false;
                Row primeiraLinha = rowList.get(0);
                if (primeiraLinha.getCellCount() > 0) {
                    String cell0 = getTextoCelula(primeiraLinha, 0);
                    if (cell0.toLowerCase().startsWith("nome")) {
                        isPrimeiraLinhaHeader = true;
                    }
                }

                int startRow = isPrimeiraLinhaHeader ? 1 : 0;
                for (int i = startRow; i < rowList.size(); i++) {
                    Row row = rowList.get(i);
                    if (row.getPhysicalCellCount() == 0) continue;

                    DespensaItem item = processarLinha(row, userId);
                    if (item != null) {
                        itens.add(item);
                        if ("INVALIDO".equals(item.getStatus())) invalidos++;
                    }
                }
            }

            Log.d(TAG, "PlanilhaHelper.lerXlsx: " + itens.size() + " itens lidos, " + invalidos + " inválidos.");

        } catch (Exception e) {
            Log.e(TAG, "PlanilhaHelper.lerXlsx: erro de leitura", e);
            return new ResultadoLeitura(itens, invalidos, "Erro ao ler a planilha: " + e.getMessage());
        }

        return new ResultadoLeitura(itens, invalidos, null);
    }

    private static DespensaItem processarLinha(Row row, String userId) {
        if (row.getCellCount() < 2) return null;

        DespensaItem item = new DespensaItem();
        boolean valido = true;

        // Coluna 0 — Nome (obrigatório)
        String nome = getTextoCelula(row, 0);
        if (nome.isEmpty()) { nome = "(sem nome)"; valido = false; }
        item.setNome(nome);

        // Coluna 1 — Categoria (obrigatório)
        String categoria = getTextoCelula(row, 1);
        if (categoria.isEmpty()) { categoria = "Outros"; valido = false; }
        item.setCategoria(categoria);

        // Coluna 2 — Quantidade (obrigatório)
        double quantidade = 1.0;
        String strQtd = getTextoCelula(row, 2).replace(",", ".");
        if (!strQtd.isEmpty()) {
            try {
                quantidade = Double.parseDouble(strQtd);
            } catch (NumberFormatException e) {
                valido = false;
            }
        } else {
            valido = false;
        }
        item.setQuantidade(quantidade);

        // Coluna 3 — Unidade
        String unidade = getTextoCelula(row, 3);
        if (unidade.isEmpty()) unidade = "unid";
        item.setUnidadeMedida(unidade);

        // Coluna 4 — Validade
        String validade = getTextoCelula(row, 4);
        if (!validade.isEmpty()) {
            validade = normalizarData(validade);
        } else {
            validade = null;
        }
        item.setDataValidade(validade);

        item.setStatus(valido ? Constants.STATUS_ATIVO : "INVALIDO");
        item.setUserId(userId);

        return item;
    }

    private static String getTextoCelula(Row row, int index) {
        try {
            String text = row.getCellText(index);
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // =========================================================================
    // ESCRITA (XLSX)
    // =========================================================================

    public static void escreverXlsx(List<DespensaItem> itens, OutputStream output) throws IOException {
        try (Workbook wb = new Workbook(output, "InventaAí", "1.0")) {
            Worksheet ws = wb.newWorksheet("Despensa");

            gerarCabecalho(ws);

            int row = 1;
            for (DespensaItem item : itens) {
                ws.value(row, 0, item.getNome());
                ws.value(row, 1, item.getCategoria() != null ? item.getCategoria() : "Outros");
                ws.value(row, 2, item.getQuantidade());
                ws.value(row, 3, item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid");
                ws.value(row, 4, formatarDataParaExibicao(item.getDataValidade()));
                row++;
            }
        }
        Log.d(TAG, "PlanilhaHelper.escreverXlsx: " + itens.size() + " itens exportados.");
    }

    public static void escreverModeloVazio(OutputStream output) throws IOException {
        try (Workbook wb = new Workbook(output, "InventaAí", "1.0")) {
            Worksheet ws = wb.newWorksheet("Despensa Modelo");

            gerarCabecalho(ws);

            // Linhas de exemplo
            ws.value(1, 0, "Arroz"); ws.value(1, 1, "Graos e Cereais"); ws.value(1, 2, 2.0); ws.value(1, 3, "kg"); ws.value(1, 4, "31/12/2026");
            ws.value(2, 0, "Leite"); ws.value(2, 1, "Laticinios"); ws.value(2, 2, 1.0); ws.value(2, 3, "L"); ws.value(2, 4, "15/07/2026");
            ws.value(3, 0, "Tomate"); ws.value(3, 1, "Hortifruti"); ws.value(3, 2, 0.5); ws.value(3, 3, "kg"); ws.value(3, 4, "20/07/2026");
        }
        Log.d(TAG, "PlanilhaHelper.escreverModeloVazio: modelo gerado.");
    }

    private static void gerarCabecalho(Worksheet ws) {
        ws.value(0, 0, "Nome");
        ws.value(0, 1, "Categoria");
        ws.value(0, 2, "Quantidade");
        ws.value(0, 3, "Unidade");
        ws.value(0, 4, "Validade");
    }

    // =========================================================================
    // HELPERS E WHATSAPP
    // =========================================================================

    public static String formatarParaWhatsApp(List<DespensaItem> itens, String nomeUsuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *Despensa de ").append(nomeUsuario != null ? nomeUsuario : "Casa").append("* — InventaAí\n\n");

        if (itens == null || itens.isEmpty()) {
            sb.append("_(despensa vazia)_");
            return sb.toString();
        }

        for (DespensaItem item : itens) {
            sb.append("• ").append(formatarQuantidade(item.getQuantidade())).append("x ").append(item.getNome());
            if (item.getCategoria() != null && !item.getCategoria().isEmpty()) {
                sb.append(" (").append(item.getCategoria()).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n_Exportado pelo InventaAí_");
        return sb.toString();
    }

    private static String normalizarData(String data) {
        if (data.matches("\\d{4}-\\d{2}-\\d{2}")) return data;
        if (data.matches("\\d{2}[/-]\\d{2}[/-]\\d{4}")) {
            String sep = data.contains("/") ? "/" : "-";
            String[] p = data.split(sep.equals("/") ? "/" : "-");
            return p[2] + "-" + p[1] + "-" + p[0];
        }
        if (data.matches("\\d{4}/\\d{2}/\\d{2}")) return data.replace("/", "-");
        return data;
    }

    private static String formatarQuantidade(double q) {
        if (q == Math.floor(q) && !Double.isInfinite(q)) return String.valueOf((long) q);
        return String.valueOf(q).replace(".", ",");
    }

    private static String formatarDataParaExibicao(String data) {
        if (data == null || data.isEmpty()) return "";
        if (data.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = data.split("-");
            return p[2] + "/" + p[1] + "/" + p[0];
        }
        return data;
    }
}