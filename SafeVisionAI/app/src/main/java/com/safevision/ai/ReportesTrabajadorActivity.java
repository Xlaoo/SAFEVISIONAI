package com.safevision.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportesTrabajadorActivity extends BaseActivity {

    private static final String TAG = "ReportesTrabajador";

    private TextView btnRegresar;
    private TextView txtTitulo;
    private ImageView imgFotoTrabajador;
    private TextView txtNombreTrabajador;
    private TextView txtAreaTrabajador;
    private TextView txtVecesReportado;
    private TextView txtSinReportes;
    private ProgressBar progressBar;
    private RecyclerView recyclerHistorial;

    private ReporteHistorialAdapter adapter;
    private final List<ReporteItem> listaReportes = new ArrayList<>();

    private int trabajadorId = -1;
    private String nombres = "";
    private String apellidos = "";
    private String dni = "";
    private String area = "Producción";
    private String fotoUrl = "";

    private String supervisorNombre = "Supervisor SST";

    private SupabaseApi supabaseApi;
    private String accessToken;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes_trabajador);

        inicializarVistas();
        recibirParametros();
        configurarSupabase();
        cargarNombreSupervisor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Siempre recargar de Supabase para reflejar cambios, eliminaciones o nuevas revisiones
        cargarDatosDesdeSupabase();
    }

    private void inicializarVistas() {
        btnRegresar = findViewById(R.id.btnRegresarReportesTrabajador);
        txtTitulo = findViewById(R.id.txtTituloReportesTrabajador);
        imgFotoTrabajador = findViewById(R.id.imgFotoTrabajadorReportes);
        txtNombreTrabajador = findViewById(R.id.txtNombreTrabajadorHeader);
        txtAreaTrabajador = findViewById(R.id.txtAreaTrabajadorHeader);
        txtVecesReportado = findViewById(R.id.txtVecesReportadoHeader);
        txtSinReportes = findViewById(R.id.txtSinReportes);
        progressBar = findViewById(R.id.progressBarReportesTrabajador);
        recyclerHistorial = findViewById(R.id.recyclerHistorialReportes);

        btnRegresar.setOnClickListener(v -> finish());

        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReporteHistorialAdapter(listaReportes, this::abrirDetalleReporte);
        recyclerHistorial.setAdapter(adapter);
    }

    private void recibirParametros() {
        Intent intent = getIntent();
        if (intent != null) {
            trabajadorId = intent.getIntExtra("trabajador_id", -1);
            nombres = obtenerTextoSeguro(intent.getStringExtra("nombres"));
            apellidos = obtenerTextoSeguro(intent.getStringExtra("apellidos"));
            dni = obtenerTextoSeguro(intent.getStringExtra("dni"));
            area = obtenerTextoSeguro(intent.getStringExtra("area"));
            if (area.isEmpty()) area = "Producción";
            fotoUrl = obtenerTextoSeguro(intent.getStringExtra("foto"));
        }

        actualizarCabeceraTrabajador();
    }

    private void actualizarCabeceraTrabajador() {
        String nombreCompleto = (nombres + " " + apellidos).trim();
        if (nombreCompleto.isEmpty()) {
            nombreCompleto = "Trabajador #" + trabajadorId;
        }

        txtTitulo.setText("Reportes de " + nombreCompleto);
        txtNombreTrabajador.setText(nombreCompleto);
        txtAreaTrabajador.setText("Área: " + area);

        cargarFotoTrabajador(fotoUrl);
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

    private void cargarNombreSupervisor() {
        SharedPreferences preferences = getSharedPreferences("SafeVisionSession", MODE_PRIVATE);
        String uid = preferences.getString("uid", null);
        String correo = preferences.getString("correo", "");

        if (uid != null && !uid.trim().isEmpty()) {
            supabaseApi.obtenerPerfilUsuario(
                    getAuthorization(),
                    "nombres,apellidos",
                    "eq." + uid
            ).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Map<String, Object> perfil = response.body().get(0);
                        String n = obtenerTextoSeguro(perfil.get("nombres"));
                        String a = obtenerTextoSeguro(perfil.get("apellidos"));
                        String nom = (n + " " + a).trim();
                        if (!nom.isEmpty()) {
                            supervisorNombre = nom;
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error cargando supervisor: " + t.getMessage());
                }
            });
        }
    }

    // =========================================================
    // CONSULTA DIRECTA A SUPABASE (FUENTE ÚNICA DE VERDAD)
    // =========================================================
    private void cargarDatosDesdeSupabase() {
        if (trabajadorId <= 0) return;

        progressBar.setVisibility(View.VISIBLE);

        final String apiKey = SupabaseConfig.API_KEY;
        final String auth = getAuthorization();

        // 1. Cargar las alertas reales de este trabajador
        supabaseApi.obtenerAlertasPorTrabajador(
                apiKey,
                auth,
                "id,created_at,estado,trabajador_id,problema,casco,chaleco,imagen,imagen_normal,imagen_zoom",
                "eq." + trabajadorId,
                "created_at.desc"
        ).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> callAlertas, Response<List<Map<String, Object>>> resAlertas) {
                if (!resAlertas.isSuccessful() || resAlertas.body() == null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ReportesTrabajadorActivity.this, "Error al consultar reportes", Toast.LENGTH_SHORT).show();
                    return;
                }

                final List<Map<String, Object>> alertasRaw = resAlertas.body();

                // 2. Cargar las acciones registradas para este trabajador
                supabaseApi.obtenerAccionesPorTrabajador(
                        apiKey,
                        auth,
                        "id,alerta_id,trabajador_id,accion,observacion,fecha",
                        "eq." + trabajadorId,
                        "fecha.desc"
                ).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> callAcciones, Response<List<Map<String, Object>>> resAcciones) {
                        progressBar.setVisibility(View.GONE);
                        List<Map<String, Object>> accionesRaw = resAcciones.body() != null ? resAcciones.body() : new ArrayList<>();

                        procesarReportesReales(alertasRaw, accionesRaw);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> callAcciones, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        procesarReportesReales(alertasRaw, new ArrayList<>());
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> callAlertas, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReportesTrabajadorActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procesarReportesReales(List<Map<String, Object>> alertas, List<Map<String, Object>> acciones) {
        ZoneId zonaLocal = ZoneId.systemDefault();

        // Mapear acciones por alerta_id para vinculación rápida
        Map<Integer, Map<String, Object>> accionesPorAlertaId = new HashMap<>();
        for (Map<String, Object> accion : acciones) {
            int alertaId = obtenerEntero(accion.get("alerta_id"));
            if (alertaId > 0 && !accionesPorAlertaId.containsKey(alertaId)) {
                accionesPorAlertaId.put(alertaId, accion);
            }
        }

        listaReportes.clear();
        int vecesReportadoRevisados = 0;

        String nombreCompleto = (nombres + " " + apellidos).trim();
        if (nombreCompleto.isEmpty()) nombreCompleto = "Trabajador #" + trabajadorId;

        for (Map<String, Object> alerta : alertas) {
            int alertaId = obtenerEntero(alerta.get("id"));
            String createdAtStr = obtenerTextoSeguro(alerta.get("created_at"));
            String estadoRaw = obtenerTextoSeguro(alerta.get("estado")).trim().toUpperCase();

            boolean esRevisado = "ATENDIDA".equals(estadoRaw) || "REVISADO".equals(estadoRaw);
            if (esRevisado) {
                vecesReportadoRevisados++;
            }

            ZonedDateTime zdt = parsearFechaIso(createdAtStr, zonaLocal);
            String fechaDisplay = zdt != null ? zdt.format(dateFormatter) : "--/--/----";
            String horaDisplay = zdt != null ? formatearHora(zdt) : "--:--";

            String problema = obtenerTextoSeguro(alerta.get("problema"));
            if (problema.isEmpty()) problema = "Infracción EPP detectada";

            String imagen = obtenerTextoSeguro(alerta.get("imagen"));
            String imagenNormal = obtenerTextoSeguro(alerta.get("imagen_normal"));
            String imagenZoom = obtenerTextoSeguro(alerta.get("imagen_zoom"));

            String estadoTexto = esRevisado ? "Revisado" : "Pendiente";

            String revisadoPor = "—";
            String fechaRevision = "—";
            String observaciones = "—";

            if (esRevisado) {
                Map<String, Object> accionRelacionada = accionesPorAlertaId.get(alertaId);
                if (accionRelacionada != null) {
                    observaciones = obtenerTextoSeguro(accionRelacionada.get("observacion"));
                    if (observaciones.isEmpty()) observaciones = "Revisión completada";

                    String fechaAccionStr = obtenerTextoSeguro(accionRelacionada.get("fecha"));
                    ZonedDateTime zdtAccion = parsearFechaIso(fechaAccionStr, zonaLocal);
                    if (zdtAccion != null) {
                        fechaRevision = zdtAccion.format(dateFormatter) + " " + formatearHora(zdtAccion);
                    }
                } else {
                    observaciones = "Atendida";
                    fechaRevision = fechaDisplay + " " + horaDisplay;
                }
                revisadoPor = supervisorNombre;
            }

            ReporteItem item = new ReporteItem(
                    alertaId,
                    trabajadorId,
                    nombreCompleto,
                    area,
                    fotoUrl,
                    fechaDisplay,
                    horaDisplay,
                    area,
                    "Cámara 01",
                    problema,
                    estadoTexto,
                    esRevisado,
                    imagen,
                    imagenNormal,
                    imagenZoom,
                    revisadoPor,
                    fechaRevision,
                    observaciones,
                    0
            );

            listaReportes.add(item);
        }

        // Actualizar totalVecesReportado en cada item para el detalle
        for (ReporteItem rep : listaReportes) {
            rep.setTotalVecesReportado(vecesReportadoRevisados);
        }

        // REGLA FUNDAMENTAL: VECES REPORTADO = cantidad REAL de reportes revisados/atendidos
        txtVecesReportado.setText(String.valueOf(vecesReportadoRevisados));

        adapter.actualizarLista(listaReportes);

        if (listaReportes.isEmpty()) {
            txtSinReportes.setVisibility(View.VISIBLE);
        } else {
            txtSinReportes.setVisibility(View.GONE);
        }
    }

    private void abrirDetalleReporte(ReporteItem item) {
        Intent intent = new Intent(this, DetalleReporteActivity.class);
        intent.putExtra("reporte_item", item);
        startActivity(intent);
    }

    private void cargarFotoTrabajador(String urlFoto) {
        if (urlFoto != null && urlFoto.startsWith("http")) {
            executor.execute(() -> {
                HttpURLConnection conexion = null;
                try {
                    URL url = new URL(urlFoto);
                    conexion = (HttpURLConnection) url.openConnection();
                    conexion.setConnectTimeout(8000);
                    conexion.setReadTimeout(8000);
                    conexion.setDoInput(true);
                    conexion.connect();
                    InputStream input = conexion.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    runOnUiThread(() -> {
                        if (bitmap != null && !isFinishing() && !isDestroyed()) {
                            imgFotoTrabajador.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception ignored) {
                } finally {
                    if (conexion != null) conexion.disconnect();
                }
            });
        }
    }

    private ZonedDateTime parsearFechaIso(String fechaIso, ZoneId zonaLocal) {
        if (fechaIso == null || fechaIso.trim().isEmpty()) return null;
        try {
            return OffsetDateTime.parse(fechaIso).atZoneSameInstant(zonaLocal);
        } catch (Exception e) {
            try {
                return Instant.parse(fechaIso).atZone(zonaLocal);
            } catch (Exception e2) {
                try {
                    return ZonedDateTime.parse(fechaIso, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(zonaLocal);
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private String formatearHora(ZonedDateTime zdt) {
        if (zdt == null) return "--:--";
        String formatted = zdt.format(timeFormatter);
        return formatted.replace("a. m.", "AM")
                .replace("p. m.", "PM")
                .replace("am", "AM")
                .replace("pm", "PM");
    }

    private int obtenerEntero(Object obj) {
        if (obj == null) return -1;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return (int) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    private String obtenerTextoSeguro(Object obj) {
        return obj != null ? obj.toString().trim() : "";
    }
}
