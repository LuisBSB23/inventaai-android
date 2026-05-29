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
 *
 * CORREÇÕES aplicadas:
 *  1. Log detalhado: o código HTTP real e o body completo da resposta de erro
 *     são exibidos no Logcat, permitindo diagnóstico preciso sem alterar a
 *     mensagem exibida ao usuário final.
 *  2. extrairReceita() defensivo: trata candidates nulo/vazio, finishReason
 *     diferente de STOP, e JSON malformado — cada caso com mensagem distinta
 *     no log, sem lançar exceção genérica que escondia a causa raiz.
 *  3. Detecção de SAFETY block: quando a API bloqueia por filtro de segurança
 *     (HTTP 200 com candidates vazio), o erro é identificado corretamente.
 */
public class GeminiService {

    // ──────────────────────────────────────────────────────────────────────────
    // Constantes
    // ──────────────────────────────────────────────────────────────────────────

    private static final String TAG = Constants.LOG_TAG;

    /**
     * Endpoint Gemini Flash Latest — sempre aponta para a versão mais recente e estável
     * do Flash disponível na conta, evitando problemas de quota de modelos específicos.
     */
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

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
            Log.e(TAG, "GeminiService: GEMINI_API_KEY está vazia no BuildConfig. "
                    + "Verifique se a chave foi adicionada ao local.properties.");
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
                Log.e(TAG, "GeminiService: falha de rede — " + e.getMessage(), e);
                callback.onErro("Sem conexão com a internet. Verifique sua rede e tente novamente.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    String bodyString = r.body() != null ? r.body().string() : "";

                    if (!r.isSuccessful()) {
                        // ── LOG DETALHADO: mostra o código HTTP real e o body completo ──
                        // Isso permite identificar se é 400, 401, 403, 429, 500, etc.
                        // sem depender da mensagem genérica exibida ao usuário.
                        Log.e(TAG, "══════════════════════════════════════════");
                        Log.e(TAG, "GeminiService: ERRO HTTP " + r.code());
                        Log.e(TAG, "GeminiService: Body da resposta de erro:");
                        Log.e(TAG, bodyString);
                        Log.e(TAG, "══════════════════════════════════════════");

                        // Tenta extrair a mensagem de erro estruturada do JSON da API
                        String mensagemApi = extrairMensagemDeErro(bodyString);
                        if (mensagemApi != null) {
                            Log.e(TAG, "GeminiService: mensagem da API → " + mensagemApi);
                        }

                        callback.onErro(traduzirErroHttp(r.code()));
                        return;
                    }

                    Log.d(TAG, "GeminiService: HTTP 200 — processando resposta...");
                    Log.d(TAG, "GeminiService: body completo = " + bodyString);

                    ReceitaResponse receita = extrairReceita(bodyString, callback);
                    if (receita != null) {
                        callback.onSucesso(receita);
                    }
                    // Se receita == null, o callback.onErro já foi chamado dentro de extrairReceita

                } catch (Exception e) {
                    Log.e(TAG, "GeminiService: exceção inesperada ao processar resposta — " + e.getMessage(), e);
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
        JsonObject text = new JsonObject();
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
        // 8192 tokens: suficiente para qualquer receita completa com passos detalhados.
        // O valor anterior (1024) era insuficiente para o gemini-flash-latest (gemini-3.5-flash),
        // que usa tokens internos de "pensamento" e truncava o JSON antes de terminar.
        genConfig.addProperty("maxOutputTokens", 8192);

        // Desativa o modo de pensamento (thinking) do modelo.
        // O gemini-flash-latest (gemini-3.5-flash) ativa "thinking" por padrão,
        // consumindo ~1000 tokens internamente antes de gerar a resposta,
        // o que causava MAX_TOKENS no JSON. thinkingBudget=0 desativa esse comportamento.
        // O campo fica dentro de generationConfig, não no body raiz.
        JsonObject thinkingConfig = new JsonObject();
        thinkingConfig.addProperty("thinkingBudget", 0);
        genConfig.add("thinkingConfig", thinkingConfig);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        return gson.toJson(body);
    }

