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

import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleReporteActivity extends BaseActivity {

    private static final String TAG = "DetalleReporteActivity";

    private TextView btnRegresar;
    private ImageView imgEvidencia;
    private ProgressBar progressBarFoto;
    private TextView txtSinFoto;

    private TextView txtTrabajador;
    private TextView txtFecha;
    private TextView txtHora;
    private TextView txtArea;
    private TextView txtCamara;
    private TextView txtDescripcion;
    private TextView txtTotalVeces;
    private TextView txtEstadoBadge;

    private View cardDetallesRevision;
    private TextView txtRevisadoPor;
    private TextView txtFechaRevision;
    private TextView txtObservaciones;

    private View cardPendienteAccion;
    private MaterialButton btnIrAlertas;

    private ReporteItem reporteItem;
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
        setContentView(R.layout.activity_detalle_reporte);

        inicializarVistas();
        configurarSupabase();
        recibirDatos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificar en tiempo real si el estado de la alerta cambió o si fue eliminada
        sincronizarConSupabase();
    }

    private void inicializarVistas() {
        btnRegresar = findViewById(R.id.btnRegresarDetalleReporte);
        imgEvidencia = findViewById(R.id.imgDetalleReporteReal);
        progressBarFoto = findViewById(R.id.progressBarFotoReporte);
        txtSinFoto = findViewById(R.id.txtSinFotoReporte);

        txtTrabajador = findViewById(R.id.txtDetalleTrabajador);
        txtFecha = findViewById(R.id.txtDetalleFecha);
        txtHora = findViewById(R.id.txtDetalleHora);
        txtArea = findViewById(R.id.txtDetalleArea);
        txtCamara = findViewById(R.id.txtDetalleCamara);
        txtDescripcion = findViewById(R.id.txtDetalleDescripcion);
        txtTotalVeces = findViewById(R.id.txtDetalleTotalVeces);
        txtEstadoBadge = findViewById(R.id.txtDetalleEstadoBadge);

        cardDetallesRevision = findViewById(R.id.cardDetallesRevision);
        txtRevisadoPor = findViewById(R.id.txtDetalleRevisadoPor);
        txtFechaRevision = findViewById(R.id.txtDetalleFechaRevision);
        txtObservaciones = findViewById(R.id.txtDetalleObservaciones);

        cardPendienteAccion = findViewById(R.id.cardPendienteAccion);
        btnIrAlertas = findViewById(R.id.btnIrAlertas);

        btnRegresar.setOnClickListener(v -> finish());

        btnIrAlertas.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleReporteActivity.this, AlertasActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
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

    private void recibirDatos() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("reporte_item")) {
            reporteItem = (ReporteItem) intent.getSerializableExtra("reporte_item");
            if (reporteItem != null) {
                mostrarDatosEnPantalla(reporteItem);
            }
        }
    }

    private void mostrarDatosEnPantalla(ReporteItem item) {
        txtTrabajador.setText(item.getTrabajadorNombre());
        txtFecha.setText(item.getFecha());
        txtHora.setText(item.getHora());
        txtArea.setText(item.getArea());
        txtCamara.setText(item.getCamara());
        txtDescripcion.setText(item.getDescripcion());
        txtTotalVeces.setText(String.valueOf(item.getTotalVecesReportado()));

        cargarImagenReal(item.getImagen());

        if (item.isRevisado()) {
            // REPORTE REVISADO (SEGUNDA IMAGEN)
            txtEstadoBadge.setText("Revisado");
            txtEstadoBadge.setBackgroundResource(R.drawable.bg_badge_revisado);
            txtEstadoBadge.setTextColor(0xFF2E7D32);

            cardDetallesRevision.setVisibility(View.VISIBLE);
            cardPendienteAccion.setVisibility(View.GONE);

            txtRevisadoPor.setText(item.getRevisadoPor());
            txtFechaRevision.setText(item.getFechaRevision());
            txtObservaciones.setText(item.getObservaciones());

        } else {
            // REPORTE PENDIENTE (TERCERA IMAGEN)
            txtEstadoBadge.setText("Pendiente");
            txtEstadoBadge.setBackgroundResource(R.drawable.bg_badge_pendiente);
            txtEstadoBadge.setTextColor(0xFFE65100);

            cardDetallesRevision.setVisibility(View.GONE);
            cardPendienteAccion.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================
    // SINCRONIZACIÓN EN TIEMPO REAL CON SUPABASE
    // =========================================================
    private void sincronizarConSupabase() {
        if (reporteItem == null || reporteItem.getAlertaId() <= 0) return;

        final String apiKey = SupabaseConfig.API_KEY;
        final String auth = getAuthorization();
        final int alertaId = reporteItem.getAlertaId();

        // Consultar si la alerta aún existe y su estado actual
        supabaseApi.obtenerAlertaPorId(
                apiKey,
                auth,
                "id,created_at,estado,trabajador_id,problema,casco,chaleco,imagen,imagen_normal,imagen_zoom",
                "eq." + alertaId
        ).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    List<Map<String, Object>> lista = response.body();
                    if (lista == null || lista.isEmpty()) {
                        // La alerta fue eliminada de Supabase
                        Toast.makeText(DetalleReporteActivity.this, "Este reporte ya no existe en el sistema", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Map<String, Object> alertaActual = lista.get(0);
                    String estadoRaw = obtenerTextoSeguro(alertaActual.get("estado")).trim().toUpperCase();
                    boolean esRevisado = "ATENDIDA".equals(estadoRaw) || "REVISADO".equals(estadoRaw);

                    if (esRevisado) {
                        // Buscar si ya tiene acción registrada
                        consultarAccionActualizada(alertaId, apiKey, auth);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Error sincronizando alerta: " + t.getMessage());
            }
        });
    }

    private void consultarAccionActualizada(int alertaId, String apiKey, String auth) {
        supabaseApi.verificarAccionExistente(
                auth,
                "accion,observacion,fecha",
                "eq." + alertaId
        ).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> accion = response.body().get(0);
                    String obs = obtenerTextoSeguro(accion.get("observacion"));
                    if (obs.isEmpty()) obs = "Atendida";
                    String fechaStr = obtenerTextoSeguro(accion.get("fecha"));
                    ZonedDateTime zdt = parsearFechaIso(fechaStr, ZoneId.systemDefault());
                    String fechaRev = zdt != null ? zdt.format(dateFormatter) + " " + formatearHora(zdt) : "--/--/----";

                    // Cambiar a vista revisado
                    txtEstadoBadge.setText("Revisado");
                    txtEstadoBadge.setBackgroundResource(R.drawable.bg_badge_revisado);
                    txtEstadoBadge.setTextColor(0xFF2E7D32);

                    cardDetallesRevision.setVisibility(View.VISIBLE);
                    cardPendienteAccion.setVisibility(View.GONE);

                    txtFechaRevision.setText(fechaRev);
                    txtObservaciones.setText(obs);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Error consultando acción: " + t.getMessage());
            }
        });
    }

    // =========================================================
    // DESCARGAR IMAGEN REAL TOMADA POR LA CÁMARA
    // =========================================================
    private void cargarImagenReal(String urlFoto) {
        if (urlFoto == null || urlFoto.trim().isEmpty() || !urlFoto.startsWith("http")) {
            progressBarFoto.setVisibility(View.GONE);
            txtSinFoto.setVisibility(View.VISIBLE);
            return;
        }

        progressBarFoto.setVisibility(View.VISIBLE);
        txtSinFoto.setVisibility(View.GONE);

        executor.execute(() -> {
            HttpURLConnection conexion = null;
            try {
                URL url = new URL(urlFoto);
                conexion = (HttpURLConnection) url.openConnection();
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);
                conexion.setDoInput(true);
                conexion.connect();

                InputStream input = conexion.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                runOnUiThread(() -> {
                    progressBarFoto.setVisibility(View.GONE);
                    if (bitmap != null && !isFinishing() && !isDestroyed()) {
                        imgEvidencia.setImageBitmap(bitmap);
                        txtSinFoto.setVisibility(View.GONE);
                    } else {
                        txtSinFoto.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBarFoto.setVisibility(View.GONE);
                    txtSinFoto.setVisibility(View.VISIBLE);
                });
            } finally {
                if (conexion != null) conexion.disconnect();
            }
        });
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

    private String obtenerTextoSeguro(Object obj) {
        return obj != null ? obj.toString().trim() : "";
    }
}
