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
import com.google.gson.JsonSyntaxException;

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

public class GeminiService {

    private static final String TAG = Constants.LOG_TAG;

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    private static final MediaType JSON_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    public interface ReceitaCallback {
        void onSucesso(ReceitaResponse receita);
        void onErro(String mensagem);
    }

    private final OkHttpClient client;
    private final Gson gson;

    public GeminiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    // =========================================================================
    // GERAR RECEITA COM ITENS (sem categoria específica)
    // =========================================================================

    public void gerarReceita(List<DespensaItem> itens, ReceitaCallback callback) {
        // Sprint 16: usa o prompt refinado sem restrição de categoria
        enviarRequisicao(construirPrompt(itens, null), callback);
    }

    // =========================================================================
    // SPRINT 15/16: GERAR RECEITA COM CATEGORIA
    // =========================================================================

    public void gerarReceitaComCategoria(List<DespensaItem> itens,
                                         String categoria,
                                         ReceitaCallback callback) {
        enviarRequisicao(construirPrompt(itens, categoria), callback);
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private void enviarRequisicao(String prompt, ReceitaCallback callback) {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "GeminiService: GEMINI_API_KEY está vazia no BuildConfig.");
            callback.onErro("Chave da API não configurada. Adicione GEMINI_API_KEY ao local.properties.");
            return;
        }

        String requestBody = construirCorpoRequisicao(prompt);
        Log.d(TAG, "GeminiService: enviando prompt...");

