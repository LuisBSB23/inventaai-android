package com.example.inventaai.util;

import android.content.Context;
import android.content.res.Configuration;

import androidx.core.content.ContextCompat;

import com.example.inventaai.R;

/**
 * CategoryColorHelper — Sprint 19-A, Tarefa #3
 *
 * Mapeia o nome da categoria de um item da despensa para um par de cores:
 *   - containerColor : cor de fundo do FrameLayout circular do ícone
 *   - onContainerColor: cor do ícone (ImageView) sobre esse fundo
 *
 * Ambas as cores respeitam WCAG AA (contraste mínimo 4.5:1) e possuem
 * variantes para tema claro e escuro definidas em colors.xml.
 *
 * Uso no DespensaAdapter:
 * <pre>
 *   CategoryColorHelper.Colors cores = CategoryColorHelper.getColors(ctx, item.getCategoria());
 *   holder.flIconContainer.setBackgroundTintList(ColorStateList.valueOf(cores.containerColor));
 *   holder.ivItemIcon.setImageTintList(ColorStateList.valueOf(cores.onContainerColor));
 * </pre>
 */
public final class CategoryColorHelper {

    // Chaves canônicas de categoria (em lower-case sem acento para comparação robusta)
    private static final String CAT_HORTIFRUTI  = "hortifruti";
    private static final String CAT_LATICINIOS  = "laticinios";
    private static final String CAT_LATICINIOS2 = "laticínios";   // com acento
    private static final String CAT_CARNES      = "carnes";
    private static final String CAT_BEBIDAS     = "bebidas";
    private static final String CAT_GRAOS       = "graos";
    private static final String CAT_GRAOS2      = "grãos";        // com acento
    private static final String CAT_CEREAIS     = "cereais";
    private static final String CAT_CONGELADOS  = "congelados";

    /** Evita instanciação — classe utilitária estática. */
    private CategoryColorHelper() {}

    /** Par de cores retornado pelo helper. */
    public static final class Colors {
        public final int containerColor;
        public final int onContainerColor;

        Colors(int container, int onContainer) {
            this.containerColor   = container;
            this.onContainerColor = onContainer;
        }
    }

    /**
     * Retorna o par de cores adequado para a categoria fornecida.
     * Detecta automaticamente o tema atual (claro/escuro).
     *
     * @param ctx      Contexto necessário para resolver recursos de cor
     * @param categoria String da categoria como salva no banco (pode ser null)
     * @return {@link Colors} com containerColor e onContainerColor resolvidos
     */
    public static Colors getColors(Context ctx, String categoria) {
        boolean isDark = isNightMode(ctx);
        String cat = categoria != null ? categoria.trim().toLowerCase() : "";

        if (cat.contains(CAT_HORTIFRUTI)) {
            return isDark
                    ? new Colors(
                    color(ctx, R.color.colorCatHortifrutiContainerDark),
                    color(ctx, R.color.colorCatHortifrutiOnContainerDark))
                    : new Colors(
                    color(ctx, R.color.colorCatHortifrutiContainer),
                    color(ctx, R.color.colorCatHortifrutiOnContainer));
        }

        if (cat.contains(CAT_LATICINIOS) || cat.contains(CAT_LATICINIOS2)) {
            return isDark
                    ? new Colors(
                    color(ctx, R.color.colorCatLaticiniosContainerDark),
                    color(ctx, R.color.colorCatLaticiniosOnContainerDark))
                    : new Colors(
                    color(ctx, R.color.colorCatLaticiniosContainer),
                    color(ctx, R.color.colorCatLaticiniosOnContainer));
        }

        if (cat.contains(CAT_CARNES)) {
            return isDark
                    ? new Colors(
                    color(ctx, R.color.colorCatCarnesContainerDark),
                    color(ctx, R.color.colorCatCarnesOnContainerDark))
                    : new Colors(
                    color(ctx, R.color.colorCatCarnesContainer),
                    color(ctx, R.color.colorCatCarnesOnContainer));
        }

        if (cat.contains(CAT_BEBIDAS)) {
            return isDark
                    ? new Colors(
                    color(ctx, R.color.colorCatBebidasContainerDark),
                    color(ctx, R.color.colorCatBebidasOnContainerDark))
                    : new Colors(
                    color(ctx, R.color.colorCatBebidasContainer),
                    color(ctx, R.color.colorCatBebidasOnContainer));
        }

        if (cat.contains(CAT_GRAOS) || cat.contains(CAT_GRAOS2) || cat.contains(CAT_CEREAIS)) {
            return isDark
                    ? new Colors(
                    color(ctx, R.color.colorCatGraosContainerDark),
                    color(ctx, R.color.colorCatGraosOnContainerDark))
                    : new Colors(
                    color(ctx, R.color.colorCatGraosContainer),
                    color(ctx, R.color.colorCatGraosOnContainer));
        }

        if (cat.contains(CAT_CONGELADOS)) {
            return isDark
                    ? new Colors(
                    color(ctx, R.color.colorCatCongeladosContainerDark),
                    color(ctx, R.color.colorCatCongeladosOnContainerDark))
                    : new Colors(
                    color(ctx, R.color.colorCatCongeladosContainer),
                    color(ctx, R.color.colorCatCongeladosOnContainer));
        }

        // Fallback — "Outros" ou qualquer categoria não mapeada
        return isDark
                ? new Colors(
                color(ctx, R.color.colorCatOutrosContainerDark),
                color(ctx, R.color.colorCatOutrosOnContainerDark))
                : new Colors(
                color(ctx, R.color.colorCatOutrosContainer),
                color(ctx, R.color.colorCatOutrosOnContainer));
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private static int color(Context ctx, int resId) {
        return ContextCompat.getColor(ctx, resId);
    }

    private static boolean isNightMode(Context ctx) {
        int uiMode = ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }
}