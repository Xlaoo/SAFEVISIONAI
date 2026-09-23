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

    RecyclerView recyclerAlertas;

    ArrayList<Alerta> lista;

    AlertaAdapter adapter;


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


        adapter = new AlertaAdapter(
                lista,
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


                    startActivity(intent);
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


        String authorization =
                "Bearer " + SupabaseConfig.API_KEY;


        Call<List<Map<String, Object>>> llamada =
                api.obtenerAlertas(
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

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            Toast.makeText(
                                    AlertasActivity.this,
                                    "No se pudieron cargar las alertas",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        lista.clear();


                        for (
                                Map<String, Object> item
                                : response.body()
                        ) {

                            int id =
                                    ((Number)
                                            item.get("id"))
                                            .intValue();


                            int trabajadorId =
                                    ((Number)
                                            item.get("trabajador_id"))
                                            .intValue();


                            String problema =
                                    String.valueOf(
                                            item.get("problema")
                                    );


                            String fecha =
                                    String.valueOf(
                                            item.get("created_at")
                                    );


                            String estado =
                                    String.valueOf(
                                            item.get("estado")
                                    );


                            boolean casco =
                                    Boolean.TRUE.equals(
                                            item.get("casco")
                                    );


                            boolean chaleco =
                                    Boolean.TRUE.equals(
                                            item.get("chaleco")
                                    );


                            String imagen =
                                    item.get("imagen") == null
                                            ? ""
                                            : String.valueOf(
                                            item.get("imagen")
                                    );


                            String titulo =
                                    problema;


                            String area =
                                    "Producción";


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
                                            imagen
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


    @Override
    protected void onResume() {

        super.onResume();

        if (lista != null && adapter != null) {

            cargarAlertas();
        }
    }
}