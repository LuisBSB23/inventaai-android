package com.example.inventaai.ui.despensa;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.util.CategoryIconHelper;
import com.example.inventaai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class DespensaAdapter extends RecyclerView.Adapter<DespensaAdapter.DespensaViewHolder> {

    // Interface de callback — Activity implementa para receber o clique
    public interface OnItemClickListener {
        void onItemClick(DespensaItem item);
    }

    private final List<DespensaItem> items;
    private final OnItemClickListener listener;

    // Controla animação de entrada: só anima posições ainda não exibidas
    private int ultimaPosicaoAnimada = -1;

    public DespensaAdapter(List<DespensaItem> items, OnItemClickListener listener) {
        this.items    = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.listener = listener;
    }

    // =========================================================================
    // DiffUtil substitui notifyDataSetChanged()
    // =========================================================================

    public void atualizarLista(List<DespensaItem> novaLista) {
        if (novaLista == null) novaLista = new ArrayList<>();

        // Calcula o diff entre a lista atual e a nova em background seria o
        // ideal para listas muito longas; aqui fazemos na thread que chama
        // (que já é a main thread, após a query ter sido feita no diskIO —
        // ver AppExecutors). Para listas de despensa típicas (<200 itens)
        // o custo é imperceptível (<1ms).
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DespensaItemDiffCallback(items, novaLista));

        // Detecta se é uma substituição completa para controlar a animação
        boolean substituicaoCompleta = items.isEmpty() || novaLista.isEmpty()
                || novaLista.size() != items.size();

        items.clear();
        items.addAll(novaLista);

        // Reseta animação somente em substituição completa
        if (substituicaoCompleta) {
            ultimaPosicaoAnimada = -1;
        }

        // Aplica apenas as mudanças pontuais detectadas pelo DiffUtil
        diffResult.dispatchUpdatesTo(this);
    }

    // =========================================================================
    // ADAPTER OVERRIDES
    // =========================================================================

    @NonNull
    @Override
    public DespensaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_despensa, parent, false);
        return new DespensaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DespensaViewHolder holder, int position) {
        DespensaItem item = items.get(position);
        holder.bind(item, listener);

        // ── Anima apenas na primeira aparição do card ──────────────────────
        if (position > ultimaPosicaoAnimada) {
            Animation anim = AnimationUtils.loadAnimation(
                    holder.itemView.getContext(), R.anim.item_appear);
            holder.itemView.startAnimation(anim);
            ultimaPosicaoAnimada = position;
        }
        // ──────────────────────────────────────────────────────────────────
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================

    static class DespensaViewHolder extends RecyclerView.ViewHolder {

        private final ImageView   ivItemIcon;
        private final TextView    tvItemName;
        private final TextView    tvItemQuantity;
        private final TextView    tvExpiryBadge;
        private final ProgressBar progressFreshness;

        DespensaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemIcon        = itemView.findViewById(R.id.ivItemIcon);
            tvItemName        = itemView.findViewById(R.id.tvItemName);
            tvItemQuantity    = itemView.findViewById(R.id.tvItemQuantity);
            tvExpiryBadge     = itemView.findViewById(R.id.tvExpiryBadge);
            progressFreshness = itemView.findViewById(R.id.progressFreshness);
        }

        void bind(DespensaItem item, OnItemClickListener listener) {
            Context ctx = itemView.getContext();

            // ── Ícone da categoria ────────────────────────────────────────
            int iconRes = CategoryIconHelper.getIcon(item.getCategoria());
            ivItemIcon.setImageResource(iconRes);

            // Nome
            tvItemName.setText(item.getNome());

            // Quantidade e unidade
            String unidade = item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid";
            String qtdStr;
            if (item.getQuantidade() == Math.floor(item.getQuantidade())) {
                qtdStr = String.valueOf((int) item.getQuantidade());
            } else {
                qtdStr = String.valueOf(item.getQuantidade());
            }
            tvItemQuantity.setText(qtdStr + " " + unidade);

            // Dias restantes e alerta
            int dias = DateUtils.calcularDiasRestantes(item.getDataValidade());
            String alerta = DateUtils.getStatusAlerta(dias);

            // Badge de dias
            if (dias < 0) {
                tvExpiryBadge.setText("Vencido");
            } else if (dias == 0) {
                tvExpiryBadge.setText("Vence hoje");
            } else if (dias == 1) {
                tvExpiryBadge.setText("1 dia");
            } else {
                tvExpiryBadge.setText(dias + " dias");
            }

            // Cores do badge e da barra conforme status de alerta
            int corBadgeFundo;
            int corBarra;
            int progressValor;

            switch (alerta) {
                case "VERMELHO":
                    corBadgeFundo  = ContextCompat.getColor(ctx, R.color.colorError);
                    corBarra       = ContextCompat.getColor(ctx, R.color.colorError);
                    progressValor  = 10;
                    break;
                case "AMARELO":
                    corBadgeFundo  = ContextCompat.getColor(ctx, R.color.colorSecondary);
                    corBarra       = ContextCompat.getColor(ctx, R.color.colorSecondaryContainer);
                    progressValor  = 40;
                    break;
                default: // VERDE
                    corBadgeFundo  = ContextCompat.getColor(ctx, R.color.colorPrimary);
                    corBarra       = ContextCompat.getColor(ctx, R.color.colorPrimary);
                    progressValor  = Math.min(100, Math.max(60, 100 - dias));
                    break;
            }

            tvExpiryBadge.getBackground().setTint(corBadgeFundo);
            progressFreshness.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(corBarra));
            progressFreshness.setProgress(progressValor);

            // Clique no card → abre DetalhesActivity
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}