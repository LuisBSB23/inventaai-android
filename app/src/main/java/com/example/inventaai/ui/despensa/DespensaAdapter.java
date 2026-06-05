package com.example.inventaai.ui.despensa;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DespensaAdapter extends RecyclerView.Adapter<DespensaAdapter.DespensaViewHolder> {

    // =========================================================================
    // Interfaces
    // =========================================================================

    public interface OnItemClickListener {
        void onItemClick(DespensaItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(DespensaItem item);
    }

    public interface OnSelecaoChangedListener {
        void onSelecaoChanged(int totalSelecionados);
    }

    // =========================================================================
    // Estado
    // =========================================================================

    private final List<DespensaItem>    items;
    private final OnItemClickListener   clickListener;
    private OnItemLongClickListener     longClickListener;
    private OnSelecaoChangedListener    selecaoChangedListener;   // Fix 2

    private final Set<Long> itensSelecionados = new HashSet<>();
    private boolean modoSelecao = false;
    private int ultimaPosicaoAnimada = -1;

    // =========================================================================
    // Construtor
    // =========================================================================

    public DespensaAdapter(List<DespensaItem> items, OnItemClickListener clickListener) {
        this.items         = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.clickListener = clickListener;
    }

    // =========================================================================
    // API pública
    // =========================================================================

    public void setOnItemLongClickListener(OnItemLongClickListener l)  { longClickListener    = l; }
    public void setOnSelecaoChangedListener(OnSelecaoChangedListener l) { selecaoChangedListener = l; } // Fix 2

    public void setModoSelecao(boolean ativo) {
        modoSelecao = ativo;
        if (!ativo) itensSelecionados.clear();
        notifyDataSetChanged();
    }

    public boolean isModoSelecao()            { return modoSelecao; }
    public int getQuantidadeSelecionados()    { return itensSelecionados.size(); }

    public List<DespensaItem> getItensSelecionados() {
        List<DespensaItem> lista = new ArrayList<>();
        for (DespensaItem item : items) {
            if (itensSelecionados.contains(item.getId())) lista.add(item);
        }
        return lista;
    }

    /** Seleciona (ou deseleciona) um item pelo id sem precisar de clique na View. */
    public void selecionarItem(long id) {
        if (itensSelecionados.contains(id)) {
            itensSelecionados.remove(id);
        } else {
            itensSelecionados.add(id);
        }
        notifyDataSetChanged();
        if (selecaoChangedListener != null) {
            selecaoChangedListener.onSelecaoChanged(itensSelecionados.size());
        }
    }

    public void limparSelecao() {
        itensSelecionados.clear();
        modoSelecao = false;
        notifyDataSetChanged();
    }

    // =========================================================================
    // DiffUtil
    // =========================================================================

    public void atualizarLista(List<DespensaItem> novaLista) {
        if (novaLista == null) novaLista = new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new DespensaItemDiffCallback(items, novaLista));
        boolean reset = items.isEmpty() || novaLista.isEmpty() || novaLista.size() != items.size();
        items.clear();
        items.addAll(novaLista);
        if (reset) ultimaPosicaoAnimada = -1;
        diff.dispatchUpdatesTo(this);
    }

    // =========================================================================
    // Adapter overrides
    // =========================================================================

    @NonNull @Override
    public DespensaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_despensa, parent, false);
        return new DespensaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DespensaViewHolder holder, int position) {
        DespensaItem item = items.get(position);
        boolean selecionado = itensSelecionados.contains(item.getId());
        holder.bind(item, clickListener, longClickListener, modoSelecao, selecionado, this);

        if (position > ultimaPosicaoAnimada) {
            Animation anim = AnimationUtils.loadAnimation(
                    holder.itemView.getContext(), R.anim.item_appear);
            holder.itemView.startAnimation(anim);
            ultimaPosicaoAnimada = position;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    // =========================================================================
    // ViewHolder
    // =========================================================================

    static class DespensaViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardItem;
        private final ImageView        ivItemIcon;
        private final TextView         tvItemName;
        private final TextView         tvItemQuantity;
        private final TextView         tvExpiryBadge;
        private final ProgressBar      progressFreshness;
        private final CheckBox         checkboxItem;

        DespensaViewHolder(@NonNull View v) {
            super(v);
            cardItem          = v.findViewById(R.id.cardItem);
            ivItemIcon        = v.findViewById(R.id.ivItemIcon);
            tvItemName        = v.findViewById(R.id.tvItemName);
            tvItemQuantity    = v.findViewById(R.id.tvItemQuantity);
            tvExpiryBadge     = v.findViewById(R.id.tvExpiryBadge);
            progressFreshness = v.findViewById(R.id.progressFreshness);
            checkboxItem      = v.findViewById(R.id.checkboxItem);
        }

        void bind(DespensaItem item,
                  OnItemClickListener clickListener,
                  OnItemLongClickListener longClickListener,
                  boolean modoSelecao,
                  boolean selecionado,
                  DespensaAdapter adapter) {

            Context ctx = itemView.getContext();

            // ── Ícone ──────────────────────────────────────────────────────
            ivItemIcon.setImageResource(CategoryIconHelper.getIcon(item.getCategoria()));

            // ── Nome ───────────────────────────────────────────────────────
            tvItemName.setText(item.getNome());

            // ── Quantidade ─────────────────────────────────────────────────
            String unidade = item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid";
            double q = item.getQuantidade();
            String qtdStr = (q == Math.floor(q)) ? String.valueOf((int) q) : String.valueOf(q);
            tvItemQuantity.setText(qtdStr + " " + unidade);

            // ── Badge validade ─────────────────────────────────────────────
            int dias = DateUtils.calcularDiasRestantes(item.getDataValidade());
            String alerta = DateUtils.getStatusAlerta(dias);

            if (dias < 0)       tvExpiryBadge.setText("Vencido");
            else if (dias == 0) tvExpiryBadge.setText("Vence hoje");
            else if (dias == 1) tvExpiryBadge.setText("1 dia");
            else                tvExpiryBadge.setText(dias + " dias");

            int corBadge, corBarra, progressValor;
            switch (alerta) {
                case "VERMELHO":
                    corBadge = ContextCompat.getColor(ctx, R.color.colorError);
                    corBarra = ContextCompat.getColor(ctx, R.color.colorError);
                    progressValor = 10; break;
                case "AMARELO":
                    corBadge = ContextCompat.getColor(ctx, R.color.colorSecondary);
                    corBarra = ContextCompat.getColor(ctx, R.color.colorSecondaryContainer);
                    progressValor = 40; break;
                default:
                    corBadge = ContextCompat.getColor(ctx, R.color.colorPrimary);
                    corBarra = ContextCompat.getColor(ctx, R.color.colorPrimary);
                    progressValor = Math.min(100, Math.max(60, 100 - dias));
            }
            tvExpiryBadge.getBackground().setTint(corBadge);
            progressFreshness.setProgressTintList(ColorStateList.valueOf(corBarra));
            progressFreshness.setProgress(progressValor);

            // ── Fix 1b: seleção via APIs do MaterialCardView ───────────────
            // Isso garante que a borda NÃO suma ao rolar (não usa setBackground).
            if (modoSelecao) {
                checkboxItem.setVisibility(View.VISIBLE);
                checkboxItem.setChecked(selecionado);
                tvExpiryBadge.setVisibility(View.GONE);
                progressFreshness.setVisibility(View.GONE);

                if (selecionado) {
                    cardItem.setStrokeColor(ContextCompat.getColor(ctx, R.color.colorPrimary));
                    cardItem.setStrokeWidth(dpToPx(ctx, 2));
                    cardItem.setCardBackgroundColor(
                            ContextCompat.getColor(ctx, R.color.colorPrimaryContainer));
                } else {
                    cardItem.setStrokeColor(
                            ContextCompat.getColor(ctx, R.color.colorSurfaceContainerHighest));
                    cardItem.setStrokeWidth(dpToPx(ctx, 1));
                    cardItem.setCardBackgroundColor(
                            ContextCompat.getColor(ctx, R.color.colorSurfaceContainerLowest));
                }
            } else {
                checkboxItem.setVisibility(View.GONE);
                tvExpiryBadge.setVisibility(View.VISIBLE);
                progressFreshness.setVisibility(View.VISIBLE);
                cardItem.setStrokeColor(
                        ContextCompat.getColor(ctx, R.color.colorSurfaceContainerHighest));
                cardItem.setStrokeWidth(dpToPx(ctx, 1));
                cardItem.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.colorSurfaceContainerLowest));
            }

            // ── Clique ─────────────────────────────────────────────────────
            itemView.setOnClickListener(v -> {
                if (modoSelecao) {
                    // Fix 2: toggle + notifica listener para atualizar contador
                    long id = item.getId();
                    if (adapter.itensSelecionados.contains(id)) {
                        adapter.itensSelecionados.remove(id);
                    } else {
                        adapter.itensSelecionados.add(id);
                    }
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_ID) adapter.notifyItemChanged(pos);

                    // Fix 2 — dispara callback com total atualizado
                    if (adapter.selecaoChangedListener != null) {
                        adapter.selecaoChangedListener.onSelecaoChanged(
                                adapter.itensSelecionados.size());
                    }
                } else {
                    if (clickListener != null) clickListener.onItemClick(item);
                }
            });

            // ── Long click ─────────────────────────────────────────────────
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item);
                    return true;
                }
                return false;
            });
        }

        private static int dpToPx(Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }
}