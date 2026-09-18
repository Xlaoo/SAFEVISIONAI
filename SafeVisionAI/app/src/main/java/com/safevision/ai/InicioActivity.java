package com.safevision.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioActivity extends BaseActivity {


    // ==========================================
    // CONTROLES
    // ==========================================

    private TextView txtNombreSupervisor;
    private TextView txtCargoSupervisor;
    private TextView txtCorreoSupervisor;

    private TextView txtCantidadTrabajadores;
    private TextView txtListaTrabajadores;

    private TextView txtCantidadAlertas;

    private MaterialButton btnVerAlertas;
    private View navCamaras;
    // ==========================================
    // SUPABASE
    // ==========================================

    private SupabaseApi supabaseApi;

    private String uidUsuario;
    private String accessToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_inicio);

        ocultarBarraNavegacion();
        // ==========================================
        // CONECTAR XML
        // ==========================================

        txtNombreSupervisor =
                findViewById(R.id.txtNombreSupervisor);

        txtCargoSupervisor =
                findViewById(R.id.txtCargoSupervisor);

        txtCorreoSupervisor =
                findViewById(R.id.txtCorreoSupervisor);

        txtCantidadTrabajadores =
                findViewById(R.id.txtCantidadTrabajadores);

        txtListaTrabajadores =
                findViewById(R.id.txtListaTrabajadores);

        txtCantidadAlertas =
                findViewById(R.id.txtCantidadAlertas);

        btnVerAlertas =
                findViewById(R.id.btnVerAlertas);

        navCamaras =
                findViewById(R.id.navCamaras);
        // ==========================================
        // SUPABASE
        // ==========================================

        supabaseApi =
                SupabaseClient
                        .getClient()
                        .create(SupabaseApi.class);


        // ==========================================
        // RECUPERAR SESIÓN
        // ==========================================

        SharedPreferences preferences =
                getSharedPreferences(
                        "SafeVisionSession",
                        MODE_PRIVATE
                );


        uidUsuario =
                preferences.getString(
                        "uid",
                        null
                );


        accessToken =
                preferences.getString(
                        "access_token",
                        null
                );


        // ==========================================
        // VALIDAR SESIÓN
        // ==========================================

        if (uidUsuario == null ||
                accessToken == null) {

            Toast.makeText(
                    this,
                    "Sesión no encontrada",
                    Toast.LENGTH_LONG
            ).show();

            volverLogin();

            return;
        }


        // ==========================================
        // VALORES INICIALES
        // ==========================================

        txtCantidadTrabajadores.setText("0");

        // Todavía no tenemos tabla alertas.
        txtCantidadAlertas.setText("0");


        // ==========================================
        // CARGAR INFORMACIÓN
        // ==========================================

        cargarSupervisor();

        cargarTrabajadores();


        // ==========================================
        // BOTÓN ALERTAS
        // ==========================================

        btnVerAlertas.setOnClickListener(v -> {

            Toast.makeText(
                    InicioActivity.this,
                    "La pantalla de alertas será el siguiente módulo",
                    Toast.LENGTH_SHORT
            ).show();

        });
        // ==========================================
