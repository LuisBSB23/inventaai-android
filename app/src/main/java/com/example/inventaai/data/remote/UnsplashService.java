package com.example.inventaai.data.remote;

import android.util.Log;

import com.example.inventaai.BuildConfig;
import com.example.inventaai.data.model.UnsplashResponse;
import com.example.inventaai.util.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UnsplashService {

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    private static final String TAG      = Constants.LOG_TAG;
    private static final String BASE_URL = "https://api.unsplash.com/";

    // -------------------------------------------------------------------------
    // Interface de callback
    // -------------------------------------------------------------------------

    public interface ImageCallback {
        /** Chamado quando uma URL de imagem válida é encontrada. */
        void onSucesso(String imageUrl);

        /** Chamado quando não há resultado ou ocorre qualquer erro. */
        void onErro(String mensagem);
    }

    // -------------------------------------------------------------------------
    // Dependências internas
    // -------------------------------------------------------------------------

    private final UnsplashApi api;

    public UnsplashService() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(UnsplashApi.class);
    }

    // -------------------------------------------------------------------------
    // Método principal
    // -------------------------------------------------------------------------

    /**
     * Busca a primeira foto relacionada ao título da receita.
     *
     * @param tituloReceita Título retornado pela IA (ex: "Frango ao Limão").
     * @param callback      Recebe a URL "regular" da imagem ou uma mensagem de erro.
     */
    public void buscarImagemReceita(String tituloReceita, ImageCallback callback) {
        String accessKey = BuildConfig.UNSPLASH_ACCESS_KEY;

        // Valida a chave antes de fazer a chamada de rede
        if (accessKey == null || accessKey.isEmpty()) {
            callback.onErro("UNSPLASH_ACCESS_KEY não configurada em local.properties.");
            return;
        }

        if (tituloReceita == null || tituloReceita.trim().isEmpty()) {
            callback.onErro("Título da receita inválido para busca de imagem.");
            return;
        }

        // Adiciona "food" ao query para ter resultados mais relevantes de culinária
        String query = tituloReceita.trim() + " food";

        Log.d(TAG, "UnsplashService: buscando imagem para \"" + query + "\"");

        api.searchPhotos(query, 1, accessKey).enqueue(new Callback<UnsplashResponse>() {

            @Override
            public void onResponse(Call<UnsplashResponse> call, Response<UnsplashResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "UnsplashService: resposta não-sucesso HTTP " + response.code());
                    callback.onErro("Não foi possível carregar a imagem (HTTP " + response.code() + ").");
                    return;
                }

                List<UnsplashResponse.Photo> resultados = response.body().getResults();

                if (resultados == null || resultados.isEmpty()) {
                    Log.w(TAG, "UnsplashService: nenhuma imagem encontrada para \"" + query + "\"");
                    callback.onErro("Nenhuma imagem encontrada para esta receita.");
                    return;
                }

                UnsplashResponse.Photo primeiraFoto = resultados.get(0);
                if (primeiraFoto.getUrls() == null) {
                    callback.onErro("URLs de imagem ausentes na resposta.");
                    return;
                }

                String url = primeiraFoto.getUrls().getRegular();
                if (url == null || url.isEmpty()) {
                    // Fallback para o tamanho "small"
                    url = primeiraFoto.getUrls().getSmall();
                }

                if (url == null || url.isEmpty()) {
                    callback.onErro("URL de imagem inválida na resposta.");
                    return;
                }

                Log.d(TAG, "UnsplashService: imagem encontrada → " + url);
                callback.onSucesso(url);
            }

            @Override
            public void onFailure(Call<UnsplashResponse> call, Throwable t) {
                Log.e(TAG, "UnsplashService: falha de rede", t);
                callback.onErro("Sem conexão para carregar a imagem da receita.");
            }
        });
    }
}