package com.example.inventaai.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.inventaai.R;

public final class GlideHelper {

    private GlideHelper() {}

    // -------------------------------------------------------------------------
    // Carregamento padrão — hero image da receita
    // -------------------------------------------------------------------------

    public static void loadImage(Context context, String urlOrPath, ImageView imageView) {
        Glide.with(context)
                .load(urlOrPath)
                .centerCrop()
                .placeholder(R.color.colorSurfaceContainerHighest)
                .error(R.drawable.ic_nav_chef)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(imageView);
    }

    // -------------------------------------------------------------------------
    // Carregamento circular — avatares de perfil
    // -------------------------------------------------------------------------

    public static void loadCircularImage(Context context, String urlOrPath, ImageView imageView) {
        Glide.with(context)
                .load(urlOrPath)
                .circleCrop()
                // AQUI FOI A CORREÇÃO: Removemos o placeholder e o error para
                // impedir que o Glide desenhe quadrados antes da foto carregar.
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(imageView);
    }
}