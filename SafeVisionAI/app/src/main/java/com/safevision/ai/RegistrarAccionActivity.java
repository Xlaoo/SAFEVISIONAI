package com.safevision.ai;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrarAccionActivity extends BaseActivity {

    private EditText edtDni;
    private EditText edtNombre;
    private EditText edtApellido;
    private EditText edtObservacion;

    private TextView txtEstadoDni;
    private TextView txtArea;

    private Spinner spinnerImplemento;
    private Spinner spinnerAccion;

    private Button btnVerificarDni;
    private Button btnRegistrarTrabajador;
    private Button btnCancelar;
    private Button btnAplicar;

    private SupabaseApi api;

    // ID del trabajador encontrado o registrado
    private int trabajadorId = -1;

    // ID de la alerta que estamos atendiendo
    private int alertaId = -1;

    // Indica si el trabajador ya existe
    private boolean trabajadorExiste = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registrar_accion);


        // =====================================================
        // REFERENCIAS
        // =====================================================

        edtDni = findViewById(R.id.edtDni);
        edtNombre = findViewById(R.id.edtNombre);
        edtApellido = findViewById(R.id.edtApellido);
        edtObservacion = findViewById(R.id.edtObservacion);

        txtEstadoDni = findViewById(R.id.txtEstadoDni);
        txtArea = findViewById(R.id.txtArea);

        spinnerImplemento =
                findViewById(R.id.spinnerImplemento);

        spinnerAccion =
                findViewById(R.id.spinnerAccion);

        btnVerificarDni =
                findViewById(R.id.btnVerificarDni);

        btnRegistrarTrabajador =
                findViewById(R.id.btnRegistrarTrabajador);

        btnCancelar =
                findViewById(R.id.btnCancelar);

        btnAplicar =
                findViewById(R.id.btnAplicar);


        // =====================================================
        // API
        // =====================================================

        api = SupabaseClient
                .getClient()
                .create(SupabaseApi.class);


        // =====================================================
        // ALERTA RECIBIDA
        // =====================================================

        alertaId =
                getIntent()
                        .getIntExtra(
                                "alerta_id",
                                -1
                        );


        // =====================================================
        // SPINNER IMPLEMENTO
        // SOLO CASCO Y CHALECO
        // =====================================================

        String[] implementos = {
                "Casco",
                "Chaleco"
        };

        ArrayAdapter<String> adapterImplementos =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        implementos
                );

        adapterImplementos.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerImplemento.setAdapter(
                adapterImplementos
        );


        // =====================================================
        // SPINNER ACCION
        // =====================================================

        String[] acciones = {
                "Retiro",
                "Colocación"
        };

        ArrayAdapter<String> adapterAcciones =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        acciones
                );

        adapterAcciones.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerAccion.setAdapter(
                adapterAcciones
        );


        // =====================================================
        // AREA POR DEFECTO
        // =====================================================

        txtArea.setText("Producción");


        // =====================================================
        // CAMPOS BLOQUEADOS INICIALMENTE
        // =====================================================

        edtNombre.setEnabled(false);
        edtApellido.setEnabled(false);


        // =====================================================
        // VERIFICAR DNI
        // =====================================================

        btnVerificarDni.setOnClickListener(
                v -> verificarDni()
        );


        // =====================================================
        // REGISTRAR TRABAJADOR
        // =====================================================

        btnRegistrarTrabajador.setOnClickListener(
                v -> registrarTrabajador()
        );


        // =====================================================
        // APLICAR
        // =====================================================

        btnAplicar.setOnClickListener(
                v -> aplicarAccion()
        );


        // =====================================================
        // CANCELAR
        // =====================================================

        btnCancelar.setOnClickListener(
                v -> finish()
        );

    }


    // =========================================================
    // VERIFICAR DNI
    // =========================================================

    private void verificarDni() {

        String dni =
                edtDni
                        .getText()
                        .toString()
                        .trim();


        if (dni.isEmpty()) {

            edtDni.setError(
                    "Ingrese el DNI"
            );

            return;
        }


        if (dni.length() != 8) {

            edtDni.setError(
                    "El DNI debe tener 8 dígitos"
            );

            return;
        }


        txtEstadoDni.setText(
                "Verificando DNI..."
        );


        btnVerificarDni.setEnabled(false);


        String authorization =
                "Bearer " +
                        SupabaseConfig.API_KEY;


        api.buscarTrabajadorPorDni(
                authorization,
                "id,nombres,apellidos,dni,area",
                "eq." + dni
        ).enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        btnVerificarDni.setEnabled(true);


                        if (!response.isSuccessful()) {

                            txtEstadoDni.setText(
                                    "Error al consultar el trabajador"
                            );

                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Error HTTP: " +
                                            response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        List<Map<String, Object>> resultado =
                                response.body();


                        if (resultado != null &&
                                !resultado.isEmpty()) {

                            // =================================
                            // TRABAJADOR ENCONTRADO
                            // =================================

                            Map<String, Object> trabajador =
                                    resultado.get(0);


                            trabajadorExiste = true;


                            trabajadorId =
                                    ((Number)
                                            trabajador.get("id"))
                                            .intValue();


                            String nombres =
                                    String.valueOf(
                                            trabajador.get("nombres")
                                    );


                            String apellidos =
                                    String.valueOf(
                                            trabajador.get("apellidos")
                                    );


                            String area =
                                    String.valueOf(
                                            trabajador.get("area")
                                    );


                            edtNombre.setText(
                                    nombres
                            );


                            edtApellido.setText(
                                    apellidos
                            );


                            txtArea.setText(
                                    area
                            );


                            edtNombre.setEnabled(false);
                            edtApellido.setEnabled(false);


                            btnRegistrarTrabajador.setVisibility(
                                    View.GONE
                            );


                            txtEstadoDni.setText(
                                    "✓ Trabajador encontrado"
                            );


                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Trabajador encontrado",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            // =================================
                            // TRABAJADOR NO ENCONTRADO
                            // =================================

                            trabajadorExiste = false;

                            trabajadorId = -1;


                            edtNombre.setText("");
                            edtApellido.setText("");


                            edtNombre.setEnabled(true);
                            edtApellido.setEnabled(true);


                            txtArea.setText(
                                    "Producción"
                            );


                            btnRegistrarTrabajador.setVisibility(
                                    View.VISIBLE
                            );


                            txtEstadoDni.setText(
                                    "Trabajador no encontrado. Regístrelo."
                            );

                        }

                    }


                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        btnVerificarDni.setEnabled(true);


                        txtEstadoDni.setText(
                                "No se pudo conectar con Supabase"
                        );


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }


    // =========================================================
    // REGISTRAR TRABAJADOR NUEVO
    // =========================================================

    private void registrarTrabajador() {

        String dni =
                edtDni
                        .getText()
                        .toString()
                        .trim();


        String nombres =
                edtNombre
                        .getText()
                        .toString()
                        .trim();


        String apellidos =
                edtApellido
                        .getText()
                        .toString()
                        .trim();


        if (dni.length() != 8) {

            Toast.makeText(
                    this,
                    "Ingrese un DNI válido",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (nombres.isEmpty()) {

            edtNombre.setError(
                    "Ingrese el nombre"
            );

            return;
        }


        if (apellidos.isEmpty()) {

            edtApellido.setError(
                    "Ingrese el apellido"
            );

            return;
        }


        btnRegistrarTrabajador.setEnabled(false);


        Map<String, Object> datos =
                new HashMap<>();


        datos.put(
                "nombres",
                nombres
        );


        datos.put(
                "apellidos",
                apellidos
        );


        datos.put(
                "dni",
                dni
        );


        datos.put(
                "area",
                "Producción"
        );


        datos.put(
                "casco",
                false
        );


        datos.put(
                "chaleco",
                false
        );


        datos.put(
                "casco_retiros",
                0
        );


        datos.put(
                "casco_colocaciones",
                0
        );


        datos.put(
                "chaleco_retiros",
                0
        );


        datos.put(
                "chaleco_colocaciones",
                0
        );


        String authorization =
                "Bearer " +
                        SupabaseConfig.API_KEY;


        api.registrarTrabajador(
                authorization,
                datos
        ).enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        btnRegistrarTrabajador.setEnabled(true);


                        if (!response.isSuccessful()) {

                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "No se pudo registrar. Código: " +
                                            response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        List<Map<String, Object>> resultado =
                                response.body();


                        if (resultado != null &&
                                !resultado.isEmpty()) {

                            Map<String, Object> trabajador =
                                    resultado.get(0);


                            trabajadorId =
                                    ((Number)
                                            trabajador.get("id"))
                                            .intValue();


                            trabajadorExiste = true;


                            edtNombre.setEnabled(false);
                            edtApellido.setEnabled(false);


                            btnRegistrarTrabajador.setVisibility(
                                    View.GONE
                            );


                            txtEstadoDni.setText(
                                    "✓ Trabajador registrado"
                            );


                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Trabajador registrado correctamente",
                                    Toast.LENGTH_SHORT
                            ).show();

                        }

                    }


                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        btnRegistrarTrabajador.setEnabled(true);


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }


    // =========================================================
    // APLICAR ACCIÓN
    // =========================================================

    private void aplicarAccion() {

        if (trabajadorId == -1) {

            Toast.makeText(
                    this,
                    "Primero debe verificar el DNI",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (alertaId == -1) {

            Toast.makeText(
                    this,
                    "No se encontró la alerta",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String implemento =
                spinnerImplemento
                        .getSelectedItem()
                        .toString();


        String accion =
                spinnerAccion
                        .getSelectedItem()
                        .toString();


        String observacion =
                edtObservacion
                        .getText()
                        .toString()
                        .trim();


        btnAplicar.setEnabled(false);


        // Primero registramos la acción
        registrarAccionEnSupabase(
                implemento,
                accion,
                observacion
        );

    }


    // =========================================================
    // GUARDAR EN acciones_alerta
    // =========================================================

    private void registrarAccionEnSupabase(
            String implemento,
            String accion,
            String observacion
    ) {

        Map<String, Object> datos =
                new HashMap<>();


        datos.put(
                "alerta_id",
                alertaId
        );


        datos.put(
                "trabajador_id",
                trabajadorId
        );


        datos.put(
                "accion",
                accion
        );


        datos.put(
                "observacion",
                observacion
        );


        String authorization =
                "Bearer " +
                        SupabaseConfig.API_KEY;


        api.registrarAccion(
                authorization,
                datos
        ).enqueue(
                new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        if (!response.isSuccessful()) {

                            btnAplicar.setEnabled(true);


                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "No se pudo guardar la acción. Código: " +
                                            response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        // Después actualizamos el contador
                        actualizarContador(
                                implemento,
                                accion
                        );

                    }


                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ) {

                        btnAplicar.setEnabled(true);


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error al guardar acción: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }


    // =========================================================
    // ACTUALIZAR CONTADOR
    // =========================================================

    private void actualizarContador(
            String implemento,
            String accion
    ) {

        // Por ahora obtenemos nuevamente al trabajador
        // para conocer sus contadores actuales.

        String authorization =
                "Bearer " +
                        SupabaseConfig.API_KEY;


        api.buscarTrabajadorPorDni(
                authorization,
                "id,casco,chaleco,casco_retiros,casco_colocaciones,chaleco_retiros,chaleco_colocaciones",
                "eq." +
                        edtDni
                                .getText()
                                .toString()
                                .trim()
        ).enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        if (!response.isSuccessful() ||
                                response.body() == null ||
                                response.body().isEmpty()) {

                            btnAplicar.setEnabled(true);

                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "No se pudo obtener el contador",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        Map<String, Object> trabajador =
                                response.body().get(0);


                        Map<String, Object> datos =
                                new HashMap<>();


                        if (implemento.equals("Casco")) {

                            if (accion.equals("Retiro")) {

                                int actual =
                                        obtenerNumero(
                                                trabajador.get("casco_retiros")
                                        );

                                datos.put(
                                        "casco_retiros",
                                        actual + 1
                                );

                                // El trabajador se retiró el casco
                                datos.put(
                                        "casco",
                                        false
                                );

                            } else {

                                int actual =
                                        obtenerNumero(
                                                trabajador.get("casco_colocaciones")
                                        );

                                datos.put(
                                        "casco_colocaciones",
                                        actual + 1
                                );

                                // El trabajador se colocó el casco
                                datos.put(
                                        "casco",
                                        true
                                );
                            }

                        } else if (implemento.equals("Chaleco")) {

                            if (accion.equals("Retiro")) {

                                int actual =
                                        obtenerNumero(
                                                trabajador.get("chaleco_retiros")
                                        );

                                datos.put(
                                        "chaleco_retiros",
                                        actual + 1
                                );

                                // El trabajador se retiró el chaleco
                                datos.put(
                                        "chaleco",
                                        false
                                );

                            } else {

                                int actual =
                                        obtenerNumero(
                                                trabajador.get("chaleco_colocaciones")
                                        );

                                datos.put(
                                        "chaleco_colocaciones",
                                        actual + 1
                                );

                                // El trabajador se colocó el chaleco
                                datos.put(
                                        "chaleco",
                                        true
                                );
                            }
                        }


                        actualizarTrabajador(
                                datos
                        );

                    }


                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        btnAplicar.setEnabled(true);


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }


    // =========================================================
    // CONVERTIR NÚMERO
    // =========================================================

    private int obtenerNumero(
            Object valor
    ) {

        if (valor == null) {

            return 0;
        }


        if (valor instanceof Number) {

            return ((Number) valor).intValue();
        }


        try {

            return Integer.parseInt(
                    String.valueOf(valor)
            );

        } catch (Exception e) {

            return 0;
        }

    }


    // =========================================================
    // ACTUALIZAR TRABAJADOR
    // =========================================================

    private void actualizarTrabajador(
            Map<String, Object> datos
    ) {

        String authorization =
                "Bearer " +
                        SupabaseConfig.API_KEY;


        api.actualizarContadoresTrabajador(
                authorization,
                "eq." + trabajadorId,
                datos
        ).enqueue(
                new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        if (!response.isSuccessful()) {

                            btnAplicar.setEnabled(true);


                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Error actualizando contador. Código: " +
                                            response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        // Finalmente marcamos la alerta
                        // como atendida.

                        actualizarAlerta();

                    }


                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ) {

                        btnAplicar.setEnabled(true);


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }


    // =========================================================
    // ACTUALIZAR ALERTA
    // =========================================================

    private void actualizarAlerta() {

        Map<String, Object> datos =
                new HashMap<>();


        datos.put(
                "estado",
                "ATENDIDA"
        );


        String authorization =
                "Bearer " +
                        SupabaseConfig.API_KEY;


        api.actualizarAlerta(
                authorization,
                "eq." + alertaId,
                datos
        ).enqueue(
                new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        btnAplicar.setEnabled(true);


                        if (!response.isSuccessful()) {

                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "La acción se guardó, pero no se pudo actualizar la alerta",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Acción registrada correctamente",
                                Toast.LENGTH_SHORT
                        ).show();


                        finish();

                    }


                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ) {

                        btnAplicar.setEnabled(true);


                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error actualizando alerta: " +
                                        t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }

                }
        );

    }

}