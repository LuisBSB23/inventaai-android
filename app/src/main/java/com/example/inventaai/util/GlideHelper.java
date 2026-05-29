package com.example.inventaai.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.inventaai.R;

/**
 * GlideHelper — utilitário centralizado para carregamento de imagens com Glide.
 *
 * Padroniza o uso do Glide em todo o aplicativo, garantindo:
 *  - Placeholder consistente durante o carregamento
 *  - Transição suave (fade-in)
 *  - Transformações (circular para avatares)
 *  - Fallback em caso de erro
 *
 * Sprint 3: adotado em ChefIAActivity (hero image) e nas telas de avatar
 * (DashboardActivity drawer header, PerfilActivity).
 */
public final class GlideHelper {

    private GlideHelper() {}

    // -------------------------------------------------------------------------
    // Carregamento padrão — hero image da receita
    // -------------------------------------------------------------------------

    /**
     * Carrega uma imagem remota (URL) ou local (caminho de arquivo) em um ImageView
     * com placeholder colorido e transição fade-in.
     *
     * @param context   Contexto da Activity ou Fragment.
     * @param urlOrPath URL remota (https://...) ou caminho absoluto local.
     * @param imageView View de destino.
     */
    public static void loadImage(Context context, String urlOrPath, ImageView imageView) {
        Glide.with(context)
                .load(urlOrPath)
                .centerCrop()
                .placeholder(R.color.colorSurfaceContainerHighest)   // cor enquanto carrega
                .error(R.drawable.ic_nav_chef)                        // ícone se falhar
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(imageView);
    }

    // -------------------------------------------------------------------------
    // Carregamento circular — avatares de perfil
    // -------------------------------------------------------------------------

    /**
     * Carrega uma imagem com corte circular — ideal para fotos de perfil.
     * Suporta tanto URLs remotas quanto caminhos locais (File ou Uri em String).
     *
     * @param context   Contexto da Activity ou Fragment.
     * @param urlOrPath URL remota ou caminho absoluto do arquivo salvo localmente.
     * @param imageView View de destino (deve ter dimensões fixas para o círculo ficar correto).
     */
    public static void loadCircularImage(Context context, String urlOrPath, ImageView imageView) {
        Glide.with(context)
                .load(urlOrPath)
                .circleCrop()
                .placeholder(R.color.colorSurfaceContainerHighest)
                .error(R.drawable.ic_nav_chef)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(imageView);
    }
}