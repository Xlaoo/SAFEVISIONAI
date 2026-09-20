package com.safevision.ai;


import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class DetalleCamaraActivity extends BaseActivity {


    private TextView titulo;
    private TextView btnRegresar;
    private TextView txtFechaHora;

    private WebView webCamara;


    Handler handler = new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_detalle_camara);



        titulo =
                findViewById(R.id.txtTituloCamara);


        btnRegresar =
                findViewById(R.id.btnRegresar);


        txtFechaHora =
                findViewById(R.id.txtFechaHora);



        webCamara =
                findViewById(R.id.webCamaraDetalle);



        String camara =
                getIntent().getStringExtra("CAMARA");


        String url =
                getIntent().getStringExtra("URL");
        System.out.println("URL RECIBIDA: " + url);


        titulo.setText(
                "Cámara - " + camara
        );



        // ==========================
        // BOTON REGRESAR
        // ==========================

        btnRegresar.setOnClickListener(v -> {


            finish();


        });



        // ==========================
        // WEBVIEW CAMARA
        // ==========================


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


        if(url != null){
            webCamara.setWebViewClient(
                    new android.webkit.WebViewClient()
            );

            if(url != null && !url.isEmpty()){


                webCamara.loadUrl(
                        url + "video"
                );


            }
            else{

                titulo.setText(
                        "Error: URL vacía"
                );

            }


        }



        actualizarFechaHora();


    }



    private void actualizarFechaHora(){


        handler.post(new Runnable() {


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

        });


    }



    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();

    }


}