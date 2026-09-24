package com.safevision.ai;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {


    private EditText txtPasswordLogin;

    private Button btnIniciarSesion;

    private TextView txtOlvidePassword;
    private TextView txtCrearCuenta;

    private SupabaseApi supabaseApi;
    private EditText txtDniLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // ==========================================
        // SERVICIO DE ALERTAS
        // ==========================================

        pedirPermisoNotificaciones();

        iniciarServicioAlertas();

        // ==========================================
        // CONECTAR CONTROLES XML
        // ==========================================

        txtDniLogin = findViewById(R.id.txtDniLogin);
        txtPasswordLogin = findViewById(R.id.txtPasswordLogin);

        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);

        txtOlvidePassword = findViewById(R.id.txtOlvidePassword);
        txtCrearCuenta = findViewById(R.id.txtCrearCuenta);

        // ==========================================
        // CONECTAR SUPABASE
        // ==========================================

        supabaseApi = SupabaseClient
                .getClient()
                .create(SupabaseApi.class);

        // ==========================================
        // BOTÓN INICIAR SESIÓN
        // ==========================================

        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());

        // ==========================================
        // CREAR CUENTA
        // ==========================================

        txtCrearCuenta.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    RegistroActivity.class
            );

            startActivity(intent);
        });

        // ==========================================
        // OLVIDÉ MI CONTRASEÑA
        // ==========================================

        txtOlvidePassword.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    RecuperarPasswordActivity.class
            );

            startActivity(intent);

        });
    }


    // =====================================================
    // INICIAR SESIÓN
    // =====================================================

    private void iniciarSesion() {

        String dni =
                txtDniLogin.getText().toString().trim();

        String password =
                txtPasswordLogin.getText().toString();

        // ==========================================
        // VALIDAR CAMPOS
        // ==========================================

        if (dni.isEmpty()) {

            txtDniLogin.setError(
                    "Ingrese su DNI"
            );

            txtDniLogin.requestFocus();
            return;
        }
        if(dni.length()!=8){

            txtDniLogin.setError(
                    "El DNI debe tener 8 números"
            );

            txtDniLogin.requestFocus();

            return;
        }

        if (password.isEmpty()) {

            txtPasswordLogin.setError(
                    "Ingrese su contraseña"
            );

            txtPasswordLogin.requestFocus();
            return;
        }

        // ==========================================
        // DESACTIVAR BOTÓN
        // ==========================================

        btnIniciarSesion.setEnabled(false);
        btnIniciarSesion.setText("INGRESANDO...");
        buscarCorreoPorDni(
                dni,
                password
        );

    }
    private void buscarCorreoPorDni(
            String dni,
            String password
    ){
        supabaseApi.verificarDni(
                "*",
                "eq." + dni.trim()
        )
                .enqueue(new Callback<List<Map<String,Object>>>() {


                    @Override
                    public void onResponse(
                            Call<List<Map<String,Object>>> call,
                            Response<List<Map<String,Object>>> response
                    ){
                        Log.d(
                                "SUPABASE_DNI",
                                "URL: " + call.request().url()
                        );
                        Log.d(
                                "SUPABASE_DNI",
                                "Código: " + response.code()
                        );


                        if(response.body()!=null){

                            Log.d(
                                    "SUPABASE_DNI",
                                    response.body().toString()
                            );
                        }


                        if(response.body()!=null
                                && !response.body().isEmpty()){


                            Map<String,Object> usuario =
                                    response.body().get(0);


                            Object correoObj = usuario.get("correo");


                            if(correoObj == null){

                                Toast.makeText(
                                        MainActivity.this,
                                        "Usuario sin correo registrado",
                                        Toast.LENGTH_LONG
                                ).show();


                                btnIniciarSesion.setEnabled(true);

                                btnIniciarSesion.setText(
                                        "INICIAR SESIÓN"
                                );


                                return;
                            }


                            String correo = correoObj.toString();


                            String uid = usuario.get("id").toString();






                            iniciarSesionSupabase(
                                    correo,
                                    password,
                                    uid
                            );


                        }else{


                            Log.e(
                                    "SUPABASE_DNI",
                                    "RESPUESTA VACIA: " + response.body()
                            );


                            Toast.makeText(
                                    MainActivity.this,
                                    "DNI no registrado",
                                    Toast.LENGTH_LONG
                            ).show();


                            btnIniciarSesion.setEnabled(true);

                            btnIniciarSesion.setText(
                                    "INICIAR SESIÓN"
                            );

                        }

                    }

                    @Override
                    public void onFailure(
                            Call<List<Map<String,Object>>> call,
                            Throwable t
                    ){

                        btnIniciarSesion.setEnabled(true);

                        btnIniciarSesion.setText(
                                "INICIAR SESIÓN"
                        );


                        Toast.makeText(
                                MainActivity.this,
                                "Error buscando DNI",
                                Toast.LENGTH_LONG
                        ).show();

                    }

                });

    }
    private void iniciarSesionSupabase(
            String correo,
            String password,
            String uid
    ){


        Map<String,String> datos =
                new HashMap<>();

        datos.put("email",correo);
        datos.put("password",password);


        supabaseApi.iniciarSesion(datos)
                .enqueue(new Callback<Map<String,Object>>() {


                    @Override
                    public void onResponse(
                            Call<Map<String,Object>> call,
                            Response<Map<String,Object>> response
                    ){
                        if(uid == null){

                            Toast.makeText(
                                    MainActivity.this,
                                    "Error obteniendo usuario",
                                    Toast.LENGTH_LONG
                            ).show();

                            btnIniciarSesion.setEnabled(true);
                            btnIniciarSesion.setText(
                                    "INICIAR SESIÓN"
                            );

                            return;
                        }

                        if(response.isSuccessful()
                                && response.body()!=null){


                            String token =
                                    String.valueOf(
                                            response.body()
                                                    .get("access_token")
                                    );


                            getSharedPreferences(
                                    "SafeVisionSession",
                                    MODE_PRIVATE
                            )
                                    .edit()
                                    .putString(
                                            "access_token",
                                            token
                                    )
                                    .putString(
                                            "correo",
                                            correo
                                    )
                                    .putString(
                                            "uid",
                                            uid
                                    )
                                    .apply();


                            Toast.makeText(
                                    MainActivity.this,
                                    "Inicio de sesión correcto",
                                    Toast.LENGTH_SHORT
                            ).show();


// IR A LA PANTALLA DE INICIO

                            Intent intent = new Intent(
                                    MainActivity.this,
                                    InicioActivity.class
                            );


// BORRAR LOGIN DEL HISTORIAL
// PARA QUE NO REGRESE CON EL BOTÓN ATRÁS

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );


                            startActivity(intent);

                            finish();

                        }else{


                            Toast.makeText(
                                    MainActivity.this,
                                    "DNI o contraseña incorrectos",
                                    Toast.LENGTH_LONG
                            ).show();


                            btnIniciarSesion.setEnabled(true);

                            btnIniciarSesion.setText(
                                    "INICIAR SESIÓN"
                            );

                        }


                    }


                    @Override
                    public void onFailure(
                            Call<Map<String,Object>> call,
                            Throwable t
                    ){

                        btnIniciarSesion.setEnabled(true);

                        btnIniciarSesion.setText(
                                "INICIAR SESIÓN"
                        );


                        Toast.makeText(
                                MainActivity.this,
                                "Error conexión",
                                Toast.LENGTH_LONG
                        ).show();

                    }


                });


    }
    // =====================================================
// PERMISO DE NOTIFICACIONES
// =====================================================

    private void pedirPermisoNotificaciones() {

        if (
                android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                    )
                            != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        200
                );
            }
        }
    }


// =====================================================
// INICIAR SERVICIO DE ALERTAS
// =====================================================

    private void iniciarServicioAlertas() {

        Intent intent =
                new Intent(
                        this,
                        AlertaNotificationService.class
                );


        if (
                android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.O
        ) {

            startForegroundService(intent);

        } else {

            startService(intent);
        }
    }
}