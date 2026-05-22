package com.example.inventaai.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Métodos utilitários para manipulação e formatação de datas no InventaAí.
 * Formato interno (banco de dados): YYYY-MM-DD
 * Formato de exibição:              dd/MM/yyyy
 */
public final class DateUtils {

    private static final String FMT_DB      = "yyyy-MM-dd";
    private static final String FMT_DISPLAY = "dd/MM/yyyy";
    private static final String TAG         = Constants.LOG_TAG;

    private DateUtils() {}

    // -------------------------------------------------------------------------
    // hoje()
    // -------------------------------------------------------------------------

    /**
     * Retorna a data atual no formato YYYY-MM-DD.
     * Exemplo: "2026-05-21"
     */
    public static String hoje() {
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_DB, Locale.getDefault());
        return sdf.format(new Date());
    }

    // -------------------------------------------------------------------------
    // calcularDiasRestantes()
    // -------------------------------------------------------------------------

    /**
     * Calcula quantos dias faltam (ou já passaram, como negativo) até a data
     * de validade informada.
     *
     * @param dataValidade Data no formato YYYY-MM-DD.
     * @return Número de dias restantes. Negativo = vencido. Integer.MAX_VALUE em erro.
     */
    public static int calcularDiasRestantes(String dataValidade) {
        if (dataValidade == null || dataValidade.isEmpty()) {
            return Integer.MAX_VALUE; // sem validade definida → sem alerta
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FMT_DB, Locale.getDefault());
            sdf.setLenient(false);

            Date validade = sdf.parse(dataValidade);

            // Zera horas/minutos/segundos da data atual para comparar só dias
            Calendar hoje = Calendar.getInstance();
            hoje.set(Calendar.HOUR_OF_DAY, 0);
            hoje.set(Calendar.MINUTE, 0);
            hoje.set(Calendar.SECOND, 0);
            hoje.set(Calendar.MILLISECOND, 0);

            long diffMs   = validade.getTime() - hoje.getTimeInMillis();
            long diffDias = TimeUnit.MILLISECONDS.toDays(diffMs);

            return (int) diffDias;

        } catch (ParseException e) {
            Log.e(TAG, "calcularDiasRestantes: data inválida '" + dataValidade + "'", e);
            return Integer.MAX_VALUE;
        }
    }

    // -------------------------------------------------------------------------
    // formatarParaExibicao()
    // -------------------------------------------------------------------------

    /**
     * Converte uma data do formato YYYY-MM-DD para dd/MM/yyyy.
     *
     * @param dataValidade Data no formato YYYY-MM-DD.
     * @return Data formatada como dd/MM/yyyy, ou a string original em caso de erro.
     */
    public static String formatarParaExibicao(String dataValidade) {
        if (dataValidade == null || dataValidade.isEmpty()) return "";
        try {
            SimpleDateFormat sdfDb      = new SimpleDateFormat(FMT_DB,      Locale.getDefault());
            SimpleDateFormat sdfDisplay = new SimpleDateFormat(FMT_DISPLAY,  Locale.getDefault());
            sdfDb.setLenient(false);
            Date data = sdfDb.parse(dataValidade);
            return sdfDisplay.format(data);
        } catch (ParseException e) {
            Log.e(TAG, "formatarParaExibicao: data inválida '" + dataValidade + "'", e);
            return dataValidade; // devolve o original para não quebrar a UI
        }
    }

    // -------------------------------------------------------------------------
    // getStatusAlerta()
    // -------------------------------------------------------------------------

    /**
     * Determina o nível de alerta de vencimento com base nos dias restantes.
     *
     * <ul>
     *   <li>VERMELHO → vencido ou vencendo hoje (diasRestantes ≤ 0)</li>
     *   <li>AMARELO  → vence dentro de {@link Constants#DIAS_ALERTA_AMARELO} dias</li>
     *   <li>VERDE    → prazo confortável</li>
     * </ul>
     *
     * @param diasRestantes Valor retornado por {@link #calcularDiasRestantes(String)}.
     * @return "VERMELHO", "AMARELO" ou "VERDE".
     */
    public static String getStatusAlerta(int diasRestantes) {
        if (diasRestantes <= Constants.DIAS_ALERTA_VERMELHO) {
            return "VERMELHO";
        } else if (diasRestantes <= Constants.DIAS_ALERTA_AMARELO) {
            return "AMARELO";
        } else {
            return "VERDE";
        }
    }

    // -------------------------------------------------------------------------
    // adicionarDias() — auxiliar interno usado pelo Repository
    // -------------------------------------------------------------------------

    /**
     * Retorna a data atual + {@code dias} no formato YYYY-MM-DD.
     * Usado internamente em queries de "próximos a vencer".
     */
    public static String hojeAdicionarDias(int dias) {
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_DB, Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dias);
        return sdf.format(cal.getTime());
    }
}