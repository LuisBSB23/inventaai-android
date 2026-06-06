package com.example.inventaai.ui.receitas;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.inventaai.R;
import com.example.inventaai.data.model.DespensaItem;
import com.example.inventaai.data.repository.DespensaRepository;
import com.example.inventaai.util.AppExecutors;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 15: BottomSheetDialog que exibe a lista de ingredientes do cross-check
 * para confirmação/edição de quantidades antes de baixar o estoque.
 *
 * <p>Cada linha tem:
 * <ul>
 *   <li>Nome do item</li>
 *   <li>Campo editável com a quantidade sugerida</li>
 *   <li>Ícone ✕ para remover o item da baixa</li>
 * </ul>
 * </p>
 */
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

    // Estrutura interna para as linhas editáveis
    private static class LinhaItem {
        DespensaItem item;
        double       qtdSugerida;
        EditText     etQtd;
        boolean      removido = false;

        LinhaItem(DespensaItem item, double qtdSugerida) {
            this.item        = item;
            this.qtdSugerida = qtdSugerida;
        }
    }

    private final List<LinhaItem> linhas = new ArrayList<>();

    // =========================================================================
    // Factory
    // =========================================================================

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

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

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
        linhas.clear();

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
            tvVazio.setTextColor(requireContext().getColor(R.color.colorOnSurfaceVariant));
            llItens.addView(tvVazio);
        } else {
            for (IngredienteMatch m : comDespensa) {
                adicionarLinhaItem(llItens, m);
            }
        }

        MaterialButton btnConfirmar = view.findViewById(R.id.btnConfirmarBaixa);
        btnConfirmar.setOnClickListener(v -> confirmarBaixa());

        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarBaixa);
        btnCancelar.setOnClickListener(v -> dismiss());
    }

    // =========================================================================
    // Linha de item com campo editável e botão remover
    // =========================================================================

    private void adicionarLinhaItem(LinearLayout container, IngredienteMatch m) {
        Context ctx  = requireContext();
        DespensaItem item = m.getItemDespensa();

        double sugestao = Math.min(
                m.getQuantidadePedida() > 0 ? m.getQuantidadePedida() : item.getQuantidade(),
                item.getQuantidade());
        if (sugestao <= 0) sugestao = item.getQuantidade();

        LinhaItem linha = new LinhaItem(item, sugestao);
        linhas.add(linha);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Nome
        TextView tvNome = new TextView(ctx);
        tvNome.setText(item.getNome());
        tvNome.setTextSize(14f);
        tvNome.setTextColor(ctx.getColor(R.color.colorOnSurface));
        LinearLayout.LayoutParams nomeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvNome.setLayoutParams(nomeParams);

        // Campo de quantidade editável
        EditText etQtd = new EditText(ctx);
        etQtd.setText(formatarQtd(sugestao));
        etQtd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etQtd.setMinWidth(140);
        etQtd.setTextSize(14f);
        etQtd.setTextColor(ctx.getColor(R.color.colorOnSurface));
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etParams.setMarginEnd(8);
        etQtd.setLayoutParams(etParams);
        linha.etQtd = etQtd;

        // Botão remover (✕)
        ImageButton btnRemover = new ImageButton(ctx);
        btnRemover.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnRemover.setBackground(null);
        btnRemover.setContentDescription("Remover item da baixa");
        btnRemover.setOnClickListener(v -> {
            linha.removido = true;
            row.setAlpha(0.35f);
            row.setEnabled(false);
            etQtd.setEnabled(false);
            btnRemover.setEnabled(false);
        });

        row.addView(tvNome);
        row.addView(etQtd);
        row.addView(btnRemover);
        container.addView(row);
    }

    // =========================================================================
    // Confirmação — delega ao DespensaRepository.processarBaixas()
    // =========================================================================

    private void confirmarBaixa() {
        List<DespensaRepository.BaixaItem> baixas = new ArrayList<>();

        for (LinhaItem linha : linhas) {
            if (linha.removido) continue; // usuário removeu este item

            double qtdReduzir;
            try {
                qtdReduzir = Double.parseDouble(
                        linha.etQtd.getText().toString().replace(",", ".").trim());
            } catch (NumberFormatException e) {
                qtdReduzir = linha.qtdSugerida;
            }

            if (qtdReduzir > 0) {
                baixas.add(new DespensaRepository.BaixaItem(linha.item, qtdReduzir));
            }
        }

        if (baixas.isEmpty()) {
            dismiss();
            if (listener != null) listener.onConfirmado();
            return;
        }

        Context ctx = requireContext().getApplicationContext();
        DespensaRepository repo = new DespensaRepository(ctx);
        String origem = receitaNome;

        AppExecutors.diskIO().execute(() -> {
            boolean ok = repo.processarBaixas(baixas, userId, origem);
            AppExecutors.mainThread().execute(() -> {
                if (ok) {
                    Toast.makeText(ctx, "Despensa atualizada!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ctx, "Erro ao atualizar despensa.", Toast.LENGTH_SHORT).show();
                }
                dismiss();
                if (listener != null) listener.onConfirmado();
            });
        });
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String formatarQtd(double qtd) {
        if (qtd == Math.floor(qtd)) return String.valueOf((int) qtd);
        return String.format("%.2f", qtd).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}