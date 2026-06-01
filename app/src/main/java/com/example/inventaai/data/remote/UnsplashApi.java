package com.example.inventaai.data.remote;

import com.example.inventaai.data.model.UnsplashResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UnsplashApi {

    /**
     * Busca fotos na Unsplash pelo termo informado.
     *
     * @param query    Texto de busca (ex: título da receita, ex: "Omelete de Espinafre").
     * @param perPage  Quantidade de resultados — use sempre 1 para pegar apenas a melhor foto.
     * @param clientId Access Key da Unsplash (lida via BuildConfig.UNSPLASH_ACCESS_KEY).
     */
    @GET("search/photos")
    Call<UnsplashResponse> searchPhotos(
            @Query("query")     String query,
            @Query("per_page")  int    perPage,
            @Query("client_id") String clientId
    );
}