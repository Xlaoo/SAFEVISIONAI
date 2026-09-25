package com.safevision.ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class DetalleAlertaActivity extends BaseActivity {

    private TextView txtTitulo;
    private TextView txtFecha;
    private TextView txtArea;

    private TextView btnRegresarDetalle;

    private Button btnRegistrarAccion;

    private ImageView imgFotoNormal;
    private ImageView imgFotoZoom;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_detalle_alerta
        );


        // =====================================================
        // REFERENCIAS
        // =====================================================

        txtTitulo =
                findViewById(
                        R.id.txtTituloDetalle
                );

        txtFecha =
                findViewById(
                        R.id.txtFechaDetalle
                );

        txtArea =
                findViewById(
                        R.id.txtAreaDetalle
                );

        btnRegresarDetalle =
                findViewById(
                        R.id.btnRegresarDetalle
                );

        btnRegistrarAccion =
                findViewById(
                        R.id.btnRegistrarAccion
                );

        imgFotoNormal =
                findViewById(
                        R.id.imgFotoNormal
                );

        imgFotoZoom =
                findViewById(
                        R.id.imgFotoZoom
                );


        // =====================================================
        // RECIBIR DATOS DE LA ALERTA
        // =====================================================

        Intent datos =
                getIntent();


        int idAlerta =
                datos.getIntExtra(
                        "alerta_id",
                        -1
                );


        int trabajadorId =
                datos.getIntExtra(
                        "trabajador_id",
                        -1
                );


        String titulo =
                datos.getStringExtra(
                        "titulo"
                );


        String fecha =
                datos.getStringExtra(
                        "fecha"
                );


        String area =
                datos.getStringExtra(
                        "area"
                );


        String problema =
                datos.getStringExtra(
                        "problema"
                );


        boolean casco =
                datos.getBooleanExtra(
                        "casco",
                        false
                );


        boolean chaleco =
                datos.getBooleanExtra(
                        "chaleco",
                        false
                );


        // =====================================================
        // RECIBIR FOTOS
        // =====================================================

        String fotoNormal =
                datos.getStringExtra(
                        "foto_normal"
                );


        String fotoZoom =
                datos.getStringExtra(
                        "foto_zoom"
                );


        // =====================================================
        // MOSTRAR INFORMACIÓN
        // =====================================================

        if (titulo != null
                && !titulo.isEmpty()) {

            txtTitulo.setText(
                    titulo
            );

        } else {

            txtTitulo.setText(
                    "Alerta"
            );
        }


        if (fecha != null
                && !fecha.isEmpty()) {

            txtFecha.setText(
                    "Fecha: " + fecha
            );

        } else {

            txtFecha.setText(
                    "Fecha:"
            );
        }


        String areaTexto = (area != null && !area.isEmpty()) ? area : "Producción";
        if (trabajadorId > 0) {
            txtArea.setText("Trabajador #" + trabajadorId + " • Área: " + areaTexto);
        } else {
            txtArea.setText("Área: " + areaTexto);
        }


        // =====================================================
        // CARGAR FOTO NORMAL
        // =====================================================

        if (esUrlValida(fotoNormal)) {

            cargarImagen(
                    fotoNormal,
                    imgFotoNormal
            );

        } else {

            imgFotoNormal.setImageResource(
                    android.R.drawable.ic_menu_report_image
            );
        }


        // =====================================================
        // CARGAR FOTO ZOOM
        // =====================================================

        if (esUrlValida(fotoZoom)) {

            cargarImagen(
                    fotoZoom,
                    imgFotoZoom
            );

        } else {

            imgFotoZoom.setImageResource(
                    android.R.drawable.ic_menu_report_image
            );
        }


        // =====================================================
        // REGRESAR
        // =====================================================

        btnRegresarDetalle.setOnClickListener(
                v -> finish()
        );


        // =====================================================
        // REGISTRAR ACCIÓN
        // =====================================================

        btnRegistrarAccion.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    DetalleAlertaActivity.this,
                                    RegistrarAccionActivity.class
                            );


                    intent.putExtra(
                            "alerta_id",
                            idAlerta
                    );


                    intent.putExtra(
                            "trabajador_id",
                            trabajadorId
                    );


                    intent.putExtra(
                            "titulo",
                            titulo
                    );


                    intent.putExtra(
                            "problema",
                            problema
                    );


                    intent.putExtra(
                            "casco",
                            casco
                    );


                    intent.putExtra(
                            "chaleco",
                            chaleco
                    );


                    startActivity(intent);

                }
        );

    }


    // =====================================================
    // DESCARGAR IMAGEN DESDE INTERNET
    // =====================================================

    private void cargarImagen(
            String urlImagen,
            ImageView imageView
    ) {

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {

                URL url =
                        new URL(
                                urlImagen
                        );


                conexion =
                        (HttpURLConnection)
                                url.openConnection();


                conexion.setConnectTimeout(
                        10000
                );


                conexion.setReadTimeout(
                        10000
                );


                conexion.setDoInput(
                        true
                );


                conexion.connect();


                InputStream input =
                        conexion.getInputStream();


                Bitmap bitmap =
                        BitmapFactory.decodeStream(
                                input
                        );


                input.close();


                runOnUiThread(() -> {

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                                bitmap
                        );

                    } else {

                        imageView.setImageResource(
                                android.R.drawable.ic_menu_report_image
                        );
                    }

                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    imageView.setImageResource(
                            android.R.drawable.ic_menu_report_image
                    );

                });


            } finally {

                if (conexion != null) {

                    conexion.disconnect();

                }

            }

        });

    }

    private boolean esUrlValida(String url) {
        if (url == null) {
            return false;
        }
        String limpia = url.trim();
        return !limpia.isEmpty()
                && !limpia.equalsIgnoreCase("null")
                && !limpia.equalsIgnoreCase("empty")
                && (limpia.startsWith("http://") || limpia.startsWith("https://"));
    }


    // =====================================================
    // DESTRUIR
    // =====================================================

    @Override
    protected void onDestroy() {

        executor.shutdownNow();

        super.onDestroy();

    }

}