    /**
     * Extrai o texto gerado da resposta do Gemini e converte para ReceitaResponse.
     *
     * CORREÇÃO: versão defensiva que trata todos os casos de resposta anômala
     * que antes causavam NullPointerException ou IndexOutOfBoundsException silenciosos,
     * mascarando a causa real do erro.
     *
     * Casos tratados:
     *  - candidates nulo ou ausente no JSON
     *  - candidates vazio (SAFETY block ou outro bloqueio — HTTP 200 com 0 resultados)
     *  - finishReason diferente de "STOP" (ex: MAX_TOKENS, RECITATION, SAFETY)
     *  - JSON de receita malformado ou incompleto retornado pelo modelo
     *
     * @return ReceitaResponse em caso de sucesso, ou null se callback.onErro já foi chamado.
     */
    private ReceitaResponse extrairReceita(String responseJson, ReceitaCallback callback) {
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();

            // ── Verificar se há candidates ────────────────────────────────────
            if (!root.has("candidates") || root.get("candidates").isJsonNull()) {
                // Pode ocorrer quando a API bloqueia o prompt por segurança
                String bloqueio = root.has("promptFeedback")
                        ? root.getAsJsonObject("promptFeedback").toString()
                        : "sem promptFeedback";
                Log.e(TAG, "GeminiService: resposta sem 'candidates'. promptFeedback = " + bloqueio);
                callback.onErro("A IA não conseguiu gerar uma receita para esses ingredientes. Tente com outros itens.");
                return null;
            }

            JsonArray candidates = root.getAsJsonArray("candidates");

            if (candidates.size() == 0) {
                Log.e(TAG, "GeminiService: array 'candidates' vazio — possível bloqueio por filtro de segurança.");
                callback.onErro("A IA bloqueou a geração desta receita. Tente novamente.");
                return null;
            }

            JsonObject candidate = candidates.get(0).getAsJsonObject();

            // ── Verificar finishReason ────────────────────────────────────────
            if (candidate.has("finishReason")) {
                String finishReason = candidate.get("finishReason").getAsString();
                if (!"STOP".equals(finishReason)) {
                    Log.e(TAG, "GeminiService: finishReason inesperado = " + finishReason
                            + ". Candidate completo: " + candidate);
                    if ("MAX_TOKENS".equals(finishReason)) {
                        Log.w(TAG, "GeminiService: JSON truncado por limite de tokens. "
                                + "Considere aumentar maxOutputTokens.");
                    }
                    // Tenta continuar mesmo assim — o JSON pode estar parcialmente utilizável
                }
            }

            // ── Extrair o texto gerado ────────────────────────────────────────
            if (!candidate.has("content")) {
                Log.e(TAG, "GeminiService: candidate sem 'content'. Candidate: " + candidate);
                callback.onErro("Resposta incompleta da IA. Tente novamente.");
                return null;
            }

            JsonArray parts = candidate
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts");

            if (parts == null || parts.size() == 0) {
                Log.e(TAG, "GeminiService: 'parts' vazio ou ausente.");
                callback.onErro("Resposta vazia da IA. Tente novamente.");
                return null;
            }

            String textoGerado = parts.get(0).getAsJsonObject().get("text").getAsString();
            Log.d(TAG, "GeminiService: texto gerado = " + textoGerado);

            // ── Limpar possíveis marcadores markdown ──────────────────────────
            String jsonLimpo = textoGerado
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // ── Converter para ReceitaResponse ────────────────────────────────
            ReceitaResponse receita = gson.fromJson(jsonLimpo, ReceitaResponse.class);

            if (receita == null) {
                Log.e(TAG, "GeminiService: gson.fromJson retornou null para: " + jsonLimpo);
                callback.onErro("Não foi possível interpretar a receita gerada. Tente novamente.");
                return null;
            }

            return receita;

        } catch (JsonSyntaxException e) {
            Log.e(TAG, "GeminiService: JSON malformado na resposta — " + e.getMessage());
            Log.e(TAG, "GeminiService: resposta bruta que causou o erro: " + responseJson);
            callback.onErro("A IA retornou uma resposta em formato inesperado. Tente novamente.");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "GeminiService: erro inesperado ao extrair receita — " + e.getMessage(), e);
            callback.onErro("Não foi possível processar a receita. Tente novamente.");
            return null;
        }
    }

    /**
     * Tenta extrair a mensagem de erro estruturada do JSON retornado pela API.
     * Estrutura esperada: { "error": { "code": 429, "message": "...", "status": "..." } }
     * Retorna null se não conseguir extrair.
     */
    private String extrairMensagemDeErro(String bodyJson) {
        try {
            JsonObject root = JsonParser.parseString(bodyJson).getAsJsonObject();
            if (root.has("error")) {
                JsonObject erro = root.getAsJsonObject("error");
                String code    = erro.has("code")    ? erro.get("code").getAsString()    : "?";
                String status  = erro.has("status")  ? erro.get("status").getAsString()  : "?";
                String message = erro.has("message") ? erro.get("message").getAsString() : "?";
                return "code=" + code + " status=" + status + " message=" + message;
            }
        } catch (Exception ignored) {
            // Body não é JSON válido ou não tem a estrutura esperada
        }
        return null;
    }

    /**
     * Retorna mensagem amigável para os códigos HTTP mais comuns da API Google.
     *
     * NOTA DE DEBUG: para ver o código HTTP real e o body completo do erro,
     * filtre o Logcat pela tag "InventaAi" e procure as linhas com "══════".
     */
    private String traduzirErroHttp(int code) {
        switch (code) {
            case 400: return "Requisição inválida enviada à IA. Verifique o Logcat (tag InventaAi) para detalhes.";
            case 401:
            case 403: return "Chave da API inválida ou sem permissão. Verifique o GEMINI_API_KEY.";
            case 429: return "Limite de requisições da API atingido. Aguarde alguns minutos e tente novamente.";
            case 500:
            case 503: return "O serviço do Gemini está temporariamente indisponível. Tente novamente mais tarde.";
            default:  return "Erro inesperado (HTTP " + code + "). Verifique o Logcat para detalhes.";
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