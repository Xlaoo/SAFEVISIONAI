package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class CamarasActivity extends BaseActivity {

    private View cardCamara1;
    private View cardCamara2;
    private View cardCamara3;
    private WebView webCamara1;
    private WebView webCamara2;
    private WebView webCamara3;
    private TextView txtEstadoCamara1;
    private TextView txtEstadoCamara2;
    private TextView txtEstadoCamara3;

    // IP de la PC donde funciona Python
    private static final String URL_CAMARA_PC =
            "http://192.168.18.127:5000/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camaras);
        webCamara1 = findViewById(R.id.webCamara1);
        webCamara2 = findViewById(R.id.webCamara2);
        webCamara3 = findViewById(R.id.webCamara3);


        configurarWebView(webCamara1);
        configurarWebView(webCamara2);
        configurarWebView(webCamara3);

        // ==========================================
        // WEBVIEWS
        // ==========================================

        cardCamara1 =
                findViewById(R.id.cardCamara1);


        cardCamara2 =
                findViewById(R.id.cardCamara2);


        cardCamara3 =
                findViewById(R.id.cardCamara3);


        // ==========================================
        // ESTADOS
        // ==========================================

        txtEstadoCamara1 =
                findViewById(R.id.txtEstadoCamara1);

        txtEstadoCamara2 =
                findViewById(R.id.txtEstadoCamara2);

        txtEstadoCamara3 =
                findViewById(R.id.txtEstadoCamara3);


        // ==========================================
        // CONFIGURAR WEBVIEWS
        // ==========================================



        // ==========================================
        // CÁMARA 1 - PC
        // ==========================================

        comprobarEstadoCamara(
                txtEstadoCamara1,
                URL_CAMARA_PC
        );
        webCamara1.loadUrl(
                URL_CAMARA_PC + "video"
        );


        // ==========================================
        // CÁMARA 2
        // ==========================================
        txtEstadoCamara2.setText(
                "🔴 Desconectada"
        );

        txtEstadoCamara2.setTextColor(
                0xFFE53935
        );


        txtEstadoCamara3.setText(
                "🔴 Desconectada"
        );

        txtEstadoCamara3.setTextColor(
                0xFFE53935
        );
        cardCamara1.setOnClickListener(v -> {


            Intent intent =
                    new Intent(
                            CamarasActivity.this,
                            DetalleCamaraActivity.class
                    );


            intent.putExtra(
                    "CAMARA",
                    "Producción"
            );


            intent.putExtra(
                    "URL",
                    URL_CAMARA_PC
            );


            startActivity(intent);


        });
        cardCamara2.setOnClickListener(v -> {


            Intent intent =
                    new Intent(
                            CamarasActivity.this,
                            DetalleCamaraActivity.class
                    );


            intent.putExtra(
                    "CAMARA",
                    "Almacén"
            );


            startActivity(intent);


        });
        cardCamara3.setOnClickListener(v -> {


            Intent intent =
                    new Intent(
                            CamarasActivity.this,
                            DetalleCamaraActivity.class
                    );


            intent.putExtra(
                    "CAMARA",
                    "Ingreso"
            );


            startActivity(intent);


        });


        // ==========================================
        // MENÚ INFERIOR
        // ==========================================

        View navInicio =
                findViewById(R.id.navInicioCamaras);

        View navCamaras =
                findViewById(R.id.navCamarasActivo);

        View navAlertas =
                findViewById(R.id.navAlertasCamaras);

        View navAjustes =
                findViewById(R.id.navAjustesCamaras);


        // ==========================================
        // BOTÓN INICIO
        // ==========================================

        navInicio.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CamarasActivity.this,
                    InicioActivity.class
            );

            startActivity(intent);

            finish();
        });


        // ==========================================
        // BOTÓN CÁMARAS
        // ==========================================

        navCamaras.setOnClickListener(v -> {

            // Ya estamos en Cámaras.
            // No hacemos nada.
        });


        // ==========================================
        // ALERTAS
        // ==========================================

        navAlertas.setOnClickListener(v -> {

            // Por ahora no hacemos nada.
            // Luego conectaremos esta opción
            // con la pantalla de Alertas.
        });


        // ==========================================
        // AJUSTES
        // ==========================================

        navAjustes.setOnClickListener(v -> {

            // Por ahora no hacemos nada.
            // Luego conectaremos esta opción
            // con la pantalla de Ajustes.
        });
    }


    // ==============================================
    // CONFIGURAR WEBVIEW
    // ==============================================
    private void configurarWebView(WebView webView){

        WebSettings settings =
                webView.getSettings();


        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setLoadWithOverviewMode(true);

        settings.setUseWideViewPort(true);


    }
    private void comprobarEstadoCamara(
            TextView estado,
            String url
    ){

        new Thread(() -> {


            try {


                java.net.URL direccion =
                        new java.net.URL(url);


                java.net.HttpURLConnection conexion =
                        (java.net.HttpURLConnection)
                                direccion.openConnection();


                conexion.setConnectTimeout(3000);

                conexion.connect();


                int codigo =
                        conexion.getResponseCode();



                runOnUiThread(() -> {


                    if(codigo == 200){


                        estado.setText(
                                "🟢 En línea"
                        );


                        estado.setTextColor(
                                0xFF00A85A
                        );


                    }else{


                        estado.setText(
                                "🔴 Desconectada"
                        );


                        estado.setTextColor(
                                0xFFE53935
                        );

                    }


                });



            }catch(Exception e){



                runOnUiThread(() -> {


                    estado.setText(
                            "🔴 Desconectada"
                    );


                    estado.setTextColor(
                            0xFFE53935
                    );


                });


            }



        }).start();


    }

    // ==============================================
    // DESTRUIR WEBVIEWS
    // ==============================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
}