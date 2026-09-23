package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ocultarBotonesSistema();
    }









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
    protected void configurarMenuInferior(){

        View navInicio = findViewById(R.id.navInicio);
        View navCamaras = findViewById(R.id.navCamaras);
        View navAlertas = findViewById(R.id.navAlertas);
        View navAjustes = findViewById(R.id.navAjustes);


        if(navInicio != null){

            navInicio.setOnClickListener(v -> {

                Intent intent = new Intent(
                        this,
                        InicioActivity.class
                );

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();

            });

        }


        if(navCamaras != null){

            navCamaras.setOnClickListener(v -> {

                Intent intent = new Intent(
                        this,
                        CamarasActivity.class
                );

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();

            });

        }



        if(navAlertas != null){

            navAlertas.setOnClickListener(v -> {

                Intent intent = new Intent(
                        this,
                        AlertasActivity.class
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();

            });

        }



        if(navAjustes != null){

            navAjustes.setOnClickListener(v -> {


                // pendiente ajustes


            });

        }

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);


        if(hasFocus){

            ocultarBotonesSistema();

        }

    }


}