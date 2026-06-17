package com.example.inventaai.ui.receitas;

import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.util.UnitConverterUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IngredienteMatchHelper {

    private IngredienteMatchHelper() {}

    // =========================================================================
    // Cruzamento simples (legado — mantido para compatibilidade)
    // =========================================================================

    public static List<IngredienteMatch> cruzar(List<String> ingredientesReceita,
                                                List<DespensaItem> itensDespensa) {
        return cruzarInterno(ingredientesReceita, itensDespensa, false);
    }

    // =========================================================================
    // Sprint 16 — Cruzamento com normalização matemática de unidades
    // =========================================================================

    public static List<IngredienteMatch> cruzarComNormalizacao(List<String> ingredientesReceita,
                                                               List<DespensaItem> itensDespensa) {
        return cruzarInterno(ingredientesReceita, itensDespensa, true);
    }

    // =========================================================================
    // Implementação interna
    // =========================================================================

    private static List<IngredienteMatch> cruzarInterno(List<String> ingredientesReceita,
                                                        List<DespensaItem> itensDespensa,
                                                        boolean normalizarUnidades) {
        List<IngredienteMatch> resultado = new ArrayList<>();

        for (String ingredienteStr : ingredientesReceita) {
            // Parse da string de ingrediente: "Nome - 200 g" ou "Nome"
            String nomeIngrediente   = extrairNome(ingredienteStr);
            double qtdPedida         = extrairQuantidade(ingredienteStr);
            String unidadePedida     = extrairUnidade(ingredienteStr);

            // Busca o item correspondente na despensa (por similaridade de nome)
            DespensaItem itemEncontrado = buscarItemPorNome(nomeIngrediente, itensDespensa);

            // Variáveis para armazenar o estado final antes de instanciar o objeto
            IngredienteMatch.Status statusFinal;
            double qtdFaltanteFinal;

            if (itemEncontrado == null) {
                // Ingrediente não encontrado na despensa
                statusFinal = IngredienteMatch.Status.FALTA;
                qtdFaltanteFinal = qtdPedida;
            } else {
                double qtdDespensa = itemEncontrado.getQuantidade();
                String unidDespensa = itemEncontrado.getUnidadeMedida();

                double qtdDespensaComp = qtdDespensa;
                double qtdPedidaComp   = qtdPedida;

                if (normalizarUnidades && qtdPedida > 0) {
                    // Sprint 16: normaliza para a menor unidade comum antes de comparar
                    double[] normalizados = UnitConverterUtils.normalizarPar(
                            qtdDespensa, unidDespensa,
                            qtdPedida,   unidadePedida);
                    qtdDespensaComp = normalizados[0];
                    qtdPedidaComp   = normalizados[1];
                }

                if (qtdPedida <= 0) {
                    // Receita não especificou quantidade → apenas verifica presença
                    statusFinal = IngredienteMatch.Status.POSSUI;
                    qtdFaltanteFinal = 0;
                } else if (qtdDespensaComp >= qtdPedidaComp) {
                    statusFinal = IngredienteMatch.Status.POSSUI;
                    qtdFaltanteFinal = 0;
                } else {
                    statusFinal = IngredienteMatch.Status.INSUFICIENTE;
                    // Faltante em unidades normalizadas
                    qtdFaltanteFinal = qtdPedidaComp - qtdDespensaComp;
                }
            }

            // Instanciação correta utilizando o construtor completo
            IngredienteMatch match = new IngredienteMatch(
                    ingredienteStr,
                    nomeIngrediente,
                    qtdPedida,
                    itemEncontrado,
                    statusFinal,
                    qtdFaltanteFinal
            );

            resultado.add(match);
        }

        return resultado;
    }

    // =========================================================================
    // Helpers de parse
    // =========================================================================

    /** Extrai o nome do ingrediente (parte antes do " - "). */
    private static String extrairNome(String ingredienteStr) {
        if (ingredienteStr == null) return "";
        String[] partes = ingredienteStr.split(" - ", 2);
        return partes[0].trim();
    }

    /** Extrai a quantidade numérica do ingrediente, se presente. */
    private static double extrairQuantidade(String ingredienteStr) {
        if (ingredienteStr == null || !ingredienteStr.contains(" - ")) return 0;
        String parteQtd = ingredienteStr.split(" - ", 2)[1].trim();
        // Procura o primeiro número (inteiro ou decimal) na string
        Pattern p = Pattern.compile("[\\d]+([.,][\\d]+)?");
        Matcher m = p.matcher(parteQtd);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group().replace(",", "."));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /** Extrai a unidade de medida do ingrediente, se presente. */
    private static String extrairUnidade(String ingredienteStr) {
        if (ingredienteStr == null || !ingredienteStr.contains(" - ")) return "";
        String parteQtd = ingredienteStr.split(" - ", 2)[1].trim();
        // Remove o número e retorna o que sobrou (a unidade)
        String unidade = parteQtd.replaceAll("[\\d]+([.,][\\d]+)?\\s*", "").trim();
        return unidade.isEmpty() ? "" : unidade;
    }

    private static DespensaItem buscarItemPorNome(String nomeIngrediente,
                                                  List<DespensaItem> itensDespensa) {
        if (nomeIngrediente == null || nomeIngrediente.isEmpty()) return null;
        String nomeNorm = normalizarTexto(nomeIngrediente);

        DespensaItem melhor = null;
        int melhorScore = 0;

        for (DespensaItem item : itensDespensa) {
            if (item.getNome() == null) continue;
            String itemNorm = normalizarTexto(item.getNome());

            int score = 0;
            if (itemNorm.equals(nomeNorm)) {
                score = 3; // correspondência exata
            } else if (itemNorm.contains(nomeNorm) || nomeNorm.contains(itemNorm)) {
                score = 2; // correspondência parcial
            } else {
                // Verifica se ao menos a primeira palavra coincide
                String[] palavrasIngrediente = nomeNorm.split("\\s+");
                String[] palavrasItem        = itemNorm.split("\\s+");
                if (palavrasIngrediente.length > 0 && palavrasItem.length > 0
                        && palavrasIngrediente[0].equals(palavrasItem[0])) {
                    score = 1;
                }
            }

            if (score > melhorScore) {
                melhorScore = score;
                melhor      = item;
            }
        }

        return melhorScore > 0 ? melhor : null;
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return s.toLowerCase(Locale.getDefault()).trim();
    }
}