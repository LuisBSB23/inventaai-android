package com.example.inventaai.util;

import java.util.HashMap;
import java.util.Map;

public final class UnitConverterUtils {

    private UnitConverterUtils() {}

    // =========================================================================
    // Tabela de densidade por ingrediente (g por ml)
    // Ingredientes não listados usam densidade 1.0 (água)
    // =========================================================================

    private static final Map<String, Double> DENSIDADE = new HashMap<String, Double>() {{
        put("farinha",     0.60);
        put("acucar",      0.85);
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

    private static final Map<String, Double> VOLUME_ML = new HashMap<String, Double>() {{
        put("xicara",           240.0);
        put("xicaras",          240.0);
        put("xícara",           240.0);
        put("xícaras",          240.0);
        put("copo",             200.0);
        put("copos",            200.0);
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
        put("ml",               1.0);
        // Sprint 16: "l" e variações consolidadas como 1000 ml
        put("l",             1000.0);
        put("litro",         1000.0);
        put("litros",        1000.0);
        put("g",                1.0);
        put("kg",            1000.0);
        put("unidade",          1.0);
        put("unidades",         1.0);
        put("unid",             1.0);
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
    // API pública — conversão geral
    // =========================================================================

    public static double converter(String nomeIngrediente, String unidade, double quantidade) {
        if (unidade == null || unidade.isEmpty()) return quantidade;

        String unidNorm = normalizar(unidade);
        Double volBase  = VOLUME_ML.get(unidNorm);

        if (volBase == null) {
            for (Map.Entry<String, Double> entry : VOLUME_ML.entrySet()) {
                if (unidNorm.contains(entry.getKey()) || entry.getKey().contains(unidNorm)) {
                    volBase = entry.getValue();
                    break;
                }
            }
        }

        if (volBase == null) return quantidade;

        double volumeTotal = quantidade * volBase;

        if ("g".equals(unidNorm) || "kg".equals(unidNorm)) return volumeTotal;

        if (unidNorm.matches("un(idades?)?|unid|fatias?|dentes?|pitadas?|folhas?|ramos?|galhos?")) {
            return quantidade;
        }

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
    // Sprint 16 — Normalização para comparação Receita × Despensa
    // =========================================================================

    public static double normalizarParaGramas(double quantidade, String unidade) {
        if (unidade == null || unidade.isEmpty()) return quantidade;
        String u = unidade.trim().toLowerCase(java.util.Locale.getDefault());
        if (u.equals("kg")) return quantidade * 1000.0;
        if (u.equals("g"))  return quantidade;
        return quantidade; // unidade não é de massa; retorna sem conversão
    }

    public static double normalizarParaMl(double quantidade, String unidade) {
        if (unidade == null || unidade.isEmpty()) return quantidade;
        String u = unidade.trim().toLowerCase(java.util.Locale.getDefault());
        if (u.equals("l") || u.equals("litro") || u.equals("litros")) return quantidade * 1000.0;
        if (u.equals("ml")) return quantidade;
        return quantidade; // unidade não é de volume; retorna sem conversão
    }

    public static boolean mesmaSistema(String unidadeA, String unidadeB) {
        if (unidadeA == null || unidadeB == null) return false;
        String a = unidadeA.trim().toLowerCase(java.util.Locale.getDefault());
        String b = unidadeB.trim().toLowerCase(java.util.Locale.getDefault());

        boolean aMassa  = a.equals("kg") || a.equals("g");
        boolean bMassa  = b.equals("kg") || b.equals("g");
        boolean aVolume = a.equals("l") || a.equals("litro") || a.equals("litros") || a.equals("ml");
        boolean bVolume = b.equals("l") || b.equals("litro") || b.equals("litros") || b.equals("ml");

        return (aMassa && bMassa) || (aVolume && bVolume);
    }


    public static double[] normalizarPar(double qtdDespensa, String unidDespensa,
                                         double qtdReceita,  String unidReceita) {
        if (unidDespensa == null) unidDespensa = "";
        if (unidReceita  == null) unidReceita  = "";

        String dU = unidDespensa.trim().toLowerCase(java.util.Locale.getDefault());
        String rU = unidReceita.trim().toLowerCase(java.util.Locale.getDefault());

        boolean dMassa  = dU.equals("kg") || dU.equals("g");
        boolean rMassa  = rU.equals("kg") || rU.equals("g");
        boolean dVolume = dU.equals("l") || dU.equals("litro") || dU.equals("litros") || dU.equals("ml");
        boolean rVolume = rU.equals("l") || rU.equals("litro") || rU.equals("litros") || rU.equals("ml");

        // Ambos são unidades de massa → normalizar para gramas
        if (dMassa && rMassa) {
            return new double[]{
                    normalizarParaGramas(qtdDespensa, unidDespensa),
                    normalizarParaGramas(qtdReceita,  unidReceita)
            };
        }

        // Ambos são unidades de volume → normalizar para ml
        if (dVolume && rVolume) {
            return new double[]{
                    normalizarParaMl(qtdDespensa, unidDespensa),
                    normalizarParaMl(qtdReceita,  unidReceita)
            };
        }

        // Sistemas incompatíveis ou unidades não conversíveis → retorna sem alterar
        return new double[]{ qtdDespensa, qtdReceita };
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
        if (DENSIDADE.containsKey(ingNorm)) return DENSIDADE.get(ingNorm);
        for (Map.Entry<String, Double> entry : DENSIDADE.entrySet()) {
            if (ingNorm.contains(entry.getKey()) || entry.getKey().contains(ingNorm)) {
                return entry.getValue();
            }
        }
        return 1.0;
    }
}