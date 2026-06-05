package com.example.inventaai.ui.receitas;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.inventaai.R;
import com.example.inventaai.data.db.DatabaseContract;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.data.repository.HistoricoRepository;
import com.example.inventaai.util.AppExecutors;
import com.example.inventaai.util.Constants;
import com.example.inventaai.util.DateUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ConfirmarIngredientesDialog extends BottomSheetDialogFragment {

    public interface OnConfirmarListener {
        void onConfirmado();
    }

    private static final String ARG_RECEITA_NOME = "receita_nome";
    private static final String ARG_USER_ID      = "user_id";

    private List<IngredienteMatch> matches;
    private String receitaNome;
    private String userId;
    private OnConfirmarListener listener;

    // Campos editáveis para cada item
    private final List<EditText> editQuantidades = new ArrayList<>();

    public static ConfirmarIngredientesDialog newInstance(
            String receitaNome,
            String userId,
            List<IngredienteMatch> matches,
            OnConfirmarListener listener) {

        ConfirmarIngredientesDialog dialog = new ConfirmarIngredientesDialog();
        Bundle args = new Bundle();
        args.putString(ARG_RECEITA_NOME, receitaNome);
        args.putString(ARG_USER_ID,      userId);
        dialog.setArguments(args);
        dialog.matches  = matches;
        dialog.listener = listener;
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            receitaNome = getArguments().getString(ARG_RECEITA_NOME, "Receita");
            userId      = getArguments().getString(ARG_USER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirmar_ingredientes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitulo = view.findViewById(R.id.tvDialogTitulo);
        tvTitulo.setText("Finalizar: " + receitaNome);

        LinearLayout llItens = view.findViewById(R.id.llItensDialog);
        editQuantidades.clear();

        // Filtra somente itens que estão na despensa (POSSUI ou INSUFICIENTE)
        List<IngredienteMatch> comDespensa = new ArrayList<>();
        if (matches != null) {
            for (IngredienteMatch m : matches) {
                if (m.getStatus() != IngredienteMatch.Status.FALTA && m.getItemDespensa() != null) {
                    comDespensa.add(m);
                }
            }
        }

        if (comDespensa.isEmpty()) {
            TextView tvVazio = new TextView(requireContext());
            tvVazio.setText("Nenhum ingrediente encontrado na despensa para deduzir.");
            tvVazio.setPadding(0, 16, 0, 16);
            llItens.addView(tvVazio);
        }

        for (IngredienteMatch m : comDespensa) {
            adicionarLinhaItem(llItens, m);
        }

        // Botão confirmar
        MaterialButton btnConfirmar = view.findViewById(R.id.btnConfirmarBaixa);
        final List<IngredienteMatch> finalComDespensa = comDespensa;
        btnConfirmar.setOnClickListener(v -> confirmarBaixa(finalComDespensa));

        // Botão cancelar
        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarBaixa);
        btnCancelar.setOnClickListener(v -> dismiss());
    }

    // ── Linha de item ─────────────────────────────────────────────────────────

    private void adicionarLinhaItem(LinearLayout container, IngredienteMatch m) {
        Context ctx = requireContext();
        DespensaItem item = m.getItemDespensa();

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rowParams);

        // Nome do item
        TextView tvNome = new TextView(ctx);
        tvNome.setText(item.getNome());
        tvNome.setTextSize(14f);
        LinearLayout.LayoutParams nomeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvNome.setLayoutParams(nomeParams);

        // Campo editável de quantidade
        EditText etQtd = new EditText(ctx);
        double sugestao = Math.min(
                m.getQuantidadePedida() > 0 ? m.getQuantidadePedida() : item.getQuantidade(),
                item.getQuantidade());
        etQtd.setText(String.valueOf(sugestao > 0 ? sugestao : item.getQuantidade()));
        etQtd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etQtd.setMinWidth(160);
        etQtd.setTextSize(14f);
        // Tag guarda o item para recuperar na confirmação
        etQtd.setTag(item);

        row.addView(tvNome);
        row.addView(etQtd);
        container.addView(row);
        editQuantidades.add(etQtd);
    }

    // ── Confirmar baixa ───────────────────────────────────────────────────────

    private void confirmarBaixa(List<IngredienteMatch> comDespensa) {
        if (editQuantidades.size() != comDespensa.size()) {
            dismiss();
            return;
        }

        Context ctx = requireContext().getApplicationContext();
        DespensaRepository despensaRepo = new DespensaRepository(ctx);
        HistoricoRepository historicoRepo = new HistoricoRepository(ctx);
        String motivoBase = "Receita Preparada: " + receitaNome;

        AppExecutors.diskIO().execute(() -> {
            for (int i = 0; i < comDespensa.size(); i++) {
                DespensaItem item       = comDespensa.get(i).getItemDespensa();
                double       qtdReduzir;
                try {
                    qtdReduzir = Double.parseDouble(
                            editQuantidades.get(i).getText().toString().replace(",", "."));
                } catch (NumberFormatException e) {
                    qtdReduzir = item.getQuantidade();
                }

                double novaQtd = item.getQuantidade() - qtdReduzir;

                if (novaQtd <= 0) {
                    // Move para histórico marcando como consumido
                    despensaRepo.moverParaHistorico(item.getId(), item.getNome(),
                            Constants.STATUS_CONSUMIDO, userId);
                } else {
                    // Atualiza quantidade
                    item.setQuantidade(novaQtd);
                    despensaRepo.atualizar(item);
                    // Registra no histórico
                    com.example.inventaai.data.model.HistoricoItem hist =
                            new com.example.inventaai.data.model.HistoricoItem();
                    hist.setIdItem(item.getId());
                    hist.setNomeCached(item.getNome());
                    hist.setMotivo(motivoBase);
                    hist.setDataAcao(DateUtils.hoje());
                    registrarHistorico(historicoRepo, item, motivoBase);
                }
            }

            AppExecutors.mainThread().execute(() -> {
                Toast.makeText(ctx, "Despensa atualizada!", Toast.LENGTH_SHORT).show();
                dismiss();
                if (listener != null) listener.onConfirmado();
            });
        });
    }

    private void registrarHistorico(HistoricoRepository repo,
                                    DespensaItem item, String motivo) {
        // Usa reflexão ao HistoricoRepository não ter insertHistorico público exposto.
        // Como o moverParaHistorico já insere no histórico, aqui fazemos insert direto
        // via DatabaseHelper para não alterar o repositório legado.
        try {
            android.database.sqlite.SQLiteDatabase db =
                    com.example.inventaai.data.db.DatabaseHelper
                            .getInstance(requireContext().getApplicationContext())
                            .getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.HistoricoEntry.COLUMN_ID_ITEM,     item.getId());
            cv.put(DatabaseContract.HistoricoEntry.COLUMN_NOME_CACHED, item.getNome());
            cv.put(DatabaseContract.HistoricoEntry.COLUMN_MOTIVO,      motivo);
            cv.put(DatabaseContract.HistoricoEntry.COLUMN_USER_ID,     userId);
            cv.put(DatabaseContract.HistoricoEntry.COLUMN_DATA_ACAO,   DateUtils.hoje());
            db.insert(DatabaseContract.HistoricoEntry.TABLE_NAME, null, cv);
        } catch (Exception e) {
            android.util.Log.e("ConfirmarDialog", "registrarHistorico: erro", e);
        }
    }
}