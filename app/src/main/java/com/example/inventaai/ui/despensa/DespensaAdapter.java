package com.example.inventaai.ui.despensa;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.util.CategoryColorHelper;
import com.example.inventaai.util.CategoryIconHelper;
import com.example.inventaai.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DespensaAdapter extends RecyclerView.Adapter<DespensaAdapter.DespensaViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DespensaItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(DespensaItem item);
    }

    public interface OnSelecaoChangedListener {
        void onSelecaoChanged(int totalSelecionados);
    }

    private final List<DespensaItem>    items;
    private final OnItemClickListener   clickListener;
    private OnItemLongClickListener     longClickListener;
    private OnSelecaoChangedListener    selecaoChangedListener;

    // CORREÇÃO 2: Substituído Set<Long> por Map para reter os objetos selecionados, resolvendo o bug de filtro
    private final Map<Long, DespensaItem> itensSelecionadosMap = new HashMap<>();
    private boolean modoSelecao = false;
    private int ultimaPosicaoAnimada = -1;

    public DespensaAdapter(List<DespensaItem> items, OnItemClickListener clickListener) {
        this.items         = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.clickListener = clickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l)   { longClickListener     = l; }
    public void setOnSelecaoChangedListener(OnSelecaoChangedListener l) { selecaoChangedListener = l; }

    public void setModoSelecao(boolean ativo) {
        modoSelecao = ativo;
        if (!ativo) itensSelecionadosMap.clear();
        notifyDataSetChanged();
    }

    public boolean isModoSelecao()            { return modoSelecao; }
    public int getQuantidadeSelecionados()    { return itensSelecionadosMap.size(); }

    public List<DespensaItem> getItensSelecionados() {
        // CORREÇÃO 2: Agora retorna diretamente os objetos armazenados no mapa, sem depender da view filtrada
        return new ArrayList<>(itensSelecionadosMap.values());
    }

    public void selecionarItem(DespensaItem item) {
        long id = item.getId();
        if (itensSelecionadosMap.containsKey(id)) {
            itensSelecionadosMap.remove(id);
        } else {
            itensSelecionadosMap.put(id, item);
        }
        notifyDataSetChanged();
        if (selecaoChangedListener != null) {
            selecaoChangedListener.onSelecaoChanged(itensSelecionadosMap.size());
        }
    }

    public void limparSelecao() {
        itensSelecionadosMap.clear();
        modoSelecao = false;
        notifyDataSetChanged();
    }

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

    @NonNull @Override
    public DespensaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_despensa, parent, false);
        return new DespensaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DespensaViewHolder holder, int position) {
        DespensaItem item = items.get(position);
        boolean selecionado = itensSelecionadosMap.containsKey(item.getId());
        holder.bind(item, clickListener, longClickListener, modoSelecao, selecionado, this);

        if (position > ultimaPosicaoAnimada) {
            Animation anim = AnimationUtils.loadAnimation(
                    holder.itemView.getContext(), R.anim.item_appear);
            holder.itemView.startAnimation(anim);
            ultimaPosicaoAnimada = position;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class DespensaViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardItem;
        private final FrameLayout      flIconContainer;
        private final ImageView        ivItemIcon;
        private final TextView         tvItemName;
        private final TextView         tvItemQuantity;
        private final TextView         tvExpiryBadge;
        private final ProgressBar      progressFreshness;
        private final CheckBox         checkboxItem;

        DespensaViewHolder(@NonNull View v) {
            super(v);
            cardItem          = v.findViewById(R.id.cardItem);
            flIconContainer   = v.findViewById(R.id.flIconContainer);
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

            ivItemIcon.setImageResource(CategoryIconHelper.getIcon(item.getCategoria()));

            CategoryColorHelper.Colors cores = CategoryColorHelper.getColors(ctx, item.getCategoria());
            flIconContainer.setBackgroundTintList(ColorStateList.valueOf(cores.containerColor));
            ivItemIcon.setImageTintList(ColorStateList.valueOf(cores.onContainerColor));

            tvItemName.setText(item.getNome());
            tvItemQuantity.setText(formatarQuantidade(item.getQuantidade(), item.getUnidadeMedida()));

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

            itemView.setOnClickListener(v -> {
                if (modoSelecao) {
                    adapter.selecionarItem(item); // Usa o novo método corrigido
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_ID) adapter.notifyItemChanged(pos);
                } else {
                    if (clickListener != null) clickListener.onItemClick(item);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item);
                    return true;
                }
                return false;
            });
        }

        private static String formatarQuantidade(double quantidade, String unidade) {
            if (unidade == null || unidade.isEmpty()) {
                return formatarNumero(quantidade) + " unid";
            }

            String unidLower = unidade.trim().toLowerCase(Locale.getDefault());

            if (unidLower.equals("kg") && quantidade < 1.0) {
                int gramas = (int) Math.round(quantidade * 1000);
                return gramas + " g";
            }

            if ((unidLower.equals("l") || unidLower.equals("litro") || unidLower.equals("litros"))
                    && quantidade < 1.0) {
                int ml = (int) Math.round(quantidade * 1000);
                return ml + " ml";
            }

            return formatarNumero(quantidade) + " " + unidade;
        }

        private static String formatarNumero(double valor) {
            if (valor == Math.floor(valor) && !Double.isInfinite(valor)) {
                return String.valueOf((int) valor);
            }
            String formatado = String.format(Locale.getDefault(), "%.3f", valor);
            formatado = formatado.replaceAll("[,.]?0+$", "");
            if (formatado.endsWith(",") || formatado.endsWith(".")) {
                formatado = formatado.substring(0, formatado.length() - 1);
            }
            return formatado;
        }

        private static int dpToPx(Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }
}