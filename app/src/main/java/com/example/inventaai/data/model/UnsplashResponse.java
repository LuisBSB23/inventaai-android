package com.example.inventaai.data.model;

import java.util.List;

/**
 * UnsplashResponse — mapeia a resposta do endpoint GET /search/photos da Unsplash API.
 *
 * Estrutura relevante do JSON retornado:
 * {
 *   "results": [
 *     {
 *       "urls": {
 *         "regular": "https://images.unsplash.com/photo-xxx?..."
 *       }
 *     }
 *   ]
 * }
 *
 * Sprint 3: usado pelo UnsplashService para extrair a URL da primeira foto.
 */
public class UnsplashResponse {

    private List<Photo> results;

    public List<Photo> getResults() {
        return results;
    }

    // -------------------------------------------------------------------------
    // Classe interna: Photo
    // -------------------------------------------------------------------------

    public static class Photo {
        private Urls urls;

        public Urls getUrls() {
            return urls;
        }
    }

    // -------------------------------------------------------------------------
    // Classe interna: Urls
    // -------------------------------------------------------------------------

    public static class Urls {
        private String regular;  // ~1080px — ideal para o hero image
        private String small;    // ~400px  — fallback mais leve

        public String getRegular() {
            return regular;
        }

        public String getSmall() {
            return small;
        }
    }
}