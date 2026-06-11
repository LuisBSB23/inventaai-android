package com.example.inventaai.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.inventaai.data.model.DespensaItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 20 — Utilitário para leitura e escrita de arquivos CSV da despensa.
 *
 * Formato padrão: Nome;Categoria;Quantidade;Unidade;Validade
 *
 * Suporta delimitadores vírgula (,) e ponto-e-vírgula (;).
 * A detecção é automática com base na primeira linha do arquivo.
 */
public final class CsvHelper {

    private static final String TAG = Constants.LOG_TAG;

    /** Cabeçalho canônico do CSV exportado pelo app. */
    public static final String CSV_HEADER = "Nome;Categoria;Quantidade;Unidade;Validade";

    /** Resultado da leitura: lista de itens + contador de inválidos. */
    public static class ResultadoLeitura {
        public final List<DespensaItem> itens;
        public final int                totalInvalidos;
        public final String             mensagemErro; // null = sucesso

        public ResultadoLeitura(List<DespensaItem> itens, int totalInvalidos, String mensagemErro) {
            this.itens          = itens;
            this.totalInvalidos = totalInvalidos;
            this.mensagemErro   = mensagemErro;
        }
    }

    private CsvHelper() {}

    // =========================================================================
    // LEITURA
    // =========================================================================

    /**
     * Lê um arquivo CSV a partir de uma Uri (SAF) e converte as linhas em
     * objetos DespensaItem. Itens com dados inválidos são incluídos na lista
     * mas marcados com status "INVALIDO" para destaque visual na preview.
     *
     * @param context Contexto Android
     * @param uri     Uri retornada pelo SAF (ACTION_OPEN_DOCUMENT)
     * @param userId  ID do usuário logado — será atribuído a todos os itens
     * @return ResultadoLeitura com a lista e contagem de inválidos
     */
    public static ResultadoLeitura lerCsv(Context context, Uri uri, String userId) {
        List<DespensaItem> itens = new ArrayList<>();
        int invalidos = 0;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return new ResultadoLeitura(itens, 0, "Não foi possível abrir o arquivo.");
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String primeiraLinha = reader.readLine();
            if (primeiraLinha == null) {
                return new ResultadoLeitura(itens, 0, "O arquivo está vazio.");
            }

            // Detecta o delimitador automaticamente
            String delimitador = detectarDelimitador(primeiraLinha);

            // Pula a linha de cabeçalho (contém "Nome")
            boolean isPrimeiraLinhaHeader =
                    primeiraLinha.trim().toLowerCase().startsWith("nome");

            String linha;
            String primeiraLinhaParaProcessar = isPrimeiraLinhaHeader ? null : primeiraLinha;

            // Processa a primeira linha se não for cabeçalho
            if (primeiraLinhaParaProcessar != null) {
                DespensaItem item = processarLinha(primeiraLinhaParaProcessar, delimitador, userId);
                if (item != null) {
                    itens.add(item);
                    if ("INVALIDO".equals(item.getStatus())) invalidos++;
                }
            }

            // Processa demais linhas
            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                DespensaItem item = processarLinha(linha, delimitador, userId);
                if (item != null) {
                    itens.add(item);
                    if ("INVALIDO".equals(item.getStatus())) invalidos++;
                }
            }