        Request request = new Request.Builder()
                .url(ENDPOINT + apiKey)
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GeminiService: falha de rede — " + e.getMessage(), e);
                callback.onErro("Sem conexão com a internet. Verifique sua rede e tente novamente.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    String bodyString = r.body() != null ? r.body().string() : "";

                    if (!r.isSuccessful()) {
                        Log.e(TAG, "GeminiService: ERRO HTTP " + r.code());
                        Log.e(TAG, "GeminiService: Body: " + bodyString);
                        String mensagemApi = extrairMensagemDeErro(bodyString);
                        if (mensagemApi != null) Log.e(TAG, "API error: " + mensagemApi);
                        callback.onErro(traduzirErroHttp(r.code()));
                        return;
                    }

                    Log.d(TAG, "GeminiService: HTTP 200 — processando...");
                    ReceitaResponse receita = extrairReceita(bodyString, callback);
                    if (receita != null) callback.onSucesso(receita);

                } catch (Exception e) {
                    Log.e(TAG, "GeminiService: exceção inesperada — " + e.getMessage(), e);
                    callback.onErro("Não foi possível processar a receita. Tente novamente.");
                }
            }
        });
    }

    private String construirPrompt(List<DespensaItem> itens, String categoria) {
        // Monta a lista de ingredientes disponíveis
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

        // Sprint 16: instrução de categoria (omitida quando "Surpresa" ou null)
        String instrucaoCategoria = "";
        if (categoria != null && !categoria.isEmpty() && !"Surpresa".equals(categoria)) {
            instrucaoCategoria = " do tipo " + categoria;
        }

        // Sprint 16: prompt estruturado com restrição de veracidade explícita
        return "Você é um chef profissional especialista em culinária brasileira. "
                + "Crie uma receita real" + instrucaoCategoria
                + ", priorizando os seguintes ingredientes disponíveis na despensa do usuário:\n"
                + lista
                + "\nATENÇÃO: Sugira APENAS receitas reais e culturalmente conhecidas. "
                + "Não invente pratos fictícios ou combinações incomuns. "
                + "A receita deve ser viável com os ingredientes listados, "
                + "podendo assumir que o usuário possui itens básicos de cozinha "
                + "(sal, óleo, água, temperos comuns).\n\n"
                + "Responda APENAS com um objeto JSON puro, sem markdown, sem blocos de código, "
                + "sem explicações antes ou depois, contendo exatamente estes campos: "
                + "\"titulo\" (string — nome real da receita), "
                + "\"tempo_preparo\" (string, ex: \"30 min\"), "
                + "\"porcoes\" (string, ex: \"4 porções\"), "
                + "\"dificuldade\" (string: Fácil, Médio ou Difícil), "
                + "\"ingredientes\" (array de strings, cada item com nome e quantidade), "
                + "\"passos\" (array de strings com as instruções). "
                + "Idioma: Português do Brasil.";
    }

    private String construirCorpoRequisicao(String prompt) {
        JsonObject text = new JsonObject();
        text.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(text);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.7);
        genConfig.addProperty("maxOutputTokens", 8192);

        JsonObject thinkingConfig = new JsonObject();
        thinkingConfig.addProperty("thinkingBudget", 0);
        genConfig.add("thinkingConfig", thinkingConfig);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        return gson.toJson(body);
    }

    private ReceitaResponse extrairReceita(String responseJson, ReceitaCallback callback) {
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();

            if (!root.has("candidates") || root.get("candidates").isJsonNull()) {
                String bloqueio = root.has("promptFeedback")
                        ? root.getAsJsonObject("promptFeedback").toString() : "sem promptFeedback";
                Log.e(TAG, "GeminiService: sem candidates. promptFeedback=" + bloqueio);
                callback.onErro("A IA não conseguiu gerar uma receita. Tente com outros itens.");
                return null;
            }

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates.size() == 0) {
                callback.onErro("A IA bloqueou a geração desta receita. Tente novamente.");
                return null;
            }

            JsonObject candidate = candidates.get(0).getAsJsonObject();

            if (candidate.has("finishReason")) {
                String fr = candidate.get("finishReason").getAsString();
                if (!"STOP".equals(fr)) {
                    Log.e(TAG, "GeminiService: finishReason=" + fr);
                    if ("MAX_TOKENS".equals(fr))
                        Log.w(TAG, "GeminiService: JSON truncado por limite de tokens.");
                }
            }

            if (!candidate.has("content")) {
                callback.onErro("Resposta incompleta da IA. Tente novamente.");
                return null;
            }

            JsonArray partsArr = candidate.getAsJsonObject("content").getAsJsonArray("parts");
            if (partsArr == null || partsArr.size() == 0) {
                callback.onErro("Resposta vazia da IA. Tente novamente.");
                return null;
            }

            String textoGerado = partsArr.get(0).getAsJsonObject().get("text").getAsString();
            Log.d(TAG, "GeminiService: texto gerado = " + textoGerado);

            // Remove possíveis blocos markdown mesmo com o prompt pedindo JSON puro
            String jsonLimpo = textoGerado
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            ReceitaResponse receita = gson.fromJson(jsonLimpo, ReceitaResponse.class);
            if (receita == null) {
                callback.onErro("Não foi possível interpretar a receita gerada. Tente novamente.");
                return null;
            }
            return receita;

        } catch (JsonSyntaxException e) {
            Log.e(TAG, "GeminiService: JSON malformado — " + e.getMessage());
            callback.onErro("A IA retornou um formato inesperado. Tente novamente.");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "GeminiService: erro inesperado — " + e.getMessage(), e);
            callback.onErro("Não foi possível processar a receita. Tente novamente.");
            return null;
        }
    }

    private String extrairMensagemDeErro(String bodyJson) {
        try {
            JsonObject root = JsonParser.parseString(bodyJson).getAsJsonObject();
            if (root.has("error")) {
                JsonObject erro = root.getAsJsonObject("error");
                return "code=" + erro.get("code") + " status=" + erro.get("status")
                        + " message=" + erro.get("message");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String traduzirErroHttp(int code) {
        switch (code) {
            case 400: return "Requisição inválida enviada à IA. Verifique o Logcat para detalhes.";
            case 401:
            case 403: return "Chave da API inválida ou sem permissão. Verifique o GEMINI_API_KEY.";
            case 429: return "Limite de requisições atingido. Aguarde alguns minutos.";
            case 500:
            case 503: return "Serviço do Gemini temporariamente indisponível. Tente mais tarde.";
            default:  return "Erro inesperado (HTTP " + code + "). Verifique o Logcat.";
        }
    }

    private String formatarQuantidade(double qtd) {
        return qtd == Math.floor(qtd) ? String.valueOf((int) qtd) : String.valueOf(qtd);
    }
}