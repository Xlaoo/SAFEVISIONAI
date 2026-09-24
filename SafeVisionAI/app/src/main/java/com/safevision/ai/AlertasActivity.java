package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                                    ((Number)
                                            item.get("id"))
                                            .intValue();


                            // =====================================
                            // TRABAJADOR
                            // =====================================

                            int trabajadorId =
                                    ((Number)
                                            item.get("trabajador_id"))
                                            .intValue();


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
                            String imagenNormal =
                                    item.get("imagen_normal") == null
                                            ? ""
                                            : String.valueOf(
                                            item.get("imagen_normal")
                                    );


                            String imagenZoom =
                                    item.get("imagen_zoom") == null
                                            ? ""
                                            : String.valueOf(
                                            item.get("imagen_zoom")
                                    );


                            String titulo =
                                    problema;


                            String area =
                                    "Producción";


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

        SupabaseApi api =
                SupabaseClient
                        .getClient()
                        .create(SupabaseApi.class);


        String authorization =
                "Bearer " + SupabaseConfig.API_KEY;


        // =============================================
        // FILTRO DE SUPABASE
        // =============================================

        String filtroId =
                "eq." + alerta.getId();


        // =============================================
        // DELETE
        // =============================================

        Call<Void> llamada =
                api.eliminarAlerta(
                        authorization,
                        filtroId
                );


        llamada.enqueue(
                new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        if (response.isSuccessful()) {


                            // =================================
                            // BORRAR DE LA LISTA
                            // =================================

                            int posicion =
                                    lista.indexOf(alerta);


                            if (posicion >= 0) {

                                lista.remove(posicion);


                                adapter.notifyItemRemoved(
                                        posicion
                                );

                            }


                            Toast.makeText(
                                    AlertasActivity.this,
                                    "Alerta eliminada correctamente",
                                    Toast.LENGTH_SHORT
                            ).show();


                        } else {

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "No se pudo eliminar la alerta",
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
    // ACTUALIZAR AL VOLVER
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (lista != null && adapter != null) {

            cargarAlertas();

        }

    }

}