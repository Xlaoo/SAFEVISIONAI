package com.safevision.ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
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
    private ImageView imgFotoZoomChaleco;
    private View layoutZoomCasco;
    private View layoutZoomChaleco;

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

        imgFotoZoomChaleco =
                findViewById(
                        R.id.imgFotoZoomChaleco
                );

        layoutZoomCasco =
                findViewById(
                        R.id.layoutZoomCascoAlerta
                );

        layoutZoomChaleco =
                findViewById(
                        R.id.layoutZoomChalecoAlerta
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
                        true
                );


        boolean chaleco =
                datos.getBooleanExtra(
                        "chaleco",
                        true
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
        // EVALUAR IMPLEMENTOS FALTANTES
        // =====================================================
        String prob = problema != null ? problema.toLowerCase() : "";
        boolean preliminarFaltaCasco = !casco || prob.contains("casco");
        final boolean finalFaltaChaleco = !chaleco || prob.contains("chaleco");
        final boolean finalFaltaCasco = (!preliminarFaltaCasco && !finalFaltaChaleco) ? true : preliminarFaltaCasco;

        // =====================================================
        // CARGAR FOTO GENERAL (SIEMPRE VISIBLE)
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
        // CARGAR FOTOS ZOOM SEGÚN IMPLEMENTOS (2 o 3 FOTOS)
        // =====================================================

        if (finalFaltaCasco && finalFaltaChaleco) {
            // CASO A: CASCO + CHALECO = 3 FOTOGRAFÍAS (General + Zoom Casco + Zoom Chaleco)
            layoutZoomCasco.setVisibility(View.VISIBLE);
            layoutZoomChaleco.setVisibility(View.VISIBLE);

            if (esUrlValida(fotoZoom)) {
                cargarImagen(fotoZoom, imgFotoZoom);
                cargarImagen(fotoZoom, imgFotoZoomChaleco);
            } else {
                imgFotoZoom.setImageResource(android.R.drawable.ic_menu_report_image);
                imgFotoZoomChaleco.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else if (finalFaltaCasco) {
            // CASO B: SOLO CASCO = 2 FOTOGRAFÍAS (General + Zoom Casco)
            layoutZoomCasco.setVisibility(View.VISIBLE);
            layoutZoomChaleco.setVisibility(View.GONE);

            if (esUrlValida(fotoZoom)) {
                cargarImagen(fotoZoom, imgFotoZoom);
            } else {
                imgFotoZoom.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            // CASO C: SOLO CHALECO = 2 FOTOGRAFÍAS (General + Zoom Chaleco)
            layoutZoomCasco.setVisibility(View.GONE);
            layoutZoomChaleco.setVisibility(View.VISIBLE);

            if (esUrlValida(fotoZoom)) {
                cargarImagen(fotoZoom, imgFotoZoomChaleco);
            } else {
                imgFotoZoomChaleco.setImageResource(android.R.drawable.ic_menu_report_image);
            }
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
                            !finalFaltaCasco
                    );


                    intent.putExtra(
                            "chaleco",
                            !finalFaltaChaleco
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
            Bitmap bitmap = descargarBitmap(urlImagen);

            if (bitmap == null && urlImagen != null && urlImagen.contains("/fotos/")) {
                String storageUrl = resolverUrlSupabaseStorage(urlImagen);
                if (storageUrl != null && !storageUrl.equals(urlImagen)) {
                    bitmap = descargarBitmap(storageUrl);
                }
            }

            final Bitmap res = bitmap;
            runOnUiThread(() -> {
                if (res != null) {
                    imageView.setImageBitmap(res);
                } else {
                    imageView.setImageResource(
                            android.R.drawable.ic_menu_report_image
                    );
                }
            });
        });
    }

    private Bitmap descargarBitmap(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) return null;
        HttpURLConnection conexion = null;
        InputStream input = null;
        try {
            URL url = new URL(urlStr);
            conexion = (HttpURLConnection) url.openConnection();
            conexion.setConnectTimeout(8000);
            conexion.setReadTimeout(8000);
            conexion.setDoInput(true);
            conexion.connect();

            if (conexion.getResponseCode() == HttpURLConnection.HTTP_OK) {
                input = conexion.getInputStream();
                return BitmapFactory.decodeStream(input);
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {}
            }
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private String resolverUrlSupabaseStorage(String urlOriginal) {
        if (urlOriginal == null || !urlOriginal.contains("/fotos/")) return null;
        try {
            int idx = urlOriginal.indexOf("/fotos/");
            String nombreArchivo = urlOriginal.substring(idx + 7);
            String urlBase = SupabaseConfig.URL;
            if (!urlBase.endsWith("/")) {
                urlBase += "/";
            }
            return urlBase + "storage/v1/object/public/fotos-alertas/" + nombreArchivo;
        } catch (Exception e) {
            return null;
        }
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