package com.safevision.ai;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

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

    // Vistas para el estado EPP unificado
    private TextView txtEstadoEpp;
    private TextView txtEstadoCasco;
    private TextView txtEstadoChaleco;

    private WebView webCamara;

    private Handler handler = new Handler();
    private OkHttpClient clienteHttp = new OkHttpClient();

    private String urlBase = "";
    private String urlVideo = "";
    private String urlEstado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_camara);

        // =====================================================
        // REFERENCIAS UI
        // =====================================================
        titulo = findViewById(R.id.txtTituloCamara);
        btnRegresar = findViewById(R.id.btnRegresar);
        txtFechaHora = findViewById(R.id.txtFechaHora);
        txtEstadoPersona = findViewById(R.id.txtEstadoPersona);

        txtEstadoEpp = findViewById(R.id.txtEstadoEpp);
        txtEstadoCasco = findViewById(R.id.txtEstadoCasco);
        txtEstadoChaleco = findViewById(R.id.txtEstadoChaleco);

        webCamara = findViewById(R.id.webCamaraDetalle);

        // =====================================================
        // CONFIGURACIÓN DE URL
        // =====================================================
        String urlIntent = getIntent().getStringExtra("URL");
        if (urlIntent != null && !urlIntent.isEmpty()) {
            urlBase = urlIntent.endsWith("/") ? urlIntent : urlIntent + "/";
        } else {
            urlBase = CameraConfig.getUrlBase();
        }

        urlVideo = urlBase + "video";
        urlEstado = urlBase + "estado";

        // =====================================================
        // NOMBRE DE LA CÁMARA
        // =====================================================
        String camara = getIntent().getStringExtra("CAMARA");
        if (camara == null || camara.isEmpty()) {
            camara = "Producción";
        }
        titulo.setText("Cámara - " + camara);

        // =====================================================
        // BOTÓN REGRESAR
        // =====================================================
        btnRegresar.setOnClickListener(v -> finish());

        // =====================================================
        // CONFIGURACIÓN WEBVIEW (VIDEO EN VIVO)
        // =====================================================
        WebSettings settings = webCamara.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webCamara.setWebViewClient(new android.webkit.WebViewClient());
        webCamara.loadUrl(urlVideo);

        // =====================================================
        // FECHA Y HORA
        // =====================================================
        actualizarFechaHora();

        // =====================================================
        // CONSULTAR ESTADO EPP (CASCO + CHALECO)
        // =====================================================
        consultarEstadoEpp();
    }

    // =========================================================
    // CONSULTAR ESTADO EPP DEL SERVIDOR (/estado)
    // =========================================================
    private void consultarEstadoEpp() {
        Request request = new Request.Builder()
                .url(urlEstado)
                .get()
                .build();

        clienteHttp.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    txtEstadoPersona.setText("🔴 Sin conexión con cámara");
                    txtEstadoPersona.setTextColor(Color.RED);
                    if (txtEstadoEpp != null) {
                        txtEstadoEpp.setText("ESTADO: SIN CONEXIÓN");
                        txtEstadoEpp.setTextColor(Color.RED);
                    }
                    if (txtEstadoCasco != null) {
                        txtEstadoCasco.setText("CASCO: --");
                        txtEstadoCasco.setTextColor(Color.DKGRAY);
                    }
                    if (txtEstadoChaleco != null) {
                        txtEstadoChaleco.setText("CHALECO: --");
                        txtEstadoChaleco.setTextColor(Color.DKGRAY);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                String respuesta = response.body().string();
                EstadoEpp epp = EstadoEpp.fromJson(respuesta);

                runOnUiThread(() -> {
                    actualizarUiEstado(epp);
                });
            }
        });

        // Consultar cada 1 segundo periódicamente
        handler.postDelayed(this::consultarEstadoEpp, 1000);
    }

    // =========================================================
    // ACTUALIZAR INTERFAZ DE USUARIO CON LOS DATOS DE EPP
    // =========================================================
    private void actualizarUiEstado(EstadoEpp epp) {
        // 1. Estado Casco
        if (txtEstadoCasco != null) {
            if (epp.isCasco()) {
                txtEstadoCasco.setText(String.format(Locale.getDefault(), "CASCO: OK (%.0f%%)", epp.getPorcentajeCasco()));
                txtEstadoCasco.setTextColor(Color.rgb(0, 168, 90)); // Verde
            } else {
                txtEstadoCasco.setText("CASCO: FALTA");
                txtEstadoCasco.setTextColor(Color.RED);
            }
        }

        // 2. Estado Chaleco
        if (txtEstadoChaleco != null) {
            if (epp.isChaleco()) {
                txtEstadoChaleco.setText(String.format(Locale.getDefault(), "CHALECO: OK (%.0f%%)", epp.getPorcentajeChaleco()));
                txtEstadoChaleco.setTextColor(Color.rgb(0, 168, 90)); // Verde
            } else {
                txtEstadoChaleco.setText("CHALECO: FALTA");
                txtEstadoChaleco.setTextColor(Color.RED);
            }
        }

        // 3. Estado General EPP
        if (txtEstadoEpp != null) {
            String estadoStr = epp.getEstado();
            txtEstadoEpp.setText("ESTADO: " + estadoStr);

            if ("EPP COMPLETO".equalsIgnoreCase(estadoStr)) {
                txtEstadoEpp.setTextColor(Color.rgb(0, 168, 90)); // Verde
            } else if ("BUSCANDO PERSONA".equalsIgnoreCase(estadoStr)) {
                txtEstadoEpp.setTextColor(Color.rgb(120, 120, 120)); // Gris
            } else if ("FALTAN CASCO Y CHALECO".equalsIgnoreCase(estadoStr)) {
                txtEstadoEpp.setTextColor(Color.RED); // Rojo crítico
            } else {
                // FALTA CASCO o FALTA CHALECO
                txtEstadoEpp.setTextColor(Color.rgb(255, 140, 0)); // Naranja
            }
        }

        // 4. Retrocompatibilidad con txtEstadoPersona
        if (txtEstadoPersona != null) {
            if (epp.isEppCompleto()) {
                txtEstadoPersona.setText("🟢 EPP COMPLETO");
                txtEstadoPersona.setTextColor(Color.rgb(0, 168, 90));
            } else if (epp.isRostroDetectado()) {
                txtEstadoPersona.setText("⚠️ " + epp.getEstado());
                txtEstadoPersona.setTextColor(Color.rgb(255, 140, 0));
            } else {
                txtEstadoPersona.setText("⚪ Buscando persona...");
                txtEstadoPersona.setTextColor(Color.DKGRAY);
            }
        }
    }

    // =========================================================
    // FECHA Y HORA
    // =========================================================
    private void actualizarFechaHora() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                txtFechaHora.setText(formato.format(new Date()));
                handler.postDelayed(this, 1000);
            }
        });
    }

    // =========================================================
    // DESTRUIR ACTIVITY
    // =========================================================
    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webCamara != null) {
            webCamara.stopLoading();
            webCamara.destroy();
        }
        super.onDestroy();
    }
}