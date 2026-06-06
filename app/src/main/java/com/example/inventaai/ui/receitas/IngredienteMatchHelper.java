package com.example.inventaai.ui.receitas;

import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.util.UnitConverterUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IngredienteMatchHelper {

    private IngredienteMatchHelper() {}

    // Pluralizações simples em PT-BR para remover antes de comparar
    private static final String[] SUFIXOS_PLURAL = { "oes", "ões", "es", "s" };

    // Unidades culinárias reconhecidas pelo extrator de texto
    private static final String REGEX_UNIDADE =
            "(xícaras?|xicaras?|colheres? de sopa|colher de sopa|colheres? de chá|colher de chá|"
                    + "colheres? de cha|colher de cha|copos?|ml|l\\b|litros?|kg|g\\b|"
                    + "unidades?|un\\b|fatias?|dentes?|pitadas?|folhas?|ramos?|galhos?)";

    public static List<IngredienteMatch> cruzar(List<String> ingredientesReceita,
                                                List<DespensaItem> itensDespensa) {
        List<IngredienteMatch> resultado = new ArrayList<>();
        if (ingredientesReceita == null) return resultado;

        for (String textoReceita : ingredientesReceita) {
            resultado.add(cruzarUm(textoReceita, itensDespensa));
        }
        return resultado;
    }

    // ── Cruzamento de um ingrediente ─────────────────────────────────────────

    private static IngredienteMatch cruzarUm(String textoReceita,
                                             List<DespensaItem> itensDespensa) {
        String nomeNorm  = extrairNome(textoReceita);
        double qtdPedida = extrairQuantidadeConvertida(textoReceita, nomeNorm);

        DespensaItem melhor      = null;
        double       melhorScore = 0;

        for (DespensaItem item : itensDespensa) {
            double score = similaridade(nomeNorm, normalizar(item.getNome()));
            if (score > melhorScore) {
                melhorScore = score;
                melhor      = item;
            }
        }

        if (melhorScore < 0.55 || melhor == null) {
            return new IngredienteMatch(textoReceita, nomeNorm, qtdPedida,
                    null, IngredienteMatch.Status.FALTA, qtdPedida);
        }

        double qtdDisponivel = melhor.getQuantidade();

        if (qtdPedida <= 0 || qtdDisponivel >= qtdPedida) {
            return new IngredienteMatch(textoReceita, nomeNorm, qtdPedida,
                    melhor, IngredienteMatch.Status.POSSUI, 0);
        } else {
            double falta = qtdPedida - qtdDisponivel;
            return new IngredienteMatch(textoReceita, nomeNorm, qtdPedida,
                    melhor, IngredienteMatch.Status.INSUFICIENTE, falta);
        }
    }

    // ── Extração de nome e quantidade ────────────────────────────────────────

    static String extrairNome(String texto) {
        if (texto == null || texto.isEmpty()) return "";

        // Se o formato for "Nome - quantidade", pega a parte antes do " - "
        String[] partesDash = texto.split(" - ", 2);
        String base = partesDash[0].trim();

        // Remove números e unidades do início: "2 xícaras de farinha" → "farinha"
        String semNumero = base.replaceFirst(
                "^[\\d,.]+\\s*(xícaras?|colheres?|colher|copos?|kg|g|ml|l|un|unidades?|pitadas?|dentes?|fatias?|folhas?|galhos?|ramos?)\\s*(de\\s*)?",
                "").trim();

        if (!semNumero.isEmpty()) base = semNumero;

        return normalizar(base);
    }

    /**
     * Sprint 15: extrai a quantidade e converte usando {@link UnitConverterUtils}.
     * Ex: "2 xícaras de farinha" → converter("farinha", "xicara", 2.0) → 300g
     */
    static double extrairQuantidadeConvertida(String texto, String nomeNorm) {
        if (texto == null) return 0;

        double qtdBruta = extrairQuantidade(texto);
        if (qtdBruta <= 0) return 0;

        String unidade = extrairUnidade(texto);
        if (unidade == null || unidade.isEmpty()) return qtdBruta;

        return UnitConverterUtils.converter(nomeNorm, unidade, qtdBruta);
    }

    static double extrairQuantidade(String texto) {
        if (texto == null) return 0;
        Pattern p = Pattern.compile("([\\d]+(?:[,.]\\d+)?)");
        Matcher m = p.matcher(texto);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "."));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * Sprint 15: extrai a unidade de medida do texto do ingrediente.
     * Ex: "2 xícaras de farinha" → "xícaras"
     */
    static String extrairUnidade(String texto) {
        if (texto == null || texto.isEmpty()) return null;
        Pattern p = Pattern.compile("[\\d,.]+\\s*(" + REGEX_UNIDADE + ")", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    // ── Normalização de strings ───────────────────────────────────────────────

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        String limpo = semAcento.toLowerCase().replaceAll("[^a-z\\s]", "").trim();
        return removerPlural(limpo);
    }

    private static String removerPlural(String palavra) {
        if (palavra.length() <= 3) return palavra;
        String[] partes = palavra.split("\\s+");
        String ultima = partes[partes.length - 1];
        for (String sufixo : SUFIXOS_PLURAL) {
            if (ultima.endsWith(sufixo) && ultima.length() > sufixo.length() + 2) {
                partes[partes.length - 1] = ultima.substring(0, ultima.length() - sufixo.length());
                break;
            }
        }
        return String.join(" ", partes);
    }

    // ── Similaridade de strings (Jaccard por bigramas) ───────────────────────

    static double similaridade(String a, String b) {
        if (a == null || b == null) return 0;
        if (a.equals(b)) return 1.0;
        if (a.contains(b) || b.contains(a)) return 0.85;

        List<String> biA = bigramas(a);
        List<String> biB = bigramas(b);
        if (biA.isEmpty() && biB.isEmpty()) return 1.0;
        if (biA.isEmpty() || biB.isEmpty()) return 0;

        int intersecao = 0;
        for (String bg : biA) { if (biB.contains(bg)) intersecao++; }
        int uniao = biA.size() + biB.size() - intersecao;
        return (double) intersecao / uniao;
    }

    private static List<String> bigramas(String s) {
        List<String> bg = new ArrayList<>();
        for (int i = 0; i < s.length() - 1; i++) {
            bg.add(s.substring(i, i + 2));
        }
        return bg;
    }
}