package com.example.inventaai.ui.chefIA;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventaai.R;
import com.example.inventaai.ui.cadastro.CadastroActivity;
import com.example.inventaai.ui.dashboard.DashboardActivity;
import com.example.inventaai.ui.historico.HistoricoActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

/**
 * ChefIAActivity — exibe a receita gerada pela IA.
 *
 * Sprint 2: Layout estático com dados de exemplo.
 * Sprint 3: Integrar chamada à API de IA e preencher os campos dinamicamente.
 */
public class ChefIAActivity extends AppCompatActivity {

    // Views
    private TextView tvRecipeTitle;
    private TextView tvRecipeDescription;
    private TextView tvTime;
    private TextView tvServings;
    private TextView tvDifficulty;
    private LinearLayout llSteps;
    private MaterialButton btnSalvarReceita;
    private MaterialButton btnNovaReceita;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chef_ia);

        vincularViews();
        preencherDadosEstaticos();
        configurarBotoes();
        configurarBottomNavigation();
    }

    // =========================================================================
    // INICIALIZAÇÃO
    // =========================================================================

    private void vincularViews() {
        tvRecipeTitle       = findViewById(R.id.tvRecipeTitle);
        tvRecipeDescription = findViewById(R.id.tvRecipeDescription);
        tvTime              = findViewById(R.id.tvTime);
        tvServings          = findViewById(R.id.tvServings);
        tvDifficulty        = findViewById(R.id.tvDifficulty);
        llSteps             = findViewById(R.id.llSteps);
        btnSalvarReceita    = findViewById(R.id.btnSalvarReceita);
        btnNovaReceita      = findViewById(R.id.btnNovaReceita);
        bottomNavigation    = findViewById(R.id.bottomNavigation);
    }

    /**
     * Preenche os campos com dados de exemplo para validação do layout na Sprint 2.
     * Na Sprint 3 esses valores virão da resposta da API de IA.
     */
    private void preencherDadosEstaticos() {
        tvRecipeTitle.setText("Frango em Crosta de Ervas");
        tvRecipeDescription.setText(
                "Uma transformação mágica dos itens básicos da sua despensa em um jantar "
                        + "leve e nutritivo. Crocante por fora, macio por dentro.");
        tvTime.setText("25 min");
        tvServings.setText("2 porções");
        tvDifficulty.setText("Fácil");
    }

    // =========================================================================
    // EVENTOS
    // =========================================================================

    private void configurarBotoes() {
        btnSalvarReceita.setOnClickListener(v -> {
            // Sprint 3: persistir receita localmente
            android.widget.Toast.makeText(this,
                    "Receita salva! (funcionalidade completa na Sprint 3)",
                    android.widget.Toast.LENGTH_SHORT).show();
        });

        btnNovaReceita.setOnClickListener(v -> {
            // Sprint 3: chamar API de IA novamente com itens atualizados da despensa
            android.widget.Toast.makeText(this,
                    "Gerando nova receita... (Sprint 3)",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    private void configurarBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_chef_ia);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_chef_ia) {
                return true;
            } else if (id == R.id.nav_pantry) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CadastroActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoricoActivity.class));
                return true;
            }

            return false;
        });
    }
}
