package com.safevision.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrarAccionActivity extends BaseActivity {

    private static final String TAG = "RegistrarAccion";

    private EditText edtDni;
    private EditText edtNombre;
    private EditText edtApellido;
    private EditText edtObservacion;

    private TextView txtEstadoDni;
    private TextView txtArea;


    private Button btnVerificarDni;
    private Button btnCancelar;
    private Button btnAplicar;

    private SupabaseApi api;

    // ID del trabajador encontrado o registrado (-1 si aún no existe)
    private int trabajadorId = -1;

    // ID de la alerta que estamos atendiendo
    private int alertaId = -1;

    // Indica si el trabajador ya existe en public.trabajadores
    private boolean trabajadorExiste = false;

    // Contadores en memoria del trabajador existente
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


        btnVerificarDni = findViewById(R.id.btnVerificarDni);
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
        String problemaAlerta = getIntent().getStringExtra("problema");
        String implementoAlerta = determinarImplemento(problemaAlerta, alertCasco, alertChaleco);
        TextView txtImplemento = findViewById(R.id.txtImplemento);
        txtImplemento.setText(implementoAlerta);
        // =====================================================

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
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // =====================================================
        // BOTONES
        // =====================================================
        btnVerificarDni.setOnClickListener(v -> verificarDniManual());
        btnAplicar.setOnClickListener(v -> aplicarAccion());

        // CANCELAR: comportamiento estrictamente de salir sin guardar ni modificar nada
        btnCancelar.setOnClickListener(v -> finish());

        // Si la alerta ya tenía un trabajador asociado previamente, precargarlo
        if (trabajadorIdIntent > 0) {
            cargarTrabajadorPorId(trabajadorIdIntent);
        }
    }

    private String determinarImplemento(
            String problema,
            boolean alertCasco,
            boolean alertChaleco) {

        String prob = problema != null
                ? problema.toLowerCase()
                : "";

        if (prob.contains("casco") || !alertCasco) {
            return "Casco";
        }

        if (prob.contains("chaleco") || !alertChaleco) {
            return "Chaleco";
        }

        return "Casco";
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
                            manejarErrorHttp(response.code(), "consultar trabajador por DNI", response);
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
                            edtNombre.requestFocus();

                            txtArea.setText("Producción");
                            txtEstadoDni.setText("Trabajador no registrado. Ingrese nombres y apellidos.");
                            Toast.makeText(
                                    RegistrarAccionActivity.this,
                                    "DNI no registrado. Complete nombres y apellidos.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        btnVerificarDni.setEnabled(true);
                        txtEstadoDni.setText("Error de conexión");
                        manejarErrorFallo("consultar trabajador por DNI", t);
                    }
                }
        );
    }

    // =========================================================
    // APLICAR ACCIÓN (ÚNICO BOTÓN PRINCIPAL DE GUARDADO)
    // =========================================================
    private void aplicarAccion() {
        if (alertaId <= 0) {
            Toast.makeText(this, "No se encontró la alerta correspondiente", Toast.LENGTH_SHORT).show();
            return;
        }

        final String dni = edtDni.getText().toString().trim();
        if (dni.length() != 8) {
            edtDni.setError("Ingrese un DNI válido de 8 dígitos");
            edtDni.requestFocus();
            return;
        }

        TextView txtImplemento = findViewById(R.id.txtImplemento);
        final String implemento = txtImplemento.getText().toString().trim();
        final String observacion = edtObservacion.getText().toString().trim();
        final String accionTexto = "Retiro de " + implemento.toLowerCase();

        // 1. Deshabilitar botón Aplicar para evitar doble clic
        btnAplicar.setEnabled(false);
        txtEstadoDni.setText("Procesando registro...");

        Log.d(TAG, "iniciando guardado");
        Log.d(TAG, "ID alerta: " + alertaId + ", EPP: " + implemento + ", estado RETIRADO");

        // 2. Consultar Supabase por DNI para determinar si existe o se debe crear
        api.buscarTrabajadorPorDni(getAuthorization(), "*", "eq." + dni).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        if (!response.isSuccessful()) {
                            manejarErrorHttp(response.code(), "buscar trabajador en Aplicar", response);
                            return;
                        }

                        List<Map<String, Object>> res = response.body();

                        if (res != null && !res.isEmpty()) {
                            // CASO A: TRABAJADOR YA EXISTE -> REUTILIZAR SIN DUPLICAR
                            Map<String, Object> trab = res.get(0);
                            poblarDatosTrabajadorExistente(trab);
                            int idExistente = obtenerNumero(trab.get("id"));
                            Log.d(TAG, "trabajador encontrado: ID " + idExistente);

                            procesarAccionYAlerta(idExistente, trab, implemento, accionTexto, observacion);
                        } else {
                            // CASO B: TRABAJADOR NO EXISTE -> VALIDAR NOMBRES Y CREARLO
                            String nombres = edtNombre.getText().toString().trim();
                            String apellidos = edtApellido.getText().toString().trim();

                            if (nombres.isEmpty()) {
                                btnAplicar.setEnabled(true);
                                edtNombre.setEnabled(true);
                                edtNombre.setError("Ingrese el nombre del trabajador");
                                edtNombre.requestFocus();
                                txtEstadoDni.setText("DNI no registrado. Complete los datos.");
                                return;
                            }

                            if (apellidos.isEmpty()) {
                                btnAplicar.setEnabled(true);
                                edtApellido.setEnabled(true);
                                edtApellido.setError("Ingrese el apellido del trabajador");
                                edtApellido.requestFocus();
                                txtEstadoDni.setText("DNI no registrado. Complete los datos.");
                                return;
                            }

                            crearTrabajadorYProcesar(dni, nombres, apellidos, "Producción", implemento, accionTexto, observacion);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        manejarErrorFallo("buscar trabajador en Aplicar", t);
                    }
                }
        );
    }

    // =========================================================
    // CREAR TRABAJADOR NUEVO Y CONTINUAR CON EL FLUJO
    // =========================================================
    private void crearTrabajadorYProcesar(
            final String dni,
            final String nombres,
            final String apellidos,
            final String area,
            final String implemento,
            final String accionTexto,
            final String observacion
    ) {
        Log.d(TAG, "creando nuevo trabajador DNI: " + dni);

        Map<String, Object> nuevoTrabajador = new HashMap<>();
        nuevoTrabajador.put("dni", dni);
        nuevoTrabajador.put("nombres", nombres);
        nuevoTrabajador.put("apellidos", apellidos);
        nuevoTrabajador.put("area", area);

        // Estado inicial de EPP: el EPP retirado se marca en false y su contador en 1
        boolean initCasco = !"Casco".equalsIgnoreCase(implemento);
        boolean initChaleco = !"Chaleco".equalsIgnoreCase(implemento);
        int initCascoRetiros = "Casco".equalsIgnoreCase(implemento) ? 1 : 0;
        int initChalecoRetiros = "Chaleco".equalsIgnoreCase(implemento) ? 1 : 0;

        nuevoTrabajador.put("casco", initCasco);
        nuevoTrabajador.put("chaleco", initChaleco);
        nuevoTrabajador.put("casco_retiros", initCascoRetiros);
        nuevoTrabajador.put("casco_colocaciones", 0);
        nuevoTrabajador.put("chaleco_retiros", initChalecoRetiros);
        nuevoTrabajador.put("chaleco_colocaciones", 0);

        api.registrarTrabajador(getAuthorization(), nuevoTrabajador).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {
                        if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                            manejarErrorHttp(response.code(), "insert trabajador", response);
                            return;
                        }

                        Map<String, Object> creado = response.body().get(0);
                        poblarDatosTrabajadorExistente(creado);
                        int nuevoId = obtenerNumero(creado.get("id"));
                        Log.d(TAG, "trabajador creado. ID trabajador: " + nuevoId);

                        // Con el trabajador creado, procedemos a registrar la acción y actualizar la alerta
                        insertarAccionYActualizarAlerta(nuevoId, accionTexto, observacion, null);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        manejarErrorFallo("insert trabajador", t);
                    }
                }
        );
    }

    // =========================================================
    // PROCESAR TRABAJADOR EXISTENTE Y CONTINUAR CON EL FLUJO
    // =========================================================
    private void procesarAccionYAlerta(
            final int idTrabajador,
            final Map<String, Object> trab,
            final String implemento,
            final String accionTexto,
            final String observacion
    ) {
        // Preparar actualización de contadores si es Casco o Chaleco
        Map<String, Object> datosContador = null;
        if ("Casco".equalsIgnoreCase(implemento)) {
            int cr = obtenerNumero(trab.get("casco_retiros")) + 1;
            datosContador = new HashMap<>();
            datosContador.put("casco_retiros", cr);
            datosContador.put("casco", false);
        } else if ("Chaleco".equalsIgnoreCase(implemento)) {
            int chr = obtenerNumero(trab.get("chaleco_retiros")) + 1;
            datosContador = new HashMap<>();
            datosContador.put("chaleco_retiros", chr);
            datosContador.put("chaleco", false);
        }

        insertarAccionYActualizarAlerta(idTrabajador, accionTexto, observacion, datosContador);
    }

    // =========================================================
    // INSERTAR ACCIÓN Y ACTUALIZAR ALERTA (SIN DUPLICADOS)
    // =========================================================
    private void insertarAccionYActualizarAlerta(
            final int idTrabajador,
            final String accionTexto,
            final String observacion,
            final Map<String, Object> datosContador
    ) {
        Log.d(TAG, "verificando si ya existe acción para alerta ID: " + alertaId);

        // Prevenir duplicación comprobando si ya se registró una acción para esta alerta
        api.verificarAccionExistente(getAuthorization(), "*", "eq." + alertaId).enqueue(
                new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> callCheck,
                            Response<List<Map<String, Object>>> resCheck
                    ) {
                        if (resCheck.isSuccessful() && resCheck.body() != null && !resCheck.body().isEmpty()) {
                            // Acción ya existía para esta alerta, continuar directamente a actualizar alerta y contadores
                            Log.d(TAG, "Acción ya existía previamente para esta alerta, omitiendo inserción duplicada");
                            actualizarContadoresYAlerta(idTrabajador, datosContador);
                        } else {
                            // Insertar nueva acción
                            ejecutarInsercionAccion(idTrabajador, accionTexto, observacion, datosContador);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> callCheck, Throwable t) {
                        // En caso de error de red en la consulta, intentar inserción normal
                        ejecutarInsercionAccion(idTrabajador, accionTexto, observacion, datosContador);
                    }
                }
        );
    }

    private void ejecutarInsercionAccion(
            final int idTrabajador,
            final String accionTexto,
            final String observacion,
            final Map<String, Object> datosContador
    ) {
        Log.d(TAG, "insert acción: " + accionTexto);

        Map<String, Object> datosAccion = new HashMap<>();
        datosAccion.put("alerta_id", alertaId);
        datosAccion.put("trabajador_id", idTrabajador);
        datosAccion.put("accion", accionTexto);
        datosAccion.put("observacion", observacion);

        api.registrarAccion(getAuthorization(), datosAccion).enqueue(
                new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> callAccion, Response<Void> resAccion) {
                        if (!resAccion.isSuccessful()) {
                            manejarErrorHttp(resAccion.code(), "insert acción", resAccion);
                            return;
                        }

                        Log.d(TAG, "acción creada");
                        actualizarContadoresYAlerta(idTrabajador, datosContador);
                    }

                    @Override
                    public void onFailure(Call<Void> callAccion, Throwable t) {
                        manejarErrorFallo("insert acción", t);
                    }
                }
        );
    }

    // =========================================================
    // ACTUALIZAR CONTADORES DE TRABAJADOR Y LUEGO ALERTA
    // =========================================================
    private void actualizarContadoresYAlerta(
            final int idTrabajador,
            final Map<String, Object> datosContador
    ) {
        if (datosContador != null && !datosContador.isEmpty()) {
            api.actualizarContadoresTrabajador(
                    getAuthorization(),
                    "eq." + idTrabajador,
                    datosContador
            ).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    actualizarAlertaYFinalizar(idTrabajador);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    actualizarAlertaYFinalizar(idTrabajador);
                }
            });
        } else {
            actualizarAlertaYFinalizar(idTrabajador);
        }
    }

    // =========================================================
    // ACTUALIZAR ESTADO DE ALERTA A "ATENDIDA" Y ASIGNAR TRABAJADOR
    // =========================================================
    private void actualizarAlertaYFinalizar(final int idTrabajador) {
        Log.d(TAG, "actualizando alerta ID " + alertaId + " a estado ATENDIDA");

        Map<String, Object> datosAlerta = new HashMap<>();
        datosAlerta.put("estado", "ATENDIDA");
        datosAlerta.put("trabajador_id", idTrabajador);

        api.actualizarAlerta(getAuthorization(), "eq." + alertaId, datosAlerta).enqueue(
                new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnAplicar.setEnabled(true);

                        if (!response.isSuccessful()) {
                            manejarErrorHttp(response.code(), "actualizar alerta", response);
                            return;
                        }

                        Log.d(TAG, "alerta actualizada");
                        Log.d(TAG, "guardado completado");

                        mostrarModalExito();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnAplicar.setEnabled(true);
                        manejarErrorFallo("actualizar alerta", t);
                    }
                }
        );
    }

    // =========================================================
    // MODAL DE ÉXITO
    // =========================================================
    private void mostrarModalExito() {
        new AlertDialog.Builder(this)
                .setTitle("Guardado exitosamente")
                .setMessage("El trabajador y la acción fueron registrados correctamente.")
                .setCancelable(false)
                .setPositiveButton("ACEPTAR", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(RegistrarAccionActivity.this, AlertasActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    // =========================================================
    // MANEJO DE ERRORES HTTP Y DE RED
    // =========================================================
    private void manejarErrorHttp(int codigoHttp, String operacion, Response<?> response) {
        btnAplicar.setEnabled(true);
        String detalle = "";
        try {
            if (response != null && response.errorBody() != null) {
                detalle = response.errorBody().string();
            }
        } catch (Exception ignored) {}

        Log.e(TAG, "ERROR " + operacion + " - HTTP: " + codigoHttp + ", mensaje: " + detalle);

        String mensajeUsuario;
        if (codigoHttp == 403) {
            mensajeUsuario = "No se pudo guardar la información. El servidor rechazó la operación. Verifique los permisos de acceso.";
        } else {
            mensajeUsuario = "No se pudo guardar la información. Inténtelo nuevamente.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Error al guardar")
                .setMessage(mensajeUsuario)
                .setPositiveButton("ACEPTAR", null)
                .show();
    }

    private void manejarErrorFallo(String operacion, Throwable t) {
        btnAplicar.setEnabled(true);
        Log.e(TAG, "ERROR " + operacion + " - mensaje: " + (t != null ? t.getMessage() : "desconocido"), t);

        new AlertDialog.Builder(this)
                .setTitle("Error al guardar")
                .setMessage("No se pudo guardar la información. Inténtelo nuevamente.")
                .setPositiveButton("ACEPTAR", null)
                .show();
    }

    // =========================================================
    // UTILIDADES DE CONVERSIÓN
    // =========================================================
    private int obtenerNumero(Object valor) {
        return obtenerNumero(valor, 0);
    }

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