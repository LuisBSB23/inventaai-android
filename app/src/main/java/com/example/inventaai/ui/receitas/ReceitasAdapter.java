package com.example.inventaai.ui.receitas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.ReceitaSalva;
import com.example.inventaai.util.DateUtils;
import com.example.inventaai.util.GlideHelper;

import java.util.ArrayList;
import java.util.List;

public class ReceitasAdapter extends RecyclerView.Adapter<ReceitasAdapter.ReceitaViewHolder> {

    // ── Interfaces ────────────────────────────────────────────────────────────
    public interface OnReceitaClickListener  { void onClick(ReceitaSalva receita); }
    public interface OnReceitaDeleteListener { void onDelete(ReceitaSalva receita, int position); }

    // ── Dados ─────────────────────────────────────────────────────────────────
    private final List<ReceitaSalva>      items;
    private final OnReceitaClickListener  clickListener;
    private final OnReceitaDeleteListener deleteListener;

    public ReceitasAdapter(List<ReceitaSalva> items,
                           OnReceitaClickListener clickListener,
                           OnReceitaDeleteListener deleteListener) {
        this.items          = items != null ? items : new ArrayList<>();
        this.clickListener  = clickListener;
        this.deleteListener = deleteListener;
    }

    public void atualizarLista(List<ReceitaSalva> novaLista) {
        items.clear();
        if (novaLista != null) items.addAll(novaLista);
        notifyDataSetChanged();
    }

    public void removerItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    @NonNull
    @Override
    public ReceitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receita_salva, parent, false);
        return new ReceitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceitaViewHolder holder, int position) {
        holder.bind(items.get(position), clickListener, deleteListener, position);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    static class ReceitaViewHolder extends RecyclerView.ViewHolder {

        private final ImageView   ivThumb;
        private final TextView    tvTitulo;
        private final TextView    tvTempo;
        private final TextView    tvDificuldade;
        private final TextView    tvDataSalvo;
        private final ImageButton btnDeletar;
        private final TextView    tvBadgeAndamento; // Sprint 14

        ReceitaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb           = itemView.findViewById(R.id.ivReceitaThumb);
            tvTitulo          = itemView.findViewById(R.id.tvReceitaTitulo);
            tvTempo           = itemView.findViewById(R.id.tvReceitaTempo);
            tvDificuldade     = itemView.findViewById(R.id.tvReceitaDificuldade);
            tvDataSalvo       = itemView.findViewById(R.id.tvDataSalvo);
            btnDeletar        = itemView.findViewById(R.id.btnDeletarReceita);
            tvBadgeAndamento  = itemView.findViewById(R.id.tvBadgeAndamento); // Sprint 14
        }

        void bind(ReceitaSalva receita,
                  OnReceitaClickListener clickListener,
                  OnReceitaDeleteListener deleteListener,
                  int position) {

            tvTitulo.setText(receita.getTitulo() != null ? receita.getTitulo() : "Receita sem título");
            tvTempo.setText(receita.getTempoPreparo() != null ? receita.getTempoPreparo() : "—");
            tvDificuldade.setText(receita.getDificuldade() != null ? receita.getDificuldade() : "—");

            String dataFormatada = DateUtils.formatarParaExibicao(receita.getDataSalvo());
            tvDataSalvo.setText("Salva em " + dataFormatada);

            // Imagem
            if (receita.getImagemUrl() != null && !receita.getImagemUrl().isEmpty()) {
                GlideHelper.loadImage(itemView.getContext(), receita.getImagemUrl(), ivThumb);
            } else {
                ivThumb.setImageResource(R.drawable.ic_empty_recipe);
                ivThumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                ivThumb.setPadding(16, 16, 16, 16);
            }

            // Sprint 14: badge "Em andamento"
            if (tvBadgeAndamento != null) {
                boolean emAndamento = receita.isEmAndamento();
                tvBadgeAndamento.setVisibility(emAndamento ? View.VISIBLE : View.GONE);
            }

            // Cliques
            itemView.setOnClickListener(v -> { if (clickListener != null) clickListener.onClick(receita); });
            btnDeletar.setOnClickListener(v -> { if (deleteListener != null) deleteListener.onDelete(receita, position); });
        }
    }
}