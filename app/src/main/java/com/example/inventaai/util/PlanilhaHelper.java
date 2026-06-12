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

                // O modelo padrão possui 10 linhas de instruções/categorias seguidas
                // do cabeçalho na linha 11 (índice 10). Os itens começam na linha 12
                // (índice 11). Caso a planilha não siga esse modelo, cai no fallback
                // que detecta o cabeçalho dinamicamente.
                final int LINHA_DADOS_MODELO = 11; // índice base-0 da primeira linha de dados

                int startRow = LINHA_DADOS_MODELO;

                // Fallback: se o arquivo não tiver linhas suficientes ou não tiver
                // o cabeçalho esperado na linha 11 (índice 10), busca "nome" em
                // qualquer linha e começa logo abaixo.
                if (rowList.size() <= LINHA_DADOS_MODELO) {
                    // Planilha curta — procura cabeçalho na primeira linha com "nome"
                    startRow = 0;
                    for (int i = 0; i < rowList.size(); i++) {
                        String cell0 = getTextoCelula(rowList.get(i), 0);
                        if (cell0.toLowerCase().startsWith("nome")) {
                            startRow = i + 1;
                            break;
                        }
                    }
                }

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

    /**
     * Gera o modelo padrão de importação, idêntico ao arquivo modelo_despensa.xlsx:
     *
     * Linhas 1–9  → instruções de categorias e unidades (leitura humana)
     * Linha 10    → vazia (separador)
     * Linha 11    → cabeçalho: Nome | Categoria | Quantidade | Unidade | Validade
     * Linha 12+   → dados do usuário (em branco no modelo)
     *
     * O leitor {@link #lerXlsx} espera exatamente esse layout e começa a
     * processar itens a partir do índice 11 (linha 12).
     */
    public static void escreverModeloVazio(OutputStream output) throws IOException {
        try (Workbook wb = new Workbook(output, "InventaAí", "1.0")) {
            Worksheet ws = wb.newWorksheet("Planilha1");

            // ── Bloco de instruções (linhas 1–9, índices 0–8) ────────────────
            ws.value(0, 0, "Categorias diponiveis:");
            ws.value(0, 2, "Unidade:");

            ws.value(1, 0, "Hortifruti");
            ws.value(1, 2, "kg - L - Unid");

            ws.value(2, 0, "Laticinios");
            ws.value(2, 2, "Ex: 1.5 (vira ML e L se começar com 0)");

            ws.value(3, 0, "Carnes");
            ws.value(3, 2, "Para ML e Gramas");

            ws.value(4, 0, "Graos e Cereais");
            ws.value(4, 2, "Ex: 0.25 com KG ou L na unidade (0 com casas decimais)");

            ws.value(5, 0, "Bebidas");

            ws.value(6, 0, "Congelados");
            ws.value(6, 2, "Validade");

            ws.value(7, 0, "Temperos");
            ws.value(7, 2, "DIA/MÊS/ANO");

            ws.value(8, 0, "Outros");

            // Linha 10 (índice 9) — vazia (separador visual)

            // ── Cabeçalho na linha 11 (índice 10) ───────────────────────────
            ws.value(10, 0, "Nome");
            ws.value(10, 1, "Categoria");
            ws.value(10, 2, "Quantidade");
            ws.value(10, 3, "Unidade");
            ws.value(10, 4, "Validade");

            // Linha 12 (índice 11) em diante → preenchida pelo usuário
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