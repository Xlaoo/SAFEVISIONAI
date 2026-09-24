package com.safevision.ai;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetalleCamaraActivity extends BaseActivity {

    private TextView titulo;
    private TextView btnRegresar;
    private TextView txtFechaHora;
    private TextView txtEstadoPersona;

    private WebView webCamara;

    private Handler handler = new Handler();

    private OkHttpClient clienteHttp = new OkHttpClient();

    // =========================================================
    // IP DE LA LAPTOP
    // =========================================================

    private static final String IP_LAPTOP = "10.237.144.107";

    private static final String URL_BASE =
            "http://" + IP_LAPTOP + ":5000/";

    private static final String URL_VIDEO =
            URL_BASE + "video";

    private static final String URL_ESTADO =
            URL_BASE + "estado";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_detalle_camara
        );


        // =====================================================
        // REFERENCIAS
        // =====================================================

        titulo =
                findViewById(
                        R.id.txtTituloCamara
                );

        btnRegresar =
                findViewById(
                        R.id.btnRegresar
                );

        txtFechaHora =
                findViewById(
                        R.id.txtFechaHora
                );

        txtEstadoPersona =
                findViewById(
                        R.id.txtEstadoPersona
                );

        webCamara =
                findViewById(
                        R.id.webCamaraDetalle
                );


        // =====================================================
        // NOMBRE DE LA CÁMARA
        // =====================================================

        String camara =
                getIntent().getStringExtra("CAMARA");

        if (camara == null || camara.isEmpty()) {

            camara = "Casco";

        }

        titulo.setText(
                "Cámara - " + camara
        );


        // =====================================================
        // BOTÓN REGRESAR
        // =====================================================

        btnRegresar.setOnClickListener(
                v -> finish()
        );


        // =====================================================
        // CONFIGURACIÓN WEBVIEW
        // =====================================================

        WebSettings settings =
                webCamara.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setLoadWithOverviewMode(true);

        settings.setUseWideViewPort(true);

        settings.setBuiltInZoomControls(false);

        settings.setDisplayZoomControls(false);

        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        );


        webCamara.setWebViewClient(
                new android.webkit.WebViewClient()
        );


        // =====================================================
        // MOSTRAR VIDEO DE LA CÁMARA
        // =====================================================

        webCamara.loadUrl(
                URL_VIDEO
        );


        // =====================================================
        // FECHA Y HORA
        // =====================================================

        actualizarFechaHora();


        // =====================================================
        // CONSULTAR ESTADO DEL CASCO
        // =====================================================

        consultarEstadoCasco();

    }


    // =========================================================
    // CONSULTAR ESTADO DEL CASCO
    // =========================================================

    private void consultarEstadoCasco() {

        Request request =
                new Request.Builder()
                        .url(URL_ESTADO)
                        .get()
                        .build();


        clienteHttp.newCall(
                request
        ).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        runOnUiThread(() -> {

                            txtEstadoPersona.setText(
                                    "🔴 Sin conexión con cámara"
                            );

                            txtEstadoPersona.setTextColor(
                                    Color.RED
                            );

                        });

                    }


                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            return;
                        }


                        String respuesta =
                                response.body().string();


                        try {

                            JSONObject json =
                                    new JSONObject(
                                            respuesta
                                    );


                            // =================================================
                            // OBJETO CASCO
                            // =================================================

                            JSONObject casco =
                                    json.getJSONObject(
                                            "casco"
                                    );


                            String estado =
                                    casco.getString(
                                            "estado"
                                    );


                            // =================================================
                            // ACTUALIZAR PANTALLA
                            // =================================================

                            runOnUiThread(() -> {

                                if (
                                        estado.equalsIgnoreCase(
                                                "CASCO OK"
                                        )
                                                ||
                                                estado.equalsIgnoreCase(
                                                        "PUESTO"
                                                )
                                ) {

                                    txtEstadoPersona.setText(
                                            "🟢 CASCO PUESTO"
                                    );

                                    txtEstadoPersona.setTextColor(
                                            Color.rgb(
                                                    0,
                                                    168,
                                                    90
                                            )
                                    );

                                } else {

                                    txtEstadoPersona.setText(
                                            "🔴 CASCO RETIRADO"
                                    );

                                    txtEstadoPersona.setTextColor(
                                            Color.RED
                                    );

                                }

                            });


                        } catch (Exception e) {

                            runOnUiThread(() -> {

                                txtEstadoPersona.setText(
                                        "⚠️ Estado no disponible"
                                );

                                txtEstadoPersona.setTextColor(
                                        Color.DKGRAY
                                );

                            });

                        }

                    }

                }
        );


        // =====================================================
        // VOLVER A CONSULTAR EN 1 SEGUNDO
        // =====================================================

        handler.postDelayed(
                this::consultarEstadoCasco,
                1000
        );

    }


    // =========================================================
    // FECHA Y HORA
    // =========================================================

    private void actualizarFechaHora() {

        handler.post(
                new Runnable() {

                    @Override
                    public void run() {

                        SimpleDateFormat formato =
                                new SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm:ss",
                                        Locale.getDefault()
                                );


                        String fecha =
                                formato.format(
                                        new Date()
                                );


                        txtFechaHora.setText(
                                fecha
                        );


                        handler.postDelayed(
                                this,
                                1000
                        );

                    }

                }
        );

    }


    // =========================================================
    // DESTRUIR ACTIVITY
    // =========================================================

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );

        webCamara.stopLoading();

        webCamara.destroy();

        super.onDestroy();

    }

}