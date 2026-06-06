package com.example.inventaai.ui.historico;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.HistoricoItem;
import com.example.inventaai.util.CategoryIconHelper;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder> {

    private final List<HistoricoItem> items;

    public HistoricoAdapter(List<HistoricoItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    // =========================================================================
    // ATUALIZAR LISTA
    // =========================================================================

    public void atualizarLista(List<HistoricoItem> novaLista) {
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
    public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historico, parent, false);
        return new HistoricoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoricoViewHolder holder, int position) {
        HistoricoItem item = items.get(position);
        holder.bind(item, position == getItemCount() - 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================

    static class HistoricoViewHolder extends RecyclerView.ViewHolder {

        private final View      frameIcon;
        private final ImageView ivActionIcon;
        private final View      viewTimelineLine;
        private final TextView  tvItemName;
        private final TextView  tvDataAcao;
        private final TextView  tvMotivo;
        private final TextView  tvObservacao;
        private final ImageView ivCategoryIcon;
        /** Sprint 15: exibe "Consumido na receita: [Nome]" */
        private final TextView  tvOrigem;

        HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            frameIcon        = itemView.findViewById(R.id.frameIcon);
            ivActionIcon     = itemView.findViewById(R.id.ivActionIcon);
            viewTimelineLine = itemView.findViewById(R.id.viewTimelineLine);
            tvItemName       = itemView.findViewById(R.id.tvItemName);
            tvDataAcao       = itemView.findViewById(R.id.tvDataAcao);
            tvMotivo         = itemView.findViewById(R.id.tvMotivo);
            tvObservacao     = itemView.findViewById(R.id.tvObservacao);
            ivCategoryIcon   = itemView.findViewById(R.id.ivCategoryIcon);
            // Sprint 15: reutiliza tvObservacao para exibir a origem
            tvOrigem         = itemView.findViewById(R.id.tvObservacao);
        }

        void bind(HistoricoItem item, boolean isLast) {
            Context ctx = itemView.getContext();

            // Nome real do item
            String nome = item.getNomeCached();
            tvItemName.setText(nome != null && !nome.isEmpty() ? nome : "Item #" + item.getIdItem());

            // Data formatada
            String dataFormatada = DateUtils.formatarParaExibicao(item.getDataAcao());
            String hojeFormatado = DateUtils.formatarParaExibicao(DateUtils.hoje());
            tvDataAcao.setText(dataFormatada.equals(hojeFormatado) ? "HOJE" : dataFormatada.toUpperCase());

            // Motivo e cor
            boolean consumido = Constants.STATUS_CONSUMIDO.equals(item.getMotivo())
                    || (item.getMotivo() != null && item.getMotivo().startsWith("Receita"));

            if (consumido) {
                tvMotivo.setText("Consumido");
                tvMotivo.setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary));
                frameIcon.setBackgroundResource(R.drawable.bg_circle_primary_container);
                ivActionIcon.setImageResource(R.drawable.ic_nav_chef);
                ivActionIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.colorOnPrimaryContainer));
            } else {
                tvMotivo.setText("Descartado");
                tvMotivo.setTextColor(ContextCompat.getColor(ctx, R.color.colorError));
                frameIcon.getBackground().setTint(
                        ContextCompat.getColor(ctx, R.color.colorErrorContainer));
                ivActionIcon.setImageResource(R.drawable.ic_nav_history);
                ivActionIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.colorOnErrorContainer));
            }

            // Ícone de categoria
            if (ivCategoryIcon != null) {
                String categoria = item.getCategoria();
                if (categoria != null && !categoria.trim().isEmpty()) {
                    ivCategoryIcon.setImageResource(CategoryIconHelper.getIcon(categoria));
                    ivCategoryIcon.setVisibility(View.VISIBLE);
                } else {
                    ivCategoryIcon.setVisibility(View.GONE);
                }
            }

            // Sprint 15: exibe origem se disponível
            if (tvOrigem != null) {
                if (item.temOrigem()) {
                    tvOrigem.setText("Consumido na receita: " + item.getOrigem());
                    tvOrigem.setVisibility(View.VISIBLE);
                } else {
                    tvOrigem.setVisibility(View.GONE);
                }
            }

            // Linha de timeline
            viewTimelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        }
    }
}