package com.example.inventaai.data.model;

import java.util.List;

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