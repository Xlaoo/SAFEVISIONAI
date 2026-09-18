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
    private View cardCamara2;
    private View cardCamara3;

    private WebView webCamara1;

    private TextView txtEstadoCamara1;
    private TextView txtEstadoCamara2;
    private TextView txtEstadoCamara3;

    // =====================================================
    // CÁMARA 1 - LAPTOP
    // =====================================================

    private static final String URL_CAMARA_PC =
            "http://10.226.222.107:5000/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camaras);


        // =====================================================
        // CARDS
        // =====================================================

        cardCamara1 = findViewById(R.id.cardCamara1);

        cardCamara2 = findViewById(R.id.cardCamara2);

        cardCamara3 = findViewById(R.id.cardCamara3);


        // =====================================================
        // ESTADOS
        // =====================================================

        txtEstadoCamara1 =
                findViewById(R.id.txtEstadoCamara1);

        txtEstadoCamara2 =
                findViewById(R.id.txtEstadoCamara2);

        txtEstadoCamara3 =
                findViewById(R.id.txtEstadoCamara3);


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
                "🟡 Comprobando conexión..."
        );

        comprobarEstadoCamara(
                txtEstadoCamara1,
                URL_CAMARA_PC
        );


        // Mostrar video de la laptop
        webCamara1.loadUrl(
                URL_CAMARA_PC + "video"
        );


        // =====================================================
        // CÁMARA 2 - CELULAR
        // =====================================================

        txtEstadoCamara2.setText(
                "🔴 Desconectada"
        );

        txtEstadoCamara2.setTextColor(
                0xFFE53935
        );


        // =====================================================
        // CÁMARA 3
        // =====================================================

        txtEstadoCamara3.setText(
                "🔴 Desconectada"
        );

        txtEstadoCamara3.setTextColor(
                0xFFE53935
        );


        // =====================================================
        // CLICK CÁMARA 1
        // =====================================================

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


        // =====================================================
        // CLICK CÁMARA 2
        // =====================================================

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

            // Aquí posteriormente pondremos
            // la IP/URL de la cámara del celular.

            startActivity(intent);
        });


        // =====================================================
        // CLICK CÁMARA 3
        // =====================================================

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