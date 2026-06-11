package com.example.inventaai.ui.sincronizacao;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;

import java.util.List;

/**
 * Sprint 20 — Adapter para a lista de pré-visualização de importação CSV.
 *
 * Cada item exibe:
 *  - Nome, quantidade, unidade, validade
 *  - Spinner de categoria editável inline
 *  - Ícone de alerta + mensagem de erro para itens inválidos
 *
 * Alterações feitas pelo usuário no Spinner são aplicadas diretamente
 * no objeto DespensaItem da lista, para que a Activity leia os valores
 * atualizados ao confirmar a importação.
 */
public class PreviewImportAdapter
        extends RecyclerView.Adapter<PreviewImportAdapter.ViewHolder> {

    private final List<DespensaItem> itens;
    private final String[]           categorias;
    private final Context            context;

    public PreviewImportAdapter(Context context,
                                List<DespensaItem> itens,
                                String[] categorias) {
        this.context    = context;
        this.itens      = itens;
        this.categorias = categorias;
    }

    // =========================================================================
    // RecyclerView.Adapter
    // =========================================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview_import, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DespensaItem item    = itens.get(position);
        boolean      invalido = "INVALIDO".equals(item.getStatus());

        // Nome
        holder.tvNome.setText(item.getNome());

        // Quantidade + Unidade
        String unidade = item.getUnidadeMedida() != null ? item.getUnidadeMedida() : "unid";
        holder.tvQuantidade.setText(formatarQtd(item.getQuantidade()) + " " + unidade);

        // Validade
        String val = item.getDataValidade();
        if (val != null && !val.isEmpty()) {
            holder.tvValidade.setText("Val: " + val);
            holder.tvValidade.setVisibility(View.VISIBLE);
        } else {
            holder.tvValidade.setVisibility(View.GONE);
        }

        // ── Spinner de Categoria ─────────────────────────────────────────────
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, categorias);
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerCategoria.setAdapter(spinnerAdapter);

        // Pré-seleciona a categoria do item (ou índice 0 se não encontrar)
        int idx = encontrarIndiceCategoria(item.getCategoria());
        holder.spinnerCategoria.setSelection(idx);

        // Persiste no modelo ao usuário trocar no Spinner
        holder.spinnerCategoria.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int pos, long id) {
                        // Usa getAdapterPosition para posição atual (evita stale position)
                        int adapterPos = holder.getAdapterPosition();
                        if (adapterPos != RecyclerView.NO_ID) {
                            itens.get(adapterPos).setCategoria(categorias[pos]);
                            // Se escolheu categoria válida, limpa flag de inválido parcialmente
                            if (itens.get(adapterPos).getCategoria() != null
                                    && !itens.get(adapterPos).getCategoria().isEmpty()) {
                                // Mantém INVALIDO só se quantidade ainda for problemática
                                // (a mensagem de erro já esclarece)
                            }
                        }
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });

        // ── Indicadores de validação ─────────────────────────────────────────
        if (invalido) {
            holder.ivAlerta.setVisibility(View.VISIBLE);
            holder.tvErro.setVisibility(View.VISIBLE);
            holder.tvErro.setText(gerarMensagemErro(item));
            // Fundo levemente avermelhado para destaque
            holder.itemView.setAlpha(0.92f);
        } else {
            holder.ivAlerta.setVisibility(View.GONE);
            holder.tvErro.setVisibility(View.GONE);
            holder.itemView.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private int encontrarIndiceCategoria(String categoria) {
        if (categoria == null) return 0;
        for (int i = 0; i < categorias.length; i++) {
            if (categorias[i].equalsIgnoreCase(categoria)) return i;
        }
        return categorias.length - 1; // "Outros" é o último
    }

    private String gerarMensagemErro(DespensaItem item) {
        StringBuilder sb = new StringBuilder();
        if (item.getNome() == null || item.getNome().equals("(sem nome)")) {
            sb.append("Nome ausente. ");
        }
        if (item.getQuantidade() <= 0) {
            sb.append("Quantidade inválida — será importada como 1. ");
        }
        if (sb.length() == 0) {
            sb.append("Verifique os dados antes de confirmar.");
        }
        return sb.toString().trim();
    }

    private String formatarQtd(double q) {
        if (q == Math.floor(q) && !Double.isInfinite(q)) {
            return String.valueOf((long) q);
        }
        return String.format(java.util.Locale.getDefault(), "%.1f", q);
    }

    // =========================================================================
    // ViewHolder
    // =========================================================================

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView  tvNome;
        final TextView  tvQuantidade;
        final TextView  tvValidade;
        final Spinner   spinnerCategoria;
        final ImageView ivAlerta;
        final TextView  tvErro;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome           = itemView.findViewById(R.id.tvNome);
            tvQuantidade     = itemView.findViewById(R.id.tvQuantidade);
            tvValidade       = itemView.findViewById(R.id.tvValidade);
            spinnerCategoria = itemView.findViewById(R.id.spinnerCategoria);
            ivAlerta         = itemView.findViewById(R.id.ivAlerta);
            tvErro           = itemView.findViewById(R.id.tvErro);
        }
    }
}