package com.example.inventaai.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public final class UnitConverterUtils {

    private UnitConverterUtils() {}

    // =========================================================================
    // Tabela de densidade por ingrediente (g por ml)
    // Ingredientes não listados usam densidade 1.0 (água)
    // =========================================================================

    private static final Map<String, Double> DENSIDADE = new HashMap<String, Double>() {{
        put("farinha",     0.60); // farinha de trigo
        put("acucar",      0.85); // açúcar cristal
        put("sal",         1.20);
        put("manteiga",    0.91);
        put("oleo",        0.92);
        put("arroz",       0.78);
        put("feijao",      0.80);
        put("aveia",       0.40);
        put("cacau",       0.50);
        put("amido",       0.60);
        put("leite",       1.03);
        put("creme",       1.00);
        put("iogurte",     1.05);
    }};

    // =========================================================================
    // Tabela base de volumes em ml por unidade
    // =========================================================================

    /** Volumes-base em ml para cada unidade padrão. */
    private static final Map<String, Double> VOLUME_ML = new HashMap<String, Double>() {{
        // Xícaras
        put("xicara",           240.0);
        put("xicaras",          240.0);
        put("xícara",           240.0);
        put("xícaras",          240.0);
        put("copo",             200.0);
        put("copos",            200.0);
        // Colheres
        put("colher de sopa",   15.0);
        put("colheres de sopa", 15.0);
        put("colher sopa",      15.0);
        put("colheres sopa",    15.0);
        put("cs",               15.0);
        put("colher de cha",    5.0);
        put("colheres de cha",  5.0);
        put("colher cha",       5.0);
        put("colheres cha",     5.0);
        put("colher de chá",    5.0);
        put("colheres de chá",  5.0);
        put("cc",               5.0);
        // Métricas
        put("ml",               1.0);
        put("l",             1000.0);
        put("litro",         1000.0);
        put("litros",        1000.0);
        put("g",                1.0);   // g já é a unidade base
        put("kg",            1000.0);
        // Unidades de contagem
        put("unidade",          1.0);
        put("unidades",         1.0);
        put("un",               1.0);
        put("fatia",            1.0);
        put("fatias",           1.0);
        put("dente",            1.0);
        put("dentes",           1.0);
        put("pitada",           0.5);
        put("pitadas",          0.5);
        put("folha",            1.0);
        put("folhas",           1.0);
        put("ramo",             1.0);
        put("ramos",            1.0);
        put("galho",            1.0);
        put("galhos",           1.0);
    }};

    // =========================================================================
    // API pública
    // =========================================================================

    public static double converter(String nomeIngrediente, String unidade, double quantidade) {
        if (unidade == null || unidade.isEmpty()) return quantidade;

        String unidNorm = normalizar(unidade);
        Double volBase  = VOLUME_ML.get(unidNorm);

        if (volBase == null) {
            // Tenta correspondência parcial
            for (Map.Entry<String, Double> entry : VOLUME_ML.entrySet()) {
                if (unidNorm.contains(entry.getKey()) || entry.getKey().contains(unidNorm)) {
                    volBase = entry.getValue();
                    break;
                }
            }
        }

        if (volBase == null) return quantidade; // unidade desconhecida

        double volumeTotal = quantidade * volBase;

        // Unidades já em g ou kg → não aplica densidade
        if ("g".equals(unidNorm) || "kg".equals(unidNorm)) return volumeTotal;

        // Para unidades de contagem (un, fatia, dente…) → não converte
        if (unidNorm.matches("un(idades?)?|fatias?|dentes?|pitadas?|folhas?|ramos?|galhos?")) {
            return quantidade;
        }

        // Aplica densidade para converter ml → g
        String ingNorm = normalizar(nomeIngrediente != null ? nomeIngrediente : "");
        Double densidade = encontrarDensidade(ingNorm);

        return volumeTotal * densidade;
    }

    public static double converterParaMl(String unidade, double quantidade) {
        if (unidade == null || unidade.isEmpty()) return quantidade;
        String unidNorm = normalizar(unidade);
        Double volBase  = VOLUME_ML.get(unidNorm);
        if (volBase == null) return quantidade;
        return quantidade * volBase;
    }

    public static String normalizarUnidade(String texto) {
        if (texto == null) return "";
        return normalizar(texto);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String s = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return s.toLowerCase().replaceAll("[^a-z\\s]", "").trim();
    }

    private static double encontrarDensidade(String ingNorm) {
        if (ingNorm.isEmpty()) return 1.0;
        // Correspondência exata
        if (DENSIDADE.containsKey(ingNorm)) return DENSIDADE.get(ingNorm);
        // Correspondência parcial
        for (Map.Entry<String, Double> entry : DENSIDADE.entrySet()) {
            if (ingNorm.contains(entry.getKey()) || entry.getKey().contains(ingNorm)) {
                return entry.getValue();
            }
        }
        return 1.0; // densidade default (água)
    }
}