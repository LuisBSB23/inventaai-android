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
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;

import java.util.List;

/**
 * HistoricoAdapter — adapter do RecyclerView da tela de Histórico.
 *
 * Cada item exibe: ícone circular (ação), linha de timeline, nome, data e motivo.
 * Cores diferenciadas: CONSUMIDO → verde primário | DESCARTADO → vermelho erro.
 */
public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder> {

    private final List<HistoricoItem> items;

    // Construtor: recebe a lista de histórico já carregada pelo repository
    public HistoricoAdapter(List<HistoricoItem> items) {
        this.items = items;
    }

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

        private final View frameIcon;
        private final ImageView ivActionIcon;
        private final View viewTimelineLine;
        private final TextView tvItemName;
        private final TextView tvDataAcao;
        private final TextView tvMotivo;
        private final TextView tvObservacao;

        HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            frameIcon        = itemView.findViewById(R.id.frameIcon);
            ivActionIcon     = itemView.findViewById(R.id.ivActionIcon);
            viewTimelineLine = itemView.findViewById(R.id.viewTimelineLine);
            tvItemName       = itemView.findViewById(R.id.tvItemName);
            tvDataAcao       = itemView.findViewById(R.id.tvDataAcao);
            tvMotivo         = itemView.findViewById(R.id.tvMotivo);
            tvObservacao     = itemView.findViewById(R.id.tvObservacao);
        }

        void bind(HistoricoItem item, boolean isLast) {
            Context ctx = itemView.getContext();

            // Formata a data para exibição (ex: "22/05/2026" → "HOJE" ou a data formatada)
            String dataFormatada = DateUtils.formatarParaExibicao(item.getDataAcao());
            String hoje = DateUtils.formatarParaExibicao(DateUtils.hoje());
            tvDataAcao.setText(dataFormatada.equals(hoje) ? "HOJE" : dataFormatada.toUpperCase());

            // Nome do item (o histórico armazena apenas id_item; para mostrar o nome real
            // será necessário um JOIN ou desnormalização na Sprint 3)
            tvItemName.setText("Item #" + item.getIdItem());

            // Motivo e cor
            boolean consumido = Constants.STATUS_CONSUMIDO.equals(item.getMotivo());
            tvMotivo.setText(consumido ? "Consumido" : "Descartado");

            if (consumido) {
                tvMotivo.setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary));
                frameIcon.setBackgroundResource(R.drawable.bg_circle_primary_container);
            } else {
                tvMotivo.setTextColor(ContextCompat.getColor(ctx, R.color.colorError));
                frameIcon.setBackgroundTintList(
                        ContextCompat.getColorStateList(ctx, R.color.colorErrorContainer));
            }

            // Linha de timeline: esconde no último item
            viewTimelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            // Observação: vazia por padrão (campo pode ser adicionado ao modelo na Sprint 3)
            tvObservacao.setVisibility(View.GONE);
        }
    }
}
