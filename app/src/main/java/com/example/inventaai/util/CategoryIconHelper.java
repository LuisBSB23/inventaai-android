package com.example.inventaai.util;

import com.example.inventaai.R;

public final class CategoryIconHelper {

    private CategoryIconHelper() {}

    /**
     * Retorna o resource ID do drawable correspondente à categoria informada.
     *
     * @param categoria Nome da categoria (case-insensitive, nullable).
     * @return ID do drawable R.drawable.ic_cat_*.
     */
    public static int getIcon(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return R.drawable.ic_cat_outros;
        }

        // Normaliza: remove espaços extras e compara sem diferenciar maiúsculas
        switch (categoria.trim().toLowerCase()) {

            case "hortifruti":
                return R.drawable.ic_cat_hortifruti;

            case "laticínios":
            case "laticinios":    // fallback sem acento (digitação manual)
                return R.drawable.ic_cat_laticinios;

            case "carnes":
                return R.drawable.ic_cat_carnes;

            case "grãos e cereais":
            case "graos e cereais":   // fallback sem acento
            case "grãos":
            case "graos":
                return R.drawable.ic_cat_graos;

            case "bebidas":
                return R.drawable.ic_cat_bebidas;

            case "congelados":
                return R.drawable.ic_cat_congelados;

            case "temperos":
                return R.drawable.ic_cat_temperos;

            default:
                return R.drawable.ic_cat_outros;
        }
    }
}