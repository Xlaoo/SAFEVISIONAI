package com.safevision.ai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

    // ID del trabajador encontrado o registrado (-1 si aún no existe)
    private int trabajadorId = -1;

    // ID de la alerta que estamos atendiendo
    private int alertaId = -1;

    // Indica si el trabajador ya existe en public.trabajadores
    private boolean trabajadorExiste = false;

    // Contadores en memoria
    private int cascoRetiros = 0;
    private int cascoColocaciones = 0;
    private int chalecoRetiros = 0;
    private int chalecoColocaciones = 0;
    private boolean estadoCasco = true;
    private boolean estadoChaleco = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_accion);

        // =====================================================
        // REFERENCIAS UI
        // =====================================================
        edtDni = findViewById(R.id.edtDni);
        edtNombre = findViewById(R.id.edtNombre);
        edtApellido = findViewById(R.id.edtApellido);
        edtObservacion = findViewById(R.id.edtObservacion);

        txtEstadoDni = findViewById(R.id.txtEstadoDni);
        txtArea = findViewById(R.id.txtArea);

        spinnerImplemento = findViewById(R.id.spinnerImplemento);
        spinnerAccion = findViewById(R.id.spinnerAccion);

        btnVerificarDni = findViewById(R.id.btnVerificarDni);
        btnRegistrarTrabajador = findViewById(R.id.btnRegistrarTrabajador);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnAplicar = findViewById(R.id.btnAplicar);

        // =====================================================
        // CLIENTE SUPABASE
        // =====================================================
        api = SupabaseClient.getClient().create(SupabaseApi.class);

        // =====================================================
        // PARÁMETROS DE LA ALERTA
        // =====================================================
        alertaId = getIntent().getIntExtra("alerta_id", -1);
        int trabajadorIdIntent = getIntent().getIntExtra("trabajador_id", -1);
        boolean alertCasco = getIntent().getBooleanExtra("casco", true);
        boolean alertChaleco = getIntent().getBooleanExtra("chaleco", true);

        // =====================================================
        // CONFIGURAR SPINNERS
        // =====================================================
        String[] implementos = {"Casco", "Chaleco"};
        ArrayAdapter<String> adapterImplementos = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                implementos
        );
        adapterImplementos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImplemento.setAdapter(adapterImplementos);

        String[] acciones = {"Retiro", "Colocación"};
        ArrayAdapter<String> adapterAcciones = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                acciones
        );
        adapterAcciones.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccion.setAdapter(adapterAcciones);

        // Preselección adecuada según la alerta recibida
        if (!alertCasco) {
            spinnerImplemento.setSelection(0); // Casco
            spinnerAccion.setSelection(0); // Retiro
        } else if (!alertChaleco) {
            spinnerImplemento.setSelection(1); // Chaleco
            spinnerAccion.setSelection(0); // Retiro
        }

        txtArea.setText("Producción");

        // Bloqueo inicial de nombres hasta verificar DNI
        edtNombre.setEnabled(false);
        edtApellido.setEnabled(false);

        // =====================================================
        // LISTENER DE CAMBIO DE DNI
        // =====================================================
        edtDni.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Al modificar el DNI, se reinicia el estado de validación
                trabajadorExiste = false;
                trabajadorId = -1;
                txtEstadoDni.setText("");
                btnRegistrarTrabajador.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // =====================================================
        // BOTONES
        // =====================================================
        btnVerificarDni.setOnClickListener(v -> verificarDniManual());
        btnRegistrarTrabajador.setOnClickListener(v -> registrarTrabajadorManual());
        btnAplicar.setOnClickListener(v -> aplicarAccion());
        btnCancelar.setOnClickListener(v -> finish());

        // Si la alerta ya tenía un trabajador asociado previamente, precargarlo
        if (trabajadorIdIntent > 0) {
            cargarTrabajadorPorId(trabajadorIdIntent);
        }
    }

    // =========================================================
    // OBTENER AUTORIZACIÓN (TOKEN DE SESIÓN O API KEY)
    // =========================================================
    private String getAuthorization() {
        SharedPreferences preferences =
                getSharedPreferences("SafeVisionSession", MODE_PRIVATE);
        String token = preferences.getString("access_token", null);
        if (token != null && !token.trim().isEmpty()) {
            return "Bearer " + token;
        }
        return "Bearer " + SupabaseConfig.API_KEY;
    }

    // =========================================================
    // PRECARGAR TRABAJADOR POR ID
    // =========================================================
    private void cargarTrabajadorPorId(int id) {
        txtEstadoDni.setText("Cargando datos del trabajador asociado...");

        api.buscarTrabajadorPorId(getAuthorization(), "*", "eq." + id).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            txtEstadoDni.setText("");
                            return;
                        }

                        Map<String, Object> trabajador = response.body().get(0);
                        poblarDatosTrabajadorExistente(trabajador);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        txtEstadoDni.setText("");
                    }
                }
        );
    }

    // =========================================================
    // MOSTRAR DATOS DE TRABAJADOR EXISTENTE
    // =========================================================
    private void poblarDatosTrabajadorExistente(Map<String, Object> trabajador) {
        trabajadorExiste = true;
        trabajadorId = obtenerNumero(trabajador.get("id"));

        String dni = trabajador.get("dni") != null ? String.valueOf(trabajador.get("dni")) : "";
        String nombres = trabajador.get("nombres") != null ? String.valueOf(trabajador.get("nombres")) : "";
        String apellidos = trabajador.get("apellidos") != null ? String.valueOf(trabajador.get("apellidos")) : "";
        String area = trabajador.get("area") != null ? String.valueOf(trabajador.get("area")) : "Producción";

        cascoRetiros = obtenerNumero(trabajador.get("casco_retiros"));
        cascoColocaciones = obtenerNumero(trabajador.get("casco_colocaciones"));
        chalecoRetiros = obtenerNumero(trabajador.get("chaleco_retiros"));
        chalecoColocaciones = obtenerNumero(trabajador.get("chaleco_colocaciones"));
        estadoCasco = obtenerBoolean(trabajador.get("casco"), true);
        estadoChaleco = obtenerBoolean(trabajador.get("chaleco"), true);

        edtDni.setText(dni);
        edtNombre.setText(nombres);
        edtApellido.setText(apellidos);
        txtArea.setText(area);

        // Bloquear nombres para evitar edición accidental o duplicados
        edtNombre.setEnabled(false);
        edtApellido.setEnabled(false);
        btnRegistrarTrabajador.setVisibility(View.GONE);

        txtEstadoDni.setText("✓ Trabajador encontrado: " + nombres + " " + apellidos);
    }

    // =========================================================
    // VERIFICAR DNI (BOTÓN "VERIFICAR")
    // =========================================================
    private void verificarDniManual() {
        String dni = edtDni.getText().toString().trim();

        if (dni.isEmpty()) {
            edtDni.setError("Ingrese el DNI");
            return;
        }

        if (dni.length() != 8) {
            edtDni.setError("El DNI debe tener 8 dígitos");
            return;
        }

        txtEstadoDni.setText("Buscando en trabajadores...");
        btnVerificarDni.setEnabled(false);

        api.buscarTrabajadorPorDni(getAuthorization(), "*", "eq." + dni).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        btnVerificarDni.setEnabled(true);

                        if (!response.isSuccessful()) {
                            txtEstadoDni.setText("Error al consultar el trabajador");
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Error HTTP: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        List<Map<String, Object>> resultado = response.body();

                        if (resultado != null && !resultado.isEmpty()) {
                            // CASO 1: EL TRABAJADOR YA EXISTE
                            poblarDatosTrabajadorExistente(resultado.get(0));
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Trabajador encontrado",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            // CASO 2: NO EXISTE
                            trabajadorExiste = false;
                            trabajadorId = -1;

                            edtNombre.setText("");
                            edtApellido.setText("");

                            edtNombre.setEnabled(true);
                            edtApellido.setEnabled(true);

                            txtArea.setText("Producción");
                            btnRegistrarTrabajador.setVisibility(View.VISIBLE);

                            txtEstadoDni.setText("Trabajador no registrado. Ingrese nombres y apellidos.");
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "DNI no registrado. Complete los datos.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        btnVerificarDni.setEnabled(true);
                        txtEstadoDni.setText("No se pudo conectar con Supabase");
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
    // REGISTRAR TRABAJADOR MANUAL (BOTÓN OPCIONAL)
    // =========================================================
    private void registrarTrabajadorManual() {
        String dni = edtDni.getText().toString().trim();
        String nombres = edtNombre.getText().toString().trim();
        String apellidos = edtApellido.getText().toString().trim();

        if (dni.length() != 8) {
            edtDni.setError("El DNI debe tener 8 dígitos");
            return;
        }

        if (nombres.isEmpty()) {
            edtNombre.setError("Ingrese el nombre");
            return;
        }

        if (apellidos.isEmpty()) {
            edtApellido.setError("Ingrese el apellido");
            return;
        }

        btnRegistrarTrabajador.setEnabled(false);

        Map<String, Object> datos = new HashMap<>();
        datos.put("dni", dni);
        datos.put("nombres", nombres);
        datos.put("apellidos", apellidos);
        datos.put("area", "Producción");
        datos.put("casco", true);
        datos.put("chaleco", true);
        datos.put("casco_retiros", 0);
        datos.put("casco_colocaciones", 0);
        datos.put("chaleco_retiros", 0);
        datos.put("chaleco_colocaciones", 0);

        api.registrarTrabajador(getAuthorization(), datos).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        btnRegistrarTrabajador.setEnabled(true);

                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "No se pudo registrar. Código: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        Map<String, Object> nuevo = response.body().get(0);
                        poblarDatosTrabajadorExistente(nuevo);

                        txtEstadoDni.setText("✓ Trabajador registrado exitosamente");
                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Trabajador registrado. Ahora puede aplicar la acción.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        btnRegistrarTrabajador.setEnabled(true);
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
    // APLICAR ACCIÓN (CONFIRMAR REGISTRO)
    // =========================================================
    private void aplicarAccion() {
        if (alertaId == -1) {
            Toast.makeText(this, "No se encontró la alerta correspondiente", Toast.LENGTH_SHORT).show();
            return;
        }

        final String dni = edtDni.getText().toString().trim();
        if (dni.length() != 8) {
            edtDni.setError("Ingrese un DNI válido de 8 dígitos");
            return;
        }

        final String implemento = spinnerImplemento.getSelectedItem().toString();
        final String accion = spinnerAccion.getSelectedItem().toString();
        final String observacion = edtObservacion.getText().toString().trim();

        btnAplicar.setEnabled(false);

        // Si ya está validado como trabajador existente con ID válido
        if (trabajadorExiste && trabajadorId > 0) {
            ejecutarAccionTrabajadorExistente(trabajadorId, dni, implemento, accion, observacion);
            return;
        }

        // Si el usuario no presionó "Verificar", buscamos primero para evitar duplicidad de DNI
        txtEstadoDni.setText("Validando DNI en el sistema...");
        api.buscarTrabajadorPorDni(getAuthorization(), "*", "eq." + dni).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        if (!response.isSuccessful()) {
                            btnAplicar.setEnabled(true);
                            txtEstadoDni.setText("Error al consultar trabajador");
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Error HTTP: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        List<Map<String, Object>> res = response.body();

                        if (res != null && !res.isEmpty()) {
                            // CASO 1: EL DNI YA EXISTE -> RECUPERAR DATOS Y NO DUPLICAR
                            Map<String, Object> trab = res.get(0);
                            poblarDatosTrabajadorExistente(trab);
                            ejecutarAccionTrabajadorExistente(trabajadorId, dni, implemento, accion, observacion);
                        } else {
                            // CASO 2: EL DNI NO EXISTE -> SOLICITAR NOMBRES Y CREAR TRABAJADOR
                            trabajadorExiste = false;
                            trabajadorId = -1;

                            String nombres = edtNombre.getText().toString().trim();
                            String apellidos = edtApellido.getText().toString().trim();

                            if (nombres.isEmpty()) {
                                btnAplicar.setEnabled(true);
                                edtNombre.setEnabled(true);
                                edtNombre.setError("Ingrese el nombre del trabajador");
                                edtNombre.requestFocus();
                                return;
                            }

                            if (apellidos.isEmpty()) {
                                btnAplicar.setEnabled(true);
                                edtApellido.setEnabled(true);
                                edtApellido.setError("Ingrese el apellido del trabajador");
                                edtApellido.requestFocus();
                                return;
                            }

                            ejecutarAccionTrabajadorNuevo(dni, nombres, apellidos, "Producción", implemento, accion, observacion);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        btnAplicar.setEnabled(true);
                        txtEstadoDni.setText("Error de conexión");
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
    // FLUJO CASO 1: TRABAJADOR EXISTENTE
    // =========================================================
    private void ejecutarAccionTrabajadorExistente(
            final int idTrabajador,
            final String dniTrabajador,
            final String implemento,
            final String accion,
            final String observacion
    ) {
        // Obtenemos los contadores más recientes del trabajador directamente
        api.buscarTrabajadorPorId(getAuthorization(), "*", "eq." + idTrabajador).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            btnAplicar.setEnabled(true);
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "No se pudieron obtener los contadores actuales del trabajador",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        Map<String, Object> trab = response.body().get(0);
                        int cr = obtenerNumero(trab.get("casco_retiros"));
                        int cc = obtenerNumero(trab.get("casco_colocaciones"));
                        int chr = obtenerNumero(trab.get("chaleco_retiros"));
                        int chc = obtenerNumero(trab.get("chaleco_colocaciones"));
                        boolean estCasco = obtenerBoolean(trab.get("casco"), true);
                        boolean estChaleco = obtenerBoolean(trab.get("chaleco"), true);

                        final String accionTexto = accion + " de " + implemento.toLowerCase();

                        // Incremento acumulativo según la acción seleccionada
                        if ("Casco".equalsIgnoreCase(implemento)) {
                            if ("Retiro".equalsIgnoreCase(accion)) {
                                cr = cr + 1;
                                estCasco = false;
                            } else {
                                cc = cc + 1;
                                estCasco = true;
                            }
                        } else if ("Chaleco".equalsIgnoreCase(implemento)) {
                            if ("Retiro".equalsIgnoreCase(accion)) {
                                chr = chr + 1;
                                estChaleco = false;
                            } else {
                                chc = chc + 1;
                                estChaleco = true;
                            }
                        }

                        final int finalCr = cr;
                        final int finalCc = cc;
                        final int finalChr = chr;
                        final int finalChc = chc;
                        final boolean finalEstCasco = estCasco;
                        final boolean finalEstChaleco = estChaleco;

                        // 1. REGISTRAR ACCIÓN EN public.acciones_alerta
                        Map<String, Object> datosAccion = new HashMap<>();
                        datosAccion.put("alerta_id", alertaId);
                        datosAccion.put("trabajador_id", idTrabajador);
                        datosAccion.put("accion", accionTexto);
                        datosAccion.put("observacion", observacion);
                        // 'fecha' se genera automáticamente en PostgreSQL con default now()

                        api.registrarAccion(getAuthorization(), datosAccion).enqueue(
                                new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> callAccion, Response<Void> resAccion) {
                                        if (!resAccion.isSuccessful()) {
                                            btnAplicar.setEnabled(true);
                                            Toast.makeText(
                                                    RegistrarAccionActivity.this,
                                                    "No se pudo guardar la acción. Código: " + resAccion.code(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            return;
                                        }

                                        // 2. ACTUALIZAR CONTADORES EN public.trabajadores
                                        Map<String, Object> datosContador = new HashMap<>();
                                        datosContador.put("casco_retiros", finalCr);
                                        datosContador.put("casco_colocaciones", finalCc);
                                        datosContador.put("chaleco_retiros", finalChr);
                                        datosContador.put("chaleco_colocaciones", finalChc);
                                        datosContador.put("casco", finalEstCasco);
                                        datosContador.put("chaleco", finalEstChaleco);

                                        api.actualizarContadoresTrabajador(
                                                getAuthorization(),
                                                "eq." + idTrabajador,
                                                datosContador
                                        ).enqueue(
                                                new Callback<Void>() {
                                                    @Override
                                                    public void onResponse(Call<Void> callTrab, Response<Void> resTrab) {
                                                        if (!resTrab.isSuccessful()) {
                                                            btnAplicar.setEnabled(true);
                                                            Toast.makeText(
                                                                    RegistrarAccionActivity.this,
                                                                    "No se pudieron actualizar los contadores. Código: " + resTrab.code(),
                                                                    Toast.LENGTH_LONG
                                                            ).show();
                                                            return;
                                                        }

                                                        // 3. ACTUALIZAR ESTADO DE LA ALERTA
                                                        actualizarAlertaYFinalizar(idTrabajador);
                                                    }

                                                    @Override
                                                    public void onFailure(Call<Void> callTrab, Throwable t) {
                                                        btnAplicar.setEnabled(true);
                                                        Toast.makeText(
                                                                RegistrarAccionActivity.this,
                                                                "Error actualizando trabajador: " + t.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    }
                                                }
                                        );
                                    }

                                    @Override
                                    public void onFailure(Call<Void> callAccion, Throwable t) {
                                        btnAplicar.setEnabled(true);
                                        Toast.makeText(
                                                RegistrarAccionActivity.this,
                                                "Error al registrar acción: " + t.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        btnAplicar.setEnabled(true);
                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error al consultar trabajador: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // FLUJO CASO 2: TRABAJADOR NUEVO
    // =========================================================
    private void ejecutarAccionTrabajadorNuevo(
            final String dni,
            final String nombres,
            final String apellidos,
            final String area,
            final String implemento,
            final String accion,
            final String observacion
    ) {
        int initCascoRetiros = 0;
        int initCascoColocaciones = 0;
        int initChalecoRetiros = 0;
        int initChalecoColocaciones = 0;
        boolean initCasco = true;
        boolean initChaleco = true;

        // Inicialización de contadores: el implemento/acción registrado comienza en 1
        if ("Casco".equalsIgnoreCase(implemento)) {
            if ("Retiro".equalsIgnoreCase(accion)) {
                initCascoRetiros = 1;
                initCasco = false;
            } else {
                initCascoColocaciones = 1;
                initCasco = true;
            }
        } else if ("Chaleco".equalsIgnoreCase(implemento)) {
            if ("Retiro".equalsIgnoreCase(accion)) {
                initChalecoRetiros = 1;
                initChaleco = false;
            } else {
                initChalecoColocaciones = 1;
                initChaleco = true;
            }
        }

        final String accionTexto = accion + " de " + implemento.toLowerCase();

        Map<String, Object> nuevoTrabajador = new HashMap<>();
        nuevoTrabajador.put("dni", dni);
        nuevoTrabajador.put("nombres", nombres);
        nuevoTrabajador.put("apellidos", apellidos);
        nuevoTrabajador.put("area", area);
        nuevoTrabajador.put("casco", initCasco);
        nuevoTrabajador.put("chaleco", initChaleco);
        nuevoTrabajador.put("casco_retiros", initCascoRetiros);
        nuevoTrabajador.put("casco_colocaciones", initCascoColocaciones);
        nuevoTrabajador.put("chaleco_retiros", initChalecoRetiros);
        nuevoTrabajador.put("chaleco_colocaciones", initChalecoColocaciones);

        // 1. CREAR EL TRABAJADOR EN public.trabajadores
        api.registrarTrabajador(getAuthorization(), nuevoTrabajador).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            btnAplicar.setEnabled(true);
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "No se pudo crear el trabajador. Código: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        int nuevoTrabajadorId = obtenerNumero(response.body().get(0).get("id"));

                        // 2. REGISTRAR LA ACCIÓN EN public.acciones_alerta
                        Map<String, Object> datosAccion = new HashMap<>();
                        datosAccion.put("alerta_id", alertaId);
                        datosAccion.put("trabajador_id", nuevoTrabajadorId);
                        datosAccion.put("accion", accionTexto);
                        datosAccion.put("observacion", observacion);
                        // 'fecha' se genera automáticamente con now()

                        api.registrarAccion(getAuthorization(), datosAccion).enqueue(
                                new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> callAcc, Response<Void> resAcc) {
                                        if (!resAcc.isSuccessful()) {
                                            btnAplicar.setEnabled(true);
                                            Toast.makeText(
                                                    RegistrarAccionActivity.this,
                                                    "Trabajador creado, pero no se pudo registrar la acción. Código: " + resAcc.code(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            return;
                                        }

                                        // 3. ACTUALIZAR ESTADO DE LA ALERTA Y ASIGNAR TRABAJADOR
                                        actualizarAlertaYFinalizar(nuevoTrabajadorId);
                                    }

                                    @Override
                                    public void onFailure(Call<Void> callAcc, Throwable t) {
                                        btnAplicar.setEnabled(true);
                                        Toast.makeText(
                                                RegistrarAccionActivity.this,
                                                "Error al registrar acción: " + t.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        btnAplicar.setEnabled(true);
                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error creando trabajador: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // ACTUALIZAR ALERTA A "ATENDIDA" Y CERRAR ACTIVIDAD
    // =========================================================
    private void actualizarAlertaYFinalizar(int idTrabajador) {
        Map<String, Object> datosAlerta = new HashMap<>();
        datosAlerta.put("estado", "ATENDIDA");
        datosAlerta.put("trabajador_id", idTrabajador);

        api.actualizarAlerta(getAuthorization(), "eq." + alertaId, datosAlerta).enqueue(
                new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnAplicar.setEnabled(true);

                        if (!response.isSuccessful()) {
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "Acción registrada, pero no se pudo actualizar el estado de la alerta. Código: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Acción registrada e incidencia atendida correctamente",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnAplicar.setEnabled(true);
                        Toast.makeText(
                                RegistrarAccionActivity.this,
                                "Error actualizando alerta: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // UTILIDADES DE CONVERSIÓN
    // =========================================================
    private int obtenerNumero(Object valor) {
        if (valor == null) {
            return 0;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(valor));
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean obtenerBoolean(Object valor, boolean defecto) {
        if (valor == null) {
            return defecto;
        }
        if (valor instanceof Boolean) {
            return (Boolean) valor;
        }
        String s = String.valueOf(valor).trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "t".equals(s)) {
            return true;
        }
        if ("false".equals(s) || "0".equals(s) || "f".equals(s)) {
            return false;
        }
        return defecto;
    }
}