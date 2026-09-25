package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertasActivity extends BaseActivity {

    private RecyclerView recyclerAlertas;

    private ArrayList<Alerta> lista;

    private AlertaAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alertas);

        configurarMenuInferior();


        recyclerAlertas =
                findViewById(R.id.recyclerAlertas);


        recyclerAlertas.setLayoutManager(
                new LinearLayoutManager(this)
        );


        lista = new ArrayList<>();


        // =====================================================
        // ADAPTER
        // =====================================================

        adapter = new AlertaAdapter(

                lista,

                // =================================================
                // CLICK EN LA ALERTA
                // =================================================

                alerta -> {

                    Intent intent =
                            new Intent(
                                    AlertasActivity.this,
                                    DetalleAlertaActivity.class
                            );


                    intent.putExtra(
                            "alerta_id",
                            alerta.getId()
                    );


                    intent.putExtra(
                            "trabajador_id",
                            alerta.getTrabajadorId()
                    );


                    intent.putExtra(
                            "titulo",
                            alerta.getTitulo()
                    );


                    intent.putExtra(
                            "fecha",
                            alerta.getFecha()
                    );


                    intent.putExtra(
                            "area",
                            alerta.getArea()
                    );


                    intent.putExtra(
                            "problema",
                            alerta.getProblema()
                    );


                    intent.putExtra(
                            "casco",
                            alerta.isCasco()
                    );


                    intent.putExtra(
                            "chaleco",
                            alerta.isChaleco()
                    );
                    intent.putExtra(
                            "foto_normal",
                            alerta.getImagenNormal()
                    );


                    intent.putExtra(
                            "foto_zoom",
                            alerta.getImagenZoom()
                    );

                    startActivity(intent);

                },


                // =================================================
                // CLICK EN PAPELERA
                // =================================================

                (alerta, position) -> {

                    eliminarAlerta(
                            alerta,
                            position
                    );

                }

        );


        recyclerAlertas.setAdapter(adapter);

        cargarAlertas();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        cargarAlertas();
    }


    // =========================================================
    // CARGAR ALERTAS DESDE SUPABASE
    // =========================================================

    private void cargarAlertas() {

        SupabaseApi api =
                SupabaseClient
                        .getClient()
                        .create(SupabaseApi.class);


        String apiKey =
                SupabaseConfig.API_KEY;

        String authorization =
                "Bearer " + SupabaseConfig.API_KEY;

        Call<List<Map<String, Object>>> llamada =
                api.obtenerAlertas(
                        apiKey,
                        authorization,
                        "*",
                        "eq.PENDIENTE",
                        "created_at.desc"
                );


        llamada.enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        if (!response.isSuccessful()) {

                            String detalleError = "";

                            try {

                                if (response.errorBody() != null) {

                                    detalleError =
                                            response.errorBody().string();

                                }

                            } catch (Exception e) {

                                detalleError =
                                        e.getMessage();

                            }

                            android.util.Log.e(
                                    "SAFEVISION_ALERTAS",
                                    "HTTP: " + response.code()
                                            + " ERROR: "
                                            + detalleError
                            );

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "Error cargando alertas: HTTP "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        if (response.body() == null) {

                            android.util.Log.e(
                                    "SAFEVISION_ALERTAS",
                                    "Supabase devolvió body NULL"
                            );

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "Supabase no devolvió datos",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        lista.clear();


                        for (
                                Map<String, Object> item
                                : response.body()
                        ) {

                            // =====================================
                            // ID
                            // =====================================

                            int id =
                                    obtenerNumero(item.get("id"), -1);


                            // =====================================
                            // TRABAJADOR
                            // =====================================

                            int trabajadorId =
                                    obtenerNumero(item.get("trabajador_id"), -1);


                            // =====================================
                            // PROBLEMA
                            // =====================================

                            String problema =
                                    String.valueOf(
                                            item.get("problema")
                                    );


                            // =====================================
                            // FECHA
                            // =====================================

                            String fecha =
                                    String.valueOf(
                                            item.get("created_at")
                                    );


                            // =====================================
                            // ESTADO
                            // =====================================

                            String estado =
                                    String.valueOf(
                                            item.get("estado")
                                    );


                            // =====================================
                            // CASCO
                            // =====================================

                            boolean casco =
                                    Boolean.TRUE.equals(
                                            item.get("casco")
                                    );


                            // =====================================
                            // CHALECO
                            // =====================================

                            boolean chaleco =
                                    Boolean.TRUE.equals(
                                            item.get("chaleco")
                                    );


                            // =====================================
                            // IMAGEN
                            // =====================================

                            String imagen =
                                    item.get("imagen") == null
                                            ? ""
                                            : String.valueOf(
                                            item.get("imagen")
                                    );
                            if ("null".equalsIgnoreCase(imagen) || "EMPTY".equalsIgnoreCase(imagen)) {
                                imagen = "";
                            }

                            String imagenNormal =
                                    item.get("imagen_normal") == null
                                            ? ""
                                            : String.valueOf(
                                            item.get("imagen_normal")
                                    );
                            if ("null".equalsIgnoreCase(imagenNormal) || "EMPTY".equalsIgnoreCase(imagenNormal)) {
                                imagenNormal = "";
                            }

                            String imagenZoom =
                                    item.get("imagen_zoom") == null
                                            ? ""
                                            : String.valueOf(
                                            item.get("imagen_zoom")
                                    );
                            if ("null".equalsIgnoreCase(imagenZoom) || "EMPTY".equalsIgnoreCase(imagenZoom)) {
                                imagenZoom = "";
                            }

                            String titulo =
                                    problema;

                            String area =
                                    trabajadorId > 0
                                            ? "Trabajador #" + trabajadorId + " • Producción"
                                            : "Producción";

                            // =====================================
                            // AGREGAR ALERTA
                            // =====================================

                            lista.add(
                                    new Alerta(
                                            id,
                                            trabajadorId,
                                            titulo,
                                            fecha,
                                            area,
                                            problema,
                                            casco,
                                            chaleco,
                                            estado,
                                            imagen,
                                            imagenNormal,
                                            imagenZoom
                                    )
                            );

                        }


                        adapter.notifyDataSetChanged();


                        if (lista.isEmpty()) {

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "No hay alertas pendientes",
                                    Toast.LENGTH_SHORT
                            ).show();

                        }

                    }


                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        Toast.makeText(
                                AlertasActivity.this,
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }


    // =========================================================
    // ELIMINAR ALERTA
    // =========================================================

    private void eliminarAlerta(
            Alerta alerta,
            int position
    ) {

        // 1. Conservar referencias a imágenes ANTES de eliminar en Supabase
        List<String> fotosParaEliminar = new ArrayList<>();
        String urlServidor = null;

        if (esReferenciaValida(alerta.getImagenNormal())) {
            fotosParaEliminar.add(alerta.getImagenNormal());
            if (urlServidor == null) {
                urlServidor = extraerUrlServidor(alerta.getImagenNormal());
            }
        }

        if (esReferenciaValida(alerta.getImagenZoom())) {
            if (!fotosParaEliminar.contains(alerta.getImagenZoom())) {
                fotosParaEliminar.add(alerta.getImagenZoom());
            }
            if (urlServidor == null) {
                urlServidor = extraerUrlServidor(alerta.getImagenZoom());
            }
        }

        if (esReferenciaValida(alerta.getImagen())) {
            if (!fotosParaEliminar.contains(alerta.getImagen())) {
                fotosParaEliminar.add(alerta.getImagen());
            }
            if (urlServidor == null) {
                urlServidor = extraerUrlServidor(alerta.getImagen());
            }
        }

        if (urlServidor == null) {
            urlServidor = "http://10.237.144.107:5000";
        }

        final String urlServidorFinal = urlServidor;

        SupabaseApi api =
                SupabaseClient
                        .getClient()
                        .create(SupabaseApi.class);

        String authorization =
                "Bearer " + SupabaseConfig.API_KEY;

        String filtroId =
                "eq." + alerta.getId();

        // 2. Eliminar acciones asociadas a la alerta primero (si existen)
        api.eliminarAccionesAlerta(
                authorization,
                filtroId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                ejecutarEliminacionAlerta(api, authorization, filtroId, alerta, fotosParaEliminar, urlServidorFinal);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                ejecutarEliminacionAlerta(api, authorization, filtroId, alerta, fotosParaEliminar, urlServidorFinal);
            }
        });

    }

    private void ejecutarEliminacionAlerta(
            SupabaseApi api,
            String authorization,
            String filtroId,
            Alerta alerta,
            List<String> fotosParaEliminar,
            String urlServidor
    ) {

        api.eliminarAlerta(
                authorization,
                filtroId
        ).enqueue(
                new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        if (response.isSuccessful()) {

                            int posicion =
                                    lista.indexOf(alerta);

                            if (posicion >= 0) {
                                lista.remove(posicion);
                                adapter.notifyItemRemoved(posicion);
                            }

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "Alerta eliminada correctamente",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // 3. Solicitar al servidor Python que elimine SOLAMENTE las imágenes físicas
                            solicitarEliminacionImagenesServidor(urlServidor, fotosParaEliminar);

                            // 4. Recargar desde Supabase para asegurar fuente de verdad
                            cargarAlertas();

                        } else {

                            String detalle = "HTTP " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    detalle += " " + response.errorBody().string();
                                }
                            } catch (Exception ignored) {
                            }

                            Log.e("SAFEVISION_ELIMINAR", detalle);

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "No se pudo eliminar la alerta: " + detalle,
                                    Toast.LENGTH_LONG
                            ).show();

                        }

                    }


                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ) {

                        Toast.makeText(
                                AlertasActivity.this,
                                "Error al eliminar: "
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }

    // =========================================================
    // SOLICITAR ELIMINACIÓN DE IMÁGENES FÍSICAS EN PYTHON
    // =========================================================

    private void solicitarEliminacionImagenesServidor(
            String urlServidor,
            List<String> fotos
    ) {

        if (fotos == null || fotos.isEmpty()) {
            return;
        }

        new Thread(() -> {

            HttpURLConnection conexion = null;

            try {

                String base =
                        urlServidor != null && !urlServidor.isEmpty()
                                ? urlServidor
                                : "http://10.237.144.107:5000";

                if (!base.endsWith("/")) {
                    base += "/";
                }

                URL url =
                        new URL(base + "eliminar_fotos");

                conexion =
                        (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexion.setRequestProperty("Accept", "application/json");
                conexion.setConnectTimeout(3000);
                conexion.setReadTimeout(3000);
                conexion.setDoOutput(true);

                JsonObject json =
                        new JsonObject();

                JsonArray arrayFotos =
                        new JsonArray();

                for (String foto : fotos) {
                    String nombreLimpio =
                            extraerNombreArchivo(foto);

                    if (nombreLimpio != null && !nombreLimpio.isEmpty()) {
                        arrayFotos.add(nombreLimpio);
                    }
                }

                json.add("fotos", arrayFotos);

                byte[] body =
                        json.toString().getBytes(StandardCharsets.UTF_8);

                try (OutputStream os = conexion.getOutputStream()) {
                    os.write(body);
                    os.flush();
                }

                int codigo =
                        conexion.getResponseCode();

                if (codigo == 200) {
                    Log.d(
                            "SAFEVISION_FOTOS",
                            "Imágenes físicas eliminadas del servidor correctamente"
                    );
                } else {
                    Log.w(
                            "SAFEVISION_FOTOS",
                            "Advertencia: El servidor respondió HTTP " + codigo + " al eliminar imágenes"
                    );
                }

            } catch (Exception e) {

                // El fallo del servidor NO debe afectar la eliminación en Supabase
                Log.w(
                        "SAFEVISION_FOTOS",
                        "Advertencia: No se pudieron eliminar las imágenes físicas en servidor: "
                                + e.getMessage()
                );

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }

            }

        }).start();

    }

    private boolean esReferenciaValida(String ref) {
        if (ref == null) {
            return false;
        }
        String limpia = ref.trim();
        return !limpia.isEmpty()
                && !limpia.equalsIgnoreCase("null")
                && !limpia.equalsIgnoreCase("empty");
    }

    private String extraerNombreArchivo(String urlOFoto) {
        if (urlOFoto == null) {
            return null;
        }
        String limpia = urlOFoto.trim();
        if (limpia.isEmpty() || limpia.equalsIgnoreCase("null") || limpia.equalsIgnoreCase("empty")) {
            return null;
        }
        int ultimaBarra = limpia.lastIndexOf('/');
        if (ultimaBarra >= 0 && ultimaBarra < limpia.length() - 1) {
            return limpia.substring(ultimaBarra + 1);
        }
        return limpia;
    }

    private String extraerUrlServidor(String urlOFoto) {
        if (urlOFoto == null) {
            return null;
        }
        String limpia = urlOFoto.trim();
        if (limpia.startsWith("http://") || limpia.startsWith("https://")) {
            try {
                URI uri = new URI(limpia);
                return uri.getScheme() + "://" + uri.getAuthority();
            } catch (Exception ignored) {
            }
        }
        return null;
    }


    // =========================================================
    // ACTUALIZAR AL VOLVER
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (lista != null && adapter != null) {

            cargarAlertas();

        }

    }

    // =========================================================
    // UTILIDADES DE CONVERSIÓN SEGURA
    // =========================================================
    private int obtenerNumero(Object valor, int defecto) {
        if (valor == null) {
            return defecto;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(valor).trim());
        } catch (Exception e) {
            return defecto;
        }
    }

}