// BOTÓN CÁMARAS
// ==========================================

        navCamaras.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            InicioActivity.this,
                            CamarasActivity.class
                    );

            startActivity(intent);

        });
    }


    // =====================================================
    // DATOS DEL SUPERVISOR
    // =====================================================

    private void cargarSupervisor() {

        String authorization =
                "Bearer " + accessToken;


        supabaseApi.obtenerPerfilUsuario(

                        authorization,

                        "nombres,apellidos,correo,rol",

                        "eq." + uidUsuario

                )

                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(

                                    Call<List<Map<String, Object>>> call,

                                    Response<List<Map<String, Object>>> response
                            ) {


                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {


                                    Map<String, Object> perfil =
                                            response.body().get(0);


                                    String nombres =
                                            obtenerTexto(
                                                    perfil,
                                                    "nombres"
                                            );


                                    String apellidos =
                                            obtenerTexto(
                                                    perfil,
                                                    "apellidos"
                                            );


                                    String correo =
                                            obtenerTexto(
                                                    perfil,
                                                    "correo"
                                            );


                                    String rol =
                                            obtenerTexto(
                                                    perfil,
                                                    "rol"
                                            );


                                    // NOMBRE

                                    txtNombreSupervisor.setText(
                                            nombres + " " + apellidos
                                    );


                                    // CARGO

                                    if (rol.equalsIgnoreCase(
                                            "supervisor"
                                    )) {

                                        txtCargoSupervisor.setText(
                                                "Supervisor de Seguridad y Salud en el Trabajo"
                                        );

                                    } else {

                                        txtCargoSupervisor.setText(
                                                rol
                                        );
                                    }


                                    // CORREO

                                    txtCorreoSupervisor.setText(
                                            correo
                                    );


                                } else {


                                    txtNombreSupervisor.setText(
                                            "Supervisor"
                                    );


                                    txtCargoSupervisor.setText(
                                            "Supervisor de Seguridad y Salud en el Trabajo"
                                    );


                                    txtCorreoSupervisor.setText(
                                            "No se pudo cargar el correo"
                                    );


                                    Log.e(
                                            "INICIO_PERFIL",
                                            "Código: "
                                                    + response.code()
                                    );
                                }

                            }


                            @Override
                            public void onFailure(

                                    Call<List<Map<String, Object>>> call,

                                    Throwable t
                            ) {


                                Log.e(
                                        "INICIO_PERFIL",
                                        "Error: "
                                                + t.getMessage()
                                );


                                Toast.makeText(
                                        InicioActivity.this,
                                        "Error cargando supervisor",
                                        Toast.LENGTH_SHORT
                                ).show();

                            }

                        }
                );
    }


    // =====================================================
    // TRABAJADORES
    // =====================================================

    private void cargarTrabajadores() {


        String authorization =
                "Bearer " + accessToken;


        supabaseApi.obtenerTrabajadores(

                        authorization,

                        "id,nombres,apellidos,dni,cargo,area,foto",

                        "created_at.asc"

                )

                .enqueue(
                        new Callback<List<Map<String, Object>>>() {


                            @Override
                            public void onResponse(

                                    Call<List<Map<String, Object>>> call,

                                    Response<List<Map<String, Object>>> response
                            ) {


                                if (response.isSuccessful()
                                        && response.body() != null) {


                                    List<Map<String, Object>> trabajadores =
                                            response.body();


                                    // ==========================================
                                    // CANTIDAD
                                    // ==========================================

                                    txtCantidadTrabajadores.setText(
                                            String.valueOf(
                                                    trabajadores.size()
                                            )
                                    );


                                    // ==========================================
                                    // SI ESTÁ VACÍA
                                    // ==========================================

                                    if (trabajadores.isEmpty()) {

                                        txtListaTrabajadores.setText(
                                                "No hay trabajadores registrados"
                                        );

                                        return;
                                    }


                                    // ==========================================
                                    // CREAR LISTADO
                                    // ==========================================

                                    StringBuilder lista =
                                            new StringBuilder();


                                    for (Map<String, Object> trabajador
                                            : trabajadores) {


                                        String nombres =
                                                obtenerTexto(
                                                        trabajador,
                                                        "nombres"
                                                );


                                        String apellidos =
                                                obtenerTexto(
                                                        trabajador,
                                                        "apellidos"
                                                );


                                        String area =
                                                obtenerTexto(
                                                        trabajador,
                                                        "area"
                                                );


                                        String cargo =
                                                obtenerTexto(
                                                        trabajador,
                                                        "cargo"
                                                );


                                        lista.append(
                                                "• "
                                        );


                                        lista.append(
                                                nombres
                                        );


                                        lista.append(
                                                " "
                                        );


                                        lista.append(
                                                apellidos
                                        );


                                        lista.append(
                                                "\n"
                                        );


                                        lista.append(
                                                "   "
                                        );


                                        lista.append(
                                                cargo
                                        );


                                        lista.append(
                                                "  •  "
                                        );


                                        lista.append(
                                                area
                                        );


                                        lista.append(
                                                "\n\n"
                                        );
                                    }


                                    txtListaTrabajadores.setText(
                                            lista.toString().trim()
                                    );


                                } else {


                                    txtCantidadTrabajadores.setText(
                                            "0"
                                    );


                                    txtListaTrabajadores.setText(
                                            "No se pudieron cargar los trabajadores"
                                    );


                                    Log.e(
                                            "TRABAJADORES",
                                            "Código HTTP: "
                                                    + response.code()
                                    );

                                }

                            }


                            @Override
                            public void onFailure(

                                    Call<List<Map<String, Object>>> call,

                                    Throwable t
                            ) {


                                txtCantidadTrabajadores.setText(
                                        "0"
                                );


                                txtListaTrabajadores.setText(
                                        "Error de conexión"
                                );


                                Log.e(
                                        "TRABAJADORES",
                                        "Error: "
                                                + t.getMessage()
                                );

                            }

                        }
                );
    }


    // =====================================================
    // OBTENER STRING SEGURO
    // =====================================================

    private String obtenerTexto(

            Map<String, Object> mapa,

            String campo
    ) {

        Object valor =
                mapa.get(campo);


        if (valor == null) {

            return "";
        }


        return valor.toString();
    }


    // =====================================================
    // VOLVER AL LOGIN
    // =====================================================

    private void volverLogin() {


        Intent intent =
                new Intent(
                        InicioActivity.this,
                        MainActivity.class
                );


        startActivity(intent);

        finish();
    }
    private void ocultarBarraNavegacion() {

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.hide(
                WindowInsetsCompat.Type.navigationBars()
        );

        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }
    @Override
    protected void onResume() {
        super.onResume();
        ocultarBarraNavegacion();
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            ocultarBarraNavegacion();
        }
    }
}