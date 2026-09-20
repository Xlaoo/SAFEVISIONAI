package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;

public class CamarasActivity extends BaseActivity {

    private View cardCamara1;

    private WebView webCamara1;

    private TextView txtEstadoCamara1;

    // =====================================================
    // CÁMARA 1 - LAPTOP
    // =====================================================

    private String URL_CAMARA_PC = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camaras);


        // =====================================================
        // CARDS
        // =====================================================

        cardCamara1 = findViewById(R.id.cardCamara1);



        // =====================================================
        // ESTADOS
        // =====================================================

        txtEstadoCamara1 =
                findViewById(R.id.txtEstadoCamara1);




        // =====================================================
        // WEBVIEW CÁMARA 1
        // =====================================================

        webCamara1 =
                findViewById(R.id.webCamara1);

        configurarWebView(webCamara1);


        // =====================================================
        // CÁMARA 1 - LAPTOP
        // =====================================================

        txtEstadoCamara1.setText(
                "🟡 Buscando cámara..."
        );


        CameraScanner.buscarCamara(
                this,
                new CameraScanner.Callback() {


                    @Override
                    public void encontrada(String url) {


                        URL_CAMARA_PC = url;
                        System.out.println("CAMARA ENCONTRADA: " + url);

                        runOnUiThread(() -> {


                            txtEstadoCamara1.setText(
                                    "🟢 Cámara encontrada"
                            );


                            webCamara1.loadUrl(
                                    url + "video"
                            );


                        });


                    }


                    @Override
                    public void error() {


                        runOnUiThread(() -> {


                            txtEstadoCamara1.setText(
                                    "🔴 Cámara no encontrada"
                            );


                        });


                    }


                }
        );





        // =====================================================
        // CLICK CÁMARA 1
        // =====================================================

        cardCamara1.setOnClickListener(v -> {


            if(URL_CAMARA_PC.isEmpty()){


                txtEstadoCamara1.setText(
                        "Espere, buscando cámara..."
                );


                return;

            }



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






        // =====================================================
        // MENÚ INFERIOR
        // =====================================================

        View navInicio =
                findViewById(R.id.navInicioCamaras);

        View navCamaras =
                findViewById(R.id.navCamarasActivo);

        View navAlertas =
                findViewById(R.id.navAlertasCamaras);

        View navAjustes =
                findViewById(R.id.navAjustesCamaras);


        // =====================================================
        // INICIO
        // =====================================================

        navInicio.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            CamarasActivity.this,
                            InicioActivity.class
                    );

            startActivity(intent);

            finish();
        });


        // =====================================================
        // CÁMARAS
        // =====================================================

        navCamaras.setOnClickListener(v -> {
            // Ya estamos en cámaras
        });


        // =====================================================
        // ALERTAS
        // =====================================================

        navAlertas.setOnClickListener(v -> {
            // Se implementará después
        });


        // =====================================================
        // AJUSTES
        // =====================================================

        navAjustes.setOnClickListener(v -> {
            // Se implementará después
        });
    }


    // =====================================================
    // CONFIGURAR WEBVIEW
    // =====================================================

    private void configurarWebView(WebView webView) {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setLoadWithOverviewMode(true);

        settings.setUseWideViewPort(true);

        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(
                new WebViewClient()
        );
    }


    // =====================================================
    // COMPROBAR CÁMARA
    // =====================================================

    private void comprobarEstadoCamara(
            TextView estado,
            String url
    ) {

        new Thread(() -> {

            HttpURLConnection conexion = null;

            try {

                URL direccion =
                        new URL(url);

                conexion =
                        (HttpURLConnection)
                                direccion.openConnection();

                conexion.setRequestMethod("GET");

                conexion.setConnectTimeout(3000);

                conexion.setReadTimeout(3000);

                conexion.connect();

                int codigo =
                        conexion.getResponseCode();


                runOnUiThread(() -> {

                    if (codigo == 200) {

                        estado.setText(
                                "🟢 En línea"
                        );

                        estado.setTextColor(
                                0xFF00A85A
                        );

                    } else {

                        estado.setText(
                                "🔴 Desconectada"
                        );

                        estado.setTextColor(
                                0xFFE53935
                        );
                    }
                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    estado.setText(
                            "🔴 Desconectada"
                    );

                    estado.setTextColor(
                            0xFFE53935
                    );
                });

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }
            }

        }).start();
    }


    // =====================================================
    // DESTRUIR
    // =====================================================

    @Override
    protected void onDestroy() {

        if (webCamara1 != null) {

            webCamara1.stopLoading();

            webCamara1.destroy();
        }

        super.onDestroy();
    }
}