package com.example.inventaai.util;

public final class Constants {

    private Constants() {}

    // -------------------------------------------------------------------------
    // Status dos itens da despensa
    // -------------------------------------------------------------------------

    /** Item disponível na despensa. */
    public static final String STATUS_ATIVO      = "ATIVO";

    /** Item retirado por consumo normal. */
    public static final String STATUS_CONSUMIDO  = "CONSUMIDO";

    /** Item retirado por vencimento ou descarte. */
    public static final String STATUS_DESCARTADO = "DESCARTADO";

    // -------------------------------------------------------------------------
    // Limiares de alerta de vencimento
    // -------------------------------------------------------------------------

    public static final int DIAS_ALERTA_AMARELO = 7;

    public static final int DIAS_ALERTA_VERMELHO = 0;

    // -------------------------------------------------------------------------
    // Tag de log padrão
    // -------------------------------------------------------------------------

    public static final String LOG_TAG = "InventaAi";
}