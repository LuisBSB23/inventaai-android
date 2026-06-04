package com.example.inventaai.ui.receitas;

import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.inventaai.R;
import com.example.inventaai.data.model.ReceitaSalva;
import com.example.inventaai.util.GlideHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ReceitaDetalheActivity extends AppCompatActivity {

    public static final String EXTRA_RECEITA = "extra_receita_salva";

    private ImageView   ivRecipeImage;
    private TextView    tvRecipeTitle;
    private TextView    tvTime;
    private TextView    tvServings;
    private TextView    tvDifficulty;
    private GridLayout  gridIngredientes;
    private LinearLayout llSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receita_detalhe);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vincularViews();

        ReceitaSalva receita = (ReceitaSalva) getIntent().getSerializableExtra(EXTRA_RECEITA);
        if (receita == null) {
            finish();
            return;
        }

        preencherReceita(receita);
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        ivRecipeImage    = findViewById(R.id.ivRecipeImage);
        tvRecipeTitle    = findViewById(R.id.tvRecipeTitle);
        tvTime           = findViewById(R.id.tvTime);
        tvServings       = findViewById(R.id.tvServings);
        tvDifficulty     = findViewById(R.id.tvDifficulty);
        gridIngredientes = findViewById(R.id.gridIngredientes);
        llSteps          = findViewById(R.id.llSteps);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    // =========================================================================
    // PREENCHER UI
    // =========================================================================

    private void preencherReceita(ReceitaSalva receita) {
        // Imagem
        if (receita.getImagemUrl() != null && !receita.getImagemUrl().isEmpty()) {
            GlideHelper.loadImage(this, receita.getImagemUrl(), ivRecipeImage);
        }

        // Metadados
        tvRecipeTitle.setText(receita.getTitulo() != null ? receita.getTitulo() : "Receita");
        tvTime.setText(receita.getTempoPreparo() != null ? receita.getTempoPreparo() : "—");
        tvServings.setText(receita.getPorcoes() != null ? receita.getPorcoes() : "—");
        tvDifficulty.setText(receita.getDificuldade() != null ? receita.getDificuldade() : "—");

        // Ingredientes
        gridIngredientes.removeAllViews();
        List<String> ingredientes = receita.getIngredientes();
        if (ingredientes != null && !ingredientes.isEmpty()) {
            for (String ingrediente : ingredientes) {
                String[] partes = ingrediente.split(" - ", 2);
                adicionarCartaoIngrediente(
                        partes[0].trim(),
                        partes.length > 1 ? partes[1].trim() : "");
            }
        }

        // Passos
        llSteps.removeAllViews();
        List<String> passos = receita.getPassos();
        if (passos != null && !passos.isEmpty()) {
            for (int i = 0; i < passos.size(); i++) {
                adicionarPasso(i + 1, passos.get(i));
            }
        }
    }

    // =========================================================================
    // HELPERS DE UI
    // =========================================================================

    private void adicionarCartaoIngrediente(String nome, String quantidade) {
        MaterialCardView card = new MaterialCardView(this);
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.width = 0;
        cardParams.setMargins(8, 8, 8, 8);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getColor(R.color.colorSurfaceContainerLowest));
        card.setRadius(dpToPx(16));
        card.setCardElevation(0f);
        card.setStrokeColor(getColor(R.color.colorSurfaceContainerHighest));
        card.setStrokeWidth(dpToPx(1));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = dpToPx(12);
        row.setPadding(pad, pad, pad, pad);

        LinearLayout colTexto = new LinearLayout(this);
        colTexto.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colTexto.setLayoutParams(colParams);

        TextView tvNome = new TextView(this);
        tvNome.setText(nome);
        tvNome.setTextSize(13f);
        tvNome.setTextColor(getColor(R.color.colorOnSurface));
        tvNome.setTypeface(getResources().getFont(R.font.inter_semibold));
        tvNome.setMaxLines(2);
        tvNome.setEllipsize(android.text.TextUtils.TruncateAt.END);
        colTexto.addView(tvNome);

        if (quantidade != null && !quantidade.isEmpty()) {
            TextView tvQtd = new TextView(this);
            tvQtd.setText(quantidade);
            tvQtd.setTextSize(12f);
            tvQtd.setTextColor(getColor(R.color.colorOnSurfaceVariant));
            tvQtd.setTypeface(getResources().getFont(R.font.inter_regular));
            colTexto.addView(tvQtd);
        }

        row.addView(colTexto);
        card.addView(row);
        gridIngredientes.addView(card);
    }

    private void adicionarPasso(int numero, String descricao) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(20));
        row.setLayoutParams(rowParams);

        TextView tvNum = new TextView(this);
        tvNum.setText(String.valueOf(numero));
        tvNum.setTextSize(13f);
        tvNum.setTextColor(getColor(R.color.colorOnPrimaryContainer));
        tvNum.setTypeface(getResources().getFont(R.font.inter_bold));
        tvNum.setGravity(android.view.Gravity.CENTER);
        tvNum.setBackground(getDrawable(R.drawable.bg_circle_primary_container));
        int size = dpToPx(32);
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(size, size);
        numParams.setMargins(0, dpToPx(4), dpToPx(16), 0);
        tvNum.setLayoutParams(numParams);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(descricao);
        tvDesc.setTextSize(15f);
        tvDesc.setTextColor(getColor(R.color.colorOnSurface));
        tvDesc.setTypeface(getResources().getFont(R.font.inter_regular));
        tvDesc.setLineSpacing(dpToPx(4), 1f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvDesc.setLayoutParams(descParams);

        row.addView(tvNum);
        row.addView(tvDesc);
        llSteps.addView(row);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}