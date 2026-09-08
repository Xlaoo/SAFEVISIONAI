package com.safevision.ai;

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

public class MainActivity extends AppCompatActivity {


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
                        "eq." + dni.trim(),
                        SupabaseConfig.API_KEY,
                        "Bearer " + SupabaseConfig.API_KEY
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

                                return;
                            }


                            String correo = correoObj.toString();


                            iniciarSesionSupabase(
                                    correo,
                                    password
                            );


                        }else{


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
            String password
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
                                    .apply();


                            Toast.makeText(
                                    MainActivity.this,
                                    "Inicio correcto",
                                    Toast.LENGTH_LONG
                            ).show();


                        }else{


                            Toast.makeText(
                                    MainActivity.this,
                                    "DNI o contraseña incorrectos",
                                    Toast.LENGTH_LONG
                            ).show();

                        }


                    }


                    @Override
                    public void onFailure(
                            Call<Map<String,Object>> call,
                            Throwable t
                    ){

                        Toast.makeText(
                                MainActivity.this,
                                "Error conexión",
                                Toast.LENGTH_LONG
                        ).show();

                    }


                });


    }
}