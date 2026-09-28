package com.safevision.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportesActivity extends BaseActivity {

    private static final String TAG = "ReportesActivity";

    private enum FiltroPeriodo {
        HOY,
        SEMANAL,
        MENSUAL
    }

    private FiltroPeriodo filtroActual = FiltroPeriodo.HOY;

    // Vistas UI - Tarjetas de Resumen
    private TextView txtTotalAlertas;
    private TextView txtAlertasAtendidas;
    private TextView txtAlertasPendientes;

    // Botones de Filtro
    private TextView btnFiltroHoy;
    private TextView btnFiltroSemanal;
    private TextView btnFiltroMensual;

    // Tarjetas Secundarias
    private TextView txtTrabajadoresReportados;
    private TextView txtTotalIncidencias;

    // Historial de Trabajadores
    private TextView txtHistorialVacio;
    private RecyclerView recyclerTrabajadoresReporte;
    private TrabajadorReporteAdapter trabajadorAdapter;

    // Estados de Carga y Error
    private ProgressBar progressBarReportes;
    private View layoutErrorReportes;
    private TextView txtMensajeError;
    private MaterialButton btnReintentarReportes;
    private NestedScrollView scrollContenidoReportes;

    // Supabase
    private SupabaseApi supabaseApi;
    private String accessToken;

    // Datos en memoria descargados de Supabase
    private final List<Map<String, Object>> listaAlertasRaw = new ArrayList<>();
    private final List<Map<String, Object>> listaAccionesRaw = new ArrayList<>();
    private final Map<Integer, Map<String, Object>> mapaTrabajadoresPorId = new HashMap<>();

    // Formateador de fecha para "Último reporte"
    private final DateTimeFormatter formatterDisplay =
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        configurarMenuInferior();

        inicializarVistas();
        configurarFiltros();
        configurarRecycler();
        configurarSupabase();

        cargarDatosDesdeSupabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver a la pantalla, refrescar datos
        cargarDatosDesdeSupabase();
    }

    private void inicializarVistas() {
        txtTotalAlertas = findViewById(R.id.txtTotalAlertas);
        txtAlertasAtendidas = findViewById(R.id.txtAlertasAtendidas);
        txtAlertasPendientes = findViewById(R.id.txtAlertasPendientes);

        btnFiltroHoy = findViewById(R.id.btnFiltroHoy);
        btnFiltroSemanal = findViewById(R.id.btnFiltroSemanal);
        btnFiltroMensual = findViewById(R.id.btnFiltroMensual);

        txtTrabajadoresReportados = findViewById(R.id.txtTrabajadoresReportados);
        txtTotalIncidencias = findViewById(R.id.txtTotalIncidencias);

        txtHistorialVacio = findViewById(R.id.txtHistorialVacio);
        recyclerTrabajadoresReporte = findViewById(R.id.recyclerTrabajadoresReporte);

        progressBarReportes = findViewById(R.id.progressBarReportes);
        layoutErrorReportes = findViewById(R.id.layoutErrorReportes);
        txtMensajeError = findViewById(R.id.txtMensajeError);
        btnReintentarReportes = findViewById(R.id.btnReintentarReportes);
        scrollContenidoReportes = findViewById(R.id.scrollContenidoReportes);

        btnReintentarReportes.setOnClickListener(v -> cargarDatosDesdeSupabase());
    }

    private void configurarFiltros() {
        btnFiltroHoy.setOnClickListener(v -> {
            if (filtroActual != FiltroPeriodo.HOY) {
                filtroActual = FiltroPeriodo.HOY;
                actualizarEstilosFiltros();
                calcularYMostrarReportes();
            }
        });

        btnFiltroSemanal.setOnClickListener(v -> {
            if (filtroActual != FiltroPeriodo.SEMANAL) {
                filtroActual = FiltroPeriodo.SEMANAL;
                actualizarEstilosFiltros();
                calcularYMostrarReportes();
            }
        });

        btnFiltroMensual.setOnClickListener(v -> {
            if (filtroActual != FiltroPeriodo.MENSUAL) {
                filtroActual = FiltroPeriodo.MENSUAL;
                actualizarEstilosFiltros();
                calcularYMostrarReportes();
            }
        });

        actualizarEstilosFiltros();
    }

    private void actualizarEstilosFiltros() {
        aplicarEstiloBotonFiltro(btnFiltroHoy, filtroActual == FiltroPeriodo.HOY);
        aplicarEstiloBotonFiltro(btnFiltroSemanal, filtroActual == FiltroPeriodo.SEMANAL);
        aplicarEstiloBotonFiltro(btnFiltroMensual, filtroActual == FiltroPeriodo.MENSUAL);
    }

    private void aplicarEstiloBotonFiltro(TextView boton, boolean seleccionado) {
        if (seleccionado) {
            boton.setBackgroundResource(R.drawable.bg_filtro_selected);
            boton.setTextColor(0xFFFFFFFF);
        } else {
            boton.setBackgroundResource(R.drawable.bg_filtro_unselected);
            boton.setTextColor(0xFF073A62);
        }
    }

    private void configurarRecycler() {
        recyclerTrabajadoresReporte.setLayoutManager(new LinearLayoutManager(this));
        trabajadorAdapter = new TrabajadorReporteAdapter(new ArrayList<>(), this::mostrarDetalleTrabajador);
        recyclerTrabajadoresReporte.setAdapter(trabajadorAdapter);
    }

    private void configurarSupabase() {
        supabaseApi = SupabaseClient.getClient().create(SupabaseApi.class);

        SharedPreferences preferences = getSharedPreferences("SafeVisionSession", MODE_PRIVATE);
        accessToken = preferences.getString("access_token", null);
    }

    private String getAuthorization() {
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return "Bearer " + accessToken;
        }
        return "Bearer " + SupabaseConfig.API_KEY;
    }

    // =========================================================
    // CARGAR DATOS REALES DESDE SUPABASE
    // =========================================================
    private void cargarDatosDesdeSupabase() {
        mostrarCargando(true);
        ocultarError();

        final String auth = getAuthorization();
        final String apiKey = SupabaseConfig.API_KEY;

        // 1. Obtener Trabajadores para mapear nombres, áreas y fotos
        supabaseApi.obtenerTrabajadores(auth, "id,dni,nombres,apellidos,area,foto", "created_at.asc")
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            mostrarFallo("No se pudieron cargar los reportes. Verifique su conexión e inténtelo nuevamente.");
                            return;
                        }

                        mapaTrabajadoresPorId.clear();
                        for (Map<String, Object> trab : response.body()) {
                            int id = obtenerEntero(trab.get("id"));
                            if (id > 0) {
                                mapaTrabajadoresPorId.put(id, trab);
                            }
                        }

                        // 2. Obtener Alertas
                        cargarAlertas(auth, apiKey);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Log.e(TAG, "Error cargando trabajadores: " + t.getMessage());
                        mostrarFallo("No se pudieron cargar los reportes. Verifique su conexión e inténtelo nuevamente.");
                    }
                });
    }

    private void cargarAlertas(final String auth, final String apiKey) {
        supabaseApi.obtenerAlertasReportes(apiKey, auth, "id,created_at,estado,trabajador_id,problema,casco,chaleco,imagen,imagen_normal,imagen_zoom", "created_at.desc")
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            mostrarFallo("No se pudieron cargar los reportes. Verifique su conexión e inténtelo nuevamente.");
                            return;
                        }

                        listaAlertasRaw.clear();
                        listaAlertasRaw.addAll(response.body());

                        // 3. Obtener Acciones de Alerta (Incidencias)
                        cargarAcciones(auth, apiKey);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Log.e(TAG, "Error cargando alertas: " + t.getMessage());
                        mostrarFallo("No se pudieron cargar los reportes. Verifique su conexión e inténtelo nuevamente.");
                    }
                });
    }

    private void cargarAcciones(final String auth, final String apiKey) {
        supabaseApi.obtenerAccionesReportes(apiKey, auth, "id,alerta_id,trabajador_id,accion,observacion,fecha", "fecha.desc")
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            mostrarFallo("No se pudieron cargar los reportes. Verifique su conexión e inténtelo nuevamente.");
                            return;
                        }

                        listaAccionesRaw.clear();
                        listaAccionesRaw.addAll(response.body());

                        mostrarCargando(false);
                        calcularYMostrarReportes();
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Log.e(TAG, "Error cargando acciones: " + t.getMessage());
                        mostrarFallo("No se pudieron cargar los reportes. Verifique su conexión e inténtelo nuevamente.");
                    }
                });
    }

    // =========================================================
    // CÁLCULO Y FILTRADO DE REPORTES CON FECHAS Y ZONA HORARIA
    // =========================================================
    private void calcularYMostrarReportes() {
        ZoneId zonaLocal = ZoneId.systemDefault();
        LocalDate hoy = LocalDate.now(zonaLocal);

        // Definición de límites para HOY, SEMANAL y MENSUAL
        LocalDate inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 1. FILTRAR Y CONTAR ALERTAS
        int totalAlertas = 0;
        int atendidas = 0;
        int pendientes = 0;

        for (Map<String, Object> alerta : listaAlertasRaw) {
            String createdAtStr = obtenerTexto(alerta.get("created_at"));
            ZonedDateTime fechaAlerta = parsearFechaIso(createdAtStr, zonaLocal);

            if (fechaAlerta == null) continue;

            LocalDate fechaLocalAlerta = fechaAlerta.toLocalDate();

            if (coincideConPeriodo(fechaLocalAlerta, hoy, inicioSemana, finSemana)) {
                totalAlertas++;

                String estado = obtenerTexto(alerta.get("estado")).trim().toUpperCase();
                if ("ATENDIDA".equals(estado)) {
                    atendidas++;
                } else if ("PENDIENTE".equals(estado)) {
                    pendientes++;
                }
            }
        }

        txtTotalAlertas.setText(String.valueOf(totalAlertas));
        txtAlertasAtendidas.setText(String.valueOf(atendidas));
        txtAlertasPendientes.setText(String.valueOf(pendientes));

        // 2. FILTRAR REPORTES (ALERTAS) Y AGRUPAR POR TRABAJADOR
        int totalIncidencias = 0;
        Set<Integer> trabajadoresDistintosSet = new HashSet<>();

        // Mapa: trabajador_id -> TrabajadorReporte
        Map<Integer, TrabajadorReporte> mapaReporteTrabajadores = new HashMap<>();

        for (Map<String, Object> alerta : listaAlertasRaw) {
            String createdAtStr = obtenerTexto(alerta.get("created_at"));
            ZonedDateTime fechaAlerta = parsearFechaIso(createdAtStr, zonaLocal);

            if (fechaAlerta == null) continue;

            LocalDate fechaLocalAlerta = fechaAlerta.toLocalDate();

            if (coincideConPeriodo(fechaLocalAlerta, hoy, inicioSemana, finSemana)) {
                totalIncidencias++;

                int trabajadorId = obtenerEntero(alerta.get("trabajador_id"));
                if (trabajadorId > 0) {
                    trabajadoresDistintosSet.add(trabajadorId);

                    TrabajadorReporte rep = mapaReporteTrabajadores.get(trabajadorId);
                    if (rep == null) {
                        Map<String, Object> datosTrabajador = mapaTrabajadoresPorId.get(trabajadorId);
                        String dni = datosTrabajador != null ? obtenerTexto(datosTrabajador.get("dni")) : "";
                        String nombres = datosTrabajador != null ? obtenerTexto(datosTrabajador.get("nombres")) : "";
                        String apellidos = datosTrabajador != null ? obtenerTexto(datosTrabajador.get("apellidos")) : "";
                        String area = datosTrabajador != null ? obtenerTexto(datosTrabajador.get("area")) : "Producción";
                        String foto = datosTrabajador != null ? obtenerTexto(datosTrabajador.get("foto")) : "";

                        rep = new TrabajadorReporte(trabajadorId, dni, nombres, apellidos, area);
                        rep.setFoto(foto);
                        mapaReporteTrabajadores.put(trabajadorId, rep);
                    }

                    // Actualizar último reporte si es más reciente
                    if (rep.getUltimoReporteFecha() == null || fechaAlerta.isAfter(rep.getUltimoReporteFecha())) {
                        rep.setUltimoReporteFecha(fechaAlerta);
                        rep.setUltimoReporteTexto(formatearFechaDisplay(fechaAlerta));
                    }
                }
            }
        }

        // VECES REPORTADO: Contar ÚNICAMENTE reportes REVISADOS/ATENDIDOS existentes en la base de datos
        for (TrabajadorReporte rep : mapaReporteTrabajadores.values()) {
            int revisadosCount = 0;
            for (Map<String, Object> a : listaAlertasRaw) {
                int tid = obtenerEntero(a.get("trabajador_id"));
                if (tid == rep.getId()) {
                    String est = obtenerTexto(a.get("estado")).trim().toUpperCase();
                    if ("ATENDIDA".equals(est) || "REVISADO".equals(est)) {
                        revisadosCount++;
                    }
                }
            }
            rep.setVecesReportado(revisadosCount);
        }

        // Actualizar tarjetas secundarias
        txtTrabajadoresReportados.setText(String.valueOf(trabajadoresDistintosSet.size()));
        txtTotalIncidencias.setText(String.valueOf(totalIncidencias));

        // 3. ACTUALIZAR LISTA DE TRABAJADORES (HISTORIAL)
        List<TrabajadorReporte> listaHistorial = new ArrayList<>(mapaReporteTrabajadores.values());

        // Ordenar por el más recientemente reportado primero
        Collections.sort(listaHistorial, (t1, t2) -> {
            if (t1.getUltimoReporteFecha() == null && t2.getUltimoReporteFecha() == null) return 0;
            if (t1.getUltimoReporteFecha() == null) return 1;
            if (t2.getUltimoReporteFecha() == null) return -1;
            return t2.getUltimoReporteFecha().compareTo(t1.getUltimoReporteFecha());
        });

        trabajadorAdapter.actualizarLista(listaHistorial);

        if (listaHistorial.isEmpty()) {
            txtHistorialVacio.setVisibility(View.VISIBLE);
        } else {
            txtHistorialVacio.setVisibility(View.GONE);
        }
    }

    private boolean coincideConPeriodo(LocalDate fecha, LocalDate hoy, LocalDate inicioSemana, LocalDate finSemana) {
        if (filtroActual == FiltroPeriodo.HOY) {
            return fecha.isEqual(hoy);
        } else if (filtroActual == FiltroPeriodo.SEMANAL) {
            return !fecha.isBefore(inicioSemana) && !fecha.isAfter(finSemana);
        } else if (filtroActual == FiltroPeriodo.MENSUAL) {
            return fecha.getYear() == hoy.getYear() && fecha.getMonthValue() == hoy.getMonthValue();
        }
        return false;
    }

    // =========================================================
    // BOTÓN "VER" TRABAJADOR: ABRIR HISTORIAL DE REPORTES
    // =========================================================
    private void mostrarDetalleTrabajador(TrabajadorReporte trabajador) {
        if (isFinishing() || isDestroyed()) return;

        Intent intent = new Intent(this, ReportesTrabajadorActivity.class);
        intent.putExtra("trabajador_id", trabajador.getId());
        intent.putExtra("nombres", trabajador.getNombres());
        intent.putExtra("apellidos", trabajador.getApellidos());
        intent.putExtra("dni", trabajador.getDni());
        intent.putExtra("area", trabajador.getArea());
        intent.putExtra("foto", trabajador.getFoto());
        startActivity(intent);
    }

    // =========================================================
    // UTILIDADES DE FECHA Y CONVERSIÓN
    // =========================================================
    private ZonedDateTime parsearFechaIso(String fechaIso, ZoneId zonaLocal) {
        if (fechaIso == null || fechaIso.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(fechaIso).atZoneSameInstant(zonaLocal);
        } catch (Exception e) {
            try {
                return Instant.parse(fechaIso).atZone(zonaLocal);
            } catch (Exception e2) {
                try {
                    return ZonedDateTime.parse(fechaIso, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(zonaLocal);
                } catch (Exception e3) {
                    Log.e(TAG, "No se pudo parsear fecha: " + fechaIso);
                    return null;
                }
            }
        }
    }

    private String formatearFechaDisplay(ZonedDateTime zdt) {
        if (zdt == null) return "--/--/----";
        String formatted = zdt.format(formatterDisplay);
        return formatted.replace("a. m.", "AM")
                .replace("p. m.", "PM")
                .replace("am", "AM")
                .replace("pm", "PM");
    }

    private int obtenerEntero(Object obj) {
        if (obj == null) return -1;
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return (int) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    private String obtenerTexto(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    // =========================================================
    // CONTROL DE ESTADO DE CARGA Y ERRORES
    // =========================================================
    private void mostrarCargando(boolean cargando) {
        progressBarReportes.setVisibility(cargando ? View.VISIBLE : View.GONE);
        if (cargando) {
            scrollContenidoReportes.setVisibility(View.GONE);
            layoutErrorReportes.setVisibility(View.GONE);
        } else {
            scrollContenidoReportes.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarFallo(String mensaje) {
        progressBarReportes.setVisibility(View.GONE);
        scrollContenidoReportes.setVisibility(View.GONE);
        layoutErrorReportes.setVisibility(View.VISIBLE);
        txtMensajeError.setText(mensaje);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void ocultarError() {
        layoutErrorReportes.setVisibility(View.GONE);
    }
}
