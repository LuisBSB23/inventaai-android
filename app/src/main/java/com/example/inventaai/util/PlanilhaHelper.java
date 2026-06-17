package com.example.inventaai.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.inventaai.data.model.DespensaItem;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Sprint 20 — Utilitário para leitura e escrita de planilhas Excel (.xlsx) da despensa.
 *
 * LEITURA: implementação própria via ZipInputStream + SAXParser, sem dependência
 * de fastexcel-reader nem de aalto-xml/StAX (ausentes no Android runtime).
 * Um .xlsx é um ZIP contendo XMLs — parseamos diretamente:
 *   xl/sharedStrings.xml  → tabela de strings compartilhadas
 *   xl/worksheets/sheet1.xml → células da primeira planilha
 *
 * ESCRITA: mantém o fastexcel (só para escrita, sem StAX).
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
    // LEITURA (XLSX) — ZipInputStream + SAXParser, sem StAX
    // =========================================================================

    public static ResultadoLeitura lerXlsx(Context context, Uri uri, String userId) {
        List<DespensaItem> itens = new ArrayList<>();
        int invalidos = 0;

        try (InputStream raw = context.getContentResolver().openInputStream(uri)) {
            if (raw == null) {
                return new ResultadoLeitura(itens, 0, "Não foi possível abrir o arquivo.");
            }

            // Lê o ZIP inteiro em memória primeiro (necessário para percorrer
            // duas entradas: sharedStrings e sheet1, sem reabrir o stream)
            byte[] zipBytes = raw.readAllBytes();

            // ── 1. Extrai a tabela de strings compartilhadas ─────────────────
            List<String> sharedStrings = extrairSharedStrings(zipBytes);

            // ── 2. Extrai as linhas da primeira planilha ─────────────────────
            List<String[]> linhas = extrairLinhas(zipBytes, sharedStrings);

            if (linhas.isEmpty()) {
                return new ResultadoLeitura(itens, 0, "A planilha está vazia.");
            }

            // ── 3. Determina linha de início dos dados ───────────────────────
            // Modelo padrão: cabeçalho na linha 11 (índice 10), dados a partir
            // do índice 11. Fallback: busca a linha com "nome" na coluna A.
            final int LINHA_DADOS_MODELO = 11;
            int startRow = LINHA_DADOS_MODELO;

            if (linhas.size() <= LINHA_DADOS_MODELO) {
                startRow = 0;
                for (int i = 0; i < linhas.size(); i++) {
                    String[] cols = linhas.get(i);
                    if (cols.length > 0 && cols[0].toLowerCase().startsWith("nome")) {
                        startRow = i + 1;
                        break;
                    }
                }
            }

            // ── 4. Processa cada linha de dados ──────────────────────────────
            for (int i = startRow; i < linhas.size(); i++) {
                String[] cols = linhas.get(i);
                // Ignora linhas completamente vazias
                boolean vazia = true;
                for (String c : cols) { if (c != null && !c.isEmpty()) { vazia = false; break; } }
                if (vazia) continue;

                DespensaItem item = processarColunas(cols, userId);
                if (item != null) {
                    itens.add(item);
                    if ("INVALIDO".equals(item.getStatus())) invalidos++;
                }
            }

            Log.d(TAG, "PlanilhaHelper.lerXlsx: " + itens.size()
                    + " itens lidos, " + invalidos + " inválidos.");

        } catch (Exception e) {
            Log.e(TAG, "PlanilhaHelper.lerXlsx: erro de leitura", e);
            return new ResultadoLeitura(itens, invalidos,
                    "Erro ao ler a planilha: " + e.getMessage());
        }

        return new ResultadoLeitura(itens, invalidos, null);
    }

    // ── Extrai xl/sharedStrings.xml ──────────────────────────────────────────

    private static List<String> extrairSharedStrings(byte[] zipBytes) throws Exception {
        final List<String> strings = new ArrayList<>();
        processarEntradaZip(zipBytes, "xl/sharedStrings.xml", stream -> {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(stream, new DefaultHandler() {
                private final StringBuilder sb = new StringBuilder();
                private boolean dentroT = false;

                @Override
                public void startElement(String uri, String localName,
                                         String qName, Attributes attrs) {
                    if ("t".equals(qName) || "t".equals(localName)) {
                        sb.setLength(0);
                        dentroT = true;
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    if (dentroT) sb.append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if ("t".equals(qName) || "t".equals(localName)) {
                        strings.add(sb.toString());
                        dentroT = false;
                    }
                }
            });
        });
        return strings;
    }

    // ── Extrai xl/worksheets/sheet1.xml ─────────────────────────────────────

    private static List<String[]> extrairLinhas(byte[] zipBytes,
                                                List<String> sharedStrings) throws Exception {
        // Descobre quantas colunas existem para alocar o array corretamente
        // Usamos 5 colunas fixas (A–E = Nome, Categoria, Quantidade, Unidade, Validade)
        final int NUM_COLS = 5;
        final List<String[]> linhas = new ArrayList<>();

        processarEntradaZip(zipBytes, "xl/worksheets/sheet1.xml", stream -> {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(stream, new DefaultHandler() {

                private String[] linhaAtual = null;
                private int     colAtual   = -1;
                private boolean isShared   = false;
                private boolean isInline   = false;
                private int     rowAtual   = -1;
                private final StringBuilder cellValue = new StringBuilder();

                /** Converte referência de célula (ex: "C5") para índice de coluna (0-based). */
                private int colIndex(String ref) {
                    int col = 0;
                    for (char c : ref.toCharArray()) {
                        if (Character.isLetter(c)) col = col * 26 + (Character.toUpperCase(c) - 'A' + 1);
                        else break;
                    }
                    return col - 1;
                }

                /** Extrai número de linha da referência (ex: "C5" → 5). */
                private int rowIndex(String ref) {
                    String num = ref.replaceAll("[A-Za-z]", "");
                    try { return Integer.parseInt(num) - 1; } catch (Exception e) { return -1; }
                }

                @Override
                public void startElement(String uri, String localName,
                                         String qName, Attributes attrs) {
                    String name = qName.isEmpty() ? localName : qName;
                    if ("row".equals(name)) {
                        linhaAtual = new String[NUM_COLS];
                        String rAttr = attrs.getValue("r");
                        try { rowAtual = rAttr != null ? Integer.parseInt(rAttr) - 1 : -1; }
                        catch (Exception e) { rowAtual = -1; }

                    } else if ("c".equals(name) && linhaAtual != null) {
                        String ref = attrs.getValue("r");
                        colAtual = ref != null ? colIndex(ref) : -1;
                        String t  = attrs.getValue("t");
                        isShared = "s".equals(t);
                        isInline = "inlineStr".equals(t);
                        cellValue.setLength(0);

                    } else if (("v".equals(name) || "t".equals(name)) && linhaAtual != null) {
                        cellValue.setLength(0);
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    cellValue.append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    String name = qName.isEmpty() ? localName : qName;

                    if (("v".equals(name) || ("t".equals(name) && isInline))
                            && linhaAtual != null && colAtual >= 0 && colAtual < NUM_COLS) {
                        String raw = cellValue.toString().trim();
                        if (isShared) {
                            try {
                                int idx = Integer.parseInt(raw);
                                raw = idx < sharedStrings.size() ? sharedStrings.get(idx) : "";
                            } catch (NumberFormatException e) { raw = ""; }
                        }
                        linhaAtual[colAtual] = raw;

                    } else if ("row".equals(name) && linhaAtual != null) {
                        // Preenche nulos com string vazia
                        for (int i = 0; i < NUM_COLS; i++) {
                            if (linhaAtual[i] == null) linhaAtual[i] = "";
                        }
                        linhas.add(linhaAtual);
                        linhaAtual = null;
                    }
                }
            });
        });

        return linhas;
    }

    // ── Percorre o ZIP e processa uma entrada específica ─────────────────────

    @FunctionalInterface
    interface StreamConsumer {
        void accept(InputStream stream) throws Exception;
    }

    private static void processarEntradaZip(byte[] zipBytes,
                                            String entryName,
                                            StreamConsumer consumer) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    consumer.accept(zis);
                    return;
                }
                zis.closeEntry();
            }
        }
        // Entrada não encontrada — silencioso (sharedStrings pode não existir
        // se todas as células forem numéricas)
    }

    // ── Processa as colunas de uma linha em um DespensaItem ──────────────────

    private static DespensaItem processarColunas(String[] cols, String userId) {
        String nome      = cols.length > 0 && cols[0] != null ? cols[0].trim() : "";
        String categoria = cols.length > 1 && cols[1] != null ? cols[1].trim() : "";
        String strQtd    = cols.length > 2 && cols[2] != null ? cols[2].trim() : "";
        String unidade   = cols.length > 3 && cols[3] != null ? cols[3].trim() : "";
        String validade  = cols.length > 4 && cols[4] != null ? cols[4].trim() : "";

        DespensaItem item = new DespensaItem();
        boolean valido = true;

        // Nome
        if (nome.isEmpty()) { nome = "(sem nome)"; valido = false; }
        item.setNome(nome);

        // Categoria
        if (categoria.isEmpty()) { categoria = "Outros"; valido = false; }
        item.setCategoria(categoria);

        // Quantidade
        double quantidade = 1.0;
        strQtd = strQtd.replace(",", ".");
        if (!strQtd.isEmpty()) {
            try { quantidade = Double.parseDouble(strQtd); }
            catch (NumberFormatException e) { valido = false; }
        } else {
            valido = false;
        }
        item.setQuantidade(quantidade);

        // Unidade
        if (unidade.isEmpty()) unidade = "unid";
        item.setUnidadeMedida(unidade);

        // Validade
        item.setDataValidade(validade.isEmpty() ? null : normalizarData(validade));

        item.setStatus(valido ? Constants.STATUS_ATIVO : "INVALIDO");
        item.setUserId(userId);

        return item;
    }

    // =========================================================================
    // ESCRITA (XLSX) — fastexcel (não usa StAX, apenas escrita)
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
     */
    public static void escreverModeloVazio(OutputStream output) throws IOException {
        try (Workbook wb = new Workbook(output, "InventaAí", "1.0")) {
            Worksheet ws = wb.newWorksheet("Planilha1");

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
            // Linha 10 (índice 9) — vazia
            ws.value(10, 0, "Nome");
            ws.value(10, 1, "Categoria");
            ws.value(10, 2, "Quantidade");
            ws.value(10, 3, "Unidade");
            ws.value(10, 4, "Validade");
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