package com.example.inventaai.ui.despensa;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DespensaAdapter — adapter do RecyclerView da tela principal (Dashboard).
 *
 * Cada card exibe: nome, quantidade+unidade, badge de dias restantes
 * e barra de frescor com cor dinâmica (verde / amarelo / vermelho).
 *
 * Uso:
 *   DespensaAdapter adapter = new DespensaAdapter(itens, item -> abrirDetalhes(item));
 *   recyclerView.setAdapter(adapter);
 */
public class DespensaAdapter extends RecyclerView.Adapter<DespensaAdapter.DespensaViewHolder> {

    // Interface de callback — Activity implementa para receber o clique
    public interface OnItemClickListener {
        void onItemClick(DespensaItem item);
    }

    private final List<DespensaItem> items;
    private final OnItemClickListener listener;

    public DespensaAdapter(List<DespensaItem> items, OnItemClickListener listener) {
        this.items    = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    // =========================================================================
    // ATUALIZAR LISTA
    // =========================================================================

    /**
     * Substitui os dados do adapter e notifica o RecyclerView.
     * Chamado pelo Dashboard a cada onResume().
     */
    public void atualizarLista(List<DespensaItem> novaLista) {
        items.clear();
        if (novaLista != null) {
            items.addAll(novaLista);
        }
        notifyDataSetChanged();
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================

    static class DespensaViewHolder extends RecyclerView.ViewHolder {

        private final TextView    tvItemName;
        private final TextView    tvItemQuantity;
        private final TextView    tvExpiryBadge;
        private final ProgressBar progressFreshness;

        DespensaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName        = itemView.findViewById(R.id.tvItemName);
            tvItemQuantity    = itemView.findViewById(R.id.tvItemQuantity);
            tvExpiryBadge     = itemView.findViewById(R.id.tvExpiryBadge);
            progressFreshness = itemView.findViewById(R.id.progressFreshness);
        }

        void bind(DespensaItem item, OnItemClickListener listener) {
            Context ctx = itemView.getContext();

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
