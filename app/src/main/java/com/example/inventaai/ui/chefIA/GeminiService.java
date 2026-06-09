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
        enviarRequisicao(construirPrompt(itens, null), callback);
    }

    // =========================================================================
    // SPRINT 15/18: GERAR RECEITA COM CATEGORIA
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

        // ── Instrução de categoria ────────────────────────────────────────────
        String instrucaoCategoria = "";
        if (categoria != null && !categoria.isEmpty() && !"Surpresa".equals(categoria)) {
            instrucaoCategoria = " do tipo " + categoria;
        }

        // ── Lista de ingredientes disponíveis (pode ser vazia) ────────────────
        StringBuilder lista = new StringBuilder();
        if (itens != null && !itens.isEmpty()) {
            for (DespensaItem item : itens) {
                lista.append("- ")
                        .append(item.getNome())
                        .append(" (")
                        .append(formatarQuantidade(item.getQuantidade()))
                        .append(" ")
                        .append(item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid")
                        .append(")\n");
            }
        }

        // ── Bloco condicional de ingredientes ─────────────────────────────────
        String blocoIngredientes;
        if (lista.length() > 0) {
            blocoIngredientes =
                    "Ingredientes disponíveis na despensa do usuário:\n"
                            + lista
                            + "\n"
                            + "Priorize esses ingredientes, mas você NÃO é obrigado a usá-los todos. "
                            + "Se a combinação resultar em algo inviável ou de sabor ruim, "
                            + "prefira uma receita autêntica que use apenas parte deles, "
                            + "assumindo que itens básicos de cozinha estão disponíveis "
                            + "(sal, óleo, água, alho, cebola, temperos comuns).\n\n";
        } else {
            // Sprint 18: despensa vazia — gera inspiração culinária livre
            blocoIngredientes =
                    "O usuário não informou ingredientes específicos. "
                            + "Crie uma receita de inspiração culinária usando ingredientes comuns "
                            + "que qualquer cozinha brasileira provavelmente possui "
                            + "(sal, óleo, farinha, ovos, leite, açúcar, temperos básicos, etc.).\n\n";
        }

        // ── Regra de Ouro (Sprint 18) ─────────────────────────────────────────
        String regraDeOuro =
                "⚠️ REGRA DE OURO: Você é um chef profissional. "
                        + "Nunca invente pratos fictícios, nomes de receitas inexistentes "
                        + "ou combinações gastronômicas incomuns. "
                        + "A autenticidade da receita é MAIS IMPORTANTE do que usar todos os ingredientes listados. "
                        + "Prefira sempre uma receita real, conhecida e culturalmente válida. "
                        + "Se os ingredientes disponíveis não formarem nenhuma receita viável, "
                        + "use apenas os que fazem sentido e complete com básicos de cozinha.\n\n";

        // ── Instrução de formato JSON estrito (Sprint 18) ─────────────────────
        String instrucaoFormato =
                "⚠️ FORMATO OBRIGATÓRIO: Responda EXCLUSIVAMENTE com um objeto JSON válido. "
                        + "NÃO inclua markdown, NÃO use blocos ```json``` ou ```, "
                        + "NÃO adicione texto antes nem depois do JSON, "
                        + "NÃO use caracteres especiais fora das strings JSON. "
                        + "O JSON deve ser parseável diretamente pelo método gson.fromJson(). "
                        + "Qualquer caractere fora do JSON irá causar falha no aplicativo.\n\n"
                        + "Campos obrigatórios no JSON:\n"
                        + "- \"titulo\": string — nome real e conhecido da receita\n"
                        + "- \"tempo_preparo\": string — ex: \"30 min\"\n"
                        + "- \"porcoes\": string — ex: \"4 porções\"\n"
                        + "- \"dificuldade\": string — exatamente uma das opções: Fácil, Médio ou Difícil\n"
                        + "- \"ingredientes\": array de strings — cada item no formato \"Nome - quantidade e unidade\"\n"
                        + "- \"passos\": array de strings — cada passo completo como uma string\n\n"
                        + "Idioma: Português do Brasil.";

        return "Você é um chef profissional especialista em culinária brasileira.\n\n"
                + regraDeOuro
                + "Crie uma receita real" + instrucaoCategoria + ".\n\n"
                + blocoIngredientes
                + instrucaoFormato;
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

            // Sprint 18: limpeza defensiva — remove markdown mesmo com prompt pedindo JSON puro
            String jsonLimpo = textoGerado
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // Sprint 18: extrai apenas o bloco JSON caso haja texto residual antes/depois
            int inicioJson = jsonLimpo.indexOf('{');
            int fimJson    = jsonLimpo.lastIndexOf('}');
            if (inicioJson != -1 && fimJson != -1 && fimJson > inicioJson) {
                jsonLimpo = jsonLimpo.substring(inicioJson, fimJson + 1);
            }

            Log.d(TAG, "GeminiService: JSON limpo = " + jsonLimpo);

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