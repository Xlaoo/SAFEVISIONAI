package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class DetalleAlertaActivity extends BaseActivity {

    TextView txtTitulo;
    TextView txtFecha;
    TextView txtArea;

    TextView btnRegresarDetalle;
    Button btnRegistrarAccion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_detalle_alerta
        );


        // =====================================================
        // REFERENCIAS DEL XML
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


        // =====================================================
        // RECIBIR ALERTA
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
        // MOSTRAR INFORMACIÓN
        // =====================================================

        if (titulo != null) {

            txtTitulo.setText(
                    titulo
            );

        } else {

            txtTitulo.setText(
                    "Alerta"
            );

        }


        if (fecha != null) {

            txtFecha.setText(
                    "Fecha: " + fecha
            );

        } else {

            txtFecha.setText(
                    "Fecha:"
            );

        }


        if (area != null) {

            txtArea.setText(
                    "Área: " + area
            );

        } else {

            txtArea.setText(
                    "Área: Producción"
            );

        }


        // =====================================================
        // BOTÓN REGRESAR
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


                    // ID DE LA ALERTA
                    intent.putExtra(
                            "alerta_id",
                            idAlerta
                    );


                    // ID DEL TRABAJADOR
                    intent.putExtra(
                            "trabajador_id",
                            trabajadorId
                    );


                    // DATOS DE LA ALERTA
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


                    startActivity(
                            intent
                    );

                }
        );

    }

}