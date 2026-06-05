package com.example.inventaai.ui.despensa;

import androidx.recyclerview.widget.DiffUtil;

import com.example.inventaai.data.model.DespensaItem;

import java.util.List;
import java.util.Objects;

public class DespensaItemDiffCallback extends DiffUtil.Callback {

    private final List<DespensaItem> listaAntiga;
    private final List<DespensaItem> listaNova;

    public DespensaItemDiffCallback(List<DespensaItem> listaAntiga,
                                    List<DespensaItem> listaNova) {
        this.listaAntiga = listaAntiga;
        this.listaNova   = listaNova;
    }

    @Override
    public int getOldListSize() {
        return listaAntiga.size();
    }

    @Override
    public int getNewListSize() {
        return listaNova.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return listaAntiga.get(oldItemPosition).getId()
                == listaNova.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        DespensaItem antigo = listaAntiga.get(oldItemPosition);
        DespensaItem novo   = listaNova.get(newItemPosition);

        return antigo.getId()         == novo.getId()
                && antigo.getQuantidade() == novo.getQuantidade()
                && Objects.equals(antigo.getNome(),          novo.getNome())
                && Objects.equals(antigo.getUnidadeMedida(), novo.getUnidadeMedida())
                && Objects.equals(antigo.getDataValidade(),  novo.getDataValidade())
                && Objects.equals(antigo.getStatus(),        novo.getStatus())
                && Objects.equals(antigo.getCategoria(),     novo.getCategoria());
    }
}