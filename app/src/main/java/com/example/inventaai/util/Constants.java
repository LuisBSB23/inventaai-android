package com.example.inventaai.util;

/**
 * Constantes globais do aplicativo InventaAí.
 * Centralizar aqui evita "magic strings/numbers" espalhados pelo código.
 */
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

    /** Itens com até N dias para vencer recebem alerta AMARELO. */
    public static final int DIAS_ALERTA_AMARELO = 3;

    /** Itens com 0 ou menos dias (vencidos) recebem alerta VERMELHO. */
    public static final int DIAS_ALERTA_VERMELHO = 0;

    // -------------------------------------------------------------------------
    // Tag de log padrão
    // -------------------------------------------------------------------------

    public static final String LOG_TAG = "InventaAi";
}