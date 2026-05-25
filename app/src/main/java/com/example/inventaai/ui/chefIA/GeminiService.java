package com.example.inventaai.ui.chefIA;

import android.util.Log;

import com.example.inventaai.BuildConfig;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.model.ReceitaResponse;
import com.example.inventaai.util.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GeminiService — encapsula toda a comunicação com a API do Gemini (Sprint 4).
 *
 * Uso:
 *   GeminiService service = new GeminiService();
 *   service.gerarReceita(itens, new GeminiService.ReceitaCallback() {
 *       public void onSucesso(ReceitaResponse receita) { ... }
 *       public void onErro(String mensagem)            { ... }
 *   });
 *
 * O callback é sempre chamado na thread de rede do OkHttp.
 * Use runOnUiThread() na Activity para atualizar a UI.
 */
public class GeminiService {

    // ──────────────────────────────────────────────────────────────────────────
    // Constantes
    // ──────────────────────────────────────────────────────────────────────────

    private static final String TAG = Constants.LOG_TAG;

    /**
     * Endpoint Gemini 2.0 Flash-Lite — modelo leve confirmado disponível na conta.
     */
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=";

    private static final MediaType JSON_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    // ──────────────────────────────────────────────────────────────────────────
    // Interface de callback
    // ──────────────────────────────────────────────────────────────────────────

    public interface ReceitaCallback {
        /** Chamado quando a IA retorna uma receita válida. */
        void onSucesso(ReceitaResponse receita);

        /** Chamado em qualquer erro (rede, parsing, chave inválida etc.). */
        void onErro(String mensagem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cliente HTTP — instância única (thread-safe)
    // ──────────────────────────────────────────────────────────────────────────

    private final OkHttpClient client;
    private final Gson gson;

    public GeminiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)   // Gemini pode demorar
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Método principal
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Gera uma receita com base nos itens da despensa fornecidos.
     *
     * @param itens    Lista de DespensaItem disponíveis (não deve estar vazia).
     * @param callback Resultado: sucesso com ReceitaResponse ou mensagem de erro.
     */
    public void gerarReceita(List<DespensaItem> itens, ReceitaCallback callback) {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        // Validação da chave antes de fazer a chamada
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onErro("Chave da API não configurada. Adicione GEMINI_API_KEY ao local.properties.");
            return;
        }

        String prompt      = construirPrompt(itens);
        String requestBody = construirCorpoRequisicao(prompt);

        Log.d(TAG, "GeminiService: enviando prompt (" + itens.size() + " ingredientes)");

        Request request = new Request.Builder()
                .url(ENDPOINT + apiKey)
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GeminiService: falha na requisição", e);
                callback.onErro("Sem conexão com a internet. Verifique sua rede e tente novamente.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    if (!r.isSuccessful()) {
                        String body = r.body() != null ? r.body().string() : "(sem corpo)";
                        Log.e(TAG, "GeminiService: HTTP " + r.code() + " — " + body);
                        callback.onErro(traduzirErroHttp(r.code()));
                        return;
                    }

                    String bodyString = r.body() != null ? r.body().string() : "";
                    Log.d(TAG, "GeminiService: resposta recebida.");

                    ReceitaResponse receita = extrairReceita(bodyString);
                    callback.onSucesso(receita);

                } catch (Exception e) {
                    Log.e(TAG, "GeminiService: erro ao processar resposta", e);
                    callback.onErro("Não foi possível processar a receita. Tente novamente.");
                }
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Monta a lista de ingredientes e o prompt completo enviado à IA.
     */
    private String construirPrompt(List<DespensaItem> itens) {
        StringBuilder lista = new StringBuilder();
        for (DespensaItem item : itens) {
            lista.append("- ")
                    .append(item.getNome())
                    .append(" (")
                    .append(formatarQuantidade(item.getQuantidade()))
                    .append(" ")
                    .append(item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid")
                    .append(")\n");
        }

        return "Atue como um Chef de cozinha profissional. "
                + "Tenho os seguintes ingredientes na minha despensa:\n"
                + lista
                + "\nCrie uma receita deliciosa priorizando os ingredientes listados. "
                + "Responda APENAS com um objeto JSON puro, sem markdown, sem blocos de código, "
                + "sem explicações antes ou depois, contendo exatamente estes campos: "
                + "\"titulo\" (string), "
                + "\"tempo_preparo\" (string, ex: \"30 min\"), "
                + "\"porcoes\" (string, ex: \"4 porções\"), "
                + "\"dificuldade\" (string: Fácil, Médio ou Difícil), "
                + "\"ingredientes\" (array de strings, cada item com nome e quantidade), "
                + "\"passos\" (array de strings com as instruções numeradas). "
                + "Idioma: Português do Brasil.";
    }

    /**
     * Serializa o prompt no formato JSON que a API do Gemini espera.
     */
    private String construirCorpoRequisicao(String prompt) {
        JsonObject text   = new JsonObject();
        text.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(text);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        // Parâmetro de geração: temperature baixa para JSON mais previsível
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.7);
        genConfig.addProperty("maxOutputTokens", 1024);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        return gson.toJson(body);
    }

    /**
     * Extrai o texto gerado da resposta do Gemini e converte para ReceitaResponse.
     * Estrutura esperada:
     *   { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
     */
    private ReceitaResponse extrairReceita(String responseJson) {
        JsonObject root       = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray  candidates = root.getAsJsonArray("candidates");
        String textoGerado    = candidates
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        Log.d(TAG, "GeminiService: texto gerado = " + textoGerado);

        // Remove possíveis marcadores markdown que o modelo possa ter inserido
        String jsonLimpo = textoGerado
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        return gson.fromJson(jsonLimpo, ReceitaResponse.class);
    }

    /**
     * Retorna mensagem amigável para os códigos HTTP mais comuns da API Google.
     */
    private String traduzirErroHttp(int code) {
        switch (code) {
            case 400: return "Requisição inválida enviada à IA. Contate o suporte.";
            case 401:
            case 403: return "Chave da API inválida ou sem permissão. Verifique o GEMINI_API_KEY.";
            case 429: return "Limite de requisições da API atingido. Aguarde um momento e tente novamente.";
            case 500:
            case 503: return "O serviço do Gemini está temporariamente indisponível. Tente novamente mais tarde.";
            default:  return "Erro inesperado (HTTP " + code + "). Tente novamente.";
        }
    }

    /** Formata a quantidade sem casas decimais desnecessárias. */
    private String formatarQuantidade(double qtd) {
        if (qtd == Math.floor(qtd)) {
            return String.valueOf((int) qtd);
        }
        return String.valueOf(qtd);
    }
}