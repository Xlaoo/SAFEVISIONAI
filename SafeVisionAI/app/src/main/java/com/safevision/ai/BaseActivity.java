package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    // Evita múltiples pulsaciones rápidas
    private static long ultimoClickNavegacion = 0;

    // Tiempo mínimo entre pulsaciones
    private static final long TIEMPO_BLOQUEO = 700;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ocultarBotonesSistema();
    }


    // =========================================================
    // OCULTAR BOTONES DEL SISTEMA
    // =========================================================

    protected void ocultarBotonesSistema() {

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(

                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                |
                                View.SYSTEM_UI_FLAG_FULLSCREEN
                                |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
    }


    // =========================================================
    // CONTROL DE DOBLE CLIC
    // =========================================================

    private boolean puedeNavegar() {

        long ahora = SystemClock.elapsedRealtime();

        if (ahora - ultimoClickNavegacion < TIEMPO_BLOQUEO) {

            return false;
        }

        ultimoClickNavegacion = ahora;

        return true;
    }


    // =========================================================
    // NAVEGACIÓN SEGURA
    // =========================================================

    private void navegarA(Class<?> actividad) {

        // Si ya estamos en esa pantalla, NO hacemos nada
        if (getClass().equals(actividad)) {
            return;
        }


        // Evitar múltiples pulsaciones rápidas
        if (!puedeNavegar()) {
            return;
        }


        Intent intent =
                new Intent(
                        this,
                        actividad
                );


        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );


        startActivity(intent);

        // Animación suave
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    }


    // =========================================================
    // MENÚ INFERIOR
    // =========================================================

    protected void configurarMenuInferior() {

        View navInicio =
                findViewById(R.id.navInicio);

        View navCamaras =
                findViewById(R.id.navCamaras);

        View navAlertas =
                findViewById(R.id.navAlertas);

        View navAjustes =
                findViewById(R.id.navAjustes);


        // =====================================================
        // INICIO
        // =====================================================

        if (navInicio != null) {

            navInicio.setOnClickListener(v -> {

                navegarA(
                        InicioActivity.class
                );

            });
        }


        // =====================================================
        // CÁMARAS
        // =====================================================

        if (navCamaras != null) {

            navCamaras.setOnClickListener(v -> {

                navegarA(
                        CamarasActivity.class
                );

            });
        }


        // =====================================================
        // ALERTAS
        // =====================================================

        if (navAlertas != null) {

            navAlertas.setOnClickListener(v -> {

                navegarA(
                        AlertasActivity.class
                );

            });
        }


        // =====================================================
        // AJUSTES
        // =====================================================

        if (navAjustes != null) {

            navAjustes.setOnClickListener(v -> {

                // Pendiente implementar Ajustes

            });
        }
    }


    // =========================================================
    // MANTENER PANTALLA COMPLETA
    // =========================================================

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);


        if (hasFocus) {

            ocultarBotonesSistema();

        }
    }
}