            reader.close();
            Log.d(TAG, "CsvHelper.lerCsv: " + itens.size() + " itens lidos, "
                    + invalidos + " inválidos.");

        } catch (IOException e) {
            Log.e(TAG, "CsvHelper.lerCsv: erro de leitura", e);
            return new ResultadoLeitura(itens, invalidos,
                    "Erro ao ler o arquivo: " + e.getMessage());
        }

        return new ResultadoLeitura(itens, invalidos, null);
    }

    /**
     * Converte uma única linha CSV em DespensaItem.
     * Se campos obrigatórios forem inválidos, o item é retornado com
     * status "INVALIDO" para destaque visual na tela de pré-visualização.
     *
     * Colunas esperadas: Nome | Categoria | Quantidade | Unidade | Validade
     */
    private static DespensaItem processarLinha(String linha, String delimitador, String userId) {
        String[] partes = linha.split(delimitador, -1);

        // Precisa ter pelo menos Nome e Categoria
        if (partes.length < 2) return null;

        DespensaItem item   = new DespensaItem();
        boolean      valido = true;

        // Coluna 0 — Nome (obrigatório)
        String nome = partes[0].trim();
        if (nome.isEmpty()) {
            nome  = "(sem nome)";
            valido = false;
        }
        item.setNome(nome);

        // Coluna 1 — Categoria (obrigatório)
        String categoria = partes.length > 1 ? partes[1].trim() : "";
        if (categoria.isEmpty()) {
            categoria = "Outros"; // fallback visível
            valido    = false;
        }
        item.setCategoria(categoria);

        // Coluna 2 — Quantidade (obrigatório, numérico)
        double quantidade = 1.0;
        if (partes.length > 2 && !partes[2].trim().isEmpty()) {
            try {
                // Suporta vírgula decimal (ex: "1,5" → 1.5)
                quantidade = Double.parseDouble(partes[2].trim().replace(",", "."));
            } catch (NumberFormatException e) {
                valido = false;
            }
        } else {
            valido = false;
        }
        item.setQuantidade(quantidade);

        // Coluna 3 — Unidade (opcional, padrão "unid")
        String unidade = partes.length > 3 ? partes[3].trim() : "unid";
        if (unidade.isEmpty()) unidade = "unid";
        item.setUnidadeMedida(unidade);

        // Coluna 4 — Data de Validade (opcional)
        String validade = partes.length > 4 ? partes[4].trim() : null;
        if (validade != null && !validade.isEmpty()) {
            // Converte formatos comuns para YYYY-MM-DD
            validade = normalizarData(validade);
        } else {
            validade = null;
        }
        item.setDataValidade(validade);

        // Status: ATIVO se válido, INVALIDO se não
        item.setStatus(valido ? Constants.STATUS_ATIVO : "INVALIDO");
        item.setUserId(userId);

        return item;
    }

    // =========================================================================
    // ESCRITA
    // =========================================================================

    /**
     * Escreve uma lista de itens ativos no OutputStream fornecido pelo SAF
     * (ACTION_CREATE_DOCUMENT). O arquivo usa ponto-e-vírgula como separador.
     *
     * @param itens  Lista de itens a exportar
     * @param output OutputStream do arquivo de destino
     */
    public static void escreverCsv(List<DespensaItem> itens, OutputStream output) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.ISO_8859_1);

        // Cabeçalho
        writer.write(CSV_HEADER);
        writer.write("\n");

        for (DespensaItem item : itens) {
            writer.write(escaparCampo(item.getNome()));
            writer.write(";");
            writer.write(escaparCampo(item.getCategoria() != null ? item.getCategoria() : "Outros"));
            writer.write(";");
            writer.write(formatarQuantidade(item.getQuantidade()));
            writer.write(";");
            writer.write(escaparCampo(item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid"));
            writer.write(";");
            writer.write(escaparCampo(formatarDataParaExibicao(item.getDataValidade())));
            writer.write("\n");
        }

        writer.flush();
        Log.d(TAG, "CsvHelper.escreverCsv: " + itens.size() + " itens exportados.");
    }

    /**
     * Gera o CSV do modelo vazio (somente cabeçalho) com uma linha de exemplo.
     */
    public static void escreverModeloVazio(OutputStream output) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.ISO_8859_1);

        writer.write(CSV_HEADER);
        writer.write("\n");

        // Linha de exemplo para orientar o usuário
        writer.write("Arroz;Graos e Cereais;2;kg;31/12/2026\n");
        writer.write("Leite;Laticinios;1;L;15/07/2026\n");
        writer.write("Tomate;Hortifruti;0.5;kg;\n");

        writer.flush();
        Log.d(TAG, "CsvHelper.escreverModeloVazio: modelo gerado.");
    }

    // =========================================================================
    // FORMATAÇÃO PARA WHATSAPP
    // =========================================================================

    /**
     * Converte a lista de itens num texto formatado para compartilhamento.
     * Exemplo de saída:
     *   🛒 Minha Despensa (InventaAí)
     *   • 2x Arroz (Grãos e Cereais)
     *   • 1x Leite (Laticínios)
     */
    public static String formatarParaWhatsApp(List<DespensaItem> itens, String nomeUsuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *Despensa de ").append(nomeUsuario != null ? nomeUsuario : "Casa")
                .append("* — InventaAí\n\n");

        if (itens == null || itens.isEmpty()) {
            sb.append("_(despensa vazia)_");
            return sb.toString();
        }

        for (DespensaItem item : itens) {
            sb.append("• ");
            sb.append(formatarQuantidade(item.getQuantidade()));
            sb.append("x ");
            sb.append(item.getNome());
            if (item.getCategoria() != null && !item.getCategoria().isEmpty()) {
                sb.append(" (").append(item.getCategoria()).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n_Exportado pelo InventaAí_");
        return sb.toString();
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /** Detecta ; ou , como delimitador com base na primeira linha. */
    private static String detectarDelimitador(String linha) {
        int contaPV = contarOcorrencias(linha, ';');
        int contaVG = contarOcorrencias(linha, ',');
        return contaPV >= contaVG ? ";" : ",";
    }

    private static int contarOcorrencias(String s, char c) {
        int count = 0;
        for (char ch : s.toCharArray()) if (ch == c) count++;
        return count;
    }

    /**
     * Normaliza datas para YYYY-MM-DD.
     * Trata formatos: DD/MM/YYYY, DD-MM-YYYY, YYYY/MM/DD.
     */
    private static String normalizarData(String data) {
        // Já está no formato correto
        if (data.matches("\\d{4}-\\d{2}-\\d{2}")) return data;

        // DD/MM/YYYY ou DD-MM-YYYY
        if (data.matches("\\d{2}[/-]\\d{2}[/-]\\d{4}")) {
            String sep = data.contains("/") ? "/" : "-";
            String[] p = data.split(sep.equals("/") ? "/" : "-");
            return p[2] + "-" + p[1] + "-" + p[0];
        }

        // YYYY/MM/DD
        if (data.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return data.replace("/", "-");
        }

        // Formato não reconhecido — retorna como está
        return data;
    }

    /** Escapa campos que contenham ponto-e-vírgula envolvendo-os em aspas. */
    private static String escaparCampo(String valor) {
        if (valor == null) return "";
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    /** Formata quantidade: sem decimais se for inteiro (ex: 2.0 → "2"). */
    private static String formatarQuantidade(double q) {
        if (q == Math.floor(q) && !Double.isInfinite(q)) {
            return String.valueOf((long) q);
        }
        return String.valueOf(q).replace(".", ",");
    }
    /**
     * Converte data de armazenamento (YYYY-MM-DD) para exibicao (DD/MM/YYYY).
     * Se ja estiver no formato DD/MM/YYYY ou vazia, retorna como esta.
     */
    private static String formatarDataParaExibicao(String data) {
        if (data == null || data.isEmpty()) return "";
        // YYYY-MM-DD -> DD/MM/YYYY
        if (data.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = data.split("-");
            return p[2] + "/" + p[1] + "/" + p[0];
        }
        return data;
    }

}